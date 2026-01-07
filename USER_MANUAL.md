# User Manual

## Overview

The plugin runs KymoButler locally (Wolfram Engine) and writes results next to the input image (if the image has a file path). It supports single-image analysis and batch processing of folders.

Menu entries:

- `Plugins > KymoButler for ImageJ > KymoButler Analyse`
- `Plugins > KymoButler for ImageJ > Improve Kymo`
- `Plugins > KymoButler for ImageJ > KymoButler Options`

## Improve Kymo

`Improve Kymo` applies a wavelet-like enhancement using successive Gaussian blur differences.
The implementation is derived from `Improve_Kymo.java` in KymoToolBox.

- **Start**: smallest blur scale (>= 1).
- **Stop**: largest blur scale.

Higher Stop values include coarser structures; lower values emphasize fine detail.

## Options

Open `Plugins > KymoButler for ImageJ > Options` and configure:

- **Use_local_Wolfram_Engine**: enable local processing.
- **WolframScript_path**: path to `wolframscript`.
- **KymoButler_local_path**: folder containing `packages/KymoButler.wl`.
- **Local_output_directory**: base folder for outputs.
- **Target_device**: `GPU` or `CPU`.
- **PProc_use_physical_units**: if enabled, postprocessing uses calibrated units.

Note: If the input image has a valid on-disk path, outputs are written to that image's folder regardless of `Local_output_directory`.

## Single-image analysis

1) Open a kymograph (time on Y, space on X).
2) `Plugins > KymoButler for ImageJ > KymoButler Analyse`.
3) Set parameters:
   - **Threshold** (default 0.2)
   - **Minimum size** (default 3)
   - **Minimum frames** (default 3)
   - **Use bidirectional model** (optional)
   - **Bidirectional decision threshold** (default 0.5)
4) Output options:
   - Add tracks to ROI Manager
   - Simplify tracks
   - Show kymograph / overlay
   - Open local output tables

Parameter notes:

- **Threshold**: probability cutoff for accepting a pixel as part of a track. Higher values are stricter (fewer detections).
- **Minimum size**: minimum object size (in pixels) required for a detection to be kept.
- **Minimum frames**: minimum number of consecutive time points for a track to be kept.
- **Bidirectional vs. unidirectional**: unidirectional outputs separate anterograde and retrograde tracks; bidirectional uses a single model that tracks both directions in one pass and labels tracks as bidirectional.

## Batch analysis

1) `Plugins > KymoButler for ImageJ > KymoButler Analyse`.
2) Enable **Batch_mode (folder)**.
3) Optionally enable:
   - **Batch_include_subfolders**
   - **Batch_show_outputs** (show images/tables during batch)
4) Choose the root folder when prompted.

The plugin attempts to open every file with ImageJ. Unsupported formats are skipped with a log message.

## Output files

Each run creates a folder:

`KymoButlerLocal_YYYY-MM-DD_HH-mm-ss_<image_name>`

Contents:

- `<image>_overlay.tif`: overlay image
- `<image>_tracks_long.csv`: track points
- `<image>_pproc_table.csv`: postprocessing table
- `<image>_pproc_hist_v.png`: velocity histogram
- `<image>_pproc_hist_t.png`: duration histogram
- `<image>_pproc_hist_dist.png`: distance histogram
- `<image>_response.json`: internal JSON response

## Track CSV format

`<image>_tracks_long.csv` contains one point per row:

```
track_id,t,x,dir,t_phys,x_phys
```

- `t`, `x`: pixel coordinates (time row, spatial column)
- `dir`: `anterograde`, `retrograde`, or `bidirectional`
- `t_phys`, `x_phys`: calibrated units using ImageJ calibration (frame interval, pixel width)

## Postprocessing table

The postprocessing table comes from `KymoButlerPProc.wl` and includes:

- Direction
- Average velocity
- Track duration
- Total distance
- Start-to-end velocity

If `PProc_use_physical_units` is off, values are in pixels and frames.
