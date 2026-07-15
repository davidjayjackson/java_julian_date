#!/usr/bin/env python3
"""Regenerate demo/EG_And-Demo.ods with live add-in formulas.

The base file is real AAVSO photometry data for the symbiotic variable star
EG Andromedae (columns: JD, Magnitude, Band, Observer Code). This script adds
two demonstration columns computed with the Julian Date add-in:

  E: Calendar Date  = FROM_JULIAN_DATE(A)   (formatted as YYYY-MM-DD)
  F: Ordinal YYYYDDD = TO_JULIAN_ORDINAL(FROM_JULIAN_DATE(A))

showing the practical case the add-in exists for: astronomical software
routinely reports timestamps as Julian Dates, and this converts a whole
column of them to ordinary calendar dates in one fill.

Requires the add-in to be installed in the target LibreOffice profile (see
docs/INSTALL.md) and a headless LibreOffice listening on a UNO socket:

    "$LO_HOME/program/soffice" --headless --norestore \
        --accept="socket,host=localhost,port=2002;urp;" &
    "$LO_HOME/program/python" tools/build_demo.py
"""
import os
import sys

import uno
from com.sun.star.beans import PropertyValue

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEMO_PATH = os.path.join(ROOT, "demo", "EG_And-Demo.ods")


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


def main():
    ctx, desktop = connect()
    url = uno.systemPathToFileUrl(DEMO_PATH)
    doc = desktop.loadComponentFromURL(url, "_blank", 0, (make_prop("Hidden", True),))
    sheet = doc.Sheets.getByIndex(0)

    used = sheet.createCursor()
    used.gotoEndOfUsedArea(False)
    last_row = used.RangeAddress.EndRow  # 0-based; row 0 is the header
    print("Data rows: %d" % last_row)

    sheet.getCellByPosition(4, 0).setString("Calendar Date")
    sheet.getCellByPosition(5, 0).setString("Ordinal YYYYDDD")

    formulas = []
    for r in range(1, last_row + 1):
        row_ref = r + 1  # 1-based spreadsheet row number
        formulas.append([
            "=FROM_JULIAN_DATE(A%d)" % row_ref,
            "=TO_JULIAN_ORDINAL(FROM_JULIAN_DATE(A%d))" % row_ref,
        ])

    target = sheet.getCellRangeByPosition(4, 1, 5, last_row)
    target.setFormulaArray(tuple(tuple(row) for row in formulas))

    # Format the Calendar Date column as YYYY-MM-DD instead of a raw serial.
    formats = doc.getNumberFormats()
    locale = uno.createUnoStruct("com.sun.star.lang.Locale")
    fmt_str = "YYYY-MM-DD"
    key = formats.queryKey(fmt_str, locale, False)
    if key == -1:
        key = formats.addNew(fmt_str, locale)
    date_col = sheet.getCellRangeByPosition(4, 1, 4, last_row)
    date_col.NumberFormat = key

    doc.calculateAll()

    # Spot-check the first data row didn't error (e.g. add-in not installed).
    first = sheet.getCellByPosition(4, 1)
    if first.getError() != 0:
        print("WARNING: E2 has error %d -- is the add-in installed? "
              "(unopkg add --force build/JulianDate.oxt)" % first.getError())
        sys.exit(1)
    print("E2 (Calendar Date) = %s" % first.getString())
    print("F2 (Ordinal)       = %s" % sheet.getCellByPosition(5, 1).getString())

    doc.store()
    doc.close(False)
    print("Saved %s" % DEMO_PATH)


if __name__ == "__main__":
    main()
