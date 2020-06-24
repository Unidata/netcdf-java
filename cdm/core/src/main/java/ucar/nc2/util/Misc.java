/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import ucar.ma2.DataType;

/** Miscellaneous static routines. */
public class Misc {
  /** Estimates pointer size in bytes */
  public static final int referenceSize = 4;
  /** Estimates object size in bytes */
  public static final int objectSize = 16;

  /**
   * The default maximum {@link #relativeDifference(float, float) relative difference} that two floats can have in
   * order to be deemed {@link #nearlyEquals(float, float) nearly equal}.
   */
  public static final float defaultMaxRelativeDiffFloat = 1.0e-5f;

  /**
   * The default maximum {@link #relativeDifference(double, double) relative difference} that two doubles can have in
   * order to be deemed {@link #nearlyEquals(double, double) nearly equal}.
   */
  public static final double defaultMaxRelativeDiffDouble = 1.0e-8;

  /** The absolute difference between two floats, i.e. {@code |a - b|}. */
  public static float absoluteDifference(float a, float b) {
    if (Float.compare(a, b) == 0) { // Shortcut: handles infinities and NaNs.
      return 0;
    } else {
      return Math.abs(a - b);
    }
  }

  /** The absolute difference between two doubles, i.e. {@code |a - b|}. */
  public static double absoluteDifference(double a, double b) {
    if (Double.compare(a, b) == 0) { // Shortcut: handles infinities and NaNs.
      return 0;
    } else {
      return Math.abs(a - b);
    }
  }

  /**
   * Returns the relative difference between two numbers, i.e. {@code |a - b| / max(|a|, |b|)}.
   * <p>
   * For cases where {@code a == 0}, {@code b == 0}, or {@code a} and {@code b} are extremely close, traditional
   * relative difference calculation breaks down. So, in those instances, we compute the difference relative to
   * {@link Float#MIN_NORMAL}, i.e. {@code |a - b| / Float.MIN_NORMAL}.
   *
   * @param a first number.
   * @param b second number.
   * @return the relative difference.
   * @see <a href="http://floating-point-gui.de/errors/comparison/">The Floating-Point Guide</a>
   * @see <a href="https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/">
   *      Comparing Floating Point Numbers, 2012 Edition</a>
   */
  public static float relativeDifference(float a, float b) {
    float absDiff = absoluteDifference(a, b);

    if (Float.compare(a, b) == 0) { // Shortcut: handles infinities and NaNs.
      return 0;
    } else if (a == 0 || b == 0 || absDiff < Float.MIN_NORMAL) {
      return absDiff / Float.MIN_NORMAL;
    } else {
      float maxAbsValue = Math.max(Math.abs(a), Math.abs(b));
      return absDiff / maxAbsValue;
    }
  }

  /** Same as {@link #relativeDifference(float, float)}, but for doubles. */
  public static double relativeDifference(double a, double b) {
    double absDiff = absoluteDifference(a, b);

    if (Double.compare(a, b) == 0) { // Shortcut: handles infinities and NaNs.
      return 0;
    } else if (a == 0 || b == 0 || absDiff < Double.MIN_NORMAL) {
      return absDiff / Double.MIN_NORMAL;
    } else {
      double maxAbsValue = Math.max(Math.abs(a), Math.abs(b));
      return absDiff / maxAbsValue;
    }
  }

  /** RelativeDifference is less than {@link #defaultMaxRelativeDiffFloat}. */
  public static boolean nearlyEquals(float a, float b) {
    return nearlyEquals(a, b, defaultMaxRelativeDiffFloat);
  }

  /** RelativeDifference is less than maxRelDiff. */
  public static boolean nearlyEquals(float a, float b, float maxRelDiff) {
    return relativeDifference(a, b) < maxRelDiff;
  }

  /** RelativeDifference is less than {@link #defaultMaxRelativeDiffDouble}. */
  public static boolean nearlyEquals(double a, double b) {
    return nearlyEquals(a, b, defaultMaxRelativeDiffDouble);
  }

  /** RelativeDifference is less than maxRelDiff. */
  public static boolean nearlyEquals(double a, double b, double maxRelDiff) {
    return relativeDifference(a, b) < maxRelDiff;
  }

  /** AbsoluteDifference is less than maxAbsDiff. */
  public static boolean nearlyEqualsAbs(float a, float b, float maxAbsDiff) {
    return absoluteDifference(a, b) <= Math.abs(maxAbsDiff);
  }

  /** AbsoluteDifference is less than maxAbsDiff. */
  public static boolean nearlyEqualsAbs(double a, double b, double maxAbsDiff) {
    return absoluteDifference(a, b) <= Math.abs(maxAbsDiff);
  }

  //////////////////////////////////////////////////////////////////////

  /** @deprecated use Arrays.toString(arr) */
  @Deprecated
  public static String showInts(int[] inta) {
    Formatter f = new Formatter();
    showInts(inta, f);
    return f.toString();
  }

  /** @deprecated use List.toString() */
  public static String showInts(List<Integer> intList) {
    if (intList == null)
      return "null";
    Formatter f = new Formatter();
    for (int i : intList)
      f.format("%d,", i);
    return f.toString();
  }

  /** @deprecated use Arrays.toString(arr) */
  public static void showInts(int[] inta, Formatter f) {
    if (inta == null) {
      f.format("null");
      return;
    }
    for (int i : inta)
      f.format("%d, ", i);
  }

  /** @deprecated use Arrays.toString(arr) */
  public static String showBytes(byte[] buff) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0)
        sbuff.append(" ");
      sbuff.append(ub);
    }
    return sbuff.toString();
  }

  /** @deprecated use Arrays.toString(arr) */
  public static void showBytes(byte[] buff, Formatter f) {
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      f.format("%3d ", ub);
    }
  }

  public static String showBits(byte[] bytes) {
    try (Formatter f = new Formatter()) {
      int count = 0;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format("%8s", Long.toBinaryString(s));
        if (++count % 10 == 0)
          f.format("%n");
      }
      return f.toString();
    }
  }

  //////////////////////////////////////////////////////////////////////

  public static boolean compare(byte[] raw1, byte[] raw2, Formatter f) {
    if (raw1 == null || raw2 == null)
      return false;

    if (raw1.length != raw2.length) {
      f.format("length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i]) {
        f.format(" %3d : %3d != %3d%n", i + 1, raw1[i], raw2[i]);
        ndiff++;
      }
    }
    if (ndiff > 0)
      f.format("Misc.compare %d bytes, %d are different %n", len, ndiff);
    return ndiff == 0 && (raw1.length == raw2.length);
  }

  public static boolean compare(float[] raw1, float[] raw2, Formatter f) {
    boolean ok = true;
    if (raw1.length != raw2.length) {
      f.format("compareFloat: length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
      ok = false;
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (!Misc.nearlyEquals(raw1[i], raw2[i]) && !Double.isNaN(raw1[i]) && !Double.isNaN(raw2[i])) {
        f.format(" %5d : %3f != %3f%n", i, raw1[i], raw2[i]);
        ndiff++;
        ok = false;
      }
    }
    if (ndiff > 0)
      f.format("Misc.compare %d floats, %d are different %n", len, ndiff);
    return ok;
  }

  public static boolean compare(int[] raw1, int[] raw2, Formatter f) {
    boolean ok = true;
    if (raw1.length != raw2.length) {
      f.format("compareFloat: length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
      ok = false;
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i]) {
        f.format(" %5d : %3d != %3d%n", i, raw1[i], raw2[i]);
        ndiff++;
        ok = false;
      }
    }
    if (ndiff > 0)
      f.format("Misc.compare %d ints, %d are different %n", len, ndiff);
    return ok;
  }

  /** @deprecated use Integer.compare(x, y) */
  @Deprecated
  public static int compare(int x, int y) {
    return Integer.compare(x, y);
  }

  /** @deprecated use Long.compare(x, y) */
  @Deprecated
  public static int compare(long x, long y) {
    return Long.compare(x, y);
  }

  /** @deprecated do not use */
  @Deprecated
  public static String stackTraceToString(StackTraceElement[] stackTrace) {
    StringBuilder buf = new StringBuilder();
    for (StackTraceElement ste : stackTrace) {
      buf.append(ste);
      buf.append("\n");
    }
    return buf.toString();
  }


  /** @deprecated use Iterables.size(Iterable it) */
  @Deprecated
  public static int getSize(Iterable ii) {
    if (ii instanceof Collection)
      return ((Collection) ii).size();
    int count = 0;
    for (Object i : ii)
      count++;
    return count;
  }

  /** @deprecated use ImmutableList.copyOf(iterator) or Lists.newArrayList(iterator) */
  @Deprecated
  public static List getList(Iterable ii) {
    if (ii instanceof List)
      return (List) ii;
    List<Object> result = new ArrayList<>();
    for (Object i : ii)
      result.add(i);
    return result;
  }



}
