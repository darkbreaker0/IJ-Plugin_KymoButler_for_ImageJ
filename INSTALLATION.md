# Installation

This plugin runs KymoButler locally using Wolfram Engine and requires ImageJ/Fiji plus Java.

## Requirements

- Fiji or ImageJ (tested with Fiji)
- Java JDK (for building the plugin)
- Wolfram Engine + `wolframscript`
- Local KymoButler repo with `packages/KymoButler.wl`

## Install the plugin JAR

1) Build the JAR (see below).
2) Copy the JAR to your ImageJ/Fiji plugins folder:
   - `Fiji.app/plugins/KymoButler4IJ_.jar`
3) Restart ImageJ/Fiji.

Download:
- Plugin JAR: https://github.com/darkbreaker0/IJ-Plugin_KymoButler_for_ImageJ/releases/latest
- KymoButler (local models and Wolfram code): https://github.com/MaxJakobs/KymoButler

## Dependencies

The plugin uses the following JARs. Fiji already ships newer versions in `Fiji.app/jars`, but the plugin checks legacy filenames in `plugins/jars`.

Fiji already ships newer versions under `Fiji.app/jars` and the plugin now accepts those names. The following versions are known to work:

- `commons-io-2.17.0.jar`
- `commons-logging-1.3.4.jar`
- `commons-codec-1.17.1.jar`
- `httpclient-4.5.14.jar`
- `httpcore-4.4.16.jar`
- `httpmime-4.5.14.jar`
- `json-20240303.jar`

If you install to `Fiji.app/plugins/jars`, either the newer names above or the older legacy names will be accepted.

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
  "$imgj\jars\commons-logging-1.3.4.jar",
  "$imgj\jars\commons-codec-1.17.1.jar",
  "$imgj\jars\httpclient-4.5.14.jar",
  "$imgj\jars\httpcore-4.4.16.jar",
  "$imgj\jars\httpmime-4.5.14.jar",
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

- `Use_local_Wolfram_Engine` = checked
- `WolframScript_path` = `wolframscript` or full path
- `KymoButler_local_path` = folder containing `packages/KymoButler.wl`
- `Local_output_directory` = base output folder (overridden by image folder if available)
- `Target_device` = `GPU` or `CPU`
- `PProc_use_physical_units` = on/off

If the local path is invalid, the plugin will warn you.
