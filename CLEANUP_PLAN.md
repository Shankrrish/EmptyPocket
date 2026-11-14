# EmptyPocket Cleanup Plan

Branch: `feature/latest`. Status as of 2026-11-14.

> [!WARNING]
> Nothing below has been compiled yet. Gradle cannot download plugins on this machine
> (`PKIX path building failed` / `SEC_E_UNTRUSTED_ROOT`). **Step 0 must happen first.**

## Step 0 - Unblock the build (blocker)
- [ ] Build in Android Studio, or from a normal terminal / different network:
      `C:\Gradle\gradle-9.9.1\bin\gradle.bat assembleDebug`
- [ ] Fix any compile errors from steps 1-3 (and the SMS, crash-log, auto-backup, month-picker work).
- [ ] Commit the generated `app/schemas/` (Room schema history).
- [ ] Install on a device and smoke test: month picker, SMS scan, crash log card, auto backup card.

## Done (uncompiled)
- [x] 1. Renamed Application class to `FinFlowApplication` (no clash with the `FinFlowApp` composable).
- [x] 2. Database safety: removed destructive fallback for v5+, `exportSchema = true` plus `room.schemaLocation`,
      demo seed only when DB is empty, restore now keeps `fontStyle`.
- [x] 3a. One month control: `MonthNavigator` + `onMonthSelected` callback + `vm.selectMonth`.
- [x] Month/year picker dialog on tapping the month label.
- [x] SMS scan: background thread, 180 days, error reporting.
- [x] In-app crash logs (Privacy, Lock & Backups).
- [x] Automatic monthly backup to a user-picked folder (12 kept).
- [x] Import file and 90 smart rules generated (outside repo, in the conversation scratch folder).

## Remaining
- [ ] 3b. **Date helper**: replace the 14 `SimpleDateFormat` and 26 `Calendar.getInstance` uses with one `java.time` helper in `DateUtils`. Do after Step 0 so each change compiles.
- [ ] 4. **Split large files** (one at a time, compile after each):
  - `DashboardScreen.kt` (~1,250 lines) into card composables.
  - `CommonComponents.kt` (~1,000 lines) into charts / month controls / badges.
  - `AddEditTransactionSheet.kt` (~800), `PortfolioDialogs.kt` (~770, 24 dialogs), `FinanceViewModel.kt` (~690).
- [ ] 5. **Remove AI Studio leftovers and unused assets**:
  - `.env.example`, `metadata.json`, `secrets` and `google.services` plugins, Gemini key handling, commented Firebase deps.
  - Unreferenced images: `img_empty_finance.jpg`, `moneytracker_launcher_icon_*.jpg`, `public/assets/aistudio/*.png`.
  - Template tests `Example*`; trim `S23AmoledThemeTest` (664 lines).
- [ ] 5b. **Unify naming**: ExpenseTracker / EmptyPocket / FinFlow / MoneyTracker / `MyApplication` theme / `com.aistudio.finflow.k7m2xp` application id.
      Changing the application id installs as a new app, so only do this before relying on the app.
- [ ] 5c. **Release signing**: `my-upload-key.jks` and `debug.keystore` are missing; set up a real keystore.
- [ ] 6. **Make SMS parsing data-driven and tested**:
  - Sample-message unit tests.
  - Fix `"cc "` false positive; handle refunds and transfers; word-boundary keyword matching.
  - Drop the hardcoded merchant map in favour of rules.
- [ ] 7. **Dedupe parallel mappings**: `categoryIcon()` vs `resolveCategoryEmoji()`; 67 hardcoded `Color(0x..)` to theme tokens;
      generic entity JSON codec in `BackupManager` instead of field-by-field decoding.
- [ ] 8. **Strings**: move hardcoded UI text to `strings.xml`.
- [ ] 9. **Review overlapping screens** (`PlanningScreen`, `CashFlowAndGoalsContent`, `RecurringManagerContent`): confirm which are reachable.

## Data and product follow-ups
- [ ] Set real opening balances per account (imported accounts start at 0).
- [ ] Imported transfers show as expense + income pairs; mark them as TRANSFER if wanted.
- [ ] Review the 90 generated rules (e.g. `amazon` maps to Rewards); rules are category-only, not income/expense-aware.
- [ ] Optional passphrase encryption for auto backups.
- [ ] Check which DB version is on the phone before the first install (`PRAGMA user_version;`); versions below 5 will reset. Export a manual backup first.
- [ ] Restore the import JSON in-app and check totals against the spreadsheet.

## Working rules
- One step at a time; review (grep for leftovers, read diffs) after each.
- Compile after every step once Step 0 is done.
- Commit after each green step.
