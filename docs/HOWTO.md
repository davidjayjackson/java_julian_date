# How to install and use the Julian Date add-in

This guide is for **using** the add-in in LibreOffice Calc — download the
pre-built extension, install it, and start calling `TO_JULIAN_DAY`,
`FROM_JULIAN_DATE`, and the rest as ordinary worksheet formulas. If you want
to build the add-in from source instead, see
[docs/INSTALL.md](INSTALL.md); for the complete function reference (every
argument, error conditions, and worked examples), see
[docs/FUNCTIONS.md](FUNCTIONS.md).

## 1. Download

Get `JulianDate.oxt` from the
[latest release](https://github.com/davidjayjackson/java_julian_date/releases/latest)
page (it's attached as a release asset — no build tools needed).

## 2. Install

**Easiest**: close any open LibreOffice windows, then double-click the
downloaded `JulianDate.oxt` file. LibreOffice's Extension Manager opens and
offers to install it — click **Install for me only** (or **Install for all
users** if you have permission and want that). Restart LibreOffice when it
asks.

**From the command line**, close LibreOffice first, then:

```bash
# Linux/macOS
"$LO_HOME/program/unopkg" add --force JulianDate.oxt
```

```powershell
# Windows
& 'C:\Program Files\LibreOffice\program\unopkg.exe' add --force JulianDate.oxt
```

Restart LibreOffice afterwards either way.

## 3. Confirm it's installed

Open Calc, click any cell, and start typing `=TO_JULIAN_DAY(` — Calc's
formula autocomplete should offer it. You can also check
**Tools ▸ Macros** is irrelevant here (this add-in has no macros); instead
open the **Function Wizard** (`Insert ▸ Function...`, or `Ctrl+F2`) and look
under the **Add-In** category — all 10 functions should be listed there with
descriptions and argument help.

If formulas show `#NAME?` instead of a value, the extension isn't registered
— see the Troubleshooting section in [docs/INSTALL.md](INSTALL.md).

## 4. One example per function

Type any of these into a blank cell. (Remember: Calc separates function
arguments with **semicolons**, not commas.)

| Function | Formula | Result |
|---|---|---|
| `TO_JULIAN_DAY` | `=TO_JULIAN_DAY(DATE(2000;1;1))` | `2451545` — the Julian Day Number |
| `TO_JULIAN_DATE` | `=TO_JULIAN_DATE(DATE(2000;1;1)+0.5)` | `2451545` — Julian Date at noon (JD `.0` = noon, `.5` = midnight) |
| `FROM_JULIAN_DAY` | `=FROM_JULIAN_DAY(2451545)` | displays as `2000-01-01` |
| `FROM_JULIAN_DATE` | `=FROM_JULIAN_DATE(2451545.25)` | displays as `2000-01-01 18:00` (the `.25` is a quarter-day past noon) |
| `TO_MJD` | `=TO_MJD(DATE(2000;1;1))` | `51544` — the Modified Julian Date |
| `FROM_MJD` | `=FROM_MJD(51544)` | displays as `2000-01-01` |
| `TO_JULIAN_ORDINAL` | `=TO_JULIAN_ORDINAL(DATE(2025;1;15))` | `2025015` — YYYYDDD (the default when `digits` is omitted) |
| `TO_JULIAN_ORDINAL` (5-digit) | `=TO_JULIAN_ORDINAL(DATE(2025;1;15);5)` | `25015` — YYDDD |
| `FROM_JULIAN_ORDINAL` | `=FROM_JULIAN_ORDINAL(2025015)` | displays as `2025-01-15` |
| `GREGORIAN_TO_JULIAN` | `=GREGORIAN_TO_JULIAN(DATE(1582;10;15))` | displays as `1582-10-05` — the historical 10-day calendar-reform gap |
| `JULIAN_TO_GREGORIAN` | `=JULIAN_TO_GREGORIAN(GREGORIAN_TO_JULIAN(DATE(1582;10;15)))` | displays as `1582-10-15` — round-trips back through the row above |

> **Note**: you can't type `=JULIAN_TO_GREGORIAN(DATE(1582;10;5))` directly —
> Calc's own built-in `DATE()` function rejects any date before 1582-10-15
> with `Err:519`, regardless of this add-in. That's why the example above
> round-trips through `GREGORIAN_TO_JULIAN` instead of constructing the
> pre-reform Julian date directly.

Every result above was verified against a live LibreOffice instance while
building this add-in — see [docs/FUNCTIONS.md](FUNCTIONS.md) for the full
reference, including each function's argument types, error conditions, and
additional edge-case examples (e.g. invalid `digits`, out-of-range ordinal
day-of-year, and the 2-digit-year windowing rule `FROM_JULIAN_ORDINAL` uses
for `YYDDD`).

## 5. Worked example with real data

`demo/juliandate_demo.ods` is a real photometry dataset from the
[AAVSO](https://www.aavso.org/) (American Association of Variable Star
Observers) for **EG Andromedae**, a well-known symbiotic variable star —
382 brightness observations spanning July 2025 to July 2026. Astronomical
data like this is universally timestamped in Julian Date, exactly the
problem this add-in solves.

Open the file (with the add-in installed) and you'll see:

| Column | Contents |
|---|---|
| A `Date` | **Computed by this add-in**: `=FROM_JULIAN_DATE(B2)`, formatted as `YYYY-MM-DD` |
| B `JD` | The observation's Julian Date, as reported by AAVSO (e.g. `2460859.543`) |
| C `Magnitude` | The star's observed brightness |
| D `Star Name` | The AAVSO star designation (`EG And`) |

Column A is a live formula — select any cell in it and you'll see the
`FROM_JULIAN_DATE` formula in the formula bar. If you see `#NAME?` there
instead of a date, it means you opened this file without installing the
add-in first — go back to step 2.

This is exactly the kind of task the add-in is for: taking a column of raw
Julian Dates from scientific/astronomical software and turning it into
ordinary calendar dates you can sort, filter, and chart by month.

The underlying data is in `demo/demo.csv` (the full AAVSO export, unfiltered
columns). To regenerate `juliandate_demo.ods` from it yourself (e.g. after
rebuilding the add-in from source), see `tools/build_demo.py`.
