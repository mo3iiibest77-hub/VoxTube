# VoxTube — Progress

Last updated: 2026-09-11 (backend TTS fix)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Documentation | Done | Foundation + Progress |
| Backend | ~75% | `google-genai` SDK, fixed `/tts`, WAV wrap, `/health` |
| Backend deploy workflow | Ready | Needs redeploy after this push |
| Android resources | Done | layouts/themes/icons |
| Android player | Still wrong stack | androidyoutubeplayer |
| Chunked synced TTS | Not done | |
| APK Actions | Missing | |
| Config placeholders | Still present | `YOUR_SERVER_IP`, YouTube key |

## Completed

- [x] Repo audit + architecture lock
- [x] `PROJECT_FOUNDATION.md` + `PROGRESS.md`
- [x] Android `res/` minimal set (compile-ready UI shell)
- [x] Backend: switch `google-generativeai` → `google-genai`
- [x] Backend: correct TTS call for `gemini-2.5-flash-preview-tts`
- [x] Backend: PCM→WAV header for Android MediaPlayer
- [x] Backend: voice validation, health endpoint, better errors

## Next steps (ordered)

1. Wire Android `BuildConfig` for backend base URL + YouTube API key (no more hardcode)
2. Migrate player to **Media3 ExoPlayer** (mute original audio)
3. Chunked dubbing: transcript entries → sequential/parallel TTS → schedule by `start`
4. GitHub Actions workflow: build + sign universal APK
5. Device E2E test

## Session log

### 2026-09-11
- Foundation docs
- Android resources
- Backend TTS rewritten for official `google-genai` + preview TTS model
- Deploy workflow will rebuild container on next push to `backend/**` (this commit)
