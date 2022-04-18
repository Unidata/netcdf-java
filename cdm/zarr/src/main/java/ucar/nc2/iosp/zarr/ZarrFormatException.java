/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

public class ZarrFormatException extends Exception {
  private static final String defaultExceptionMessage = "Invalid Zarr format: one or more objects were skipped.";

  private static final String customExceptionMessageFormat =
      "Invalid Zarr format (field: %s, value: %s): one or more objects were skipped.";

  public ZarrFormatException() {
    super(defaultExceptionMessage);
  }

  public ZarrFormatException(String invalidField, String value) {
    super(String.format(customExceptionMessageFormat, invalidField, value));
  }

  public ZarrFormatException(String msg) {
    super(msg);
  }
}
