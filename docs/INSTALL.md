# Julian Date Calc Add-In — build & install

A LibreOffice Calc add-in (UNO component, **Java**) exposing Julian date/calendar
conversions as worksheet formulas. Pure computation, no network access, no
third-party dependencies — see the top-level [README.md](../README.md) for the
function reference and worked examples.

## 1. Prerequisites

### Windows

1. **LibreOffice + SDK.** Default install path `C:\Program Files\LibreOffice`,
   with the SDK under `…\LibreOffice\sdk`. The SDK provides
   `sdk\bin\unoidl-write.exe` and `sdk\bin\javamaker.exe`.
2. **A JDK to build with** — any JDK 8 or newer (`javac`, `jar`). The build
   targets **Java 8 bytecode** (`--release 8`), so the add-in runs on the
   JRE LibreOffice accepts out of the box. Pass its path with `-Jdk`, or set
   `JAVA_HOME`, or put `javac` on `PATH`.
3. **Runtime JRE — nothing to change.** The component is Java-8 bytecode using
   only the JDK standard library (no I/O of any kind), so LibreOffice's
   existing/default JRE (8+) runs it as-is.

Confirm the tools resolve:

```powershell
& 'C:\Program Files\LibreOffice\sdk\bin\unoidl-write.exe' --help
& 'C:\Program Files\LibreOffice\sdk\bin\javamaker.exe'    # prints usage
javac -version   # any 8+
```

### Debian / Ubuntu

1. **LibreOffice + SDK** from the distro's repositories:

   ```bash
   sudo apt install libreoffice libreoffice-dev libreoffice-dev-common
   ```

   This lays out `program/` and `sdk/bin/` under a system prefix (typically
   `/usr/lib/libreoffice`); pass that as `--libreoffice` (or `LO_HOME`).

2. **A JDK 8**: `sudo apt install openjdk-8-jdk-headless` (or any 8+ JDK).

### Slackware (and any Linux without a packaged SDK)

Slackware doesn't package a separate LibreOffice SDK, so download the generic
Linux tarballs directly from
<https://download.documentfoundation.org/libreoffice/stable/> (the `_rpm.tar.gz`
main package and matching `_rpm_sdk.tar.gz` for your version/architecture) and
extract each `.rpm` inside them with `rpm2cpio`/`cpio` into a prefix directory
— no root required:

```bash
tar xzf LibreOffice_*_rpm.tar.gz LibreOffice_*_rpm_sdk.tar.gz
mkdir -p ~/opt
for rpm in LibreOffice_*_rpm/RPMS/*.rpm LibreOffice_*_rpm_sdk/RPMS/*.rpm; do
    rpm2cpio "$rpm" | cpio -idm --no-absolute-filenames -D ~/opt
done
mv ~/opt/opt/libreoffice* ~/libreoffice26.2   # adjust to the extracted version
export LO_HOME=~/libreoffice26.2
```

This lays out the same `program/` and `sdk/bin/` tree the Windows/Debian
installs have (`unoidl-write`, `javamaker`, `types.rdb`,
`program/classes/*.jar`). `rpm2cpio`/`cpio` are available on Slackware by
default; if not, `slackpkg install cpio rpm` (or build from source — both are
tiny, dependency-free C programs).

For a JDK 8, either use a Slackware package if you have one, or fetch a build
straight from Eclipse Adoptium/Temurin and unpack it under your home directory:

```bash
curl -s "https://api.adoptium.net/v3/assets/latest/8/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse" \
  | grep -o '"link": *"[^"]*tar.gz"' | head -1 | cut -d'"' -f4
# download that URL, then:
mkdir -p ~/jdks && tar xzf OpenJDK8U-jdk_x64_linux_hotspot_*.tar.gz -C ~/jdks
export JAVA_HOME=~/jdks/jdk8u<version>   # match the extracted directory name
export PATH="$JAVA_HOME/bin:$PATH"
```

### Java vendor allow-list (all Linux/macOS installs)

LibreOffice only loads a JVM whose `java.vendor` appears in
`$LO_HOME/program/javavendors.xml` (Sun, Oracle, IBM, Blackdown, BEA, Azul,
Amazon by default). A stock Temurin/Adoptium build reports vendor `Temurin`,
which is **not** on that list by default — `unopkg` will fail with
`CannotRegisterImplementationException: Could not create Java implementation
loader` when installing the extension. Add an entry for it in your local
`javavendors.xml` (this file lives inside your own LibreOffice install, not a
system-shared one, so editing it is safe):

```xml
<vendor name="Temurin">
  <minVersion>1.8.0</minVersion>
</vendor>
```

Insert it next to the other `<vendor>` entries, inside `<vendorInfos>`. (If
your JDK came from your distro's package manager, its vendor is usually
already on the list and this step is unnecessary.)

Confirm the tools resolve:

```bash
"$LO_HOME/sdk/bin/unoidl-write"          # prints usage
"$LO_HOME/sdk/bin/javamaker"             # prints usage
"$JAVA_HOME/bin/javac" -version          # any 8+
```

## 2. Build the .oxt

### Windows

From the project root:

```powershell
# JAVA_HOME set, or javac on PATH:
pwsh -File build.ps1
# or point at a specific JDK / LibreOffice install:
pwsh -File build.ps1 -LibreOffice 'C:\Program Files\LibreOffice' -Jdk 'C:\Program Files\Java\jdk-11'
```

This runs the full pipeline and produces **`build\JulianDate.oxt`**:

```
1. unoidl-write  idl\**                     -> build\types\XJulianDate.rdb
2. javamaker     build\types\XJulianDate.rdb -> build\gen\**.class
3. javac         src\**.java                -> build\classes\**.class
4. jar           classes + bindings         -> build\oxt\julian-date.jar
5. zip           staging tree               -> build\JulianDate.oxt
```

### Linux (Debian/Ubuntu, Slackware, or any distro)

```bash
export JAVA_HOME=~/jdks/jdk8u<version>   # or wherever your JDK 8 lives
export LO_HOME=~/libreoffice26.2         # or wherever LibreOffice + SDK live (e.g. /usr/lib/libreoffice)
./build.sh
# or pass paths explicitly instead of the env vars:
./build.sh --jdk ~/jdks/jdk8u<version> --libreoffice ~/libreoffice26.2
```

This produces `build/JulianDate.oxt` via the same five steps. Two JDK-8-specific
quirks it works around, in case you're compiling by hand:

- **`javac --release 8` doesn't exist on JDK 8 itself** (the flag was added in
  JDK 9); `build.sh` detects a `1.x` `javac -version` and falls back to
  `-source 8 -target 8`, which is equivalent for a straight JDK-8 build.
- **`jar` on JDK 8 can reject duplicate directory entries** when packaging two
  class trees that share a package path; `build.sh` merges both trees into one
  staging directory first, then jars that single tree.

## 3. Install into LibreOffice

Close LibreOffice first, then use `unopkg` from the program dir:

```powershell
& 'C:\Program Files\LibreOffice\program\unopkg.exe' add --force build\JulianDate.oxt
# list / remove:
& 'C:\Program Files\LibreOffice\program\unopkg.exe' list
& 'C:\Program Files\LibreOffice\program\unopkg.exe' remove com.example.juliandate
```

You can also install by double-clicking `build\JulianDate.oxt` (opens the
Extension Manager). Restart LibreOffice afterwards.

Linux:

```bash
"$LO_HOME/program/unopkg" add --force build/JulianDate.oxt
# list / remove:
"$LO_HOME/program/unopkg" list
"$LO_HOME/program/unopkg" remove com.example.juliandate
```

## 4. Try it

In any sheet:

```
=TO_JULIAN_DAY(TODAY())
=TO_JULIAN_ORDINAL(TODAY())
=GREGORIAN_TO_JULIAN(DATE(1582;10;15))     -> displays as 1582-10-05
```

See the top-level [README.md](../README.md) function reference table for
worked examples of every function.

## Automated test

`tools/test_julian.py` drives a headless LibreOffice over a UNO socket and
checks all 10 functions plus round-trip and error-path behavior:

```powershell
& 'C:\Program Files\LibreOffice\program\soffice.exe' --headless --norestore --accept="socket,host=localhost,port=2002;urp;"
& 'C:\Program Files\LibreOffice\program\python.exe' tools\test_julian.py   # prints RESULT: PASS
```

Linux — same idea, LibreOffice's bundled `python` (it ships the `uno` module)
instead of `python.exe`, run in the background so the shell isn't blocked:

```bash
"$LO_HOME/program/soffice" --headless --norestore --accept="socket,host=localhost,port=2002;urp;" &
"$LO_HOME/program/python" tools/test_julian.py   # prints RESULT: PASS
```

## Troubleshooting

- `unoidl-write` / `javamaker` "not found" → pass the right
  `--libreoffice`/`-LibreOffice` path; the SDK must be installed (it's a
  separate package/download from LibreOffice itself on most platforms).
- Functions show `#NAME?` → the extension isn't registered; confirm with
  `unopkg list` and restart LibreOffice.
- A function cell shows an error value (e.g. `Err:502`) → the argument was
  rejected (bad `digits`, out-of-range ordinal day-of-year, etc.) — see the
  function reference in the top-level README for each function's valid input
  range.
- `unopkg add` fails with `CannotRegisterImplementationException: Could not
  create Java implementation loader` (Linux/macOS) → your JDK's vendor isn't
  in `$LO_HOME/program/javavendors.xml`'s allow-list — see the Java vendor
  allow-list note in Prerequisites.
