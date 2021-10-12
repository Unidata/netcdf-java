/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

public class UnknownFilterException extends Exception {

  private static final String nameExceptionMessage = "Unknown filter: no filter found with name %s";

  private static final String idExceptionMessage = "Unknown filter: no filter found with id %d";

  public UnknownFilterException(String name) {
    super(String.format(nameExceptionMessage, name));
  }

  public UnknownFilterException(int id) {
    super(String.format(idExceptionMessage, id));
  }
}
