package ucar.nc2.dataset;

import org.junit.Test;
import ucar.nc2.constants.CF;

import static com.google.common.truth.Truth.assertThat;

public class TestVerticalCT {

  @Test
  public void testType() {
    assertThat(VerticalCT.Type.getType("bad")).isNull();
    assertThat(VerticalCT.Type.getType(CF.ocean_s_coordinate)).isEqualTo(VerticalCT.Type.OceanS);
  }

  @Test
  public void testBuilder() {
    VerticalCT vertCT = VerticalCT.builder().setType(VerticalCT.Type.LnPressure).build();
    assertThat(vertCT.getVerticalTransformType()).isEqualTo(VerticalCT.Type.LnPressure);
  }
}
