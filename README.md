# offer-matrix-android

Android client for the Offer Matrix project.

## Local secret setup

Do not commit speech SDK credentials or license files.

Add these entries to your local `local.properties` file:

```properties
speechDialogAppId=your_app_id
speechDialogAppKey=your_app_key
speechDialogAccessToken=your_access_token
```

If your speech SDK also requires a license asset, place the real file at:

```text
app/src/main/assets/tts_license
```

That file is gitignored and should stay local only.
