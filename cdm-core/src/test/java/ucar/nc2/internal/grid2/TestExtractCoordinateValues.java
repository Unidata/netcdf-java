/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ExtractCoordinateValues} */
public class TestExtractCoordinateValues {

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
        new ExtractCoordinateValues("latitude_144", valuesArray, Optional.of(boundsArray), true);

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
        new ExtractCoordinateValues("latitude_144_flipped", valuesArray, Optional.of(boundsArray), true);

    assertThat(subject.boundsAreRegular).isTrue();
    assertThat(subject.boundsAreContiguous).isTrue();
    assertThat(subject.isAscending).isFalse();
    assertThat(subject.isInterval).isFalse(); // LOOK heres the change
    assertThat(subject.isRegular).isTrue();
    assertThat(subject.ncoords).isEqualTo(n);
    assertThat(subject.bound1).isEqualTo(bound1);
    assertThat(subject.bound2).isEqualTo(bound2);
  }

  private static double[] values = new double[] {-89.375, -88.125, -86.875, -85.625, -84.375, -83.125, -81.875, -80.625,
      -79.375, -78.125, -76.875, -75.625, -74.375, -73.125, -71.875, -70.625, -69.375, -68.125, -66.875, -65.625,
      -64.375, -63.125, -61.875, -60.625, -59.375, -58.125, -56.875, -55.625, -54.375, -53.125, -51.875, -50.625,
      -49.375, -48.125, -46.875, -45.625, -44.375, -43.125, -41.875, -40.625, -39.375, -38.125, -36.875, -35.625,
      -34.375, -33.125, -31.875, -30.625, -29.375, -28.125, -26.875, -25.625, -24.375, -23.125, -21.875, -20.625,
      -19.375, -18.125, -16.875, -15.625, -14.375, -13.125, -11.875, -10.625, -9.375, -8.125, -6.875, -5.625, -4.375,
      -3.125, -1.875, -0.625, 0.625, 1.875, 3.125, 4.375, 5.625, 6.875, 8.125, 9.375, 10.625, 11.875, 13.125, 14.375,
      15.625, 16.875, 18.125, 19.375, 20.625, 21.875, 23.125, 24.375, 25.625, 26.875, 28.125, 29.375, 30.625, 31.875,
      33.125, 34.375, 35.625, 36.875, 38.125, 39.375, 40.625, 41.875, 43.125, 44.375, 45.625, 46.875, 48.125, 49.375,
      50.625, 51.875, 53.125, 54.375, 55.625, 56.875, 58.125, 59.375, 60.625, 61.875, 63.125, 64.375, 65.625, 66.875,
      68.125, 69.375, 70.625, 71.875, 73.125, 74.375, 75.625, 76.875, 78.125, 79.375, 80.625, 81.875, 83.125, 84.375,
      85.625, 86.875, 88.125, 89.375};

  double[][] bounds2 = new double[][] {{-90.0, -88.75}, {-88.75, -87.5}, {-87.5, -86.25}, {-86.25, -85.0},
      {-85.0, -83.75}, {-83.75, -82.5}, {-82.5, -81.25}, {-81.25, -80.0}, {-80.0, -78.75}, {-78.75, -77.5},
      {-77.5, -76.25}, {-76.25, -75.0}, {-75.0, -73.75}, {-73.75, -72.5}, {-72.5, -71.25}, {-71.25, -70.0},
      {-70.0, -68.75}, {-68.75, -67.5}, {-67.5, -66.25}, {-66.25, -65.0}, {-65.0, -63.75}, {-63.75, -62.5},
      {-62.5, -61.25}, {-61.25, -60.0}, {-60.0, -58.75}, {-58.75, -57.5}, {-57.5, -56.25}, {-56.25, -55.0},
      {-55.0, -53.75}, {-53.75, -52.5}, {-52.5, -51.25}, {-51.25, -50.0}, {-50.0, -48.75}, {-48.75, -47.5},
      {-47.5, -46.25}, {-46.25, -45.0}, {-45.0, -43.75}, {-43.75, -42.5}, {-42.5, -41.25}, {-41.25, -40.0},
      {-40.0, -38.75}, {-38.75, -37.5}, {-37.5, -36.25}, {-36.25, -35.0}, {-35.0, -33.75}, {-33.75, -32.5},
      {-32.5, -31.25}, {-31.25, -30.0}, {-30.0, -28.75}, {-28.75, -27.5}, {-27.5, -26.25}, {-26.25, -25.0},
      {-25.0, -23.75}, {-23.75, -22.5}, {-22.5, -21.25}, {-21.25, -20.0}, {-20.0, -18.75}, {-18.75, -17.5},
      {-17.5, -16.25}, {-16.25, -15.0}, {-15.0, -13.75}, {-13.75, -12.5}, {-12.5, -11.25}, {-11.25, -10.0},
      {-10.0, -8.75}, {-8.75, -7.5}, {-7.5, -6.25}, {-6.25, -5.0}, {-5.0, -3.75}, {-3.75, -2.5}, {-2.5, -1.25},
      {-1.25, 0.0}, {0.0, 1.25}, {1.25, 2.5}, {2.5, 3.75}, {3.75, 5.0}, {5.0, 6.25}, {6.25, 7.5}, {7.5, 8.75},
      {8.75, 10.0}, {10.0, 11.25}, {11.25, 12.5}, {12.5, 13.75}, {13.75, 15.0}, {15.0, 16.25}, {16.25, 17.5},
      {17.5, 18.75}, {18.75, 20.0}, {20.0, 21.25}, {21.25, 22.5}, {22.5, 23.75}, {23.75, 25.0}, {25.0, 26.25},
      {26.25, 27.5}, {27.5, 28.75}, {28.75, 30.0}, {30.0, 31.25}, {31.25, 32.5}, {32.5, 33.75}, {33.75, 35.0},
      {35.0, 36.25}, {36.25, 37.5}, {37.5, 38.75}, {38.75, 40.0}, {40.0, 41.25}, {41.25, 42.5}, {42.5, 43.75},
      {43.75, 45.0}, {45.0, 46.25}, {46.25, 47.5}, {47.5, 48.75}, {48.75, 50.0}, {50.0, 51.25}, {51.25, 52.5},
      {52.5, 53.75}, {53.75, 55.0}, {55.0, 56.25}, {56.25, 57.5}, {57.5, 58.75}, {58.75, 60.0}, {60.0, 61.25},
      {61.25, 62.5}, {62.5, 63.75}, {63.75, 65.0}, {65.0, 66.25}, {66.25, 67.5}, {67.5, 68.75}, {68.75, 70.0},
      {70.0, 71.25}, {71.25, 72.5}, {72.5, 73.75}, {73.75, 75.0}, {75.0, 76.25}, {76.25, 77.5}, {77.5, 78.75},
      {78.75, 80.0}, {80.0, 81.25}, {81.25, 82.5}, {82.5, 83.75}, {83.75, 85.0}, {85.0, 86.25}, {86.25, 87.5},
      {87.5, 88.75}, {88.75, 90.0}};

}
