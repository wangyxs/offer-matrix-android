# offer-matrix-android

[中文](./README.zh-CN.md) | [English](./README.en.md)

`offer-matrix-android` is an Android client for an AI mock interview and interview training platform. It is built around a full interview-prep loop: role-based practice, resume-aware questioning, interview records, and post-interview analysis.

## Highlights

- AI mock interviews with voice interaction
- Role-based interview preparation for different job tracks
- Resume-aware interview flow for more personalized questions
- Training workflows with generated questions and favorites
- Interview record saving and analysis feedback
- Resume and materials management for each selected role

## Use Cases

- Mobile-first interview preparation for job seekers
- Focused practice for frontend, backend, AI, product, testing, and operations roles
- More realistic mock interviews using resume context
- Ongoing improvement through saved records and interview review

## Tech Stack

- Kotlin
- Jetpack Compose
- Android View + Compose hybrid UI
- Retrofit
- Media3
- CameraX
- ByteDance Speech Dialog SDK

## Main Modules

```text
app/src/main/java/com/example/offermatrix/
├── interview/      # Voice interview session and speech dialog logic
├── network/        # Retrofit API models and client
├── ui/navigation/  # Navigation layer
├── ui/screens/     # Login, role, training, interview, analysis, profile screens
├── ui/viewmodel/   # Presentation state
└── utils/          # Shared helpers
```

## Local Setup

Do not commit speech SDK credentials or license files.

Add these entries to your local `local.properties` file:

```properties
speechDialogAppId=your_app_id
speechDialogAppKey=your_app_key
speechDialogAccessToken=your_access_token
```

If your speech SDK also requires a local license asset, place the real file at:

```text
app/src/main/assets/tts_license
```

That file is gitignored and should stay local only.

## Related Repository

- Backend API: [offer-matrix-api](https://github.com/wangyxs/offer-matrix-api)

## Keywords

AI interview, mock interview app, Android interview prep, resume-based interview, role-based training, Jetpack Compose, career prep
