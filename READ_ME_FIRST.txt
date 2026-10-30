AGENT 2 DELIVERY - copy over the project root (merge folders, overwrite when asked).

Overwrites: shared/.../domain/coach/catalog/SubstanceCatalog.kt (Agent 1's empty stub), README.md, docs/DEVELOPMENT_STATUS.md, docs/PROJECT_CONTEXT.md
Adds:       shared/.../domain/coach/catalog/Substance{Nicotine,Drinks,Food,Drugs}.kt
            shared/src/commonTest/.../domain/coach/catalog/SubstanceCatalogTest.kt

WARNING: if Agent 3 or you already changed README.md / docs/*.md, do NOT overwrite them blindly - merge the "Round 2 - Agent 2" sections by hand.
Kotlin NOT compiled yet. Run: gradlew.bat :shared:testAndroidHostTest   then   gradlew.bat :androidApp:assembleDebug
