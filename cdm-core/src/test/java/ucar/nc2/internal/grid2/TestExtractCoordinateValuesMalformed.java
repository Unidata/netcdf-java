/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import org.junit.Ignore;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/** In this case, the bounds are flipped (lower, upper) */
@Ignore("Malformed File")
public class TestExtractCoordinateValuesMalformed {

  // fdrom cdmUnitTest/conventions/cf/jonathan/fixed.fw0.0Sv.nc
  @Test
  public void testContinguousAscending() {
    int n = values.length;
    Array<Double> valuesArray = Arrays.factory(ArrayType.DOUBLE, new int[] {n}, values);

    double[] bounds = new double[2 * n];
    double[] bound1 = new double[n];
    double[] bound2 = new double[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
      bounds[count++] = bounds2[i][0];
      bounds[count++] = bounds2[i][1];
      bound1[i] = bounds2[i][0];
      bound2[i] = bounds2[i][1];
    }
    Array<Double> boundsArray = Arrays.factory(ArrayType.DOUBLE, new int[] {n, 2}, bounds);

    ExtractCoordinateValues subject =
        new ExtractCoordinateValues("latitude_73", valuesArray, Optional.of(boundsArray), true);

    assertThat(subject.boundsAreContiguous).isTrue();
    assertThat(subject.boundsAreRegular).isTrue();
    assertThat(subject.isAscending).isTrue();
    assertThat(subject.isInterval).isFalse(); // LOOK heres the change
    assertThat(subject.isRegular).isTrue();
    assertThat(subject.ncoords).isEqualTo(n);
    assertThat(subject.bound1).isEqualTo(bound1);
    assertThat(subject.bound2).isEqualTo(bound2);
  }

  @Test
  public void testContinguousDescending() {
    int n = values.length;

    double[] valuesFlipped = new double[n];
    double[] bounds = new double[2 * n];
    double[] bound1 = new double[n];
    double[] bound2 = new double[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
      int ip = n - i - 1;
      valuesFlipped[i] = values[ip];
      bounds[count++] = bounds2[ip][1];
      bounds[count++] = bounds2[ip][0];
      bound1[i] = bounds2[ip][1];
      bound2[i] = bounds2[ip][0];
    }

    Array<Double> valuesArray = Arrays.factory(ArrayType.DOUBLE, new int[] {n}, valuesFlipped);
    Array<Double> boundsArray = Arrays.factory(ArrayType.DOUBLE, new int[] {n, 2}, bounds);

    ExtractCoordinateValues subject =
        new ExtractCoordinateValues("latitude_73_flipped", valuesArray, Optional.of(boundsArray), true);

    assertThat(subject.boundsAreContiguous).isTrue();
    assertThat(subject.boundsAreRegular).isTrue();
    assertThat(subject.isAscending).isFalse();
    assertThat(subject.isInterval).isFalse(); // LOOK heres the change
    assertThat(subject.isRegular).isTrue();
    assertThat(subject.ncoords).isEqualTo(n);
    assertThat(subject.bound1).isEqualTo(bound1);
    assertThat(subject.bound2).isEqualTo(bound2);
  }

  private static double[] values = new double[] {-90.0, -87.5, -85.0, -82.5, -80.0, -77.5, -75.0, -72.5, -70.0, -67.5,
      -65.0, -62.5, -60.0, -57.5, -55.0, -52.5, -50.0, -47.5, -45.0, -42.5, -40.0, -37.5, -35.0, -32.5, -30.0, -27.5,
      -25.0, -22.5, -20.0, -17.5, -15.0, -12.5, -10.0, -7.5, -5.0, -2.5, 0.0, 2.5, 5.0, 7.5, 10.0, 12.5, 15.0, 17.5,
      20.0, 22.5, 25.0, 27.5, 30.0, 32.5, 35.0, 37.5, 40.0, 42.5, 45.0, 47.5, 50.0, 52.5, 55.0, 57.5, 60.0, 62.5, 65.0,
      67.5, 70.0, 72.5, 75.0, 77.5, 80.0, 82.5, 85.0, 87.5, 90.0};

  double[][] bounds2 = new double[][] {{-88.75, -90.0}, {-86.25, -88.75}, {-83.75, -86.25}, {-81.25, -83.75},
      {-78.75, -81.25}, {-76.25, -78.75}, {-73.75, -76.25}, {-71.25, -73.75}, {-68.75, -71.25}, {-66.25, -68.75},
      {-63.75, -66.25}, {-61.25, -63.75}, {-58.75, -61.25}, {-56.25, -58.75}, {-53.75, -56.25}, {-51.25, -53.75},
      {-48.75, -51.25}, {-46.25, -48.75}, {-43.75, -46.25}, {-41.25, -43.75}, {-38.75, -41.25}, {-36.25, -38.75},
      {-33.75, -36.25}, {-31.25, -33.75}, {-28.75, -31.25}, {-26.25, -28.75}, {-23.75, -26.25}, {-21.25, -23.75},
      {-18.75, -21.25}, {-16.25, -18.75}, {-13.75, -16.25}, {-11.25, -13.75}, {-8.75, -11.25}, {-6.25, -8.75},
      {-3.75, -6.25}, {-1.25, -3.75}, {1.25, -1.25}, {3.75, 1.25}, {6.25, 3.75}, {8.75, 6.25}, {11.25, 8.75},
      {13.75, 11.25}, {16.25, 13.75}, {18.75, 16.25}, {21.25, 18.75}, {23.75, 21.25}, {26.25, 23.75}, {28.75, 26.25},
      {31.25, 28.75}, {33.75, 31.25}, {36.25, 33.75}, {38.75, 36.25}, {41.25, 38.75}, {43.75, 41.25}, {46.25, 43.75},
      {48.75, 46.25}, {51.25, 48.75}, {53.75, 51.25}, {56.25, 53.75}, {58.75, 56.25}, {61.25, 58.75}, {63.75, 61.25},
      {66.25, 63.75}, {68.75, 66.25}, {71.25, 68.75}, {73.75, 71.25}, {76.25, 73.75}, {78.75, 76.25}, {81.25, 78.75},
      {83.75, 81.25}, {86.25, 83.75}, {88.75, 86.25}, {90.0, 88.75}};

}
