# VoxTube — Progress

Last updated: 2026-09-11 (Android CI workflow)

## Current Status Snapshot

| Area | Status | Notes |
|------|--------|-------|
| Docs | Done | |
| Backend | ~75% | Redeploy VPS after TTS changes |
| Android shell | Compile-ready | |
| Timed dubbing v1 | Done | Chunk windows + mute + schedule |
| CI build APK | **Added** | `.github/workflows/build-android.yml` → artifact `voxtube-debug-apk` |
| Signed release | Pending | Needs keystore secrets |

## GitHub Secrets to set

| Secret | Required for |
|--------|----------------|
| `GEMINI_API_KEY` | Backend deploy |
| `SERVER_HOST` / `SERVER_USER` / `SERVER_SSH_KEY` | Backend deploy |
| `BACKEND_BASE_URL` | Android APK (your VPS URL, e.g. `http://x.x.x.x:8000/`) |
| `YOUTUBE_API_KEY` | Android feed/search |
| (later) keystore + passwords | Signed release APK |

## Completed this session

- Foundation docs
- Android `res/`
- Backend TTS fix (`google-genai`)
- BuildConfig wiring
- YouTube id parse fix
- Chunked timed dubbing
- **Debug APK GitHub Actions workflow**

## Next

1. Set secrets in GitHub repo settings
2. Confirm backend deploy + `/health` on VPS
3. Download debug APK from Actions → install on phone
4. Add release signing workflow when keystore secrets ready
5. Iterate on TTS quality / rate limits / caching

## Session log

### 2026-09-11
Bootstrap complete for a testable pipeline end-to-end (minus live secrets & device install).
