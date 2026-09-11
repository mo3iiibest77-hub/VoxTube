# PROGRESS — YoutubeFa

> **For the next AI session:** Read PROJECT.md first, then this file. You'll know exactly where we are and what to do next. Tell Mo3iBest "I've read both files, we're at [current phase], next step is [X]" so he knows you're up to speed.

---

## Current Status

**Phase:** 0 — Setup & Planning  
**Date:** 2026-09-11  
**What's done:** Planning, architecture decisions, file structure design  
**What's next:** Create GitHub repo and write the first code files

---

## Completed Steps

### ✅ Step 0 — Planning (DONE)
- Defined what the app does
- Chose tech stack (Kotlin + Compose + ExoPlayer + FastAPI + Gemini TTS)
- Decided no YouTube Data API needed
- Decided backend needed on Mo3iBest's VPS
- Mo3iBest got Gemini API key (from aistudio.google.com)
- Created PROJECT.md and PROGRESS.md

---

## Current Phase Detail

### 🔄 Phase 1 — Repo & Backend (UP NEXT)

**Step 1.1 — Create GitHub repo**
- Mo3iBest creates repo (suggested name: `youtubefa`)
- Public repo
- Add PROJECT.md and PROGRESS.md as first commit

**Step 1.2 — Backend (FastAPI)**
- File: `backend/main.py`
- Endpoint: `POST /transcript` → takes YouTube URL, returns Persian subtitle with timestamps
- Endpoint: `POST /tts` → takes text + timestamps, calls Gemini TTS, returns audio segments
- File: `backend/requirements.txt`

**Step 1.3 — GitHub Actions workflow**
- File: `.github/workflows/build.yml`
- Triggers on push to main
- Builds debug APK first (no signing needed for testing)
- Later: signed release APK with keystore secrets

**Step 1.4 — Android app skeleton**
- Basic Jetpack Compose UI
- One screen: URL input + Play button
- ExoPlayer integration

---

## Phases Overview

| Phase | What | Status |
|-------|------|--------|
| 0 | Planning + docs | ✅ Done |
| 1 | Repo + Backend + GitHub Actions + App skeleton | 🔄 Next |
| 2 | Subtitle fetch working end-to-end | ⬜ Pending |
| 3 | Gemini TTS integration | ⬜ Pending |
| 4 | Audio/video sync | ⬜ Pending |
| 5 | UI polish + release APK | ⬜ Pending |

---

## Important Context for Next AI

- Mo3iBest communicates in Persian (Farsi)
- He has a VPS running (already hosts Xray/X-UI VPN infra) — backend goes there
- He has GitHub Actions experience from previous Android projects
- He has a Keystore file already (used before for signing)
- Gemini API key is obtained and ready
- No YouTube Data API key — not needed
- Build must happen on GitHub Actions, not locally
- He's on Android phone + PC, no laptop

---

## Blockers / Open Questions

- [ ] What is the VPS IP/domain for backend deployment?
- [ ] Does the VPS have Python 3.10+ installed?
- [ ] What's the GitHub repo name Mo3iBest chose?
- [ ] Keystore stored as GitHub Secret already or needs to be added?

---

## Next Message to Send (for next AI session)

Copy-paste this to the next Claude/AI session after sharing both files:

> "این پروژه رو داریم می‌سازیم. PROJECT.md و PROGRESS.md رو بخون و بگو کجاییم و قدم بعدی چیه."
