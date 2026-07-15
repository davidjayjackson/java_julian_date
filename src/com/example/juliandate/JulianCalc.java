package com.example.juliandate;

/**
 * Pure calendar math for the Julian Date Calc add-in: no UNO dependencies,
 * so it can be exercised directly from a plain {@code main}/test harness.
 *
 * <p>All conversions use the proleptic Gregorian and proleptic Julian
 * calendars (the same leap-year rule applied uniformly to every date, with
 * no 1582-style adoption-date discontinuity) via the Fliegel-Van Flandern
 * / Richards Julian Day Number algorithms.
 *
 * <p>Calc date serials are handled relative to the default LibreOffice/Excel
 * null date, 1899-12-30 = serial 0 ({@link #JDN0}). A document configured
 * with a different null date (Tools &gt; Options &gt; Calc &gt; Calculate)
 * will be off by the difference in days, since these functions don't query
 * the document's NullDate property.
 */
public final class JulianCalc {

    private JulianCalc() {
    }

    /** Julian Day Number of the default Calc null date, 1899-12-30. */
    public static final long JDN0 = gregorianYmdToJdn(1899, 12, 30);

    // ------------------------------------------------------------------ //
    // Forward: proleptic calendar Y/M/D -> Julian Day Number             //
    // ------------------------------------------------------------------ //

    /**
     * Fliegel &amp; Van Flandern (1968), proleptic Gregorian calendar.
     *
     * <p>Deliberately uses Java's truncating {@code /} rather than
     * {@link Math#floorDiv}: the published formula's {@code (month-14)/12}
     * term relies on C-style truncation-toward-zero to fold January/
     * February into the previous year's months 13/14 (valid for the
     * {@code month} range 1-12 this method is always called with; a
     * floor division here would double-shift and corrupt the result).
     */
    public static long gregorianYmdToJdn(long year, long month, long day) {
        long a = (month - 14) / 12;
        return (1461 * (year + 4800 + a)) / 4
                + (367 * (month - 2 - 12 * a)) / 12
                - (3 * ((year + 4900 + a) / 100)) / 4
                + day - 32075;
    }

    /** Fliegel &amp; Van Flandern (1968), proleptic Julian calendar. See
     *  {@link #gregorianYmdToJdn} for why this uses truncating division. */
    public static long julianYmdToJdn(long year, long month, long day) {
        long a = (month - 9) / 7;
        return 367 * year
                - (7 * (year + 5001 + a)) / 4
                + (275 * month) / 9
                + day + 1729777;
    }

    // ------------------------------------------------------------------ //
    // Inverse: Julian Day Number -> calendar Y/M/D (Richards' algorithm) //
    // ------------------------------------------------------------------ //

    /**
     * Richards' algorithm (the standard modern restatement of Fliegel-Van
     * Flandern's inverse). {@code gregorian} toggles the one correction
     * term that distinguishes the Gregorian century leap-year rule from the
     * Julian calendar's uniform every-4-years rule.
     *
     * @return {@code {year, month, day}}
     */
    public static long[] jdnToYmd(long jdn, boolean gregorian) {
        final long y = 4716, j = 1401, m = 2, n = 12, r = 4, p = 1461,
                v = 3, u = 5, s = 153, w = 2, bConst = 274277, cConst = -38;

        long f = jdn + j;
        if (gregorian) {
            f += Math.floorDiv(Math.floorDiv(4 * jdn + bConst, 146097) * 3, 4) + cConst;
        }
        long e = r * f + v;
        long g = Math.floorDiv(Math.floorMod(e, p), r);
        long h = u * g + w;
        long day = Math.floorDiv(Math.floorMod(h, s), u) + 1;
        long month = Math.floorMod(Math.floorDiv(h, s) + m, n) + 1;
        long year = Math.floorDiv(e, p) - y + Math.floorDiv(n + m - month, n);
        return new long[] { year, month, day };
    }

    public static long[] jdnToGregorianYmd(long jdn) {
        return jdnToYmd(jdn, true);
    }

    public static long[] jdnToJulianYmd(long jdn) {
        return jdnToYmd(jdn, false);
    }

    // ------------------------------------------------------------------ //
    // Astronomical Julian Day / Julian Date / Modified Julian Date       //
    // ------------------------------------------------------------------ //

    private static void requireFinite(double v, String name) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(name + " must be a finite number");
        }
    }

    /** Calc date serial -&gt; (integer) Julian Day Number for that calendar day. */
    public static long toJulianDay(double calcSerial) {
        requireFinite(calcSerial, "date");
        return JDN0 + (long) Math.floor(calcSerial);
    }

    /** Calc date/time serial -&gt; Julian Date (with time-of-day fraction). */
    public static double toJulianDate(double calcSerial) {
        requireFinite(calcSerial, "date");
        return (JDN0 - 0.5) + calcSerial;
    }

    /** Julian Day Number -&gt; Calc date serial (no time component). */
    public static double fromJulianDay(double jdn) {
        requireFinite(jdn, "jdn");
        return jdn - JDN0;
    }

    /** Julian Date -&gt; Calc date/time serial. */
    public static double fromJulianDate(double jd) {
        requireFinite(jd, "jd");
        return jd - JDN0 + 0.5;
    }

    /** Calc date/time serial -&gt; Modified Julian Date (JD - 2400000.5). */
    public static double toMjd(double calcSerial) {
        return toJulianDate(calcSerial) - 2400000.5;
    }

    /** Modified Julian Date -&gt; Calc date/time serial. */
    public static double fromMjd(double mjd) {
        requireFinite(mjd, "mjd");
        return fromJulianDate(mjd + 2400000.5);
    }

    // ------------------------------------------------------------------ //
    // Ordinal / business "Julian date" (YYDDD / YYYYDDD day-of-year)     //
    // ------------------------------------------------------------------ //

    /**
     * @param calcSerial a Calc date serial
     * @param digits     5 (YYDDD) or 7 (YYYYDDD)
     */
    public static long toJulianOrdinal(double calcSerial, int digits) {
        requireFinite(calcSerial, "date");
        if (digits != 5 && digits != 7) {
            throw new IllegalArgumentException("digits must be 5 (YYDDD) or 7 (YYYYDDD), got " + digits);
        }
        long dayJdn = JDN0 + (long) Math.floor(calcSerial);
        long[] ymd = jdnToGregorianYmd(dayJdn);
        long year = ymd[0];
        long dayOfYear = dayJdn - gregorianYmdToJdn(year, 1, 1) + 1;
        if (digits == 5) {
            long yy = ((year % 100) + 100) % 100;
            return yy * 1000 + dayOfYear;
        }
        return year * 1000 + dayOfYear;
    }

    /**
     * Auto-detects YYDDD vs YYYYDDD by magnitude: values below 100000 are
     * read as 5-digit YYDDD (2-digit year, windowed 00-68 -&gt; 2000-2068,
     * 69-99 -&gt; 1969-1999, matching the common {@code strptime %y}
     * convention); values at or above 100000 are read as 7-digit YYYYDDD.
     *
     * <p>Because numeric cells drop leading zeros, a 7-digit ordinal for a
     * year before 0100 (e.g. year 45, day 5 -&gt; 0045005) is numerically
     * indistinguishable from a 5-digit YYDDD value and will be
     * misinterpreted; such dates should be entered as an explicit
     * date/DATE() construct instead.
     */
    public static double fromJulianOrdinal(double ordinalValue) {
        requireFinite(ordinalValue, "ordinal");
        long ordinal = Math.round(ordinalValue);
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative, got " + ordinal);
        }
        long year;
        long dayOfYear;
        if (ordinal < 100000) {
            long yy = ordinal / 1000;
            dayOfYear = ordinal % 1000;
            year = (yy <= 68) ? 2000 + yy : 1900 + yy;
        } else {
            year = ordinal / 1000;
            dayOfYear = ordinal % 1000;
        }
        long jan1Jdn = gregorianYmdToJdn(year, 1, 1);
        long daysInYear = gregorianYmdToJdn(year, 12, 31) - jan1Jdn + 1;
        if (dayOfYear < 1 || dayOfYear > daysInYear) {
            throw new IllegalArgumentException(
                    "day-of-year " + dayOfYear + " is out of range for year " + year
                            + " (1-" + daysInYear + ")");
        }
        return (jan1Jdn + dayOfYear - 1) - JDN0;
    }

    // ------------------------------------------------------------------ //
    // Proleptic Julian <-> Gregorian calendar conversion                 //
    // ------------------------------------------------------------------ //

    /**
     * Reinterprets a Gregorian-calendar date as the equivalent proleptic
     * Julian-calendar date, returned as a Calc serial whose displayed
     * year/month/day are the Julian calendar's labels for that same day.
     */
    public static double gregorianToJulian(double calcSerial) {
        return convertCalendar(calcSerial, true);
    }

    /**
     * Reinterprets a Julian-calendar date as the equivalent proleptic
     * Gregorian-calendar date, returned as a Calc serial whose displayed
     * year/month/day are the Gregorian calendar's labels for that same day.
     */
    public static double julianToGregorian(double calcSerial) {
        return convertCalendar(calcSerial, false);
    }

    private static double convertCalendar(double calcSerial, boolean gregorianToJulian) {
        requireFinite(calcSerial, "date");
        long intDay = (long) Math.floor(calcSerial);
        double frac = calcSerial - intDay;
        long jdn = JDN0 + intDay;

        long[] ymd = gregorianToJulian ? jdnToJulianYmd(jdn) : jdnToGregorianYmd(jdn);
        long newJdn = gregorianToJulian
                ? gregorianYmdToJdn(ymd[0], ymd[1], ymd[2])
                : julianYmdToJdn(ymd[0], ymd[1], ymd[2]);
        return (newJdn - JDN0) + frac;
    }
}
