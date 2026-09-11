# VoxTube — Progress

Last updated: 2026-09-11 (resources added)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Documentation | Done | `PROJECT_FOUNDATION.md` + this file |
| Backend skeleton | ~60% | FastAPI `/transcript` + `/tts`; TTS needs verify |
| Backend deploy workflow | Ready | SSH + Docker |
| Android resources (`res/`) | **Added** | layouts + themes + strings + adaptive icons |
| Android auth + home shell | Skeleton | Still needs real API keys / base URL |
| Android player | Broken for goal | androidyoutubeplayer; no mute/sync |
| Chunked synced TTS | Not implemented | |
| APK build/sign Actions | Missing | |
| Base URL / API keys | Placeholders | |

## Completed

- [x] Full repo audit
- [x] Foundation + Progress docs in git
- [x] Minimal `res/` so ViewBinding Activities can compile (auth / main / player / item_video)
- [x] Adaptive launcher icons + theme + proguard stub

## Next steps (ordered)

1. **Backend TTS verification + harden**
   - Fix Gemini TTS client/model usage if broken
   - Env-only API key, better errors
2. **Config wiring**
   - BuildConfig / secrets for backend base URL + YouTube API key
3. **Media3 ExoPlayer migration**
   - Mute original audio; timeline as clock
4. **Chunked dubbing pipeline**
   - Per-subtitle TTS + schedule on video position
5. **GitHub Actions: assemble + sign APK**
6. **E2E test on device**

## Session log

### 2026-09-11
- Audit complete; architecture locked
- Added foundation docs
- Added Android resource tree required by existing Activities
- Next: backend TTS fix + config, then player swap
