# DeepFilterNet3 reference pair

- `speech-and-music.wav`: 12.000 s, 48 kHz, mono, 16-bit. A talker mixed with music at 0 dB SNR.
  - Talker: LibriVox recording of *Cleek of Scotland Yard* by Thomas W. Hanshew, chapter 6, read by
    Ruth Golding, [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)
    ([archive.org item](https://archive.org/details/cleek_scotland_yard_rg_librivox)).
  - Music: "Where You Are Now" by Alex Beroza (c) 2011, ft. WolfFly, vocals by Rhiannon Delpero,
    [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
    ([ccMixter](https://ccmixter.org/files/AlexBeroza/33065), a cappella by ascension on
    [ccMixter](https://ccmixter.org/files/ascension/27734)). Cut and mixed, not otherwise changed.
- `speech-and-music-reference.wav`: the DeepFilterNet3 output of the upstream Python package
  `deepfilternet==0.5.6` (`df.enhance.enhance`, default settings), 16-bit.

Rebuild the reference in a Python 3.11 venv with `deepfilternet==0.5.6 "torch<2.6" "torchaudio<2.6" soundfile`:

```python
import soundfile, torch
from df.enhance import enhance, init_df

model, state, _ = init_df(model_base_dir="DeepFilterNet3")
samples, rate = soundfile.read("speech-and-music.wav", dtype="float32")
enhanced = enhance(model, state, torch.from_numpy(samples[None, :].copy()))
soundfile.write("speech-and-music-reference.wav", enhanced[0].numpy(), rate, subtype="PCM_16")
```
