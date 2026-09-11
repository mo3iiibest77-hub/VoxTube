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

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("voxtube")

app = FastAPI(title="VoxTube Backend", version="0.2.1")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "").strip()
TTS_MODEL = os.environ.get("TTS_MODEL", "gemini-2.5-flash-preview-tts")
DEFAULT_VOICE = os.environ.get("TTS_VOICE", "Charon")

VALID_VOICES = {
    "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Aoede",
    "Callirrhoe", "Autonoe", "Enceladus", "Iapetus", "Umbriel", "Algieba",
    "Despina", "Erinome", "Algenib", "Rasalgethi", "Laomedeia", "Achernar",
    "Alnilam", "Schedar", "Gacrux", "Pulcherrima", "Achird", "Zubenelgenubi",
    "Vindemiatrix", "Sadachbia", "Sadaltager", "Sulafat",
}


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
    """Support both dict-style and object-style transcript entries."""
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


def fetch_transcript(video_id: str) -> list:
    """Try several strategies to get captions."""
    preferred = ["fa", "en", "en-US", "en-GB"]

    # 1) Direct preferred languages
    try:
        raw = YouTubeTranscriptApi.get_transcript(video_id, languages=preferred)
        items = [_entry_to_dict(e) for e in raw]
        if items:
            return items
    except NoTranscriptFound:
        pass
    except TranscriptsDisabled:
        raise
    except Exception as e:
        logger.warning("get_transcript preferred failed: %s", e)

    # 2) List all and pick best
    try:
        listing = YouTubeTranscriptApi.list_transcripts(video_id)
        # manual preferred first
        for code in preferred:
            try:
                t = listing.find_transcript([code])
                raw = t.fetch()
                items = [_entry_to_dict(e) for e in raw]
                if items:
                    return items
            except Exception:
                continue
        # any generated/manual
        for t in listing:
            try:
                raw = t.fetch()
                items = [_entry_to_dict(e) for e in raw]
                if items:
                    return items
            except Exception as e:
                logger.warning("fetch transcript %s failed: %s", getattr(t, "language_code", "?"), e)
                continue
    except TranscriptsDisabled:
        raise
    except Exception as e:
        logger.warning("list_transcripts failed: %s", e)
        raise

    raise NoTranscriptFound(video_id)


@app.get("/")
def root():
    return {
        "status": "VoxTube backend running",
        "version": "0.2.1",
        "tts_model": TTS_MODEL,
        "gemini_configured": bool(GEMINI_API_KEY),
    }


@app.get("/health")
def health():
    return {"ok": True, "gemini_configured": bool(GEMINI_API_KEY)}


@app.post("/transcript")
def get_transcript(req: TranscriptRequest):
    try:
        video_id = extract_video_id(req.url)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    try:
        transcript = fetch_transcript(video_id)
    except TranscriptsDisabled:
        raise HTTPException(
            status_code=403,
            detail="زیرنویس برای این ویدیو غیرفعال است",
        )
    except NoTranscriptFound:
        raise HTTPException(
            status_code=404,
            detail="زیرنویسی برای این ویدیو پیدا نشد",
        )
    except VideoUnavailable:
        raise HTTPException(status_code=404, detail="ویدیو در دسترس نیست")
    except CouldNotRetrieveTranscript as e:
        raise HTTPException(
            status_code=502,
            detail=f"یوتیوب زیرنویس را برنگرداند (ممکن است IP سرور بلاک باشد): {e}",
        )
    except Exception as e:
        logger.exception("transcript failed")
        msg = str(e)
        if "no element found" in msg.lower() or "ParseError" in msg:
            raise HTTPException(
                status_code=502,
                detail="یوتیوب پاسخ خالی داد — IP سرور احتمالاً توسط یوتیوب محدود شده",
            )
        raise HTTPException(status_code=500, detail=msg)

    return {
        "video_id": video_id,
        "transcript": transcript,
    }


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
