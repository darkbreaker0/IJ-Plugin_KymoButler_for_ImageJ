# Installation

This plugin runs KymoButler locally using Wolfram Engine and requires ImageJ/Fiji plus Java.

## Requirements

- Fiji or ImageJ (tested with Fiji)
- Java JDK (for building the plugin)
- Wolfram Engine + `wolframscript`
- Local KymoButler repo with `packages/KymoButler.wl`

## Install Wolfram Engine

Download and install Wolfram Engine from:

- https://www.wolfram.com/engine/

After installation, ensure `wolframscript` is available on your PATH or note the full path for the plugin options.

## Install the plugin JAR

A prebuilt JAR is available on the GitHub Releases page. You can optionally build it from source.

1) Build the JAR (see below).
2) Copy the JAR to your ImageJ/Fiji plugins folder:
   - `Fiji.app/plugins/KymoButler4IJ_.jar`
3) Restart ImageJ/Fiji.

Download:
- Plugin JAR: https://github.com/darkbreaker0/IJ-Plugin_KymoButler_for_ImageJ/releases/latest
- KymoButler (local models and Wolfram code): https://github.com/MaxJakobs/KymoButler

## Dependencies

This local-only plugin requires:

- `commons-io-2.17.0.jar`
- `json-20240303.jar`

Fiji already ships these (or compatible newer versions) in `Fiji.app/jars`.
The plugin accepts both legacy names (for old installs) and current Fiji jar names in either:

- `Fiji.app/jars`
- `Fiji.app/plugins/jars`

## Build from source (Windows PowerShell)

Adjust paths as needed:

```powershell
$imgj = "C:\Users\user-adm\Desktop\Fiji.app"
$src  = "C:\Users\user-adm\Desktop\Apps\IJ-Plugin_KymoButler_for_ImageJ\KymoButler_\src"
$out  = "$src\build"
$jdk  = "C:\Path\To\JDK"

New-Item -ItemType Directory -Force -Path $out | Out-Null

$cp = @(
  "$imgj\jars\ij-1.53t.jar",
  "$imgj\jars\commons-io-2.17.0.jar",
  "$imgj\jars\json-20240303.jar"
) -join ";"

Get-ChildItem -Recurse -Filter *.java $src |
  ForEach-Object { $_.FullName -replace '\\','/' } |
  ForEach-Object { '"' + $_ + '"' } |
  Set-Content "$out\sources.txt"

& "$jdk\bin\javac.exe" -encoding UTF-8 -classpath $cp -d $out "@$out\sources.txt"
Copy-Item -Path "$src\plugins.config" -Destination "$out\plugins.config" -Force

$jarOut = "$imgj\plugins\KymoButler4IJ_.jar"
& "$jdk\bin\jar.exe" -cf $jarOut -C $out .
```

Restart Fiji after building.

## Configure the plugin

In Fiji, open `Plugins > KymoButler for ImageJ > Options`:

- Local mode is always enabled
- `WolframScript_path` = `wolframscript` or full path
- `KymoButler_local_path` = folder containing `packages/KymoButler.wl`
- `Local_output_directory` = base output folder (overridden by image folder if available)
- `Target_device` = `GPU` or `CPU`
- `PProc_use_physical_units` = on/off

If the local path is invalid, the plugin will warn you.
