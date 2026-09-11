# VoxTube — Progress

Last updated: 2026-09-11 (BuildConfig wiring)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Documentation | Done | |
| Backend | ~75% | google-genai TTS fixed; redeploy on VPS when ready |
| Android resources | Done | |
| Config | Done | `BuildConfig.BACKEND_BASE_URL` + `YOUTUBE_API_KEY` |
| Android player | Pending | still androidyoutubeplayer |
| Chunked synced TTS | Pending | |
| APK Actions | Pending | |

## Completed this session

- [x] Foundation docs in git
- [x] Minimal Android `res/`
- [x] Backend TTS rewrite (`google-genai` + WAV)
- [x] Remove hard-coded server IP / YouTube key from source; use BuildConfig + gradle props

## How to set config

Local / CI example:

```bash
./gradlew assembleDebug -PBACKEND_BASE_URL=https://your-vps:8000/ -PYOUTUBE_API_KEY=xxxx
```

Or copy `gradle.properties.example` → `gradle.properties` (gitignored if secrets).

## Next steps

1. Media3 ExoPlayer migration + mute original audio
2. Chunked dubbing pipeline (timed TTS)
3. GitHub Actions: build + sign APK (inject secrets as -P)
4. E2E on device

## Session log

### 2026-09-11
- Audit → docs → resources → backend TTS → BuildConfig
