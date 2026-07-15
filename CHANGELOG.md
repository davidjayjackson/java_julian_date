# Changelog

## 1.0.1

- Docs/demo only, no functional changes to the add-in.
- Added `docs/HOWTO.md`, an end-user install guide with a verified worked
  example for every function.
- Added a worked demo (`demo/juliandate_demo.ods`, built from
  `demo/demo.csv` via `tools/build_demo.py`): real AAVSO photometry data for
  the variable star EG Andromedae, with a live `FROM_JULIAN_DATE` formula
  converting its Julian Date column to calendar dates.

## 1.0.0

- Initial release: `TO_JULIAN_DAY`, `TO_JULIAN_DATE`, `FROM_JULIAN_DAY`,
  `FROM_JULIAN_DATE`, `TO_MJD`, `FROM_MJD`, `TO_JULIAN_ORDINAL`,
  `FROM_JULIAN_ORDINAL`, `GREGORIAN_TO_JULIAN`, `JULIAN_TO_GREGORIAN`.
- Verified end-to-end (all 10 functions, round-trips, and error paths) against
  a live headless LibreOffice instance.
