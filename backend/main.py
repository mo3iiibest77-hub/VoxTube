from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import (
    TranscriptsDisabled,
    NoTranscriptFound,
    VideoUnavailable,
    CouldNotRetrieveTranscript,
)
from google import genai
from google.genai import types
import re
import os
import base64
import struct
import logging
import json
import subprocess
import tempfile

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("voxtube")

app = FastAPI(title="VoxTube Backend", version="0.2.3")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "").strip()
TTS_MODEL = os.environ.get("TTS_MODEL", "gemini-2.5-flash-preview-tts")
DEFAULT_VOICE = os.environ.get("TTS_VOICE", "Charon")
# socks5h://127.0.0.1:1080  or  http://127.0.0.1:8082
PROXY_URL = os.environ.get("PROXY_URL", "").strip()

VALID_VOICES = {
    "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Aoede",
    "Callirrhoe", "Autonoe", "Enceladus", "Iapetus", "Umbriel", "Algieba",
    "Despina", "Erinome", "Algenib", "Rasalgethi", "Laomedeia", "Achernar",
    "Alnilam", "Schedar", "Gacrux", "Pulcherrima", "Achird", "Zubenelgenubi",
    "Vindemiatrix", "Sadachbia", "Sadaltager", "Sulafat",
}


def get_proxies() -> dict | None:
    if not PROXY_URL:
        return None
    return {"http": PROXY_URL, "https": PROXY_URL}


def get_client() -> genai.Client:
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY is not set on the server")
    return genai.Client(api_key=GEMINI_API_KEY)


class TranscriptRequest(BaseModel):
    url: str


class TTSRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)
    voice: str = DEFAULT_VOICE


def extract_video_id(url: str) -> str:
    patterns = [
        r"(?:v=|/)([0-9A-Za-z_-]{11}).*",
        r"(?:youtu\.be/)([0-9A-Za-z_-]{11})",
        r"(?:embed/)([0-9A-Za-z_-]{11})",
        r"^([0-9A-Za-z_-]{11})$",
    ]
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    raise ValueError("Invalid YouTube URL")


def pcm_to_wav(pcm_data: bytes, sample_rate: int = 24000, channels: int = 1, sample_width: int = 2) -> bytes:
    data_size = len(pcm_data)
    byte_rate = sample_rate * channels * sample_width
    block_align = channels * sample_width
    chunk_size = 36 + data_size
    header = struct.pack(
        "<4sI4s4sIHHIIHH4sI",
        b"RIFF",
        chunk_size,
        b"WAVE",
        b"fmt ",
        16,
        1,
        channels,
        sample_rate,
        byte_rate,
        block_align,
        sample_width * 8,
        b"data",
        data_size,
    )
    return header + pcm_data


def _entry_to_dict(entry) -> dict:
    if isinstance(entry, dict):
        return {
            "text": entry.get("text", ""),
            "start": float(entry.get("start", 0)),
            "duration": float(entry.get("duration", 0)),
        }
    return {
        "text": getattr(entry, "text", "") or "",
        "start": float(getattr(entry, "start", 0) or 0),
        "duration": float(getattr(entry, "duration", 0) or 0),
    }


def fetch_via_api(video_id: str) -> list:
    preferred = ["fa", "en", "en-US", "en-GB"]
    proxies = get_proxies()
    if proxies:
        logger.info("youtube-transcript-api via proxy %s", PROXY_URL)

    try:
        raw = YouTubeTranscriptApi.get_transcript(
            video_id, languages=preferred, proxies=proxies
        )
        items = [_entry_to_dict(e) for e in raw]
        if items:
            return items
    except TranscriptsDisabled:
        raise
    except NoTranscriptFound:
        pass
    except Exception as e:
        logger.warning("api get_transcript failed: %s", e)

    try:
        listing = YouTubeTranscriptApi.list_transcripts(video_id, proxies=proxies)
        for code in preferred:
            try:
                t = listing.find_transcript([code])
                items = [_entry_to_dict(e) for e in t.fetch()]
                if items:
                    return items
            except Exception:
                continue
        for t in listing:
            try:
                items = [_entry_to_dict(e) for e in t.fetch()]
                if items:
                    return items
            except Exception as e:
                logger.warning("api fetch lang failed: %s", e)
    except TranscriptsDisabled:
        raise
    except Exception as e:
        logger.warning("api list_transcripts failed: %s", e)
        raise

    raise NoTranscriptFound(video_id)


def fetch_via_ytdlp(video_id: str) -> list:
    """Fallback: yt-dlp can pull official/auto captions more reliably."""
    url = f"https://www.youtube.com/watch?v={video_id}"
    env = os.environ.copy()
    if PROXY_URL:
        # yt-dlp respects these
        env["ALL_PROXY"] = PROXY_URL
        env["HTTPS_PROXY"] = PROXY_URL
        env["HTTP_PROXY"] = PROXY_URL
        logger.info("yt-dlp via proxy %s", PROXY_URL)

    with tempfile.TemporaryDirectory() as td:
        outtmpl = os.path.join(td, "subs")
        cmd = [
            "yt-dlp",
            "--skip-download",
            "--write-auto-sub",
            "--write-sub",
            "--sub-langs",
            "fa.*,en.*,en",
            "--sub-format",
            "json3/vtt/srt/best",
            "--convert-subs",
            "vtt",
            "-o",
            outtmpl,
            url,
        ]
        if PROXY_URL:
            cmd.extend(["--proxy", PROXY_URL])

        try:
            proc = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=90,
                env=env,
            )
        except subprocess.TimeoutExpired:
            raise RuntimeError("yt-dlp timeout")

        if proc.returncode != 0:
            logger.warning("yt-dlp stderr: %s", proc.stderr[-800:] if proc.stderr else "")
            raise RuntimeError(proc.stderr[-400:] if proc.stderr else "yt-dlp failed")

        # find any .vtt
        vtts = [f for f in os.listdir(td) if f.endswith(".vtt")]
        if not vtts:
            # try json3
            jsons = [f for f in os.listdir(td) if "json" in f]
            if jsons:
                return _parse_json3(os.path.join(td, jsons[0]))
            raise NoTranscriptFound(video_id)

        # prefer fa then en
        vtts_sorted = sorted(
            vtts,
            key=lambda n: (0 if "fa" in n else 1 if "en" in n else 2, n),
        )
        return _parse_vtt(os.path.join(td, vtts_sorted[0]))


def _parse_vtt(path: str) -> list:
    items = []
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()

    # simple WebVTT parser
    blocks = re.split(r"\n\n+", content)
    time_re = re.compile(
        r"(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})"
    )
    for block in blocks:
        lines = [ln.strip() for ln in block.splitlines() if ln.strip()]
        if not lines:
            continue
        m = None
        text_lines = []
        for ln in lines:
            m2 = time_re.search(ln)
            if m2:
                m = m2
                continue
            if ln.startswith("WEBVTT") or ln.isdigit() or ln.startswith("NOTE"):
                continue
            # strip tags
            clean = re.sub(r"<[^>]+>", "", ln).strip()
            if clean:
                text_lines.append(clean)
        if not m or not text_lines:
            continue
        start = (
            int(m.group(1)) * 3600
            + int(m.group(2)) * 60
            + int(m.group(3))
            + int(m.group(4)) / 1000.0
        )
        end = (
            int(m.group(5)) * 3600
            + int(m.group(6)) * 60
            + int(m.group(7))
            + int(m.group(8)) / 1000.0
        )
        text = " ".join(text_lines)
        # skip duplicate consecutive
        if items and items[-1]["text"] == text:
            continue
        items.append({"text": text, "start": start, "duration": max(0.01, end - start)})
    if not items:
        raise NoTranscriptFound("empty vtt")
    return items


def _parse_json3(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    items = []
    for ev in data.get("events", []):
        segs = ev.get("segs") or []
        text = "".join(s.get("utf8", "") for s in segs).strip()
        if not text or text == "\n":
            continue
        start = float(ev.get("tStartMs", 0)) / 1000.0
        dur = float(ev.get("dDurationMs", 0)) / 1000.0
        items.append({"text": text, "start": start, "duration": dur})
    if not items:
        raise NoTranscriptFound("empty json3")
    return items


def fetch_transcript(video_id: str) -> list:
    last_err = None

    # 1) youtube-transcript-api
    try:
        return fetch_via_api(video_id)
    except TranscriptsDisabled as e:
        last_err = e
        logger.warning("api: transcripts disabled, trying yt-dlp")
    except Exception as e:
        last_err = e
        logger.warning("api failed: %s — trying yt-dlp", e)

    # 2) yt-dlp fallback
    try:
        return fetch_via_ytdlp(video_id)
    except Exception as e:
        logger.warning("yt-dlp failed: %s", e)
        if isinstance(last_err, TranscriptsDisabled):
            raise last_err
        raise RuntimeError(f"api={last_err}; ytdlp={e}")


@app.get("/")
def root():
    return {
        "status": "VoxTube backend running",
        "version": "0.2.3",
        "tts_model": TTS_MODEL,
        "gemini_configured": bool(GEMINI_API_KEY),
        "proxy_configured": bool(PROXY_URL),
    }


@app.get("/health")
def health():
    return {
        "ok": True,
        "gemini_configured": bool(GEMINI_API_KEY),
        "proxy_configured": bool(PROXY_URL),
        "proxy_url": PROXY_URL or None,
        "version": "0.2.3",
    }


@app.post("/transcript")
def get_transcript(req: TranscriptRequest):
    try:
        video_id = extract_video_id(req.url)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    try:
        transcript = fetch_transcript(video_id)
    except TranscriptsDisabled:
        raise HTTPException(status_code=403, detail="زیرنویس برای این ویدیو غیرفعال است")
    except NoTranscriptFound:
        raise HTTPException(status_code=404, detail="زیرنویسی برای این ویدیو پیدا نشد")
    except VideoUnavailable:
        raise HTTPException(status_code=404, detail="ویدیو در دسترس نیست")
    except Exception as e:
        logger.exception("transcript failed")
        raise HTTPException(status_code=500, detail=str(e)[:500])

    return {"video_id": video_id, "transcript": transcript}


@app.post("/tts")
def text_to_speech(req: TTSRequest):
    voice = req.voice if req.voice in VALID_VOICES else DEFAULT_VOICE
    text = req.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="text is empty")

    try:
        client = get_client()
        prompt = (
            "Speak the following text clearly and naturally. "
            "If the text is Persian, use a natural Persian accent and pacing.\n\n"
            f"{text}"
        )

        response = client.models.generate_content(
            model=TTS_MODEL,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_modalities=["AUDIO"],
                speech_config=types.SpeechConfig(
                    voice_config=types.VoiceConfig(
                        prebuilt_voice_config=types.PrebuiltVoiceConfig(
                            voice_name=voice
                        )
                    )
                ),
            ),
        )

        part = response.candidates[0].content.parts[0]
        if not part.inline_data or not part.inline_data.data:
            raise HTTPException(status_code=502, detail="Gemini returned empty audio")

        raw = part.inline_data.data
        mime = (part.inline_data.mime_type or "").lower()

        if "l16" in mime or "pcm" in mime or mime == "" or not mime.startswith("audio/wav"):
            audio_bytes = pcm_to_wav(raw)
            out_mime = "audio/wav"
        else:
            audio_bytes = raw
            out_mime = part.inline_data.mime_type or "audio/wav"

        return {
            "audio_base64": base64.b64encode(audio_bytes).decode("utf-8"),
            "mime_type": out_mime,
            "voice": voice,
            "model": TTS_MODEL,
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("tts failed")
        raise HTTPException(status_code=500, detail=f"TTS error: {e}")
