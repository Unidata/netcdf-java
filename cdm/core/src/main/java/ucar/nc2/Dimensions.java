/* Copyright Unidata */
package ucar.nc2;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

/**
 * Static helper methnods for Dimension.
 *
 * @author caron
 * @since 10/3/2019.
 */
public class Dimensions {

  /**
   * Make a ucar.ma2.Section from an ordered set of Dimension onjects.
   */
  public static ucar.ma2.Section makeSectionFromDimensions(Iterable<Dimension> dimensions) {
    try {
      List<Range> list = new ArrayList<>();
      for (Dimension d : dimensions) {
        int len = d.getLength();
        if (len > 0)
          list.add(new Range(d.getShortName(), 0, len - 1));
        else if (len == 0)
          list.add(Range.EMPTY); // LOOK empty not named
        else {
          assert d.isVariableLength();
          list.add(Range.VLEN); // LOOK vlen not named
        }
      }
      return new ucar.ma2.Section(list).makeImmutable();

    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  /**
   * Make a space-delineated String from a list of Dimension names. Inverse of makeDimensionsList().
   *
   * @return space-delineated String of Dimension names.
   */
  public static String makeDimensionsString(List<Dimension> dimensions) {
    if (dimensions == null)
      return "";

    Formatter buf = new Formatter();
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      String dimName = myd.getShortName();

      if (i != 0)
        buf.format(" ");

      if (myd.isVariableLength()) {
        buf.format("*");
      } else if (myd.isShared()) {
        buf.format("%s", dimName);
      } else {
        // if (dimName != null) // LOOK losing anon dim name
        // buf.format("%s=", dimName);
        buf.format("%d", myd.getLength());
      }
    }
    return buf.toString();
  }

  /**
   * Create a dimension list using the dimensions names. Inverse of makeDimensionsString().
   *
   * @param dimensions list of possible dimensions, may not be null
   * @param dimString  : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for anon dimension. null or
   *                   empty String is a scalar.
   * @return list of dimensions
   * @throws IllegalArgumentException if cant find dimension or parse error.
   */
  public static List<Dimension> makeDimensionsList(List<Dimension> dimensions, String dimString) {
    List<Dimension> newDimensions = new ArrayList<>();
    if (dimString == null) // scalar
      return newDimensions; // empty list
    dimString = dimString.trim();
    if (dimString.isEmpty()) // scalar
      return newDimensions; // empty list

    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension dim;
      if (dimName.equals("*"))
        dim = Dimension.VLEN;
      else
        dim = dimensions.stream().filter(d -> d.getShortName().equals(dimName)).findFirst().orElse(null);
      if (dim == null) {
        // if numeric - then its anonymous dimension
        try {
          int len = Integer.parseInt(dimName);
          dim = Dimension.builder().setLength(len).setIsShared(false).build();
        } catch (Exception e) {
          throw new IllegalArgumentException("Dimension " + dimName + " does not exist");
        }
      }
      newDimensions.add(dim);
    }
    return newDimensions;
  }

  /** Make a list of private dimensions from an array of lengths */
  public static List<Dimension> makeDimensionsAnon(int[] shape) {
    List<Dimension> newDimensions = new ArrayList<>();
    if ((shape == null) || (shape.length == 0)) { // scalar
      return newDimensions;
    }
    for (int len : shape)
      newDimensions.add(Dimension.builder().setLength(len).setIsShared(false).build());
    return newDimensions;
  }
}