/*
 * VisAD system for interactive analysis and visualization of numerical
 * data. Copyright (C) 1996 - 2011 Bill Hibbard, Curtis Rueden, Tom
 * Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
 * Tommy Jasmin.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA
 */

package ucar.nc2.iosp.mcidas;

/**
 * Class for defining a few common atmospheric science units which don't
 * conform to the standards used by the VisAD netCDF Units package.
 * Use the makeSymbol method to return a "proper" unit symbol from a
 * common one (ex: input mph to get mi/h). Also allows one to input
 * a common symbol in upper case (MPH) and get the proper one back (mi/h).
 * 
 * @author Tom Whittaker, SSEC
 */
public class MetUnits {

  /*
   * static {
   * try {
   * UnitsDB du = DefaultUnitsDB.instance();
   * Unit hpa = du.get("hPa");
   * hpa = hpa.clone("hPa");
   * du.putSymbol("HPA", hpa);
   * du.putSymbol("hPa", hpa);
   * du.putSymbol("MB", hpa);
   * du.putSymbol("mb", hpa);
   * Unit pvu = Parser.parse("10-6 m2 s-1K kg-1");
   * pvu = pvu.clone("PVU");
   * du.putSymbol("PVU",pvu);
   * du.putSymbol("pvu",pvu);
   * } catch (Exception e) {
   * System.err.println("Unable to update UnitsDB");
   * }
   * }
   */

  /**
   * Create a 'proper' unit symbol from a common one (ie: mph -> mi/h instead
   * of milliphots)
   *
   * @param commonSymbol is the String of the original unit name
   *
   * @return commonSymbol converted to "proper" symbol or commonSymbol if
   *         unknown
   */
  public static String makeSymbol(String commonSymbol) {
    if (commonSymbol == null)
      return null;
    String in = commonSymbol.trim();
    String out = in;
    if (in.equalsIgnoreCase("m")) {
      out = "m";
    } else if (in.equalsIgnoreCase("sec")) {
      out = "s";
    } else if (in.equalsIgnoreCase("mps")) {
      out = "m/s";
    } else if (in.equalsIgnoreCase("mph")) {
      out = "mi/h";
    } else if (in.equalsIgnoreCase("kph")) {
      out = "km/h";
    } else if (in.equalsIgnoreCase("fps")) {
      out = "ft/s";
    } else if (in.equalsIgnoreCase("km")) {
      out = "km";
    } else if (in.equalsIgnoreCase("dm")) {
      out = "dm";
    } else if (in.equalsIgnoreCase("cm")) {
      out = "cm";
    } else if (in.equalsIgnoreCase("mm")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("mi")) {
      out = "mi";
    } else if (in.equalsIgnoreCase("pa")) {
      out = "Pa";
    } else if (in.equalsIgnoreCase("nmi")) {
      out = "nmi";
    } else if (in.equalsIgnoreCase("in")) {
      out = "in";
    } else if (in.equalsIgnoreCase("deg")) {
      out = "deg";
    } else if (in.equalsIgnoreCase("yd")) {
      out = "yd";
    } else if (in.equalsIgnoreCase("ft")) {
      out = "ft";
    } else if (in.equalsIgnoreCase("f")) {
      out = "degF";
    } else if (in.equalsIgnoreCase("c")) {
      out = "degC";
    } else if (in.equalsIgnoreCase("k")) {
      out = "K";
    } else if (in.equalsIgnoreCase("inhg")) {
      out = "inhg";
    } else if (in.equalsIgnoreCase("kt")) {
      out = "kt";
    } else if (in.equalsIgnoreCase("kts")) {
      out = "kt";
    } else if (in.equalsIgnoreCase("g/kg")) {
      out = "g/kg";
    } else if (in.equalsIgnoreCase("degrees n")) {
      out = "degrees_north";
    } else if (in.equalsIgnoreCase("degrees e")) {
      out = "degrees_east";
    } else if (in.equalsIgnoreCase("degree n")) {
      out = "degrees_north";
    } else if (in.equalsIgnoreCase("degree e")) {
      out = "degrees_east";
    } else if (in.equalsIgnoreCase("degree e")) {
      out = "degrees_east";
    } else if (in.equalsIgnoreCase("degree k")) {
      out = "K";
    } else if (in.equalsIgnoreCase("degrees k")) {
      out = "K";
    } else if (in.equalsIgnoreCase("degree c")) {
      out = "Celsius";
    } else if (in.equalsIgnoreCase("degrees c")) {
      out = "Celsius";
    } else if (in.equalsIgnoreCase("degree f")) {
      out = "Fahrenheit";
    } else if (in.equalsIgnoreCase("degrees f")) {
      out = "Fahrenheit";
    } else if (in.equalsIgnoreCase("gp m")) {
      out = "gpm";
    } else if (in.equalsIgnoreCase("gp_m")) {
      out = "gpm";
    } else if (in.equalsIgnoreCase("kg")) {
      out = "kg";
    } // handle Kg

    // the following are decidedly McIDAS
    else if (in.equalsIgnoreCase("paps")) {
      out = "Pa/s";
    } else if (in.equalsIgnoreCase("mgps")) {
      out = "km^2(kg/s)";
    } else if (in.equalsIgnoreCase("m2s2")) {
      out = "m^2/s^2";
    } else if (in.equalsIgnoreCase("kpm")) {
      out = "K/m";
    } else if (in.equalsIgnoreCase("m2ps")) {
      out = "m^2/s";
    } else if (in.equalsIgnoreCase("gpkg")) {
      out = "g/kg";
    } else if (in.equalsIgnoreCase("kgm2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kgm3")) {
      out = "kg/m^3";
    } else if (in.equalsIgnoreCase("mmps")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("ps")) {
      out = "s-1";
    } else if (in.equalsIgnoreCase("wpm2")) {
      out = "W/m^2";
    } else if (in.equalsIgnoreCase("nm2")) {
      out = "N/m^2";
    } else if (in.equalsIgnoreCase("kgps")) {
      out = "kg/kg/s";
    } else if (in.equalsIgnoreCase("ps2")) {
      out = "s^-2";
    } else if (in.equalsIgnoreCase("pspm")) {
      out = "s^-1m^-1";
    } else if (in.equalsIgnoreCase("jpkg")) {
      out = "J/kg";
    } else if (in.equalsIgnoreCase("kgkg")) {
      out = "kg/kg";
    } else if (in.equalsIgnoreCase("psps")) {
      out = "s^-1";
    } else if (in.equalsIgnoreCase("kps")) {
      out = "K/s";
    } else if (in.equalsIgnoreCase("k ft")) {
      out = "k ft";
    } else if (in.equalsIgnoreCase("1/sr")) {
      out = "sr-1";
    } else if (in.equalsIgnoreCase("mw**")) {
      out = "mW/m^2/sr/cm-1";
    } else if (in.equalsIgnoreCase("mwm2")) {
      out = "mW/m^2/sr/cm-1";
    } else if (in.equalsIgnoreCase("wm**")) {
      out = "W/m^2/sr/micron";
    } else if (in.equalsIgnoreCase("wp**")) {
      out = "W/m^2/sr";
    } else if (in.equalsIgnoreCase("mb")) {
      out = "hPa";
    } else if (in.equalsIgnoreCase("mbag")) {
      out = "hPa";
    } else if (in.equalsIgnoreCase("dbz")) {
      out = "dBz";
    }

    // the following handle (rightly or wrongly) converting mass/area to length
    else if (in.equalsIgnoreCase("kg/m**2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kg/m2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kg/m^2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kg m-2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kg.m-2")) {
      out = "mm";
    } else if (in.equalsIgnoreCase("kg/m**2/s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg/m**2s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg/m2/s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg/m2s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg/m^2/s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg/m^2s")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg m-2 s-1")) {
      out = "mm/s";
    } else if (in.equalsIgnoreCase("kg.m-2.s-1")) {
      out = "mm/s";
    }

    // and a few more
    else if (in.equalsIgnoreCase("W/m**2")) {
      out = "W/m^2";
    } else if (in.equalsIgnoreCase("W/m**2s")) {
      out = "W/m^2/s";
    }

    return out;

  }
}
