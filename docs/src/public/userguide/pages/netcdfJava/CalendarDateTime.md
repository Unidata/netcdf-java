---
title: CDM Calendar Date Handling
last_updated: 2020-01-19
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: cdm_calendar_date_time_ref.html
---

As of CDM version 4.3, dates are no longer handled by the [UDUNITS library](https://www.unidata.ucar.edu/software/udunits/){:target="_blank"}.
This allows handling [non-standard Calendars](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/build/cf-conventions.html#calendar){:target="_blank"}.
This change only affects datetime coordinate handling, called [time coordinates](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/build/cf-conventions.html#time-coordinate){:target="_blank"} in CF.
For all [dimensional units](http://en.wikipedia.org/wiki/Dimensional_analysis){:target="_blank"}, the UDUNITS package is still used.

## Definitions and Background
* **time** is a duration of time (a dimensional unit).
  UDUNITS handles this using a base unit of `seconds`.
  All other udunit time units are fixed multiples of a second.
* **datetime** is an instant of historical time.
  UDUNITS represents a datetime as a fixed number of seconds since a reference date, eg `12 days since 1990-02-03 12:22`.
* **calendar date** (e.g. `1990-02-03 12:22`) is a representation of a datetime using calendar fields (`millisec`, `second`, `minute`, `hour`, `day`, `month`, `year`).
  This allows one to correctly represent the same instant of time that, for example, is printed in a newspaper or is referenced in a history book.
  We are following the design from the [joda-time library](http://joda-time.sourceforge.net/userguide.html){:target="_blank"}, which has a very good discussion of the issues in datetime handling software.

## CDM datetime coordinates
The CDM library allows datetime coordinates to be specified in the following forms:

1. [**udunit coordinate**](#cdm-udunit-date-grammar): Variables of any numeric type (`byte`, `short`, `int`, `long`, `float`, `double`) with a `units` attribute that is a udunit date string, e.g. `hours since 1930-01-01`.
   This is the only form that conforms to the current NetCDF Climate and Forecast (CF) Metadata Conventions specification.
2. [**calendar field coordinate**](#cdm-calendar_field-unit-grammar): Variables of any integer type (`byte`, `short`, `int`, `long`) with a `units` attribute that is a udunit date string but with `calendar` added, e.g. `calendar months since 1930-01-01`.
3. [**ISO String coordinate**](#w3c-profile-of-iso-8601): Variables of type `String` or `char` in which the values follow (a slightly extended version of) the W3C profile of ISO 8601.

## Supported Calendars
CDM 4.3 and above supports the following values of the calendar attribute, which is attached to the datetime coordinate:

* **gregorian** or **standard**: Mixed Gregorian/Julian calendar.
  Same as UDUNITS except that the dates 1582-10-05 to 1582-10-14 do not exist, and neither does year 0.
* **proleptic_gregorian** or **ISO8601**: A Gregorian calendar extended to dates before 1582-10-15.
  This corresponds to the ISO8601 standard.
  This is the CDM default.
* **julian**: proleptic Julian calendar system.
* **noleap** or **365_day**: Gregorian calendar without leap years (all years are 365 days long).
* **all_leap** or **366_day**: Gregorian calendar with every year being a leap year (all years are 366 days long).
* **uniform30day** or **360_day**: All years are 360 days long.
   Each year consists of 12 months, and each month is 30 days long.
* **none**: Currently ignored by the CDM, but is available to the application to process.

None of these calendars handle leap seconds, but with enough effort one could define a [UTC](http://en.wikipedia.org/wiki/Coordinated_Universal_Time){:target="_blank"} or [TAI](http://en.wikipedia.org/wiki/International_Atomic_Time){:target="_blank"} calendar.

These calendars are implemented in the CDM with classes from Jon Blower's `uk.ac.rdg.resc.edal.time` package.
## Getting a non-standard CalendarDate

In `CalendarDate` class:
`public static CalendarDate of(Calendar cal, int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute);`

In CalendarDateFormatter class:
`public static CalendarDate isoStringToCalendarDate(Calendar calt, String iso) throws IllegalArgumentException;`

## Supported Operations
* A `ucar.nc2.time.CalendarDate` object is now used instead of a `java.util.Date`.
* `CalendarDate.getCalendar()` retrieves the calendar being used.
* `CalendarDate.equals(cdate)`, `CalendarDate.isBefore(cdate)`, `CalendarDate.isAfter(cdate)` allow for comparisons between two `CalendarDates`.
* `CalendarDate.getDifferenceInMsecs(cdate)` calculates the difference between two `CalendarDate` in `milliseconds`.
* A `CalendarDate` is obtained from a `ucar.nc2.time.CalendarDateUnit` (instead of a `ucar.units.Unit` or `ucar.nc2.units.DateUnit`), and are created in classes like `ucar.nc2.datset.CoordinateAxis1DTime`.

## CDM udunit date grammar
The CDM library version 4.3 and after continues to accept UDUNITS for time coordinate units, but it does not use the udunit library to process them.

The udunit date grammar is difficult to understand. Here's an approximate regular expression:

~~~
period SINCE [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
~~~

The following specifies the CDM udunit date grammar:

~~~
udunitDate     = period SINCE reference_date
period         = "millisec" | "msec" | "second" | "sec" | "s" | "minute" | "min" | "hour" | "hr" | "day" | 
                 "week" | "month" | "mon" | "year" | "yr"
period         = period + "s" (plural form)
reference_date = iso8601 formatted date as described below
SINCE          = literal (case insensitive)
~~~

where
* msec = millisec = seconds / 1000

UDUNITS defines the periods as _fixed_ multiples of seconds.
The non-obvious ones are:
* **day** = 86400.0 seconds
* **week** = 7 days = 604800.0 seconds
* **year** = 3.15569259747E7 seconds (365 days of length 86400.0 seconds)
* **month** = year/12 = 2629743.831225 seconds

The udunit package has some other periods that are no longer supported by CDM udunit date grammar, such as `sidereal day`, `lunar month`, `fortnight`, `eon`, etc.
Also CDM does not support the full menagerie of udunit prefixes like `nano`, `mega`, `kilo`, and so on.
Contact us if this is a problem.

CF and CDM agree that one should not use `month` or `year` as the period in a udunit, in order to avoid the following sort of problems:

~~~
 0 months since 1930-01-01 == 1930-01-01T00:00:00Z
 1 months since 1930-01-01 == 1930-01-31T10:29:03Z
 2 months since 1930-01-01 == 1930-03-02T20:58:07Z
 3 months since 1930-01-01 == 1930-04-02T07:27:11Z
 4 months since 1930-01-01 == 1930-05-02T17:56:15Z
 5 months since 1930-01-01 == 1930-06-02T04:25:19Z
 6 months since 1930-01-01 == 1930-07-02T14:54:22Z
 7 months since 1930-01-01 == 1930-08-02T01:23:26Z
 8 months since 1930-01-01 == 1930-09-01T11:52:30Z
 9 months since 1930-01-01 == 1930-10-01T22:21:34Z
10 months since 1930-01-01 == 1930-11-01T08:50:38Z
11 months since 1930-01-01 == 1930-12-01T19:19:42Z
~~~

~~~
 0 years since 1850-01-01 == 1850-01-01T00:00:00Z
10 years since 1850-01-01 == 1860-01-01T10:07:39Z
20 years since 1850-01-01 == 1869-12-31T20:15:19Z
30 years since 1850-01-01 == 1880-01-01T06:22:59Z
40 years since 1850-01-01 == 1889-12-31T16:30:38Z
50 years since 1850-01-01 == 1900-01-01T02:38:18Z
60 years since 1850-01-01 == 1910-01-01T12:45:58Z
70 years since 1850-01-01 == 1920-01-01T22:53:38Z
80 years since 1850-01-01 == 1930-01-01T09:01:17Z
90 years since 1850-01-01 == 1940-01-01T19:08:57Z
~~~

## CDM calendar_field unit grammar
The CDM accepts an extended form of udunit grammar, called a calendar field unit:

~~~
CALENDAR period SINCE reference_date
~~~

The presence of `CALENDAR` (case insensitive) means that the CDM library manipulates the calendar field directly, rather than converting the period to a fixed multiple of seconds.
The actual result depends on the calendar used.
Note that values of the period must be an integer.

For example:
~~~
 1 calendar months since 1930-01-01 00:00:00Z == 1930-02-01T00:00:00.000Z
 2 calendar months since 1930-01-01 00:00:00Z == 1930-03-01T00:00:00.000Z
 3 calendar months since 1930-01-01 00:00:00Z == 1930-04-01T00:00:00.000Z
 4 calendar months since 1930-01-01 00:00:00Z == 1930-05-01T00:00:00.000Z
 5 calendar months since 1930-01-01 00:00:00Z == 1930-06-01T00:00:00.000Z
 6 calendar months since 1930-01-01 00:00:00Z == 1930-07-01T00:00:00.000Z
 7 calendar months since 1930-01-01 00:00:00Z == 1930-08-01T00:00:00.000Z
 8 calendar months since 1930-01-01 00:00:00Z == 1930-09-01T00:00:00.000Z
 9 calendar months since 1930-01-01 00:00:00Z == 1930-10-01T00:00:00.000Z
10 calendar months since 1930-01-01 00:00:00Z == 1930-11-01T00:00:00.000Z
11 calendar months since 1930-01-01 00:00:00Z == 1930-12-01T00:00:00.000Z
12 calendar months since 1930-01-01 00:00:00Z == 1931-01-01T00:00:00.000Z
~~~

~~~
 1 calendar years since 1930-01-01 00:00:00Z == 1931-01-01T00:00:00.000Z
 2 calendar years since 1930-01-01 00:00:00Z == 1932-01-01T00:00:00.000Z
 3 calendar years since 1930-01-01 00:00:00Z == 1933-01-01T00:00:00.000Z
 4 calendar years since 1930-01-01 00:00:00Z == 1934-01-01T00:00:00.000Z
 5 calendar years since 1930-01-01 00:00:00Z == 1935-01-01T00:00:00.000Z
 6 calendar years since 1930-01-01 00:00:00Z == 1936-01-01T00:00:00.000Z
 7 calendar years since 1930-01-01 00:00:00Z == 1937-01-01T00:00:00.000Z
 8 calendar years since 1930-01-01 00:00:00Z == 1938-01-01T00:00:00.000Z
 9 calendar years since 1930-01-01 00:00:00Z == 1939-01-01T00:00:00.000Z
10 calendar years since 1930-01-01 00:00:00Z == 1940-01-01T00:00:00.000Z
11 calendar years since 1930-01-01 00:00:00Z == 1941-01-01T00:00:00.000Z
12 calendar years since 1930-01-01 00:00:00Z == 1942-01-01T00:00:00.000Z
~~~

Note that invalid dates are decremented until valid. For example:
~~~
 0 calendar months since 1930-01-31 00:00:00Z == 1930-01-31T00:00:00.000Z
 1 calendar months since 1930-01-31 00:00:00Z == 1930-02-28T00:00:00.000Z (1930-02-31
                                                                           1930-02-30
                                                                           1930-02-29 not valid)
 2 calendar months since 1930-01-31 00:00:00Z == 1930-03-31T00:00:00.000Z
 3 calendar months since 1930-01-31 00:00:00Z == 1930-04-30T00:00:00.000Z (1930-04-31 not valid)
 4 calendar months since 1930-01-31 00:00:00Z == 1930-05-31T00:00:00.000Z
 5 calendar months since 1930-01-31 00:00:00Z == 1930-06-30T00:00:00.000Z (1930-06-31 not valid)
 6 calendar months since 1930-01-31 00:00:00Z == 1930-07-31T00:00:00.000Z
 7 calendar months since 1930-01-31 00:00:00Z == 1930-08-31T00:00:00.000Z
 8 calendar months since 1930-01-31 00:00:00Z == 1930-09-30T00:00:00.000Z (1930-09-31 not valid)
 9 calendar months since 1930-01-31 00:00:00Z == 1930-10-31T00:00:00.000Z
10 calendar months since 1930-01-31 00:00:00Z == 1930-11-30T00:00:00.000Z (1930-11-31 not valid)
11 calendar months since 1930-01-31 00:00:00Z == 1930-12-31T00:00:00.000Z
12 calendar months since 1930-01-31 00:00:00Z == 1931-01-31T00:00:00.000Z
~~~

and:
~~~
 0 calendar years since 2008-02-29 00:00:00Z == 2008-02-29T00:00:00.000Z
 1 calendar years since 2008-02-29 00:00:00Z == 2009-02-28T00:00:00.000Z (2009-02-29 not valid)
 2 calendar years since 2008-02-29 00:00:00Z == 2010-02-28T00:00:00.000Z (2010-02-29 not valid)
 3 calendar years since 2008-02-29 00:00:00Z == 2011-02-28T00:00:00.000Z (2011-02-29 not valid)
 4 calendar years since 2008-02-29 00:00:00Z == 2012-02-29T00:00:00.000Z
 5 calendar years since 2008-02-29 00:00:00Z == 2013-02-28T00:00:00.000Z (2013-02-29 not valid)
 6 calendar years since 2008-02-29 00:00:00Z == 2014-02-28T00:00:00.000Z (2014-02-29 not valid)
 7 calendar years since 2008-02-29 00:00:00Z == 2015-02-28T00:00:00.000Z (2015-02-29 not valid)
 8 calendar years since 2008-02-29 00:00:00Z == 2016-02-29T00:00:00.000Z
 9 calendar years since 2008-02-29 00:00:00Z == 2017-02-28T00:00:00.000Z (2017-02-29 not valid)
10 calendar years since 2008-02-29 00:00:00Z == 2018-02-28T00:00:00.000Z (2018-02-29 not valid)
11 calendar years since 2008-02-29 00:00:00Z == 2019-02-28T00:00:00.000Z (2019-02-29 not valid)
12 calendar years since 2008-02-29 00:00:00Z == 2020-02-29T00:00:00.000Z
13 calendar years since 2008-02-29 00:00:00Z == 2021-02-28T00:00:00.000Z (2021-02-29 not valid)
14 calendar years since 2008-02-29 00:00:00Z == 2022-02-28T00:00:00.000Z (2022-02-29 not valid)
~~~

## W3C profile of ISO 8601
The CDM uses the [W3C profile of ISO 8601](http://www.w3.org/TR/NOTE-datetime.html){:target="_blank"} formatting for reading and writing calendar dates:

> The formats defined by the W3C profile of ISO 8601 are as follows.
>
> Exactly the components shown here must be present, with exactly this punctuation.
> Note that the `T` appears literally in the string, to indicate the beginning of the time element, as specified in ISO 8601.
>
>  ~~~
>  Year:
>    YYYY (eg 1997)
>  Year and month:
>    YYYY-MM (eg 1997-07)
>  Complete date:
>    YYYY-MM-DD (eg 1997-07-16)
>  Complete date plus hours and minutes:
>    YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
>  Complete date plus hours, minutes and seconds:
>    YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
>  Complete date plus hours, minutes, seconds and a decimal fraction of a second
>    YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
>  ~~~
>
>  where:
>
>  ~~~
>  YYYY = four-digit year
>  MM   = two-digit month (01=January, etc.)
>  DD   = two-digit day of month (01 through 31)
>  hh   = two digits of hour (00 through 23) (am/pm NOT allowed)
>  mm   = two digits of minute (00 through 59)
>  ss   = two digits of second (00 through 59)
>  s    = one or more digits representing a decimal fraction of a second
>  TZD  = time zone designator (Z or +hh:mm or -hh:mm)
>  ~~~

but with the following differences, to allow backwards compatibility with UDUNITS:
* You may use a space (` `) instead of the `T`
* The year may be preceded by a `+` (ignored) or a `-` (makes the date `BCE`)
* The date part uses a `-` delimiter instead of a fixed number of digits for each field
* The time part uses a `:` delimiter instead of a fixed number of digits for each field
* The time zone designator may be `Z`, `UTC`, or `GMT` (case insensitive) or `+hh:mm` or `-hh:mm`
* The time zone may be omitted, and then `UTC` is assumed.

In addition, the following must be done:
* Any fields that are not specified are set to zero.

## Possible extentions (not implemented)

~~~
N [calendar] UNITS since DATE         // current


N [calendar] UNITS since 65500 BCE   // assume we mean 0Z on 1 Jan. always calendar years
N [calendar] UNITS since 65500k BCE
N [calendar] UNITS since 65M BCE


N calendar Myears before 1980-01-01
~~~

