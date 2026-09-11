# YoutubeFa — Persian YouTube Dubber

## What This Project Does

A Native Android app that takes a YouTube video link and plays it with **Persian (Farsi) AI voice** instead of the original audio.

The user pastes a YouTube URL → the app fetches the Persian subtitle (already on YouTube) → sends the text to Gemini TTS API → syncs the generated Persian audio with the video timeline → plays the video with Persian dubbing.

---

## Why We're Building This

- Channels like Arantik (آرانتیک) make great Persian science content, but the best science/educational YouTube channels are in English
- No free Android app exists that does real Persian dubbing (not just subtitles)
- We want to watch channels like Veritasium, Kurzgesagt, Vsauce, Isaac Arthur — in Persian, with synced voice

---

## Architecture

```
User pastes YouTube URL
        ↓
Fetch Persian subtitle + timestamps (youtube-transcript-api via backend)
        ↓
Send text chunks to Gemini TTS API (with timestamps)
        ↓
Generate Persian audio segments
        ↓
Sync audio segments with video timeline
        ↓
Play video (muted original) + Persian audio overlay
```

---

## Tech Stack

| Layer | Choice | Reason |
|-------|--------|--------|
| Android | Kotlin + Jetpack Compose | Native, modern |
| Video Player | ExoPlayer (Media3) | Best Android video player, supports audio tracks |
| Backend | Python FastAPI (lightweight) | Handles YouTube transcript fetch + Gemini calls |
| Subtitle Fetch | youtube-transcript-api (Python) | No API key needed, free |
| TTS | Gemini TTS API | Free tier, good Persian quality |
| Build | GitHub Actions | No laptop needed, builds APK automatically |
| Hosting | Backend on a VPS (user already has servers) | Mo3iBest runs VPN infrastructure |

---

## API Keys Needed

| Key | Status | Notes |
|-----|--------|-------|
| Gemini API Key | ✅ Got it | From aistudio.google.com, free tier |
| YouTube Data API | ❌ Not needed | Using youtube-transcript-api instead |
| Keystore (APK signing) | ✅ Has it | Used in previous GitHub Actions projects |

---

## Key Decisions Made

1. **No YouTube Data API** — youtube-transcript-api handles subtitles without any key
2. **Backend needed** — youtube-transcript-api is Python, can't run on Android directly. A small FastAPI server (on Mo3iBest's existing VPS) handles this
3. **Gemini TTS not "read aloud" button** — We use the actual Gemini TTS API, which produces the same quality voice but is programmable
4. **Audio replacement not overlay** — Original English audio is muted, Persian audio plays instead, synced to timestamps
5. **GitHub Actions build** — Mo3iBest has no laptop, builds happen on GitHub's servers

---

## Repo Structure (Planned)

```
youtubefa/
├── PROJECT.md              ← This file
├── PROGRESS.md             ← Current status and next steps
├── app/                    ← Android app (Kotlin)
│   ├── src/
│   └── build.gradle.kts
├── backend/                ← Python FastAPI server
│   ├── main.py
│   └── requirements.txt
├── .github/
│   └── workflows/
│       └── build.yml       ← GitHub Actions — builds APK
└── README.md
```

---

## User Profile

- **Name:** Mo3iBest
- **Location:** Neyshabur, Iran
- **Has:** Android phone, PC (no laptop), VPS servers, GitHub account with Actions experience
- **Knows:** Xray/X-UI, VPN infra, server management, some coding
- **Wants:** App built collaboratively, pushed to GitHub, auto-built via Actions
