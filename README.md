# KymoButler for ImageJ (Local Wolfram model)

This fork runs KymoButler locally through Wolfram Engine instead of relying on the discontinued cloud API. It provides:

- Local AI analysis (CPU/GPU) via `KymoButler.wl`
- Batch processing of folders (including subfolders)
- Overlay images + ROI tracks + postprocessing outputs
- Improve Kymo filter (wavelet-like enhancement) built in

Original KymoButler is a Wolfram-based kymograph analysis tool by Max Jakobs et al.:
https://github.com/MaxJakobs/KymoButler

Original paper:
https://elifesciences.org/articles/42288

For installation, see `INSTALLATION.md`. For usage and outputs, see `USER_MANUAL.md`.

## Credits / Acknowledgements

This fork incorporates the Improve Kymo functionality from the
[KymoToolBox](https://github.com/fabricecordelieres/IJ-Plugin_KymoToolBox)
ImageJ plugin by Fabrice P. Cordelieres (GPL-3.0).

KymoToolBox reference:
Zala D. et al., "Vesicular glycolysis provides on-board energy for fast
axonal transport", Cell 152(3):479-491 (2013).
