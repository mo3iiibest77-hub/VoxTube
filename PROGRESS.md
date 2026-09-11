# PROGRESS — VoxTube

> **AI بعدی:** اول PROJECT.md بخون، بعد این رو.
> بعد از خوندن بگو: "خوندم، الان Phase [X] هستیم، قدم بعدی [Y] هست."

---

## وضعیت فعلی

**Phase:** 1 — Repo & Backend  
**آخرین آپدیت:** 2026-09-11  
**چی تموم شده:** Planning، architecture، PROJECT.md، PROGRESS.md  
**قدم بعدی:** نوشتن `backend/main.py`

---

## مراحل تموم‌شده

### ✅ Phase 0 — Planning (DONE)
- تعریف پروژه
- انتخاب tech stack
- تصمیم: YouTube Data API لازم نیست
- تصمیم: backend روی VPS خودش
- Gemini API key گرفته شده
- PROJECT.md و PROGRESS.md نوشته شد
- Repo `VoxTube` ساخته شد روی GitHub

---

## Phase فعلی — جزئیات

### 🔄 Phase 1 — Backend + GitHub Actions + App Skeleton

**Step 1.1 — Backend** ← **اینجاییم**

فایل `backend/main.py`:
- `POST /transcript` — YouTube URL می‌گیره، subtitle فارسی + timestamp برمی‌گردونه
- `POST /tts` — text + timestamps می‌گیره، Gemini TTS صدا می‌سازه، audio segments برمی‌گردونه

فایل `backend/requirements.txt`:
fastapi
uvicorn
youtube-transcript-api
google-generativeai


**Step 1.2 — GitHub Actions**

فایل `.github/workflows/build.yml`:
- trigger: push به main
- اول: debug APK (بدون sign — برای تست)
- بعد: signed release APK با keystore secrets

**Step 1.3 — Android skeleton**

- یه screen: URL input + Play button
- ExoPlayer setup
- HTTP call به backend

---

## وضعیت Blockers

| Blocker | وضعیت |
|---|---|
| VPS IP/domain | ✅ دارد — SSH می‌زنه |
| Python روی VPS | ✅ نصبه |
| Keystore | ⚠️ باید چک بشه — از QuietStorm داره ولی باید GitHub Secret بشه توی VoxTube repo |
| GitHub Secrets | ⚠️ هنوز add نشده برای VoxTube |

---

## جدول کلی Phases

| Phase | چی | وضعیت |
|---|---|---|
| 0 | Planning + docs | ✅ Done |
| 1 | Backend + GitHub Actions + App skeleton | 🔄 در حال انجام |
| 2 | Subtitle fetch end-to-end | ⬜ بعدی |
| 3 | Gemini TTS integration | ⬜ بعدی |
| 4 | Audio/video sync | ⬜ بعدی |
| 5 | UI polish + release APK | ⬜ بعدی |

---

## Context مهم برای AI بعدی

- Mo3iBest از VPS هلند کار می‌کنه (SSH)، نه لوکال
- Build فقط از GitHub Actions — لپ‌تاپ نداره
- Keystore از پروژه QuietStorm-VPN قبلیشه
- Gemini API key آمادست
- youtube-transcript-api بدون API key کار می‌کنه — این مهمه
