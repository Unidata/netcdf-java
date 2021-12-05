/* Copyright Unidata */
package ucar.unidata.geoloc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test that the algorithm for longitude wrapping works */
@RunWith(Parameterized.class)
public class TestLongitudeWrap {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {100, -100, 200});

    return result;
  }

  double lat1, lat2, expected;

  public TestLongitudeWrap(double lat1, double lat2, double expected) {
    this.lat1 = lat1;
    this.lat2 = lat2;
    this.expected = expected;
  }

  @Test
  public void doit() {
    double compute = lonDiff(lat1, lat2);
    logger.debug("({} - {}) = {}, expect {}", lat1, lat2, compute, expected);
    assertThat(Misc.nearlyEquals(expected, compute)).isTrue();
    assertThat(Math.abs(compute)).isLessThan(360.0);
  }

  /**
   * Starting from lon1, find
   * Find difference (lon1 - lon2) normalized so that maximum value is += 180.
   *
   * @param lon1 start
   * @param lon2 end
   * @return
   */
  static public double lonDiff(double lon1, double lon2) {
    return Math.IEEEremainder(lon1 - lon2, 720.0);
  }
}
