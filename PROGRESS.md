# VoxTube — Progress

Last updated: 2026-09-11 (session: initial full audit + foundation)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Documentation | Done (this commit) | Foundation + Progress tracked in git |
| Backend skeleton | ~60% | FastAPI `/transcript` + `/tts` exist; TTS model/SDK needs verification |
| Backend deploy workflow | Ready | SSH + Docker on push to `backend/**` |
| Android resources (`res/`) | **Missing** | Project does **not compile** |
| Android auth + home shell | Skeleton | Google Sign-In + YouTube Data API calls present |
| Android player | Broken for goal | Uses androidyoutubeplayer; no mute/sync |
| Chunked synced TTS | Not implemented | Full-text single TTS only |
| APK build/sign Actions | Missing | No workflow yet |
| Base URL / API keys | Placeholders | `YOUR_SERVER_IP`, `YOUR_YOUTUBE_API_KEY` |

## Completed this session

- [x] Full repository audit
- [x] Removed progress/foundation docs from ignore list
- [x] Added `PROJECT_FOUNDATION.md` (locked architecture)
- [x] Added `PROGRESS.md`

## Next steps (ordered, smallest first)

1. **Make Android project compilable**
   - Create minimal `res/` (layouts, values, mipmap placeholders, themes)
   - Align package/resources with existing Activities

2. **Fix backend hard requirements**
   - Verify Gemini TTS call against current `google-generativeai` + available TTS model name
   - Ensure `GEMINI_API_KEY` from env
   - Health + clearer error responses

3. **Replace player with Media3 ExoPlayer**
   - Mute original audio
   - Keep video timeline as master clock

4. **Implement real dubbing pipeline**
   - Transcript → list of timed chunks
   - TTS per chunk (or batched)
   - Schedule playback on ExoPlayer timeline / secondary audio player synced to position

5. **Wire real config**
   - BuildConfig fields for backend base URL + YouTube API key (from secrets in CI)

6. **GitHub Actions: build + sign APK**
   - Assemble release, sign with keystore secrets, upload artifact

7. **End-to-end test** on device (Android 8+)

## Known blockers / decisions to keep

- User has no laptop → all builds via GitHub Actions
- Must stay on free tiers
- Keystore available from QuietStorm-VPN project (to be added as secrets when signing workflow is created)
- Prefer incremental fixes over full rewrite

## Session log

### 2026-09-11
- Read entire repo tree and all source files
- Confirmed missing `res/`, no APK workflow, incomplete sync logic
- Architecture locked in `PROJECT_FOUNDATION.md`
- Proceeding without per-step user confirmation (user instruction)
