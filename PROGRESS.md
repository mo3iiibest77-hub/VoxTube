# VoxTube — Progress

Last updated: 2026-09-11 (CI harden)

## Snapshot

| Area | Status |
|------|--------|
| Docs | Done |
| Backend TTS | Fixed (google-genai) |
| Android UI shell | Done |
| Config | BuildConfig |
| Timed dubbing v1 | Done |
| CI debug APK | Workflow ready (needs secrets + first green run) |
| Signed release | Later |

## Secrets checklist

- `GEMINI_API_KEY`
- `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`
- `BACKEND_BASE_URL` (public VPS URL ending with `/`)
- `YOUTUBE_API_KEY`

## Done this session

Full audit → foundation → resources → backend TTS → config → feed fix → chunked dubbing → Android CI

## You do next

1. GitHub → Settings → Secrets: مقادیر بالا
2. Actions → Build Android APK → Run workflow (یا push کوچک)
3. Artifact را دانلود و روی گوشی نصب کن
4. Backend را با deploy workflow روی VPS بالا بیاور و `/health` را چک کن
