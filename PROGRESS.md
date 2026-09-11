# VoxTube — Progress

Last updated: 2026-09-11 (test-build readiness)

## Goal this iteration

**First installable debug APK** with Gemini-only backend path. No OpenRouter.

## What was fixed for test build

- [x] `usesCleartextTraffic` + network security config (HTTP backend on VPS)
- [x] **Skip login** button (no Google OAuth required for test)
- [x] **Paste YouTube URL** on home → open player (works without YouTube Data API key)
- [x] Auth still has Google Sign-In optional
- [x] `gradle.properties` AndroidX flags

## Still required from you for a working test

### Minimum to test dubbing

1. **Backend running** on a reachable host with `GEMINI_API_KEY` set in the container env
2. **GitHub Secret `BACKEND_BASE_URL`** = that public URL ending with `/`  
   Example: `http://YOUR_VPS_IP:8000/`
3. Run **Build Android APK** workflow → install artifact

### Optional

- `YOUTUBE_API_KEY` → trending/search list works (URL paste works without it)
- `SERVER_*` → only if you want auto-deploy; otherwise manual `git pull` + docker on VPS is fine

## Test path on phone

1. Open app → **ورود آزمایشی**
2. Paste a YouTube link that has English/Persian subtitles
3. Open → **دوبله فارسی**
4. Wait for TTS chunks (needs backend + Gemini)

## Known limits (test v1)

- Google Sign-In may fail without OAuth client setup — use skip
- Mute is best-effort on YouTube iframe player
- Long videos = many Gemini TTS calls (free quota)
- No signed release yet

## Next after first successful dub on device

- Tune chunk size / rate limits
- Caching
- Polish errors
