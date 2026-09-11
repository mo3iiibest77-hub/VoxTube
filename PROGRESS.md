# VoxTube — Progress

Last updated: 2026-09-11 (YouTube id parse fix)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Documentation | Done | |
| Backend | ~75% | TTS fixed with google-genai |
| Android resources | Done | |
| Config | Done | BuildConfig |
| YouTube feed parsing | Fixed | videos.list string id vs search object |
| Player | Pending Media3 | |
| Chunked dubbing | Pending | |
| APK Actions | Pending | |

## Completed this session

1. Foundation + Progress docs
2. Android `res/` (compile shell)
3. Backend TTS (`google-genai`, WAV wrap, health)
4. BuildConfig for backend URL + YouTube key
5. YouTube `id` deserialization fix

## Next (priority)

1. **Media3 ExoPlayer** — mute original track; prepare for timed AI audio
2. Chunked TTS scheduling on video timeline
3. GitHub Actions signed APK workflow
4. Point `BACKEND_BASE_URL` at live VPS and redeploy backend container

## Notes for next session

- Deploy workflow triggers on `backend/**` pushes — backend already pushed; ensure VPS secrets (`GEMINI_API_KEY`, SSH) are set in GitHub.
- YouTube Data API key required for feed; store only as CI secret / local gradle prop.
- ExoPlayer cannot natively play youtube.com URLs without a stream extractor; migration plan should use either:
  - Keep WebView/YouTube player for video + separate MediaPlayer for AI audio (simpler, limited mute), or
  - Use a stream extraction approach (ToS-sensitive) or official YouTube IFrame + JS volume control.
- Decision deferred: implement best practical mute path on current player first if ExoPlayer YouTube stream is blocked.
