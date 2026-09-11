# VoxTube — Persian YouTube Dubber

> این فایل entry point هر AI assistant هست که روی این پروژه کار می‌کنه.
> قبل از هر کاری این رو کامل بخون. بعد PROGRESS.md رو بخون.

---

## 1. WHAT THIS PROJECT IS

یه Native Android app که لینک یوتیوب می‌گیره و ویدیو رو با **صدای فارسی AI** پخش می‌کنه — نه subtitle، بلکه دوبله واقعی.

Flow کلی:
User لینک یوتیوب می‌ده
↓
Backend subtitle فارسی + timestamp می‌کشه (youtube-transcript-api)
↓
Gemini TTS API متن رو به صدای فارسی تبدیل می‌کنه
↓
Android app صدای فارسی رو با timeline ویدیو sync می‌کنه
↓
ویدیو با صدای اصلی mute + صدای فارسی روش پخش می‌شه

---

## 2. WHY THIS EXISTS

کانال‌های علمی/آموزشی بهتر دنیا انگلیسی‌ان (Veritasium، Kurzgesagt، Vsauce، Isaac Arthur). هیچ اپ Android رایگانی نیست که دوبله فارسی واقعی با sync بده — فقط subtitle وجود داره. VoxTube این gap رو می‌بنده.

---

## 3. TECH STACK — CONFIRMED DECISIONS

| Layer | انتخاب | دلیل |
|---|---|---|
| Android | Kotlin + Jetpack Compose | Native، مدرن |
| Video Player | ExoPlayer (Media3) | بهترین Android video player |
| Backend | Python FastAPI | subtitle fetch + Gemini calls |
| Subtitle | youtube-transcript-api | بدون API key، رایگان |
| TTS | Gemini TTS API | رایگان، کیفیت خوب فارسی |
| Build | GitHub Actions | لپ‌تاپ لازم نیست |
| Hosting | VPS هلند (Mo3iBest's server) | سرور از قبل داره |

---

## 4. REPO STRUCTURE
VoxTube/
├── PROJECT.md ← این فایل — اول بخون
├── PROGRESS.md ← وضعیت فعلی — دوم بخون
├── app/ ← Android app (Kotlin)
│ ├── src/main/
│ │ ├── java/com/mo3ibest/voxtube/
│ │ └── res/
│ ├── build.gradle.kts
│ └── AndroidManifest.xml
├── backend/ ← Python FastAPI
│ ├── main.py
│ └── requirements.txt
├── .github/
│ └── workflows/
│ └── build.yml ← GitHub Actions — APK می‌سازه
└── README.md

---

## 5. API KEYS & SECRETS

| چی | وضعیت | کجا |
|---|---|---|
| Gemini API Key | ✅ دارد | از aistudio.google.com |
| YouTube Data API | ❌ لازم نیست | youtube-transcript-api جاشو می‌گیره |
| Keystore (APK sign) | ✅ دارد | از پروژه قبلی — باید GitHub Secret بشه |

**GitHub Secrets لازم برای build:**
- `GEMINI_API_KEY`
- `KEYSTORE_FILE` (base64)
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `STORE_PASSWORD`

---

## 6. WHAT TO PRESERVE — دست نزن

- معماری backend/frontend جدا بمونه — backend روی VPS، app روی Android
- youtube-transcript-api رو با چیز دیگه‌ای replace نکن — API key نمی‌خواد
- ExoPlayer رو با چیز دیگه‌ای replace نکن — بهترین گزینه‌ست
- GitHub Actions workflow رو فقط extend کن، rewrite نکن

---

## 7. HOW AI SHOULD WORK HERE

1. این فایل رو کامل بخون، بعد PROGRESS.md رو
2. قبل از نوشتن کد، بگو چی الان توی repo هست
3. کوچکترین قدم ممکن رو propose کن، نه rewrite کامل
4. هر قدم که تموم شد، بگو PROGRESS.md رو چطور آپدیت کنیم
5. اگه چیزی با این فایل conflict داره، صریح بگو — چیزی رو مخفی نکن
6. کد رو کامل بنویس، placeholder نذار

---

## 8. DEFINITION OF MVP DONE

MVP وقتی تموم‌ه که:
- یه لینک یوتیوب بدی
- اپ subtitle فارسی رو بکشه
- Gemini TTS صدا بسازه
- صدا sync بشه با ویدیو
- ویدیو با صدای فارسی پخش بشه

UI نباید perfect باشه. کرش نباید بده. این کافیه برای MVP.

---

## 9. USER PROFILE

- **اسم:** Mo3iBest
- **ابزار:** Android phone + PC + VPS هلند (SSH)، گوشی Termux هم داره
- **تجربه:** Xray/X-UI/VPN infra، GitHub Actions (قبلاً APK build کرده)
- **زبان:** فارسی — ولی technical terms انگلیسی رو می‌فهمه
- **محدودیت:** لپ‌تاپ نداره — همه build از GitHub Actions
