import os, math, glob
import pathlib
RES=str(pathlib.Path(__file__).resolve().parent.parent / 'androidApp/src/main/res')
os.makedirs(RES+'/drawable',exist_ok=True); os.makedirs(RES+'/layout',exist_ok=True)
for f in ['drawable/widget_bg_round.xml','drawable/widget_btn_plus.xml','drawable/widget_btn_minus.xml','drawable/widget_progress.xml','drawable/widget_dot.xml','drawable/ic_w_bell.xml']:
    if os.path.exists(f'{RES}/{f}'): os.remove(f'{RES}/{f}')
for f in glob.glob(RES+'/layout/widget_*.xml'): os.remove(f)
NS='xmlns:android="http://schemas.android.com/apk/res/android"'
def w(path,txt):
    open(f'{RES}/{path}','w',encoding='utf-8').write(txt)

# ---------- shapes (white; tinted at runtime) ----------
def shape(name,kind,radius=None,stroke=False,size=None):
    body=''
    if size: body+=f'    <size android:width="{size}dp" android:height="{size}dp" />\n'
    body+= '    <stroke android:width="1dp" android:color="#FFFFFFFF" />\n' if stroke else '    <solid android:color="#FFFFFFFF" />\n'
    if radius is not None: body+=f'    <corners android:radius="{radius}dp" />\n'
    w(f'drawable/{name}.xml',f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Plain white shape: the renderer tints it (setColorFilter) to the card colors. -->\n<shape {NS} android:shape="{kind}">\n{body}</shape>\n')
shape('widget_card_26','rectangle',26); shape('widget_card_26_border','rectangle',26,True)
shape('widget_card_22','rectangle',22); shape('widget_card_22_border','rectangle',22,True)
shape('widget_badge_bg','rectangle',12)
shape('widget_circle','oval'); shape('widget_circle_border','oval',stroke=True)
shape('widget_pill','rectangle',100)
shape('widget_square','rectangle',6,size=22); shape('widget_square_border','rectangle',6,True,size=22)
# Today's frame in the week strip: a real 2 dp outline in its own colour (white on the dark card, navy on the light card).
# It is NOT tinted at runtime (a colour filter on a stroke-only shape can paint it filled).
for _n,_c in [('widget_square_today_on_dark','#FFFFFFFF'),('widget_square_today_on_light','#FF1D1A39')]:
    w(f'drawable/{_n}.xml',f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Outline only, never filled: marks the current day in the good-habit week strip. -->\n<shape {NS} android:shape="rectangle">\n    <size android:width="22dp" android:height="22dp" />\n    <stroke android:width="2dp" android:color="{_c}" />\n    <corners android:radius="6dp" />\n</shape>\n')
w('drawable/widget_pill_clip.xml',f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Progress fill: ImageView.setImageLevel(0..10000) clips it; tinted by the renderer. -->\n<clip {NS} android:drawable="@drawable/widget_pill" android:clipOrientation="horizontal" android:gravity="start|fill_vertical" />\n')

# ---------- icons: same 24x24 strokes as NixIcons.kt ----------
def circle(cx,cy,r): return f'M{cx-r},{cy} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0'
sun=circle(12,12,4)
for i in range(8):
    a=i*math.pi/4
    sun+=f' M{12+7*math.cos(a):.2f},{12+7*math.sin(a):.2f} L{12+9.4*math.cos(a):.2f},{12+9.4*math.sin(a):.2f}'
icons={
 'flame':'M12,3 C12,3 18,8 18,14 C18,17.6 15.5,20.5 12,20.5 C8.5,20.5 6,17.6 6,14 C6,11.5 7.6,10 8.5,8.5 C9.5,10.5 10.5,11 11,11 C11.6,8.6 12,6 12,3',
 'shield':'M12,3 L19.5,6 L19.5,11.5 C19.5,16 16.5,19.5 12,21 C7.5,19.5 4.5,16 4.5,11.5 L4.5,6 Z',
 'moon':'M19.5,14.5 C18.5,15 17.3,15.3 16,15.3 C11.8,15.3 8.7,12.2 8.7,8 C8.7,6.7 9,5.5 9.5,4.5 C6.4,5.5 4.2,8.4 4.2,11.8 C4.2,16.1 7.7,19.6 12,19.6 C15.4,19.6 18.4,17.4 19.5,14.5 Z',
 'sparkle':'M12,3 L14,10 L21,12 L14,14 L12,21 L10,14 L3,12 L10,10 Z',
 'leaf':'M5,19 C5,10 10,5 19,5 C19,14 14,19 5,19 Z M5,19 L13,11',
 'sun':sun,
 'check':circle(12,12,9)+' M8,12.5 L11,15.5 L16,9.5',
 'clock':circle(12,12,9)+' M12,7 L12,12 L15.5,14',
 'chevron':'M9,5 L16,12 L9,19',
 'plus':'M12,5 L12,19 M5,12 L19,12',
 'minus':'M5,12 L19,12',
 'tick':'M5,12.5 L10,17.5 L19,7',
}
for n,p in icons.items():
    w(f'drawable/ic_w_{n}.xml',f'<?xml version="1.0" encoding="utf-8"?>\n<vector {NS} android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n    <path android:pathData="{p}" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" />\n</vector>\n')

# ---------- layout helpers ----------
def tv(id,size=12,style='',extra='',w_='wrap_content',h='wrap_content',lines=1,color='#FFFFFFFF',text='',margin='',ell=True):
    fam=''
    if 'medium' in style: fam='\n        android:fontFamily="sans-serif-medium"'
    elif 'bold' in style: fam='\n        android:textStyle="bold"'
    caps='\n        android:textAllCaps="true"\n        android:letterSpacing="0.12"' if 'caps' in style else ''
    tnum='\n        android:fontFeatureSettings="tnum"' if 'tnum' in style else ''
    ell='\n        android:ellipsize="end"' if ell else ''
    return f'''<TextView
        android:id="@+id/{id}"
        android:layout_width="{w_}"
        android:layout_height="{h}"{margin}
        android:maxLines="{lines}"{ell}
        android:includeFontPadding="false"
        android:text="{text}"
        android:textColor="{color}"
        android:textSize="{size}sp"{fam}{caps}{tnum}{extra} />'''
def img(id,src,w_,h,extra='',margin=''):
    return f'''<ImageView
        android:id="@+id/{id}"
        android:layout_width="{w_}"
        android:layout_height="{h}"{margin}
        android:importantForAccessibility="no"
        android:src="@drawable/{src}"{extra} />'''
def fl(id,w_,h,children,extra='',margin=''):
    idp=f'\n        android:id="@+id/{id}"' if id else ''
    return f'''<FrameLayout{idp}
        android:layout_width="{w_}"
        android:layout_height="{h}"{margin}{extra}>
{children}
    </FrameLayout>'''
def lin(id,orient,w_,h,children,extra='',margin='',gravity=''):
    idp=f'\n        android:id="@+id/{id}"' if id else ''
    g=f'\n        android:gravity="{gravity}"' if gravity else ''
    return f'''<LinearLayout{idp}
        android:layout_width="{w_}"
        android:layout_height="{h}"
        android:orientation="{orient}"{g}{margin}{extra}>
{children}
    </LinearLayout>'''
def m(**k):
    return ''.join(f'\n        android:layout_{a}="{v}dp"' if a!='gravity' else f'\n        android:layout_gravity="{v}"' for a,v in k.items())
def badge(size,icon,end=12):
    return fl('w_badge','%ddp'%size,'%ddp'%size,
        img('w_badge_bg','widget_badge_bg','match_parent','match_parent',' android:scaleType="fitXY"')+'\n'+
        img('w_badge_icon','ic_w_flame','%ddp'%icon,'%ddp'%icon,'',m(gravity='center')),
        margin=m(marginEnd=end))
def root(card,content):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATED by the widget layout generator; see docs. Card look mirrors the in-app cards. -->
<FrameLayout {NS}
    android:id="@+id/w_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    {img('w_bg','widget_card_%d'%card,'match_parent','match_parent',' android:scaleType="fitXY"')}

    {img('w_border','widget_card_%d_border'%card,'match_parent','match_parent',' android:scaleType="fitXY"')}

    {content}
</FrameLayout>
'''
def content(inner,gravity='center_vertical',orient='vertical'):
    return lin('w_content',orient,'match_parent','match_parent',inner,gravity=gravity,extra='\n        android:padding="16dp"')

def ghost(id,text):
    # Invisible, zero-height text with the Chronometer's font, so its WIDTH equals the printed glyphs. The renderer sets
    # its text size to the counter size. Used to place the HRS / MIN / SEC labels exactly under their digit pairs.
    return f'''<TextView
        android:id="@+id/{id}"
        android:layout_width="wrap_content"
        android:layout_height="0dp"
        android:visibility="invisible"
        android:maxLines="1"
        android:includeFontPadding="false"
        android:text="{text}"
        android:textSize="30sp"
        android:textStyle="bold"
        android:letterSpacing="0.1"
        android:fontFeatureSettings="tnum" />'''

# Unit labels under the counters (DAYS / HRS / MIN / SEC): one size for the quit card and the event card.
COUNTER_LBL_SP=6.5

def counter(mt=0,labels=True,days_gap=12,lbl_size=COUNTER_LBL_SP):
    # days_gap: space between the DAYS block and the clock (bad habit 12, event 6).
    # The Chronometer ALWAYS prints "H:MM:SS" (the renderer prefixes "0:" inside the first hour of a day, so the hours
    # field never disappears). Hours are 1 or 2 digits. Everything under the clock is RIGHT-aligned to the clock, so
    # MIN and SEC sit under their fixed two-digit pairs; the HRS cell is as wide as the hours digits (invisible ghost
    # text with the clock's font, text set by the renderer) or as wide as the label if that is wider, and the label is
    # centered in it. letterSpacing on the Chronometer is matched by the ghosts; WidgetLayoutPlanner.CLOCK_EM accounts for it.
    mtm=m(marginTop=mt) if mt else ''
    days=lin('w_days_block','vertical','wrap_content','wrap_content',
        tv('w_days',30,'bold tnum',text='12')+'\n'+tv('w_days_label',lbl_size,'medium caps',text='days',margin=m(marginTop=2)),margin=m(marginEnd=days_gap),gravity='center_horizontal')
    def cell(ghost_id,lbl_id,t,gh_text='00'):
        return fl(None,'wrap_content','wrap_content',
            ghost(ghost_id,gh_text)+'\n'+tv(lbl_id,lbl_size,'medium caps',text=t,ell=False,extra='\n        android:layout_gravity="center"'))
    lab=lin('w_clock_labels','horizontal','wrap_content','wrap_content',
        cell('w_gh_h','w_lbl_h','hrs','0')+'\n'+
        ghost('w_sp_hm',':')+'\n'+cell('w_gh_m','w_lbl_m','min')+'\n'+ghost('w_sp_ms',':')+'\n'+cell('w_gh_s','w_lbl_s','sec'),
        margin=m(marginTop=2,gravity='end'))
    clock=lin('w_clock_block','vertical','wrap_content','wrap_content',
        f'''<Chronometer
        android:id="@+id/w_clock"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:letterSpacing="0.1"
        android:textColor="#FFFFFFFF"
        android:textSize="30sp"
        android:textStyle="bold"
        android:fontFeatureSettings="tnum" />'''+'\n'+lab)
    return lin('w_counter','horizontal','wrap_content','wrap_content',days+'\n'+clock,margin=mtm,gravity='bottom')

def circle_btn(id,size,icon,border,ms=0):
    kids=img(id+'_bg','widget_circle','match_parent','match_parent',' android:scaleType="fitXY"')+'\n'
    if border: kids+=img(id+'_bd','widget_circle_border','match_parent','match_parent',' android:scaleType="fitXY"')+'\n'
    kids+=img(id+'_icon','ic_w_'+icon,'%ddp'%int(size*0.5),'%ddp'%int(size*0.5),'',m(gravity='center'))
    return fl(id,'%ddp'%size,'%ddp'%size,kids,'\n        android:clickable="true"\n        android:focusable="true"',margin=(m(marginStart=ms) if ms else ''))

def progress(mt=12,h=6):
    return fl('w_prog','match_parent','%ddp'%h,
        img('w_prog_track','widget_pill','match_parent','match_parent',' android:scaleType="fitXY"')+'\n'+
        img('w_prog_fill','widget_pill_clip','match_parent','match_parent',' android:scaleType="fitXY"'),margin=m(marginTop=mt))

def week(sq=22,letter_sp=11,mt=12,sq_mt=4):
    cols=[]
    for i in range(7):
        cols.append(lin(None,'vertical','0dp','wrap_content',
            tv(f'w_wk_letter{i}',letter_sp,'',text='M',extra='\n        android:gravity="center"',w_='match_parent')+'\n'+
            fl(f'w_wk{i}','match_parent',f'{sq}dp',
               img(f'w_wk_sq{i}','widget_square','match_parent','match_parent',' android:scaleType="fitCenter"')+'\n'+
               img(f'w_wk_bd{i}','widget_square_border','match_parent','match_parent',' android:scaleType="fitCenter"'),margin=m(marginTop=sq_mt)),
            extra='\n        android:layout_weight="1"',margin=(m(marginStart=3) if i else '')))
    return lin('w_week','horizontal','match_parent','wrap_content','\n'.join(cols),margin=m(marginTop=mt))

def streak(mt=12,sp=12,flame=16):
    return lin('w_streak_row','horizontal','match_parent','wrap_content',
        img('w_flame','ic_w_flame',f'{flame}dp',f'{flame}dp')+'\n'+
        tv('w_streak_text',sp,'medium',text='3 day streak',margin=m(marginStart=6),w_='0dp',extra='\n        android:layout_weight="1"')+'\n'+
        tv('w_total_text',sp,'',text='24 times in total',margin=m(marginStart=6)),
        margin=m(marginTop=mt),gravity='center_vertical')

def textcol(sub=True,name_lines=1,name_size=17):
    inner=tv('w_name',name_size,'medium',lines=name_lines,w_='match_parent')
    if sub: inner+='\n'+tv('w_sub',12,'',w_='match_parent',margin=m(marginTop=1))
    return lin(None,'vertical','0dp','wrap_content',inner,extra='\n        android:layout_weight="1"')

def wr(name,card,inner,gravity='center_vertical',orient='vertical'):
    w(f'layout/{name}.xml',root(card,content(inner,gravity,orient)))

CENTER_TXT='\n        android:gravity="center"'
WEIGHT='\n        android:layout_weight="1"'

# Habit-name sizes (sp). WidgetLayoutPlanner estimates name wrapping with the same numbers; keep them in sync.
# ===== BAD (quit): everything centered, no category line, no stage dots =====
def bad_header(badge_size,icon,name_size,lines,end=12):
    return lin(None,'horizontal','wrap_content','wrap_content',
        badge(badge_size,icon,end)+'\n'+tv('w_name',name_size,'medium',lines=lines,w_='wrap_content',extra=CENTER_TXT),
        gravity='center_vertical')
wr('widget_bad_full',26,
   bad_header(40,22,20,2)+'\n'+
   tv('w_eyebrow',11,'medium caps',text='clear for',margin=m(marginTop=10))+'\n'+
   counter(mt=6)+'\n'+tv('w_saved',13,'medium',text='EGP 340.00 saved',margin=m(marginTop=10),extra='\n        android:visibility="gone"')+'\n'+
   tv('w_longest',12,'',text='Longest streak 12d',margin=m(marginTop=10)),
   gravity='center')
wr('widget_bad_row',26,
   badge(36,20)+'\n'+tv('w_name',18,'medium',w_='0dp',lines=2,extra=WEIGHT+CENTER_TXT)+'\n'+counter(0),orient='horizontal')
wr('widget_bad_slim',26,
   bad_header(26,15,17,1,8)+'\n'+counter(mt=4),gravity='center')

# ===== GOOD =====
# The planner picks the layout: full (controls beside the name), full_narrow (controls below the name; also used for
# short narrow widgets), row (4x2-like, with the streak row under it) or slim (name + buttons only).
wr('widget_good_full',22,
   lin(None,'horizontal','match_parent','wrap_content',
       badge(40,22)+'\n'+textcol(name_lines=3,name_size=20)+'\n'+circle_btn('w_minus',38,'minus',True,8)+'\n'+
       circle_btn('w_plus',44,'plus',False,8),gravity='center_vertical')+'\n'+
   progress(12,6)+'\n'+week()+'\n'+streak())
wr('widget_good_full_narrow',22,
   lin(None,'horizontal','match_parent','wrap_content',badge(32,18)+'\n'+textcol(name_lines=3,name_size=17),gravity='center_vertical')+'\n'+
   lin(None,'horizontal','match_parent','wrap_content',
       tv('w_count',24,'bold tnum',text='3/8',w_='0dp',extra=WEIGHT)+'\n'+
       circle_btn('w_minus',32,'minus',True,6)+'\n'+
       circle_btn('w_plus',40,'plus',False,6),margin=m(marginTop=8),gravity='center_vertical')+'\n'+
   progress(10,6)+'\n'+week()+'\n'+streak())
wr('widget_good_row',22,
   lin(None,'horizontal','match_parent','wrap_content',
       badge(36,20)+'\n'+
       lin(None,'vertical','0dp','wrap_content',
           tv('w_name',18,'medium',w_='match_parent',lines=3)+'\n'+tv('w_sub',12,'',w_='match_parent',margin=m(marginTop=1))+'\n'+progress(6,4),
           extra=WEIGHT)+'\n'+
       circle_btn('w_minus',34,'minus',True,8)+'\n'+circle_btn('w_plus',40,'plus',False,8),gravity='center_vertical')+'\n'+
   streak(6))
wr('widget_good_slim',22,
   lin(None,'vertical','0dp','wrap_content',
       tv('w_name',17,'medium',w_='match_parent',lines=3)+'\n'+tv('w_sub',12,'',w_='match_parent',margin=m(marginTop=1)),
       extra=WEIGHT)+'\n'+
   circle_btn('w_minus',32,'minus',True,6)+'\n'+circle_btn('w_plus',38,'plus',False,6),orient='horizontal')

# ===== EVENT =====
# Everything is centered. The category line (11 -> 8.5 sp) is smaller than on the other cards; the unit labels under
# the counters (6.5 sp) are the same size as on the quit card. No reminder count is shown.
EV_CAT_SP=8.5
EV_LBL_SP=COUNTER_LBL_SP   # same size as the labels under the quit counter
EVENT_DAYS_GAP=6   # the day counter sits closer to the clock than on the quit card
# header: category, title and date, centered over the whole width (no reminder pill: reminders are not shown on the widget).
hdr_col=lin('w_hdr','vertical','match_parent','wrap_content',
    tv('w_eyebrow_cat',EV_CAT_SP,'medium caps',w_='match_parent',text='travel',extra=CENTER_TXT)+'\n'+
    tv('w_name',20,'medium',w_='match_parent',margin=m(marginTop=3),extra=CENTER_TXT)+'\n'+
    tv('w_date',12,'',w_='match_parent',margin=m(marginTop=1),extra=CENTER_TXT))
wr('widget_event_full',22,
   hdr_col+'\n'+
   tv('w_eyebrow',11,'medium caps',text='starts in',margin=m(marginTop=10))+'\n'+counter(mt=6,days_gap=EVENT_DAYS_GAP,lbl_size=EV_LBL_SP),
   gravity='center')
wr('widget_event_row',22,
   lin(None,'vertical','0dp','wrap_content',
       tv('w_eyebrow_cat',EV_CAT_SP,'medium caps',w_='match_parent',text='travel',extra=CENTER_TXT)+'\n'+
       tv('w_name',18,'medium',w_='match_parent',lines=1,extra=CENTER_TXT)+'\n'+
       tv('w_date',12,'',w_='match_parent',extra=CENTER_TXT),extra=WEIGHT)+'\n'+counter(0,days_gap=EVENT_DAYS_GAP,lbl_size=EV_LBL_SP),orient='horizontal')
wr('widget_event_slim',22,
   tv('w_name',17,'medium',w_='match_parent',extra=CENTER_TXT)+'\n'+counter(mt=4,days_gap=EVENT_DAYS_GAP,lbl_size=EV_LBL_SP),
   gravity='center')

# ===== message (empty / error / tap-to-choose) =====
w('layout/widget_message.xml',root(22,lin('w_content','vertical','match_parent','match_parent',
   tv('w_name',15,'medium',w_='match_parent',lines=2,text='NixUra')+'\n'+tv('w_caption',12,'',w_='match_parent',lines=2,margin=m(marginTop=3)),
   gravity='center_vertical',extra='\n        android:padding="14dp"')))
print(len(glob.glob(RES+'/layout/widget_*.xml')),'layouts')
