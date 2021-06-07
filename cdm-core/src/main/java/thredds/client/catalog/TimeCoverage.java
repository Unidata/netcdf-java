/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.text.ParseException;
import ucar.nc2.calendar.CalendarDate;

/**
 * A range of dates, using DateType start/end, and/or a TimeDuration.
 * You can use a DateType = "present" and a time duration to specify "real time" intervals, eg
 * "last 3 days" uses endDate = "present" and duration = "3 days".
 * <p>
 * </p>
 * A DateRange can be specified in any of the following ways:
 * <ol>
 * <li>a start date and end date
 * <li>a start date and duration
 * <li>an end date and duration
 * </ol>
 */
@Immutable
public class TimeCoverage {
  private final DateType start, end;
  private final TimeDuration duration, resolution;
  private final boolean isEmpty, isMoving, useStart, useEnd, useDuration, useResolution;

  /**
   * Determine if the given date is contained in this date range.
   * The date range includes the start and end dates.
   *
   * @param date the gicen date
   * @return true if date in inside this range
   */
  public boolean contains(CalendarDate date) {
    if (isEmpty) {
      return false;
    }
    DateType datet = new DateType(date);
    if (datet.before(getStart())) {
      return false;
    }
    return !getEnd().before(datet);
  }

  /**
   * Determine if the given range intersects this TimeCoverage.
   *
   * @param start_want range starts here
   * @param end_want range ends here
   * @return true if ranges intersect
   */
  public boolean intersects(CalendarDate start_want, CalendarDate end_want) {
    if (isEmpty) {
      return false;
    }
    DateType endWantT = new DateType(end_want);
    if (endWantT.before(getStart())) {
      return false;
    }
    DateType startWantT = new DateType(start_want);
    return !getEnd().before(startWantT);
  }

  /**
   * Determine if the given range intersects this TimeCoverage.
   *
   * @param other date range
   * @return true if ranges intersect
   */
  public boolean intersects(TimeCoverage other) {
    return intersects(other.getStart().getCalendarDate(), other.getEnd().getCalendarDate());
  }

  /**
   * Intersect with another TimeCoverage
   *
   * @param clip intersect with this date range
   * @return new TimeCoverage that is the intersection
   */
  public TimeCoverage intersect(TimeCoverage clip) {
    if (isEmpty)
      return this;
    if (clip.isEmpty)
      return clip;

    DateType ss = getStart();
    DateType s = ss.before(clip.getStart()) ? clip.getStart() : ss;

    DateType ee = getEnd();
    DateType e = ee.before(clip.getEnd()) ? ee : clip.getEnd();

    return create(s, e, null, resolution);
  }

  /**
   * Extend this TimeCoverage by the given one.
   * 
   * @return new TimeCoverage that is extended if needed.
   */
  public TimeCoverage extend(TimeCoverage dr) {
    boolean localEmpty = isEmpty;

    DateType start = (localEmpty || dr.getStart().before(getStart())) ? dr.getStart() : this.getStart();

    DateType end = (localEmpty || getEnd().before(dr.getEnd())) ? dr.getEnd() : this.getEnd();

    return create(start, end, null, resolution);
  }

  /**
   * Extend this TimeCoverage by the given date.
   * 
   * @return new TimeCoverage that is extended if needed.
   */
  public TimeCoverage extend(CalendarDate date) {
    DateType extended = new DateType(date);
    DateType start = extended.before(getStart()) ? extended : this.getStart();
    DateType end = getEnd().before(extended) ? extended : this.getEnd();
    return create(start, end, null, resolution);
  }

  /**
   * Get the starting Date.
   */
  public DateType getStart() {
    return (isMoving && !useStart) ? this.end.subtract(duration) : start;
  }

  /**
   * Get the ending Date.
   */
  public DateType getEnd() {
    return (isMoving && !useEnd) ? this.start.add(duration) : end;
  }

  /**
   * Get the duration of the interval
   */
  public TimeDuration getDuration() {
    if (isMoving && !useDuration) {
      long min = start.getCalendarDate().getMillisFromEpoch();
      long max = end.getCalendarDate().getMillisFromEpoch();
      double secs = .001 * (max - min);
      if (secs < 0) {
        secs = 0;
      }

      // LOOK probably wrong
      if (resolution == null) {
        return new TimeDuration(duration.getTimeUnit().newValueInSeconds(secs));
      } else {
        // make it a multiple of resolution
        double resSecs = resolution.getValueInSeconds();
        double closest = Math.round(secs / resSecs);
        secs = closest * resSecs;
        return new TimeDuration(duration.getTimeUnit().newValueInSeconds(secs));
      }
    }
    return duration;
  }

  /**
   * Get the time resolution.
   */
  public TimeDuration getResolution() {
    return resolution;
  }

  /**
   * Get if the start is fixed.
   */
  public boolean fixedStart() {
    return useStart;
  }

  /**
   * Get if the end is fixed.
   */
  public boolean fixedEnd() {
    return useEnd;
  }

  /**
   * Get if the duration is fixed.
   */
  public boolean fixedDuration() {
    return useDuration;
  }

  /**
   * Get if the resolution is set.
   */
  public boolean fixedResolution() {
    return useResolution;
  }

  /**
   * Return true if start date equals end date, so date range is a point.
   */
  public boolean isPoint() {
    return !isEmpty && start.equals(end);
  }

  /**
   * If the range is empty
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  @Override
  public String toString() {
    return "start= " + start + " end= " + end + " duration= " + duration + " resolution= " + resolution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TimeCoverage))
      return false;
    TimeCoverage oo = (TimeCoverage) o;
    if (useStart && !start.equals(oo.start))
      return false;
    if (useEnd && !end.equals(oo.end))
      return false;
    if (useDuration && !duration.equals(oo.duration))
      return false;
    return !useResolution || resolution.equals(oo.resolution);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (useStart)
        result = 37 * result + start.hashCode();
      if (useEnd)
        result = 37 * result + end.hashCode();
      if (useDuration)
        result = 37 * result + duration.hashCode();
      if (useResolution)
        result = 37 * result + resolution.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode; // Bloch, item 8

  /////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsulates a range of dates, using DateType start/end, and/or a TimeDuration.
   * A DateRange can be specified in any of the following ways:
   * <ol>
   * <li>a start date and end date
   * <li>a start date and duration
   * <li>an end date and duration
   * </ol>
   *
   * @param start starting date; may be null
   * @param end ending date; may be null
   * @param duration time duration; may be null
   * @param resolution time resolution; may be null
   */
  public static TimeCoverage create(@Nullable DateType start, @Nullable DateType end, @Nullable TimeDuration duration,
      @Nullable TimeDuration resolution) {
    return new Builder(start, end, duration, resolution).build();
  }

  static Builder builder() {
    return new Builder();
  }

  public static Builder builder(@Nullable DateType start, @Nullable DateType end, @Nullable TimeDuration duration,
      @Nullable TimeDuration resolution) {
    return new Builder(start, end, duration, resolution);
  }

  private TimeCoverage(Builder builder) {
    this.start = builder.start;
    this.end = builder.end;
    this.duration = builder.duration;
    this.resolution = builder.resolution;

    this.isEmpty = builder.isEmpty;
    this.isMoving = builder.isMoving;
    this.useStart = builder.useStart;
    this.useEnd = builder.useEnd;
    this.useDuration = builder.useDuration;
    this.useResolution = builder.useResolution;
  }

  public static class Builder {
    private DateType start, end;
    private TimeDuration duration, resolution;
    private boolean isEmpty, isMoving, useStart, useEnd, useDuration, useResolution;

    Builder() {}

    Builder(@Nullable DateType start, @Nullable DateType end, @Nullable TimeDuration duration,
        @Nullable TimeDuration resolution) {
      this.start = start;
      this.end = end;
      this.duration = duration;
      this.resolution = resolution;

      useStart = (start != null) && !start.isBlank();
      useEnd = (end != null) && !end.isBlank();
      useDuration = (duration != null) && !duration.isBlank();
      useResolution = (resolution != null);

      boolean invalid = true;
      if (useStart && useEnd) {
        invalid = false;
        this.isMoving = this.start.isPresent() || this.end.isPresent();
        useDuration = false;
        recalcDuration();

      } else if (useStart && useDuration) {
        invalid = false;
        this.isMoving = this.start.isPresent();
        this.end = this.start.add(duration);

      } else if (useEnd && useDuration) {
        invalid = false;
        this.isMoving = this.end.isPresent();
        this.start = this.end.subtract(duration);
      }

      if (invalid)
        throw new IllegalArgumentException("DateRange must have 2 of start, end, duration");

      checkIfEmpty();
    }

    private void checkIfEmpty() {
      if (this.start.isPresent() && this.end.isPresent())
        isEmpty = true;
      else if (this.start.isPresent() || this.end.isPresent())
        isEmpty = duration.getValue() <= 0;
      else
        isEmpty = this.end.before(this.start);

      if (isEmpty) {
        duration = new TimeDuration(duration.getTimeUnit().newValue(0));
      }
    }

    // choose a resolution based on # seconds
    private String chooseResolution(double time) {
      if (time < 180) // 3 minutes
        return "secs";
      time /= 60; // minutes
      if (time < 180) // 3 hours
        return "minutes";
      time /= 60; // hours
      if (time < 72) // 3 days
        return "hours";
      time /= 24; // days
      if (time < 90) // 3 months
        return "days";
      time /= 30; // months
      if (time < 36) // 3 years
        return "months";
      return "years";
    }

    /**
     * Set the duration of the interval. Makes useDuration true.
     * If useStart, recalculate end, else recalculate start.
     *
     * @param duration duration of the interval
     */
    public void setDuration(TimeDuration duration) {
      this.duration = duration;
      useDuration = true;

      if (useStart) {
        this.isMoving = this.start.isPresent();
        this.end = this.start.add(duration);
        useEnd = false;

      } else {
        this.isMoving = this.end.isPresent();
        this.start = this.end.subtract(duration);
      }
      checkIfEmpty();
    }

    /**
     * Set the ending Date. Makes useEnd true.
     * If useStart, recalculate the duration, else recalculate start.
     *
     * @param end ending Date
     */
    public void setEnd(DateType end) {
      this.end = end;
      useEnd = true;

      if (useStart) {
        this.isMoving = this.start.isPresent() || this.end.isPresent();
        useDuration = false;
        recalcDuration();

      } else {
        this.isMoving = this.end.isPresent();
        this.start = this.end.subtract(duration);
      }
      checkIfEmpty();
    }

    /**
     * Set the time resolution.
     *
     * @param resolution the time resolution
     */
    public void setResolution(TimeDuration resolution) {
      this.resolution = resolution;
      useResolution = true;
    }

    /**
     * Set the starting Date. Makes useStart true.
     * If useEnd, recalculate the duration, else recalculate end.
     *
     * @param start starting Date
     */
    public void setStart(DateType start) {
      this.start = start;
      useStart = true;

      if (useEnd) {
        this.isMoving = this.start.isPresent() || this.end.isPresent();
        useDuration = false;
        recalcDuration();

      } else {
        this.isMoving = this.start.isPresent();
        this.end = this.start.add(duration);
      }
      checkIfEmpty();
    }

    // assumes not moving
    private void recalcDuration() {
      long min = start.getCalendarDate().getMillisFromEpoch();
      long max = end.getCalendarDate().getMillisFromEpoch();
      double secs = .001 * (max - min);
      if (secs < 0)
        secs = 0;

      if (duration == null) {
        try {
          duration = TimeDuration.parse(chooseResolution(secs));
        } catch (ParseException e) {
          // cant happen
          throw new RuntimeException(e);
        }
      }

      if (resolution == null) {
        duration = new TimeDuration(duration.getTimeUnit().newValueInSeconds(secs));
      } else {
        // make it a multiple of resolution
        double resSecs = resolution.getValueInSeconds();
        double closest = Math.round(secs / resSecs);
        secs = closest * resSecs;
        duration = new TimeDuration(duration.getTimeUnit().newValueInSeconds(secs));
      }
    }

    public TimeCoverage build() {
      return new TimeCoverage(this);
    }
  }
}
