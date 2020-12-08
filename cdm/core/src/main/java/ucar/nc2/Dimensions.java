/* Copyright Unidata */
package ucar.nc2;

import com.google.common.collect.ImmutableList;

import java.util.*;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

/**
 * Static helper methods for Dimension.
 *
 * @author caron
 * @since 10/3/2019.
 */
public class Dimensions {

  private Dimensions() {}

  /** A Function that finds a Dimension by name. */
  public interface FindDimensionByName {
    @Nullable
    Optional<Dimension> findByName(String dimName);
  }

  /** Make a ucar.ma2.Section.Builder from an ordered set of Dimension objects. */
  public static ucar.ma2.Section.Builder makeSectionFromDimensions(Iterable<Dimension> dimensions) {
    try {
      Section.Builder builder = Section.builder();
      for (Dimension d : dimensions) {
        int len = d.getLength();
        if (len > 0)
          builder.appendRange(new Range(d.getShortName(), 0, len - 1));
        else if (len == 0)
          builder.appendRange(Range.EMPTY); // LOOK empty not named
        else {
          assert d.isVariableLength();
          builder.appendRange(Range.VLEN); // LOOK vlen not named
        }
      }
      return builder;

    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  /** Get the total number of elements the dimensions represent. */
  public static long getSize(Iterable<Dimension> dimensions) {
    long size = 1;
    for (Dimension dim : dimensions) {
      if (dim.getLength() >= 0)
        size *= dim.getLength();
    }
    return size;
  }


  /** Make an array of Dimension lengths from all the dimensions in a variable, including parentStructures. */
  public static int[] makeShapeAll(Variable v) {
    return makeShape(makeDimensionsAll(v));
  }

  /** Make an array of Dimension lengths. */
  public static int[] makeShape(Iterable<Dimension> dimensions) {
    return makeSectionFromDimensions(dimensions).build().getShape();
  }

  /** Make a space-delineated String from a list of Dimension names, inverse of makeDimensionsList(). */
  public static String makeDimensionsString(Iterable<Dimension> dimensions) {
    if (dimensions == null)
      return "";

    int count = 0;
    Formatter buf = new Formatter();
    for (Dimension myd : dimensions) {
      String dimName = myd.getShortName();

      if (count != 0) {
        buf.format(" ");
      }
      count++;

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
   * @param finder interface to find a Dimension by name.
   * @param dimString space separated list of dimension names.
   * @return equivalent list of Dimension objects.
   * @throws IllegalArgumentException if cant find or parse the name.
   */
  public static ImmutableList<Dimension> makeDimensionsList(FindDimensionByName finder, String dimString)
      throws IllegalArgumentException {
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
        d = finder.findByName(dimName).orElse(null);
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
  public static ImmutableList<Dimension> makeDimensionsAnon(int[] shape) {
    if ((shape == null) || (shape.length == 0)) { // scalar
      return ImmutableList.of();
    }
    ImmutableList.Builder<Dimension> newDimensions = ImmutableList.builder();
    for (int len : shape) {
      newDimensions.add(Dimension.builder().setIsVariableLength(len == -1).setLength(len).setIsShared(false).build());
    }
    return newDimensions.build();
  }

  /**
   * Get list of Dimensions, including parent Structure(s), if any.
   *
   * @return array of Dimension, rank of v plus all parent Structures.
   */
  public static ImmutableList<Dimension> makeDimensionsAll(Variable v) {
    ImmutableList.Builder<Dimension> dimsAll = ImmutableList.builder();
    addDimensionsAll(dimsAll, v);
    return dimsAll.build();
  }

  private static void addDimensionsAll(ImmutableList.Builder<Dimension> result, Variable v) {
    if (v.isMemberOfStructure()) {
      addDimensionsAll(result, v.getParentStructure());
    }

    for (int i = 0; i < v.getRank(); i++)
      result.add(v.getDimension(i));
  }

  /**
   * Test if all the Dimensions in subset are in set
   * 
   * @param subset is this a subset
   * @param set of this?
   */
  public static boolean isSubset(Collection<Dimension> subset, Collection<Dimension> set) {
    for (Dimension d : subset) {
      if (!(set.contains(d))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Can this be a coordinate system for v?
   * True if each dimension of v is in this domain, or is 1 dimensional.
   */
  public static boolean isCoordinateSystemFor(Collection<Dimension> domain, Variable v) {
    for (Dimension d : Dimensions.makeDimensionsAll(v)) {
      if (!domain.contains(d) && (d.getLength() != 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test if all the Strings in subset are in set
   * 
   * @param subset is this a subset
   * @param set of this?
   */
  public static boolean isSubset(Set<String> subset, Set<String> set) {
    for (String d : subset) {
      if (!(set.contains(d))) {
        return false;
      }
    }
    return true;
  }

  /** Make the set of Dimensions used by axes. */
  public static ImmutableSet<Dimension> makeDomain(Iterable<? extends Variable> axes, boolean addAnon) {
    ImmutableSet.Builder<Dimension> domain = ImmutableSet.builder();
    for (Variable axis : axes) {
      axis.getDimensions().stream().filter(d -> addAnon | d.isShared()).forEach(d -> domain.add(d));
    }
    return domain.build();
  }
}
