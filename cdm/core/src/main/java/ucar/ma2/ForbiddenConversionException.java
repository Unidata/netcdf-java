/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Thrown for an attempt to make a forbidden conversion on primitive data values,
 * eg boolean to double.
 */
public final class ForbiddenConversionException extends RuntimeException {
  public ForbiddenConversionException() {}

  public ForbiddenConversionException(String s) {
    super(s);
  }
}
