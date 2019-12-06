/* Copyright Unidata */
package ucar.nc2;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

/**
 * Static helper methods for Dimension.
 *
 * @author caron
 * @since 10/3/2019.
 */
public class Dimensions {

  public interface Find {
    @Nullable
    Dimension findByName(String dimName);
  }

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
   * Make a list of Dimensions from a list of names.
   * 
   * @param finder interface to find a Dikmension by name.
   * @param dimString space seperated list of dimension names.
   * @return equivalent list of Dimension objects.
   * @throws IllegalArgumentException if cant find or parse the name.
   */
  public static ImmutableList<Dimension> makeDimensionsList(Find finder, String dimString) throws IllegalArgumentException {
    if (dimString == null) // scalar
      return ImmutableList.of(); // empty list
    dimString = dimString.trim();
    if (dimString.isEmpty()) // scalar
      return ImmutableList.of(); // empty list

    ImmutableList.Builder<Dimension> newDimensions = ImmutableList.builder();
    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension d;
      if (dimName.equals("*")) {
        d = Dimension.VLEN;
      } else {
        d = finder.findByName(dimName);
      }

      if (d == null) {
        // if numeric - then its anonymous dimension
        try {
          int len = Integer.parseInt(dimName);
          d = Dimension.builder().setLength(len).setIsShared(false).build();
        } catch (Exception e) {
          throw new IllegalArgumentException("Dimension " + dimName + " does not exist");
        }
      }
      newDimensions.add(d);
    }

    return newDimensions.build();
  }

  /** Make a list of private dimensions from an array of lengths */
  public static List<Dimension> makeDimensionsAnon(int[] shape) {
    List<Dimension> newDimensions = new ArrayList<>();
    if ((shape == null) || (shape.length == 0)) { // scalar
      return newDimensions;
    }
    for (int len : shape) {
      newDimensions.add(Dimension.builder().setIsVariableLength(len == -1).setLength(len).setIsShared(false).build());
    }
    return newDimensions;
  }

  /**
   * Get list of Dimensions, including parents if any.
   *
   * @return array of Dimension, rank of v plus all parents.
   */
  public static List<Dimension> makeDimensionsAll(Variable v) {
    List<Dimension> dimsAll = new ArrayList<>();
    addDimensionsAll(dimsAll, v);
    return dimsAll;
  }

  private static void addDimensionsAll(List<Dimension> result, Variable v) {
    if (v.isMemberOfStructure())
      addDimensionsAll(result, v.getParentStructure());

    for (int i = 0; i < v.getRank(); i++)
      result.add(v.getDimension(i));
  }
}
