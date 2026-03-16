# offer-matrix-android

Android client for an AI mock interview and interview training platform.

This app helps candidates prepare for real interviews with role-based practice, resume-aware mock interviews, generated question sets, favorites, and interview record analysis. It is designed for job seekers who want a mobile-first interview prep workflow instead of a static question bank.

## What It Does

- AI mock interview on Android with voice interaction
- Resume-aware interview flow for more personalized questioning
- Role-based preparation for jobs such as frontend, backend, AI, product, testing, and operations
- Interview question training with generated question sets and favorites
- Interview record saving and post-interview analysis
- Materials and resume management tied to selected roles

## Product Positioning

This repository is the mobile application layer of the Offer Matrix project. The product focuses on:

- AI interview practice
- role-specific job preparation
- resume-based personalized questioning
- interview feedback and training loops

## Tech Stack

- Kotlin
- Jetpack Compose
- Android View + Compose hybrid screens
- Retrofit
- Media3
- CameraX
- ByteDance Speech Dialog SDK

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

## Project Structure

```text
app/src/main/java/com/example/offermatrix/
├── interview/      # Voice interview session and speech dialog logic
├── network/        # Retrofit API models and client
├── ui/navigation/  # App navigation
├── ui/screens/     # Login, role, training, interview, analysis, profile pages
├── ui/viewmodel/   # Presentation state
└── utils/          # Shared helpers
```

## Related Repository

- Backend API: [offer-matrix-api](https://github.com/wangyxs/offer-matrix-api)

## Keywords

AI interview, mock interview app, Android interview prep, resume-based interview, job training, Jetpack Compose, career prep
