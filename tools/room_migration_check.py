import re,glob,sqlite3,sys
ents={}
for f in glob.glob('shared/src/commonMain/kotlin/**/entity/*.kt',recursive=True):
    t=open(f).read().replace('\r','')
    for m in re.finditer(r'@Entity\((.*?)\)\s*data class (\w+)\((.*?)\n\)',t,re.S):
        args,cls,body=m.groups()
        table=re.search(r'tableName\s*=\s*"(\w+)"',args).group(1)
        cols=[]
        for line in body.split('\n'):
            line=line.strip().rstrip(',')
            mm=re.search(r'val (\w+):\s*([\w?]+)',line)
            if not mm: continue
            prop,typ=mm.groups()
            nm=re.search(r'@ColumnInfo\(name = "(\w+)"',line)
            name=nm.group(1) if nm else prop
            sql={'Long':'INTEGER','Int':'INTEGER','Boolean':'INTEGER','String':'TEXT'}[typ.rstrip('?')]
            cols.append((name,sql,0 if typ.endswith('?') else 1,'@PrimaryKey' in line))
        ents[table]=cols
db=sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
db.executescript('''
CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, created_at INTEGER NOT NULL, archived_at INTEGER);
CREATE TABLE habit_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habit_id INTEGER NOT NULL, event_type TEXT NOT NULL, occurred_at INTEGER NOT NULL, FOREIGN KEY(habit_id) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE RESTRICT);
CREATE INDEX index_habit_events_habit_id ON habit_events(habit_id);
INSERT INTO habits(name,created_at) VALUES('Smoking',1000);
INSERT INTO habit_events(habit_id,event_type,occurred_at) VALUES(1,'TRACKING_START',1000);
''')
# Runs every statement of MIGRATION_1_2 then MIGRATION_2_3. Rows for the v2 tables are added right after the v1->v2 step,
# so the v2->v3 backfill is exercised on real data.
V2_ROWS=[
 "INSERT INTO good_habits(name,daily_target,created_at) VALUES('Walk',1,2000)",
 "INSERT INTO good_habit_logs(good_habit_id,day_index,count) VALUES(1,10,1)",
 "INSERT INTO good_habit_logs(good_habit_id,day_index,count) VALUES(99,11,1)",
 "INSERT INTO events(title,category,target_at,reminders,created_at) VALUES('Trip','OTHER',9000,'',3000)",
]
src=open('shared/src/commonMain/kotlin/com/example/reclaimapp/data/local/Migrations.kt').read()
stmts=[];i=0
while True:
    i=src.find('connection.execSQL(',i)
    if i<0: break
    j=i+len('connection.execSQL(');d=1;k=j
    while d:
        c=src[k]
        if c=='"':
            k+=1
            while src[k]!='"':
                if src[k]=='\\':k+=1
                k+=1
        elif c=='(':d+=1
        elif c==')':d-=1
        k+=1
    inner=src[j:k-1]
    stmts.append(''.join(re.findall(r'"((?:[^"\\]|\\.)*)"',inner)))
    i=k
print(len(stmts),"migration statements")
v3_start=next(i for i,s in enumerate(stmts) if 'sync_id' in s)
v4_start=next(i for i,s in enumerate(stmts) if 'coach_conversations' in s)
V3_ROWS=[
 "INSERT INTO coach_messages(role,text,created_at) VALUES('COACH','Hi',100)",
 "INSERT INTO coach_messages(role,text,created_at) VALUES('USER','"+"x"*80+"',200)",
 "INSERT INTO coach_messages(role,text,created_at) VALUES('COACH','Ok',300)",
]
for i,s in enumerate(stmts):
    if i==v3_start:
        for row in V2_ROWS: db.execute(row)
    if i==v4_start:
        for row in V3_ROWS: db.execute(row)
    db.execute(s)
ok=True
conv=db.execute('select id,length(title),created_at,updated_at from coach_conversations').fetchall()
if conv!=[(1,60,100,300)]: print('V4 CONVERSATION WRONG',conv); ok=False
if db.execute('select count(*) from coach_messages where conversation_id<>1').fetchone()[0]: print('V4 MESSAGES NOT MOVED'); ok=False
print('v3->v4 existing chat checked')
empty=sqlite3.connect(':memory:')
empty.execute('CREATE TABLE coach_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, created_at INTEGER NOT NULL)')
for s in stmts[v4_start:]: empty.execute(s)
if empty.execute('select count(*) from coach_conversations').fetchone()[0]: print('V4 INVENTED A CONVERSATION'); ok=False
print('v3->v4 with no chat checked')
for table in ['habits','habit_events','good_habits','good_habit_logs','events']:
    blank=db.execute(f"select count(*) from {table} where sync_id='' or updated_at=0 or dirty<>1").fetchone()[0]
    if blank: print('BACKFILL PROBLEM',table,blank); ok=False
if db.execute("select count(*) from good_habit_logs g join good_habits h on g.good_habit_id=h.id where g.sync_id<>h.sync_id||':'||g.day_index").fetchone()[0]:
    print('LOG SYNC IDS WRONG'); ok=False
print('v2->v3 backfill checked')
for table,cols in ents.items():
    got={r[1]:(r[2],r[3],r[5]>0) for r in db.execute(f'PRAGMA table_info({table})')}
    exp={c[0]:(c[1],c[2],c[3]) for c in cols}
    if set(got)!=set(exp): print('COLUMN SET MISMATCH',table,set(got)^set(exp)); ok=False; continue
    for n,e in exp.items():
        if got[n]!=e: print('MISMATCH',table,n,'expected',e,'got',got[n]); ok=False
print('entities checked:',sorted(ents))
print('tables not covered by an entity:',sorted({r[0] for r in db.execute("select name from sqlite_master where type='table'")}-set(ents)-{'sqlite_sequence'}))
print('indexes:',[r[0] for r in db.execute("select name from sqlite_master where type='index' and name not like 'sqlite_%'")])
print('v1 data kept:',db.execute('select id,name,color_argb from habits').fetchall(),db.execute('select count(*) from habit_events').fetchone())
n=0
for f in glob.glob('shared/src/commonMain/kotlin/**/dao/*.kt',recursive=True):
    for raw in re.findall(r'@Query\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+),?\s*\)',open(f,encoding='utf-8').read()):
        q=''.join(re.findall(r'"((?:[^"\\]|\\.)*)"',raw))
        n+=1
        try: db.execute('EXPLAIN '+re.sub(r':\w+','1',q))
        except Exception as e: print('SQL ERROR',q,e); ok=False
print(n,'DAO queries validated')
for tbl in ['habit_events','habits','good_habit_logs','good_habits','events','coach_messages','coach_conversations']:
    db.execute(f'DELETE FROM {tbl}')
print('clearAll order OK with foreign keys ON')
# unique index behaviour for REPLACE upsert
db.execute("INSERT OR REPLACE INTO good_habit_logs(good_habit_id,day_index,count,sync_id) VALUES(1,10,1,'g:10')")
db.execute("INSERT OR REPLACE INTO good_habit_logs(good_habit_id,day_index,count,sync_id) VALUES(1,10,3,'g:10')")
print('upsert rows for same day:',db.execute('select count(*),max(count) from good_habit_logs').fetchone())
print('ALL OK' if ok else 'PROBLEMS')
