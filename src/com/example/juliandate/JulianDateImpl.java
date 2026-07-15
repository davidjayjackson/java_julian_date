package com.example.juliandate;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;

/**
 * LibreOffice Calc add-in exposing Julian date/calendar worksheet functions.
 *
 * <p>Implements the custom {@link XJulianDate} interface plus the standard
 * add-in plumbing ({@code com.sun.star.sheet.XAddIn}, {@code XServiceName},
 * {@code XServiceInfo}). Function display names, descriptions and
 * per-argument help live in config/CalcAddIns.xcu; the {@code XAddIn}
 * accessors below return the same data as a safe in-code fallback and,
 * crucially, supply the programmatic&lt;-&gt;display name mapping that Calc
 * actually uses to resolve {@code =TO_JULIAN_DAY(...)} formulas.
 *
 * <p>All the real math lives in {@link JulianCalc}, which has no UNO
 * dependency. Bad input there throws {@code java.lang.IllegalArgumentException},
 * which this class converts to the UNO {@link IllegalArgumentException} that
 * Calc renders as a cell error value rather than an exception string.
 */
public final class JulianDateImpl extends WeakBase
        implements XJulianDate,
                   com.sun.star.sheet.XAddIn,
                   com.sun.star.lang.XServiceName,
                   com.sun.star.lang.XServiceInfo {

    /** Implementation name: must match the AddInInfo node in CalcAddIns.xcu. */
    private static final String IMPLEMENTATION_NAME = "com.example.juliandate.JulianDateImpl";

    /** The one service that marks this component as a Calc add-in. */
    private static final String ADDIN_SERVICE = "com.sun.star.sheet.AddIn";

    private static final String[] SERVICE_NAMES = { ADDIN_SERVICE, IMPLEMENTATION_NAME };

    /** TO_JULIAN_ORDINAL's default digit count when [digits] is omitted: YYYYDDD. */
    private static final int DEFAULT_ORDINAL_DIGITS = 7;

    /** Current locale (tracked for XLocalizable; metadata is English-only here). */
    private Locale locale = new Locale("en", "US", "");

    // ------------------------------------------------------------------ //
    // XJulianDate - the actual worksheet functions                       //
    // ------------------------------------------------------------------ //

    public double toJulianDay(double date) throws IllegalArgumentException {
        try {
            return JulianCalc.toJulianDay(date);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double toJulianDate(double date) throws IllegalArgumentException {
        try {
            return JulianCalc.toJulianDate(date);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double fromJulianDay(double jdn) throws IllegalArgumentException {
        try {
            return JulianCalc.fromJulianDay(jdn);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double fromJulianDate(double jd) throws IllegalArgumentException {
        try {
            return JulianCalc.fromJulianDate(jd);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double toMjd(double date) throws IllegalArgumentException {
        try {
            return JulianCalc.toMjd(date);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double fromMjd(double mjd) throws IllegalArgumentException {
        try {
            return JulianCalc.fromMjd(mjd);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double toJulianOrdinal(double date, Object digits) throws IllegalArgumentException {
        try {
            int d = DEFAULT_ORDINAL_DIGITS;
            Object v = unwrapAny(digits);
            if (v != null) {
                if (!(v instanceof Number)) {
                    throw new IllegalArgumentException("digits must be a number (5 or 7)");
                }
                d = (int) Math.round(((Number) v).doubleValue());
            }
            return JulianCalc.toJulianOrdinal(date, d);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    /** Unwrap a UNO {@code any}: omitted/void arguments arrive wrapped as a VOID Any. */
    private static Object unwrapAny(Object arg) {
        if (arg instanceof com.sun.star.uno.Any) {
            return ((com.sun.star.uno.Any) arg).getObject();
        }
        return arg;
    }

    public double fromJulianOrdinal(double ordinal) throws IllegalArgumentException {
        try {
            return JulianCalc.fromJulianOrdinal(ordinal);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double gregorianToJulian(double date) throws IllegalArgumentException {
        try {
            return JulianCalc.gregorianToJulian(date);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    public double julianToGregorian(double date) throws IllegalArgumentException {
        try {
            return JulianCalc.julianToGregorian(date);
        } catch (RuntimeException e) {
            throw asCalcError(e);
        }
    }

    /** Normalize any thrown error into a Calc-facing IllegalArgumentException. */
    private static IllegalArgumentException asCalcError(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return (IllegalArgumentException) e;
        }
        return new IllegalArgumentException(e.getMessage());
    }

    // ------------------------------------------------------------------ //
    // XAddIn - function metadata                                         //
    //                                                                    //
    // Calc uses getDisplayFunctionName() as the AUTHORITATIVE display    //
    // (formula) name; CalcAddIns.xcu only supplies wizard help. So these //
    // must map programmatic <-> display names explicitly, or the cell   //
    // formula (=TO_JULIAN_DAY(...)) resolves to #NAME?.                  //
    // ------------------------------------------------------------------ //

    /** { programmatic, display } for every exposed function. */
    private static final String[][] FUNCS = {
        { "toJulianDay",       "TO_JULIAN_DAY" },
        { "toJulianDate",      "TO_JULIAN_DATE" },
        { "fromJulianDay",     "FROM_JULIAN_DAY" },
        { "fromJulianDate",    "FROM_JULIAN_DATE" },
        { "toMjd",              "TO_MJD" },
        { "fromMjd",            "FROM_MJD" },
        { "toJulianOrdinal",   "TO_JULIAN_ORDINAL" },
        { "fromJulianOrdinal", "FROM_JULIAN_ORDINAL" },
        { "gregorianToJulian", "GREGORIAN_TO_JULIAN" },
        { "julianToGregorian", "JULIAN_TO_GREGORIAN" },
    };

    private static String funcDescription(String prog) {
        switch (prog) {
            case "toJulianDay":
                return "Returns the (integer) Julian Day Number for a Calc date.";
            case "toJulianDate":
                return "Returns the Julian Date (with time-of-day fraction) for a Calc date/time.";
            case "fromJulianDay":
                return "Converts a Julian Day Number to a Calc date serial.";
            case "fromJulianDate":
                return "Converts a Julian Date (with fractional day) to a Calc date/time serial.";
            case "toMjd":
                return "Returns the Modified Julian Date (JD - 2400000.5) for a Calc date/time.";
            case "fromMjd":
                return "Converts a Modified Julian Date to a Calc date/time serial.";
            case "toJulianOrdinal":
                return "Returns the ordinal day-of-year number (YYDDD or YYYYDDD; default YYYYDDD) for a Calc date.";
            case "fromJulianOrdinal":
                return "Converts a YYDDD/YYYYDDD ordinal day-of-year number (auto-detected by magnitude) to a Calc date serial.";
            case "gregorianToJulian":
                return "Reinterprets a proleptic Gregorian-calendar date as the equivalent proleptic Julian-calendar date.";
            case "julianToGregorian":
                return "Reinterprets a proleptic Julian-calendar date as the equivalent proleptic Gregorian-calendar date.";
            default:
                return "";
        }
    }

    private static String[] argNames(String prog) {
        switch (prog) {
            case "toJulianDay":
            case "toJulianDate":
            case "toMjd":
            case "gregorianToJulian":
            case "julianToGregorian":
                return new String[] { "date" };
            case "fromJulianDay":
                return new String[] { "jdn" };
            case "fromJulianDate":
                return new String[] { "jd" };
            case "fromMjd":
                return new String[] { "mjd" };
            case "toJulianOrdinal":
                return new String[] { "date", "digits" };
            case "fromJulianOrdinal":
                return new String[] { "ordinal" };
            default:
                return new String[0];
        }
    }

    private static String[] argDescriptions(String prog) {
        switch (prog) {
            case "toJulianDay":
                return new String[] { "A Calc date serial (or cell reference)." };
            case "toJulianDate":
                return new String[] { "A Calc date/time serial (or cell reference)." };
            case "toMjd":
                return new String[] { "A Calc date/time serial (or cell reference)." };
            case "gregorianToJulian":
                return new String[] { "A Calc date serial, read as a Gregorian-calendar date." };
            case "julianToGregorian":
                return new String[] { "A Calc date serial, read as a Julian-calendar date." };
            case "fromJulianDay":
                return new String[] { "A Julian Day Number." };
            case "fromJulianDate":
                return new String[] { "A Julian Date, optionally with a fractional day/time." };
            case "fromMjd":
                return new String[] { "A Modified Julian Date." };
            case "toJulianOrdinal":
                return new String[] {
                    "A Calc date serial (or cell reference).",
                    "Optional: 5 for YYDDD or 7 for YYYYDDD. Omitted defaults to 7 (YYYYDDD)."
                };
            case "fromJulianOrdinal":
                return new String[] {
                    "A YYDDD or YYYYDDD ordinal day-of-year number; auto-detected by magnitude "
                        + "(below 100000 is YYDDD, otherwise YYYYDDD)."
                };
            default:
                return new String[0];
        }
    }

    public String getProgrammaticFuntionName(String displayName) {
        for (String[] f : FUNCS) {
            if (f[1].equals(displayName)) return f[0];
        }
        return "";
    }

    public String getDisplayFunctionName(String programmaticName) {
        for (String[] f : FUNCS) {
            if (f[0].equals(programmaticName)) return f[1];
        }
        return "";
    }

    public String getFunctionDescription(String programmaticName) {
        return funcDescription(programmaticName);
    }

    public String getDisplayArgumentName(String programmaticName, int argument) {
        String[] a = argNames(programmaticName);
        return (argument >= 0 && argument < a.length) ? a[argument] : "";
    }

    public String getArgumentDescription(String programmaticName, int argument) {
        String[] a = argDescriptions(programmaticName);
        return (argument >= 0 && argument < a.length) ? a[argument] : "";
    }

    public String getProgrammaticCategoryName(String programmaticName) {
        return "Add-In";
    }

    public String getDisplayCategoryName(String programmaticName) {
        return "Add-In";
    }

    // ------------------------------------------------------------------ //
    // XLocalizable (inherited via XAddIn)                                //
    // ------------------------------------------------------------------ //

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    // ------------------------------------------------------------------ //
    // XServiceName / XServiceInfo                                        //
    // ------------------------------------------------------------------ //

    public String getServiceName() {
        return IMPLEMENTATION_NAME;
    }

    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    public boolean supportsService(String service) {
        for (String s : SERVICE_NAMES) {
            if (s.equals(service)) return true;
        }
        return false;
    }

    public String[] getSupportedServiceNames() {
        return SERVICE_NAMES.clone();
    }

    // ------------------------------------------------------------------ //
    // UNO component registration entry points                           //
    // ------------------------------------------------------------------ //

    public static XSingleComponentFactory __getComponentFactory(String implName) {
        if (IMPLEMENTATION_NAME.equals(implName)) {
            return Factory.createComponentFactory(JulianDateImpl.class, SERVICE_NAMES);
        }
        return null;
    }

    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, regKey);
    }
}
