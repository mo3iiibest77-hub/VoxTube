from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import TranscriptsDisabled, NoTranscriptFound
import google.generativeai as genai
import re
import os
import base64

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
genai.configure(api_key=GEMINI_API_KEY)

class TranscriptRequest(BaseModel):
    url: str

class TTSRequest(BaseModel):
    text: str
    voice: str = "Charon"

def extract_video_id(url: str) -> str:
    patterns = [
        r"(?:v=|\/)([0-9A-Za-z_-]{11})",
        r"(?:youtu\.be\/)([0-9A-Za-z_-]{11})",
    ]
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    raise ValueError("Invalid YouTube URL")

@app.get("/")
def root():
    return {"status": "VoxTube backend running"}

@app.post("/transcript")
def get_transcript(req: TranscriptRequest):
    try:
        video_id = extract_video_id(req.url)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    try:
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=["fa"])
    except NoTranscriptFound:
        try:
            transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=["en"])
        except Exception as e:
            raise HTTPException(status_code=404, detail=f"No transcript found: {str(e)}")
    except TranscriptsDisabled:
        raise HTTPException(status_code=403, detail="Transcripts disabled for this video")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    return {
        "video_id": video_id,
        "transcript": [
            {
                "text": entry["text"],
                "start": entry["start"],
                "duration": entry["duration"]
            }
            for entry in transcript
        ]
    }

@app.post("/tts")
def text_to_speech(req: TTSRequest):
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not set")

    try:
        client = genai.Client()
        response = client.models.generate_content(
            model="gemini-2.5-flash-preview-tts",
            contents=req.text,
            config=genai.types.GenerateContentConfig(
                response_modalities=["AUDIO"],
                speech_config=genai.types.SpeechConfig(
                    voice_config=genai.types.VoiceConfig(
                        prebuilt_voice_config=genai.types.PrebuiltVoiceConfig(
                            voice_name=req.voice
                        )
                    )
                )
            )
        )

        audio_data = response.candidates[0].content.parts[0].inline_data.data
        audio_b64 = base64.b64encode(audio_data).decode("utf-8")

        return {
            "audio_base64": audio_b64,
            "mime_type": "audio/wav"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
