# java_julian_date

[![Latest release](https://img.shields.io/github/v/release/davidjayjackson/java_julian_date)](https://github.com/davidjayjackson/java_julian_date/releases/latest)

A free, open-source LibreOffice Calc add-in (UNO component, **Java**) that
converts between calendar dates and both common meanings of "Julian date":

| Function | Signature | Returns |
|---|---|---|
| `TO_JULIAN_DAY` | `TO_JULIAN_DAY(date)` | Julian Day Number (integer) |
| `TO_JULIAN_DATE` | `TO_JULIAN_DATE(date)` | Julian Date, with time-of-day fraction |
| `FROM_JULIAN_DAY` | `FROM_JULIAN_DAY(jdn)` | Calc date serial |
| `FROM_JULIAN_DATE` | `FROM_JULIAN_DATE(jd)` | Calc date/time serial |
| `TO_MJD` | `TO_MJD(date)` | Modified Julian Date (JD − 2400000.5) |
| `FROM_MJD` | `FROM_MJD(mjd)` | Calc date/time serial |
| `TO_JULIAN_ORDINAL` | `TO_JULIAN_ORDINAL(date; [digits])` | ordinal day-of-year, e.g. `2025015` (default) or `25015` |
| `FROM_JULIAN_ORDINAL` | `FROM_JULIAN_ORDINAL(ordinal)` | Calc date serial (auto-detects YYDDD vs YYYYDDD) |
| `GREGORIAN_TO_JULIAN` | `GREGORIAN_TO_JULIAN(date)` | Calc date serial, relabeled onto the proleptic Julian calendar |
| `JULIAN_TO_GREGORIAN` | `JULIAN_TO_GREGORIAN(date)` | Calc date serial, relabeled onto the proleptic Gregorian calendar |

See **[docs/FUNCTIONS.md](docs/FUNCTIONS.md)** for the complete reference —
every argument, error conditions, epoch/offset handling, calendar-reform
caveats, and worked examples for all 10 functions.

> In Calc's UI, arguments are separated by **semicolons**:
> `=TO_JULIAN_ORDINAL(A1; 5)`.

**Just want to install and use it?** See
**[docs/HOWTO.md](docs/HOWTO.md)** — download the pre-built
[v1.0.1 release](https://github.com/davidjayjackson/java_julian_date/releases/tag/v1.0.1),
install it, and try it on a real worked example
(`demo/juliandate_demo.ods`, real AAVSO variable-star photometry data). No build
tools required.

---

## 1. Prerequisites & build

See **[docs/INSTALL.md](docs/INSTALL.md)** for full per-platform prerequisites
(Windows, Debian/Ubuntu, Slackware) and troubleshooting.

```bash
# Linux
export JAVA_HOME=~/jdks/jdk8u<version>
export LO_HOME=~/libreoffice26.2
./build.sh
"$LO_HOME/program/unopkg" add --force build/JulianDate.oxt
```

```powershell
# Windows
pwsh -File build.ps1
& 'C:\Program Files\LibreOffice\program\unopkg.exe' add --force build\JulianDate.oxt
```

Both scripts run the same dependency-free pipeline — `unoidl-write` →
`javamaker` → `javac` (Java 8) → `jar` → zip — with no Ant, no Maven, and no
bundled third-party jars.

## 2. Try it

```
=TO_JULIAN_DAY(DATE(2000;1;1))              -> 2451545
=TO_JULIAN_ORDINAL(DATE(2025;1;15))         -> 2025015
=GREGORIAN_TO_JULIAN(DATE(1582;10;15))      -> displays as 1582-10-05
```

## Implementation

A Java UNO add-in (`com.sun.star.sheet.AddIn`), packaged as
`build/JulianDate.oxt`. Built, installed, and **verified end-to-end** (all 10
functions plus round-trip and error-path checks) against a live headless
LibreOffice 26.2 instance.

### Layout

| Path | Purpose |
|---|---|
| `idl/com/example/juliandate/XJulianDate.idl` | Custom UNO interface (the 10 functions) |
| `src/com/example/juliandate/JulianCalc.java` | Pure-Java calendar math (Fliegel-Van Flandern / Richards' algorithm), no UNO dependency |
| `src/com/example/juliandate/JulianDateImpl.java` | The add-in: `XJulianDate` + `XAddIn` + `XServiceName`/`XServiceInfo`, display↔programmatic name mapping, UNO registration |
| `registration/CalcAddIns.xcu` | Function display names, descriptions, argument help, and **`CompatibilityName`** (so formulas survive an `.xls`/`.xlsx` round-trip) |
| `registration/{manifest,description}.xml`, `MANIFEST.MF` | `.oxt` manifest, extension metadata, jar `RegistrationClassName` |
| `build.sh` / `build.ps1` | `unoidl-write` → `javamaker` → `javac` (Java 8) → `jar` → zip `.oxt` |
| `tools/test_julian.py` | Headless end-to-end test (all 10 functions + round-trips + error paths) |
| `tools/build_demo.py` | Regenerates `demo/juliandate_demo.ods` from `demo/demo.csv` |
| `demo/demo.csv` | Source data: real AAVSO variable-star (EG Andromedae) photometry export |
| `demo/juliandate_demo.ods` | Worked example: `demo.csv`'s Julian Date column converted to calendar dates with a live add-in formula |
| `docs/HOWTO.md` | End-user guide: install the pre-built release and use it, no build tools needed |
| `docs/INSTALL.md` | Full per-platform build / install / run instructions |
| `docs/FUNCTIONS.md` | Complete function reference: every argument, epoch handling, calendar-reform caveats, worked examples |

### Key implementation notes

- **No third-party dependencies.** Pure JDK 8 standard library — no Ant, no
  Maven, no bundled jars — avoids classloader conflicts inside LibreOffice's
  embedded JVM.
- **Null-date offset is self-derived, not hardcoded.** The Calc epoch
  (default 1899-12-30 = serial 0) is converted to a Julian Day Number using
  the *same* Fliegel-Van Flandern algorithm used for every other conversion,
  so there's one source of truth for the offset — documented in full in
  `docs/FUNCTIONS.md`.
- **Both calendar forward/inverse conversions are algorithmically distinct**
  and independently verified against known reference points: J2000.0 (JD
  2451545.0 = 2000-01-01 noon) and the 1582 Gregorian calendar reform
  (Gregorian Oct 15, 1582 = Julian Oct 5, 1582 — a 10-day gap at the time,
  grown to 13 days by the 20th century from additional skipped Julian leap
  years).
- **Errors surface as Calc error values** (e.g. `Err:502`), not exception
  strings — bad `digits`, an out-of-range ordinal day-of-year, or non-finite
  input all raise a UNO `IllegalArgumentException`.
- **`CompatibilityName` is set for every function** in `CalcAddIns.xcu` —
  without it, formulas silently lose their function name (turning into
  `#NAME?`) when a workbook is saved and reopened as `.xls`/`.xlsx`.

### Verified results

```
=TO_JULIAN_DAY(DATE(2000;1;1))                                    -> 2451545
=TO_JULIAN_DATE(DATE(2000;1;1)+0.5)                                -> 2451545.0
=FROM_JULIAN_DAY(2451545)                                          -> 2000-01-01
=TO_MJD(DATE(2000;1;1))                                            -> 51544.0
=TO_JULIAN_ORDINAL(DATE(2025;1;15))                                -> 2025015
=TO_JULIAN_ORDINAL(DATE(2025;1;15); 5)                             -> 25015
=FROM_JULIAN_ORDINAL(2025015)                                      -> 2025-01-15
=GREGORIAN_TO_JULIAN(DATE(1582;10;15))                             -> 1582-10-05
=JULIAN_TO_GREGORIAN(GREGORIAN_TO_JULIAN(DATE(2025;1;15)))         -> 2025-01-15 (round-trips)
```

### License

Released under the [MIT License](LICENSE).
