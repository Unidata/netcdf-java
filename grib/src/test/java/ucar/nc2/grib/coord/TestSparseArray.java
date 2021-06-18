package ucar.nc2.grib.coord;

import org.junit.Test;
import ucar.nc2.util.Misc;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test SparseArray class. */
public class TestSparseArray {

  @Test
  public void testInfo() {
    int[] sizes = new int[] {3, 10, 10};
    int[] track = new int[3 * 10 * 10];
    List<Short> list = new ArrayList<>();
    for (int i = 0; i < 3 * 10 * 10; i++) {
      track[i] = i % 11 == 0 ? 0 : 1;
      list.add((short) i);
    }

    SparseArray<Short> sa = new SparseArray<>(sizes, track, list, 0);

    Formatter info = new Formatter();
    sa.showInfo(info, null);
    System.out.printf("%s%n", info.toString());

    assertThat(sa.getDensity()).isWithin(Misc.defaultMaxRelativeDiffFloat).of(0.906667f);
  }
}
