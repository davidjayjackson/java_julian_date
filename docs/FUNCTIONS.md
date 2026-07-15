# Function reference

All functions are pure computation (no network/file I/O) and return
synchronously. In the Calc UI, arguments are separated by **semicolons**:
`=TO_JULIAN_ORDINAL(A1; 5)`.

Every function is registered with a `CompatibilityName` in
[`registration/CalcAddIns.xcu`](../registration/CalcAddIns.xcu) equal to its
display name, so formulas survive a round-trip through `.xls`/`.xlsx` without
turning into `#NAME?`.

## Epoch / null-date handling

Calc represents dates as a serial number of days since a configurable "null
date" (Tools ▸ Options ▸ Calc ▸ Calculate), **default 1899-12-30 = serial 0**.
Every function below assumes that default. The add-in derives the Julian Day
Number of the null date itself from the same Fliegel-Van Flandern algorithm
used for every other conversion (`JDN0 = 2415019`, i.e. `TO_JULIAN_DAY(0)` =
`2415019`), rather than hardcoding a separate magic constant — this *is* the
"explicit and documented null-date offset."

**If a document uses a non-default null date**, results from every function
here will be off by the difference in days, since the functions don't query
the document's `NullDate` property (this is a deliberate scope limit — see
`docs/INSTALL.md`/README for the full add-in scope).

## Calendar-reform caveat

`GREGORIAN_TO_JULIAN`/`JULIAN_TO_GREGORIAN` are **proleptic**: the same
calendar rule (Gregorian's every-4-years-except-century-years-unless-divisible-
by-400, or the Julian calendar's uniform every-4-years) is applied to *every*
date, with no jump at any particular historical adoption date. This is a
deliberate, documented choice, not a bug: real-world adoption dates varied by
country from 1582 (Catholic Europe) to 1926 (Turkey), so there is no single
"correct" discontinuity date to hardcode. If you need the actual historical
record for a specific country/region, apply that country's adoption date
yourself around these functions.

---

### `TO_JULIAN_DAY(date)`

Returns the (integer-valued) astronomical **Julian Day Number** for a Calc date.

| Arg | Type | Description |
|---|---|---|
| `date` | number | A Calc date serial (or cell reference / `DATE()` call). |

```
=TO_JULIAN_DAY(DATE(2000;1;1))     -> 2451545
=TO_JULIAN_DAY(DATE(2025;1;15))    -> 2460691
```

### `TO_JULIAN_DATE(date)`

Returns the **Julian Date**, including the time-of-day fraction (JD `.5` =
midnight, JD `.0` = noon).

```
=TO_JULIAN_DATE(DATE(2000;1;1)+0.5)   -> 2451545.0      (noon, 2000-01-01)
=TO_JULIAN_DATE(DATE(2000;1;1))       -> 2451544.5      (midnight, 2000-01-01)
```

### `FROM_JULIAN_DAY(jdn)`

Converts a Julian Day Number back to a Calc date serial (no time component —
the result is the start of that calendar day).

```
=FROM_JULIAN_DAY(2451545)   -> 36526   (displays as 2000-01-01)
```

### `FROM_JULIAN_DATE(jd)`

Converts a Julian Date (with optional fractional day) back to a Calc
date/time serial.

```
=FROM_JULIAN_DATE(2451545.25)   -> 36526.75   (2000-01-01 18:00)
```

### `TO_MJD(date)` / `FROM_MJD(mjd)`

Modified Julian Date: `MJD = JD - 2400000.5`. Same epoch/time handling as
`TO_JULIAN_DATE`/`FROM_JULIAN_DATE`.

```
=TO_MJD(DATE(2000;1;1))       -> 51544.0
=TO_MJD(DATE(2025;1;15))      -> 60690.0
=FROM_MJD(51544)              -> 36526   (2000-01-01)
```

### `TO_JULIAN_ORDINAL(date, [digits])`

Business/"ordinal" Julian date: the year plus zero-padded day-of-year, packed
into a single number. `digits` is optional: `5` for `YYDDD` (2-digit year),
`7` for `YYYYDDD` (4-digit year). **Omitted defaults to `7` (YYYYDDD).**

| Arg | Type | Description |
|---|---|---|
| `date` | number | A Calc date serial. |
| `digits` | number, optional | `5` or `7`. Any other value raises an error. |

```
=TO_JULIAN_ORDINAL(DATE(2025;1;15))       -> 2025015   (default: YYYYDDD)
=TO_JULIAN_ORDINAL(DATE(2025;1;15); 7)    -> 2025015
=TO_JULIAN_ORDINAL(DATE(2025;1;15); 5)    -> 25015
=TO_JULIAN_ORDINAL(DATE(2025;1;15); 6)    -> Err:502 (digits must be 5 or 7)
```

### `FROM_JULIAN_ORDINAL(ordinal)`

Converts a `YYDDD` or `YYYYDDD` ordinal number back to a Calc date serial,
**auto-detecting the format by magnitude**: values below 100000 are read as
5-digit `YYDDD`, at or above 100000 as 7-digit `YYYYDDD`.

The 2-digit year in `YYDDD` is windowed the same way `strptime("%y")` does:
`00`-`68` → `2000`-`2068`, `69`-`99` → `1969`-`1999`.

```
=FROM_JULIAN_ORDINAL(2025015)   -> 45672   (2025-01-15)
=FROM_JULIAN_ORDINAL(25015)     -> 45672   (2025-01-15, windowed: 25 -> 2025)
=FROM_JULIAN_ORDINAL(99015)     -> displays as 1999-01-15 (windowed: 99 -> 1999)
=FROM_JULIAN_ORDINAL(2025400)   -> Err:502 (day 400 doesn't exist in 2025)
```

> **Caveat**: because numeric cells drop leading zeros, a 7-digit YYYYDDD
> ordinal for a year before 0100 (e.g. year 45, day 5 → `0045005`) is
> numerically indistinguishable from a 5-digit YYDDD value and will be
> misinterpreted. This is inherent to auto-detecting a bare number's format
> by magnitude; such dates should be entered via `DATE()` instead.

### `GREGORIAN_TO_JULIAN(date)`

Reinterprets a Gregorian-calendar date as the equivalent proleptic
**Julian-calendar** date — i.e. "what year/month/day would the Julian
calendar use to label this same day?" Returns a Calc serial whose displayed
Y/M/D are the Julian calendar's labels.

```
=GREGORIAN_TO_JULIAN(DATE(1582;10;15))   -> displays as 1582-10-05
```

(This is the well-known 10-day gap at the original 1582 calendar reform:
Gregorian October 15, 1582 was "October 5, 1582" by the old Julian reckoning
— the Gregorian calendar skipped October 5-14 that year. By the 20th century
the gap had grown to 13 days due to additional skipped Julian leap years.)

### `JULIAN_TO_GREGORIAN(date)`

The inverse: reinterprets a Julian-calendar date (its Y/M/D as typed/
displayed) as the equivalent proleptic **Gregorian-calendar** date.

```
=JULIAN_TO_GREGORIAN(GREGORIAN_TO_JULIAN(DATE(1582;10;15)))   -> DATE(1582;10;15)  (round-trips)
```

## Errors

Invalid input (bad `digits`, an out-of-range `FROM_JULIAN_ORDINAL` day-of-year,
non-finite numbers, etc.) raises a UNO `IllegalArgumentException`, which Calc
shows as an error value in the cell (e.g. `Err:502`) rather than a crash or an
exception string.
