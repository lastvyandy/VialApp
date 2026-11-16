# Native Android: Google sign-in and Premium

## Google sign-in
- Deploy the backend with `GOOGLE_CLIENT_ID` set to the OAuth **Web application** client ID used to verify ID tokens. The public features endpoint exposes only this public identifier.
- In the same Google Cloud project, create an OAuth Android client for package `cl.vialapp.app` and the signing certificate SHA-1. Register an appropriate Android client for debug, release, and Google Play App Signing certificates as needed. Use `gradlew signingReport` to find local fingerprints.
- For a new Google account, select Create account and confirm age and terms. Existing users with password login can link Google from Profile by authenticating with their password; the Google email must match.
- No OAuth client secret is stored in the APK.

## Google Play Premium
- Android uses one-time, consumable access passes, not auto-renewing subscriptions. Configure all nine product IDs in Play Console:
  - `premium_daily_b`, `premium_daily_c`, `premium_daily_all`
  - `premium_biweekly_b`, `premium_biweekly_c`, `premium_biweekly_all`
  - `premium_monthly_b`, `premium_monthly_c`, `premium_monthly_all`
- In backend deployment secrets set `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`, enable Android Publisher API and grant the account purchase-related access to the app. Set `GOOGLE_PLAY_PACKAGE_NAME=cl.vialapp.app`; keep `FREE_PREMIUM_MODE=false` to allow payments. Run the repository's existing migrations.
- Install a signed AAB via Play's internal testing track and configure license testing. The app displays official localized Play prices and verifies each purchased token server-side. The server grants access idempotently and handles consumption; Android does not grant Premium locally. Pending/unconsumed purchases can be recovered via Profile > Premium > Sync purchases.
- Automatic refund/revocation notifications and recurring subscriptions are not included. Do not go live before testing purchase, cancellation, pending purchase, account mismatch, and retry flows with test accounts.
