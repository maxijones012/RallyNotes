#!/usr/bin/env python3
"""Produce the redistributable CarlFM OGG fragments shipped in Rally GPS AI.

Build-time dependency: piper-tts==1.8.0, ffmpeg (neither shipped in APK).
Pinned model: rhasspy/piper-voices v1.0.0, es_ES/carlfm/x_low (MIT collection,
public-domain training data, trained from scratch).
"""
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import wave
from pathlib import Path

VOICE_BASE = 'https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/es/es_ES/carlfm/x_low/'
MODEL_NAME = 'es_ES-carlfm-x_low.onnx'
EXPECTED_SHA256 = 'd69677323a907cd4963f42b29c20a98b5d6bfa7f3e64df339915e4650c00d125'
ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / 'app/src/main/res/raw'
CACHE = ROOT / '.voice-build-cache'
NUMBERS = {1: 'uno', 2: 'dos', 3: 'tres', 4: 'cuatro', 5: 'cinco', 6: 'seis'}


def spanish_number(num: int) -> str:
    small = ('cero', 'uno', 'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete',
             'ocho', 'nueve', 'diez', 'once', 'doce', 'trece', 'catorce',
             'quince', 'dieciséis', 'diecisiete', 'dieciocho', 'diecinueve')
    if num < 20:
        return small[num]
    tens = ('', '', 'veinte', 'treinta', 'cuarenta', 'cincuenta', 'sesenta',
            'setenta', 'ochenta', 'noventa')
    if num < 100:
        return tens[num // 10] if num % 10 == 0 else f'{tens[num//10]} y {small[num%10]}'
    if num == 100:
        return 'cien'
    if num < 200:
        return f'ciento {spanish_number(num-100)}'
    if num < 300:
        return 'doscientos' if num == 200 else f'doscientos {spanish_number(num-200)}'
    if num < 400:
        return 'trescientos' if num == 300 else f'trescientos {spanish_number(num-300)}'
    return 'cuatrocientos'


def required_text() -> dict[str, str]:
    result = {
        'en': 'En', 'metros': 'metros', 'ahora': 'Ahora', 'larga': 'larga',
        'enlaza': 'enlaza', 'horquilla_izquierda': 'horquilla izquierda',
        'horquilla_derecha': 'horquilla derecha',
    }
    for grade, name in NUMBERS.items():
        result[f'izquierda_{grade}'] = f'izquierda {name}'
        result[f'derecha_{grade}'] = f'derecha {name}'
        result[f'se_cierra_a_{grade}'] = f'se cierra a {name}'
        result[f'abre_a_{grade}'] = f'abre a {name}'
    for distance in range(20, 401, 10):
        result[f'dist_{distance}'] = spanish_number(distance)
    assert len(result) == 70, len(result)
    return result


def download(file_name: str, expected_sha: str | None = None) -> Path:
    CACHE.mkdir(parents=True, exist_ok=True)
    dest = CACHE / file_name
    if dest.is_file() and (not expected_sha or hashlib.sha256(dest.read_bytes()).hexdigest() == expected_sha):
        return dest
    req = urllib.request.Request(VOICE_BASE + file_name, headers={'User-Agent': 'Rally-GPS-AI-voice-builder/0.2.4'})
    with urllib.request.urlopen(req, timeout=150) as resp, dest.open('wb') as out:
        shutil.copyfileobj(resp, out)
    if expected_sha and hashlib.sha256(dest.read_bytes()).hexdigest() != expected_sha:
        dest.unlink(missing_ok=True)
        raise ValueError('SHA256 de CarlFM incorrecto: se canceló la compilación')
    return dest


def main() -> None:
    from piper import PiperVoice
    RAW.mkdir(parents=True, exist_ok=True)
    if not shutil.which('ffmpeg'):
        raise RuntimeError('Se requiere ffmpeg para comprimir clips')
    model = download(MODEL_NAME, EXPECTED_SHA256)
    download(MODEL_NAME + '.json')
    voice = PiperVoice.load(str(model))
    phrases = required_text()
    with tempfile.TemporaryDirectory() as scratch:
        temp = Path(scratch)
        for i, (token, phrase) in enumerate(phrases.items(), 1):
            wav_path = temp / (token + '.wav')
            with wave.open(str(wav_path), 'wb') as wav:
                voice.synthesize_wav(phrase, wav)
            for variant in ('clear', 'radio'):
                ogg = RAW / f'rally_carlfm_{variant}_{token}.ogg'
                audio_filter = ('highpass=f=180,lowpass=f=3700,acompressor=threshold=-23dB:ratio=3:'
                                'attack=8:release=80,atempo=1.06,volume=1.25') if variant == 'radio' else 'volume=1.0'
                subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y',
                                '-i', str(wav_path), '-af', audio_filter, '-c:a', 'libvorbis',
                                '-q:a', '2', str(ogg)], check=True)
                if ogg.stat().st_size < 250:
                    raise ValueError(f'Audio vacío: {ogg.name}')
            if i % 10 == 0:
                print(f'CarlFM: {i}/{len(phrases)} frases, dos perfiles')
    built = list(RAW.glob('rally_carlfm_*.ogg'))
    expected = {f'rally_carlfm_{mode}_{key}.ogg' for key in phrases for mode in ('clear', 'radio')}
    actual = {p.name for p in built}
    if not expected.issubset(actual):
        raise ValueError(f'Faltan clips: {sorted(expected - actual)[:10]}')
    print(f'Pack listo: {len(expected)} segmentos OGG, total {sum(p.stat().st_size for p in built)//1024} KiB')

if __name__ == '__main__':
    main()
