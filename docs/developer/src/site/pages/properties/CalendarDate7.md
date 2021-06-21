# Calendar Date handling in version 7

As of version 7, julian and mixed Gregorian calendars are no longer supported. For CF encoded datasets, julian, gregorian, and 
proleptic_gregorian are all mapped to the ISO8601 calendar. The noleap, all_leap, and uniform30day are each 
implemented and we believe them to work correctly.

The decision to stop supporting julian and mixed Gregorian calendars is based on balancing the long term maintainablilty 
of the netcdf-java library with the actual needs of these calendars in real world scientific datasets.

If you have a real world dataset that is adversely affected by this, please contact us and we will evaluate.

### Background

1. The choice of the mixed Gregorian calendar by CF was mostly motivated by the fact that the udunit library used it.
While this was a reasonable choice at the time it was made, subsequent standardization of the ISO8601 standard makes it 
a burden.

2. ISO8601 implementation libraries are available in all major programming languages. Julian and mixed Gregorian calendars are not
available in the standard Java libraries.

3. The julian and mixed Gregorian calendars are "real world historical calendars", that is, they are used in some contexts
to express historical dates. These differ from ISO before some cutoff. Udunits uses a cutoff date in 1582, when the gregorian
calendar was first introduced. It was adopted at different times in different places. For example, The British started 
using it in 1752. So there is is at least this cutoff date that needs to be specified to be correct.

4. We believe that most CF datasets that are depending on mixed gregorian datasets are doing so inadvertantly. 
In the case where they are modeling climate before 1582, they will be much better served using a non-standard calendar,
or of using the continuous ISO calendar. That is to say, they have no need to skip 10 days in 1582 to match one arbitrary 
encoding of historical dates.

5. We believe that most if not all users of netCDF and CDM do not need this feature and are better served using ISO dates. 
The CF/udunits specification for dates leaves out the cutoff date. There may be other subtleties of historical encodings
best left to specialists. In short, its better for a library to not do something, than to do it wrong.

6. If you have a need to store historical encoded dates, we suggest that you:
  1. Code the historical dates as strings, and process these dates yourself. 
  2. Also map your historical dates to ISO in some way that meets your needs and allows you to 
  use the standard ISO libaries. 

### Java

1. The java.time library does not support julian and mixed Gregorian calendars.

2. The julian calendar is supported by threeten-extra. A variant of the mixed Gregorian calendar (BritishCutoverChronology) is supported.

````
 * The British calendar system follows the rules of the Julian calendar
 * until 1752 and the rules of the Gregorian (ISO) calendar since then.
 * The Julian differs from the Gregorian only in terms of the leap year rule.
 * <p>
 * The Julian and Gregorian calendar systems are linked to Rome and the Vatican
 * with the Julian preceding the Gregorian. The Gregorian was introduced to
 * handle the drift of the seasons through the year due to the inaccurate
 * Julian leap year rules. When first introduced by the Vatican in 1582,
 * the cutover resulted in a "gap" of 10 days.
 * <p>
 * While the calendar was introduced in 1582, it was not adopted everywhere.
 * Britain did not adopt it until the 1752, when Wednesday 2nd September 1752
 * was followed by Thursday 14th September 1752.
````

3. The difficulty of supporting non-standard calendars. 

The designers of the java.time API strongly recommend against user-facing alternate Chronologies.

These are snippets from [ChronoLocalDate javadoc](https://docs.oracle.com/javase/8/docs/api/java/time/chrono/ChronoLocalDate.html):

#### When to use this interface
The design of the API encourages the use of LocalDate rather than this interface, even in the case where the application needs to deal with multiple calendar systems.
This concept can seem surprising at first, as the natural way to globalize an application might initially appear to be to abstract the calendar system. However, as explored below, abstracting the calendar system is usually the wrong approach, resulting in logic errors and hard to find bugs. As such, it should be considered an application-wide architectural decision to choose to use this interface as opposed to LocalDate.

### Architectural issues to consider
These are some of the points that must be considered before using this interface throughout an application.
1) Applications using this interface, as opposed to using just LocalDate, face a significantly higher probability of bugs. This is because the calendar system in use is not known at development time. A key cause of bugs is where the developer applies assumptions from their day-to-day knowledge of the ISO calendar system to code that is intended to deal with any arbitrary calendar system. The section below outlines how those assumptions can cause problems The primary mechanism for reducing this increased risk of bugs is a strong code review process. This should also be considered a extra cost in maintenance for the lifetime of the code.

2) This interface does not enforce immutability of implementations. While the implementation notes indicate that all implementations must be immutable there is nothing in the code or type system to enforce this. Any method declared to accept a ChronoLocalDate could therefore be passed a poorly or maliciously written mutable implementation.

3) Applications using this interface must consider the impact of eras. LocalDate shields users from the concept of eras, by ensuring that getYear() returns the proleptic year. That decision ensures that developers can think of LocalDate instances as consisting of three fields - year, month-of-year and day-of-month. By contrast, users of this interface must think of dates as consisting of four fields - era, year-of-era, month-of-year and day-of-month. The extra era field is frequently forgotten, yet it is of vital importance to dates in an arbitrary calendar system. For example, in the Japanese calendar system, the era represents the reign of an Emperor. Whenever one reign ends and another starts, the year-of-era is reset to one.

4) The only agreed international standard for passing a date between two systems is the ISO-8601 standard which requires the ISO calendar system. Using this interface throughout the application will inevitably lead to the requirement to pass the date across a network or component boundary, requiring an application specific protocol or format.

5) Long term persistence, such as a database, will almost always only accept dates in the ISO-8601 calendar system (or the related Julian-Gregorian). Passing around dates in other calendar systems increases the complications of interacting with persistence.

6) Most of the time, passing a ChronoLocalDate throughout an application is unnecessary, as discussed in the last section below.

### False assumptions causing bugs in multi-calendar system code
As indicated above, there are many issues to consider when try to use and manipulate a date in an arbitrary calendar system. These are some of the key issues.
Code that queries the day-of-month and assumes that the value will never be more than 31 is invalid. Some calendar systems have more than 31 days in some months.

Code that adds 12 months to a date and assumes that a year has been added is invalid. Some calendar systems have a different number of months, such as 13 in the Coptic or Ethiopic.

Code that adds one month to a date and assumes that the month-of-year value will increase by one or wrap to the next year is invalid. Some calendar systems have a variable number of months in a year, such as the Hebrew.

Code that adds one month, then adds a second one month and assumes that the day-of-month will remain close to its original value is invalid. Some calendar systems have a large difference between the length of the longest month and the length of the shortest month. For example, the Coptic or Ethiopic have 12 months of 30 days and 1 month of 5 days.

Code that adds seven days and assumes that a week has been added is invalid. Some calendar systems have weeks of other than seven days, such as the French Revolutionary.

Code that assumes that because the year of date1 is greater than the year of date2 then date1 is after date2 is invalid. This is invalid for all calendar systems when referring to the year-of-era, and especially untrue of the Japanese calendar system where the year-of-era restarts with the reign of every new Emperor.

Code that treats month-of-year one and day-of-month one as the start of the year is invalid. Not all calendar systems start the year when the month value is one.

In general, manipulating a date, and even querying a date, is wide open to bugs when the calendar system is unknown at development time. This is why it is essential that code using this interface is subjected to additional code reviews. It is also why an architectural decision to avoid this interface type is usually the correct one.
