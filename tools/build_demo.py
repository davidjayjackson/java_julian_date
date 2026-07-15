#!/usr/bin/env python3
"""Regenerate demo/juliandate_demo.ods from demo/demo.csv.

demo/demo.csv is a real AAVSO (aavso.org) photometry export for the
symbiotic variable star EG Andromedae -- one row per observation, full
AAVSO column set (JD, Magnitude, Band, Observer Code, etc.). The demo
workbook keeps a curated 4-column subset and adds a live add-in formula:

  A: Date       = FROM_JULIAN_DATE(B)   (formatted as YYYY-MM-DD)
  B: JD         (from the CSV, unchanged)
  C: Magnitude  (from the CSV, unchanged)
  D: Star Name  (from the CSV, unchanged)

showing the practical case the add-in exists for: astronomical software
routinely reports timestamps as Julian Dates, and FROM_JULIAN_DATE converts
them back to ordinary calendar dates.

Requires the add-in to be installed in the target LibreOffice profile (see
docs/INSTALL.md) and a headless LibreOffice listening on a UNO socket:

    "$LO_HOME/program/soffice" --headless --norestore \
        --accept="socket,host=localhost,port=2002;urp;" &
    "$LO_HOME/program/python" tools/build_demo.py
"""
import csv
import os
import sys

import uno
from com.sun.star.beans import PropertyValue

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(ROOT, "demo", "demo.csv")
DEMO_PATH = os.path.join(ROOT, "demo", "juliandate_demo.ods")


def make_prop(name, value):
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def connect():
    local_ctx = uno.getComponentContext()
    resolver = local_ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local_ctx)
    ctx = resolver.resolve(
        "uno:socket,host=localhost,port=2002;urp;StarOffice.ComponentContext")
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    return ctx, desktop


def read_rows():
    with open(CSV_PATH, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            yield float(row["JD"]), float(row["Magnitude"]), row["Star Name"]


def main():
    rows = list(read_rows())
    print("Data rows: %d" % len(rows))

    ctx, desktop = connect()
    doc = desktop.loadComponentFromURL(
        "private:factory/scalc", "_blank", 0, (make_prop("Hidden", True),))
    sheet = doc.Sheets.getByIndex(0)
    sheet.Name = "demo"

    headers = ["Date", "JD", "Magnitude", "Star Name"]
    for c, h in enumerate(headers):
        sheet.getCellByPosition(c, 0).setString(h)

    for r, (jd, mag, star) in enumerate(rows, start=1):
        row_ref = r + 1  # 1-based spreadsheet row number
        sheet.getCellByPosition(0, r).setFormula("=FROM_JULIAN_DATE(B%d)" % row_ref)
        sheet.getCellByPosition(1, r).setValue(jd)
        sheet.getCellByPosition(2, r).setValue(mag)
        sheet.getCellByPosition(3, r).setString(star)

    # Format the Date column as YYYY-MM-DD instead of a raw serial.
    formats = doc.getNumberFormats()
    locale = uno.createUnoStruct("com.sun.star.lang.Locale")
    fmt_str = "YYYY-MM-DD"
    key = formats.queryKey(fmt_str, locale, False)
    if key == -1:
        key = formats.addNew(fmt_str, locale)
    date_col = sheet.getCellRangeByPosition(0, 1, 0, len(rows))
    date_col.NumberFormat = key

    doc.calculateAll()

    # Spot-check the first data row didn't error (e.g. add-in not installed).
    first = sheet.getCellByPosition(0, 1)
    if first.getError() != 0:
        print("WARNING: A2 has error %d -- is the add-in installed? "
              "(unopkg add --force build/JulianDate.oxt)" % first.getError())
        sys.exit(1)
    print("A2 (Date) = %s" % first.getString())

    url = uno.systemPathToFileUrl(DEMO_PATH)
    doc.storeToURL(url, (make_prop("FilterName", "calc8"),))
    doc.close(False)
    print("Saved %s" % DEMO_PATH)


if __name__ == "__main__":
    main()
