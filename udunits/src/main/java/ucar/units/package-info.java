/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * DO NOT USE, internal to the Java-NetCDF library.
 * Provides support for parsing and formatting string unit specification,
 * converting numerical values between compatible units, and performing arithmetic
 * on units (such as dividing one unit by another).
 * <h2>
 * Examples of Intended Use</h2>
 * The following code will print the string<tt> "5 knots is 2.57222 m/s"</tt>:
 * <blockquote><tt>UnitFormat format = UnitFormatManager.instance();</tt>
 * <br>
 * <tt>Unit meter = format.parse("meter");</tt>
 * <br>
 * <tt>Unit second = format.parse("second");</tt>
 * <br>
 * <tt>Unit meterPerSecondUnit = meter.divideBy(second);</tt>
 * <br>
 * <tt>Unit knot = format.parse("knot");</tt>
 * <br>
 * <tt>if (meterPerSecondUnit.isCompatible(knot) {</tt>
 * <br>
 * <tt>&nbsp;&nbsp;&nbsp; System.out.println("5 knots is " +</tt>
 * <br>
 * <tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; knot.convertTo(5, meterPerSecondUnit)
 * +</tt>
 * <br>
 * <tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ' ' + format.format(meterPerSecondUnit));</tt>
 * <br>
 * <tt>}</tt>
 * <br>
 * <tt></tt>&nbsp;</blockquote>
 */
package ucar.units;
