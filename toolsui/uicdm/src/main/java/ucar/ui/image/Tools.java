/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.image;

public class Tools {
  private static boolean debug;

  static void log(String s) {
    if (debug)
      System.out.println(s);
  }
}
