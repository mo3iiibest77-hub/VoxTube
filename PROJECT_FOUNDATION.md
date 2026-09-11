# VoxTube — Project Foundation

Last updated: 2026-09-11

## Vision

VoxTube is a **native Android YouTube client replacement** that:

1. Signs the user in with their **Google account**
2. Shows their **personal YouTube feed** (subscriptions, recommended, history) via YouTube Data API v3
3. On every video:
   - Fetches timed subtitles (prefer Persian, fallback English)
   - Sends each subtitle chunk to the backend
   - Backend generates Persian AI voice with Gemini TTS
   - **Mutes original audio**
   - Plays Persian AI audio **synced to video timestamps**

Result: watching Veritasium / Kurzgesagt / Vsauce / etc. with natural Persian AI voice, no English audio, no reading subtitles.

---

## Package & Identity

- Package name: `com.mo3ibest.voxtube`
- Min SDK: 26 (Android 8.0+)
- Target / Compile SDK: 34
- Universal APK (all ABIs)

---

## Architecture Decisions (locked)

### Android
| Decision | Choice | Why |
|----------|--------|-----|
| Language | Kotlin | Standard |
| DI | Hilt | Already present |
| UI | ViewBinding for now (Compose later if needed) | Functionality first; resources must compile |
| Auth | Google Sign-In + YouTube scope | Required for personal feed |
| Feed / Search / History | YouTube Data API v3 | Required for real subscriptions & recommendations |
| Player | **Media3 ExoPlayer** (replace androidyoutubeplayer) | Need full control over audio track (mute + custom audio) |
| Network | Retrofit + OkHttp | Already present |
| Dubbing strategy | Per-subtitle-chunk TTS + timestamp sync | Real-time feel + accurate lip-sync approximation |

### Backend
| Decision | Choice |
|----------|--------|
| Framework | FastAPI |
| Subtitles | `youtube-transcript-api` (no YouTube Data API key needed for transcripts) |
| TTS + optional translation | Gemini (`google-generativeai`) free tier |
| Deploy | Docker on personal VPS via GitHub Actions SSH |
| Auth between app ↔ backend | None for v1 (CORS open). Can add later |

### Build & Release
| Decision | Choice |
|----------|--------|
| CI | GitHub Actions only (no local laptop) |
| Signing | Existing keystore from QuietStorm-VPN project (store as GitHub secret) |
| Artifact | Signed universal APK |

### Cost constraints (hard)
- Everything free-tier: Gemini free tier, GitHub Actions free minutes, youtube-transcript-api (no key)
- No paid YouTube Data API quota tricks beyond free daily quota

---

## Current vs Target Player Pipeline

**Current (broken):**
1. androidyoutubeplayer loads video
2. One big transcript string (truncated to 2000 chars)
3. Single TTS call → one audio file
4. MediaPlayer plays it independently (no mute, no sync)

**Target:**
1. ExoPlayer plays YouTube stream (or progressive) with **volume = 0** (or audio disabled)
2. Backend returns list of `{text, start, duration, audio_base64?}` or app requests TTS per chunk
3. App schedules each chunk’s audio at `start` time relative to video position
4. Optional: pre-generate / cache next N chunks while playing

---

## Backend API (target)

```
GET  /                     → health
POST /transcript           → { url } → { video_id, transcript: [{text, start, duration}] }
POST /tts                  → { text, voice? } → { audio_base64, mime_type }
POST /dub                  → (future) { url, voice? } → full timed audio package
```

Base URL comes from BuildConfig / remote config, **never** hard-coded IP in source for release.

---

## Non-goals (v1)

- Offline download of full dubbed videos
- Multi-language UI beyond Persian labels
- Comments / community features
- Upload
- Paid subscriptions / ads

---

## Secrets (GitHub + VPS)

| Secret | Used for |
|--------|----------|
| `GEMINI_API_KEY` | Backend TTS |
| `SERVER_HOST` / `SERVER_USER` / `SERVER_SSH_KEY` | Deploy workflow |
| `YOUTUBE_API_KEY` | Android (or proxy via backend later) |
| Keystore + passwords | APK signing workflow |

Never commit real keys.
