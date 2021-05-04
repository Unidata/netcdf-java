/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

/** A mix-in interface for evaluating if a value is missing. */
public interface IsMissingEvaluator {
  /** true if there may be missing data */
  boolean hasMissing();

  /** Test if val is a missing data value */
  boolean isMissing(double val);
}
