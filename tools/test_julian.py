#!/usr/bin/env python3
"""Headless end-to-end test for the Julian Date Calc add-in.

Connects to a running `soffice --headless --accept="socket,host=localhost,port=2002;urp;"`,
opens a new Calc document, enters formulas for all 10 add-in functions plus a
couple of round-trip checks, recalculates, and asserts the results. Prints
"RESULT: PASS" or "RESULT: FAIL" and exits 0/1 accordingly.

Run with LibreOffice's own bundled Python (it ships the `uno` module):
    "$LO_HOME/program/soffice" --headless --norestore \
        --accept="socket,host=localhost,port=2002;urp;" &
    "$LO_HOME/program/python" tools/test_julian.py
"""
import sys

import uno
from com.sun.star.beans import PropertyValue


def connect():
    local_ctx = uno.getComponentContext()
    resolver = local_ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local_ctx)
    ctx = resolver.resolve(
        "uno:socket,host=localhost,port=2002;urp;StarOffice.ComponentContext")
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    return ctx, smgr, desktop


def make_prop(name, value):
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def open_calc(desktop):
    hidden = make_prop("Hidden", True)
    return desktop.loadComponentFromURL(
        "private:factory/scalc", "_blank", 0, (hidden,))


def set_formula(sheet, cell_addr, formula):
    cell = sheet.getCellRangeByName(cell_addr)
    cell.setFormula(formula)


def get_value(sheet, cell_addr):
    return sheet.getCellRangeByName(cell_addr).getValue()


def get_string(sheet, cell_addr):
    return sheet.getCellRangeByName(cell_addr).getString()


def main():
    ctx, smgr, desktop = connect()
    doc = open_calc(desktop)
    sheet = doc.Sheets.getByIndex(0)

    cases = [
        # (cell, formula, expected numeric value, tolerance)
        ("A1", "=DATE(2000;1;1)", 36526, 0),
        ("A2", "=TO_JULIAN_DAY(A1)", 2451545, 0),
        ("A3", "=TO_JULIAN_DATE(A1+0.5)", 2451545, 1e-9),
        ("A4", "=FROM_JULIAN_DAY(A2)", 36526, 0),
        ("A5", "=TO_MJD(A1)", 51544, 1e-9),
        ("A6", "=FROM_MJD(A5)", 36526, 1e-9),
        ("A7", "=DATE(2025;1;15)", None, None),
        ("A8", "=TO_JULIAN_ORDINAL(A7)", 2025015, 0),
        ("A9", "=TO_JULIAN_ORDINAL(A7;5)", 25015, 0),
        ("A10", "=FROM_JULIAN_ORDINAL(2025015)", None, None),  # compare to A7 below
        ("A11", "=FROM_JULIAN_ORDINAL(25015)", None, None),    # compare to A7 below
        ("A12", "=DATE(1582;10;15)", None, None),
        ("A13", "=GREGORIAN_TO_JULIAN(A12)", None, None),      # expect 1582-10-05
        ("A14", "=JULIAN_TO_GREGORIAN(A13)", None, None),      # roundtrip -> A12
        ("A15", "=YEAR(A13)&\"-\"&MONTH(A13)&\"-\"&DAY(A13)", None, None),
    ]

    for addr, formula, _, _ in cases:
        set_formula(sheet, addr, formula)

    doc.calculateAll()

    failures = []

    def check(name, got, want, tol=1e-9):
        if abs(got - want) > tol:
            failures.append("%s: got %r want %r" % (name, got, want))
        else:
            print("ok   %-40s = %r" % (name, got))

    check("TO_JULIAN_DAY(2000-01-01)", get_value(sheet, "A2"), 2451545, 0)
    check("TO_JULIAN_DATE(2000-01-01 noon)", get_value(sheet, "A3"), 2451545.0)
    check("FROM_JULIAN_DAY roundtrip", get_value(sheet, "A4"), 36526, 0)
    check("TO_MJD(2000-01-01)", get_value(sheet, "A5"), 51544.0)
    check("FROM_MJD roundtrip", get_value(sheet, "A6"), 36526.0)
    check("TO_JULIAN_ORDINAL YYYYDDD default", get_value(sheet, "A8"), 2025015, 0)
    check("TO_JULIAN_ORDINAL YYDDD", get_value(sheet, "A9"), 25015, 0)
    check("FROM_JULIAN_ORDINAL YYYYDDD roundtrip", get_value(sheet, "A10"), get_value(sheet, "A7"))
    check("FROM_JULIAN_ORDINAL YYDDD roundtrip", get_value(sheet, "A11"), get_value(sheet, "A7"))
    check("JULIAN_TO_GREGORIAN(GREGORIAN_TO_JULIAN(x)) roundtrip",
          get_value(sheet, "A14"), get_value(sheet, "A12"))

    julian_label = get_string(sheet, "A15")
    if julian_label != "1582-10-5":
        failures.append("GREGORIAN_TO_JULIAN(1582-10-15) label: got %r want '1582-10-5'" % julian_label)
    else:
        print("ok   %-40s = %r" % ("GREGORIAN_TO_JULIAN(1582-10-15) label", julian_label))

    # error path: bad digits argument should be a Calc error, not a crash
    set_formula(sheet, "B1", "=TO_JULIAN_ORDINAL(A1;6)")
    doc.calculateAll()
    b1 = sheet.getCellRangeByName("B1")
    if b1.getError() == 0:
        failures.append("TO_JULIAN_ORDINAL(date;6) should be a Calc error (invalid digits), got %r" % b1.getValue())
    else:
        print("ok   %-40s = Err:%d" % ("TO_JULIAN_ORDINAL invalid digits -> error", b1.getError()))

    doc.close(False)

    if failures:
        print("\n".join(failures))
        print("RESULT: FAIL")
        sys.exit(1)
    print("RESULT: PASS")


if __name__ == "__main__":
    main()
