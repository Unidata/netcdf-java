/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

/** Thrown if an attempt is made to use an invalid Range to index an array. */
public class InvalidRangeException extends Exception {
  public InvalidRangeException(String s) {
    super(s);
  }
}
