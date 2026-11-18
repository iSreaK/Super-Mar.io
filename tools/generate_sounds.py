"""Petits effets synthétisés originaux. Python standard uniquement ; aucun asset distant."""
import math
import pathlib
import struct
import wave

OUT = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
OUT.mkdir(parents=True, exist_ok=True)
RATE = 22050

def save(name, notes):
    samples = []
    for start, end, duration in notes:
        count = int(RATE * duration)
        phase = 0.0
        for i in range(count):
            phase += (start + (end - start) * i / count) / RATE
            envelope = min(1, i / 100) * min(1, (count - i) / 200)
            value = (1 if phase % 1 < 0.4 else -1) * envelope * 0.20
            samples.append(struct.pack('<h', int(value * 32767)))
    with wave.open(str(OUT / (name + '.wav')), 'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(RATE)
        f.writeframes(b''.join(samples))

save('jump', [(230, 700, .12)])
save('coin', [(950, 950, .06), (1450, 1450, .12)])
save('stomp', [(180, 60, .08)])
save('hurt', [(420, 110, .24), (110, 50, .18)])
save('power', [(n, n, .07) for n in [330, 440, 550, 660, 880]])
save('finish', [(n, n, .12) for n in [523, 659, 784, 1047, 784, 1047]])
print(f'6 effets générés dans {OUT}')
