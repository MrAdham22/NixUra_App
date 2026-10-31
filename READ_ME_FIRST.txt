AGENT 5 DELIVERY v3 - widgets look like the in-app cards + "Add to Home screen" + final docs (Agents 1-5 merged in README/docs).

1) Copy this folder's contents over your project root (merge folders, overwrite when asked).
2) DELETE these old files (v1/v2 layouts/drawables, now unused):
     androidApp/src/main/res/layout/widget_counter_compact.xml
     androidApp/src/main/res/layout/widget_counter_wide.xml
     androidApp/src/main/res/layout/widget_good_compact.xml
     androidApp/src/main/res/layout/widget_good_wide.xml
     androidApp/src/main/res/drawable/widget_bg_round.xml
     androidApp/src/main/res/drawable/widget_btn_plus.xml
     androidApp/src/main/res/drawable/widget_btn_minus.xml
     androidApp/src/main/res/drawable/widget_progress.xml
   (Leaving them does no harm, but the old layouts are not used any more.)
3) Strings.kt: nothing to do. If you pasted my widget block into it earlier, it is simply unused now (you may delete it).
4) THREE SCREEN FILES are included only so you can copy-paste. If you have since changed or merged other agents' versions of them
   (Agent 4 edits BadHabitDetailScreen), DON'T overwrite - add just these lines instead (see PATCH_SCREENS.txt):
     BadHabitDetailScreen.kt, GoodHabitDetailScreen.kt, EventFormScreen.kt
5) Build: gradlew.bat :shared:testAndroidHostTest   then   gradlew.bat :androidApp:assembleDebug
   Gradle sync needs nothing new. Send me any compiler error text.
6) After installing: remove and re-add the widgets once (the layouts changed).
NOT compiled with Gradle / not run on a device by me.
