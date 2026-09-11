# VoxTube — Progress

Last updated: 2026-09-11 (chunked timed dubbing)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Docs | Done | |
| Backend | ~75% | google-genai TTS + WAV |
| Android res + config | Done | |
| YouTube id parse | Fixed | |
| Dubbing | **v1 chunked** | merge ~12s windows, parallel TTS (2), schedule by `onCurrentSecond`, mute() |
| Player stack | Still youtubeplayer | ExoPlayer deferred (YouTube stream constraints) |
| APK Actions | Pending | |

## Completed this session

1. Foundation + Progress in git
2. Android resources
3. Backend TTS rewrite
4. BuildConfig secrets wiring
5. YouTube id deserializer
6. **Chunked dubbing pipeline** (PlayerViewModel + PlayerActivity)

## How dubbing works now

1. `POST /transcript` → timed entries
2. Merge into ~12s text windows
3. Up to 2 concurrent `POST /tts` calls
4. On play: `youTubePlayer.mute()` + tick every 250ms
5. When video time ≥ chunk.start → play that WAV via MediaPlayer

## Remaining limitations

- Mute is best-effort (IFrame player API)
- TTS duration may not equal subtitle window (speed mismatch)
- Long videos = many TTS calls (free-tier rate limits)
- No caching of chunks yet

## Next steps

1. GitHub Actions: assembleDebug/Release + sign APK + upload artifact
2. Optional: backend `/dub` batch endpoint to reduce round-trips
3. Chunk cache on disk keyed by videoId
4. After VPS URL known: set `BACKEND_BASE_URL` in CI
5. Real device test

## Session log

### 2026-09-11
Full bootstrap of docs, resources, backend TTS, config, feed fix, timed dubbing v1.
