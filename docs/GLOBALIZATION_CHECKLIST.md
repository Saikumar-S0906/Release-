# Globalization / Localization checklist

Use after adding English strings (e.g. Create Extra / C-flows).

## Commands

```powershell
cd scripts
# Sync all locale files (EN fallback for new keys)
python generate_full_translations.py
# Translate missing keys via Google Translate + cache
python generate_full_translations.py --translate
python generate_plurals.py --translate
python fix_string_escapes.py
```

## Automated

```powershell
.\gradlew :app:testDebugUnitTest
.\gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.pixijoy.ai.LocaleResourcesTest"
.\gradlew :app:lintDebug
```

`LocaleResourcesTest` checks: core strings non-blank across 24 locales, Arabic RTL, Hindi `nav_home` translated, Hindi plurals.

## Manual (also in UI_RESPONSIVE_QA + Flow 29)

1. Force RTL (Developer options) — nav / back usable
2. Language: Hindi, Arabic, Spanish — Create Tools + Generate labels not English-only where translated
3. Font scale 1.3 — Tools chips / CTAs not clipped
4. Manifest `supportsRtl=true`
5. No hard-coded user-facing English in new C-tool hosts (use `stringResource`)

## C-flow strings to keep localized

`create_hub_tools`, `create_extra_*`, `flow27_remix`, `create_extra_credits_left`
