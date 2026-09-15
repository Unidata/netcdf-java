package ucar.nc2.grib.grib2;

import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestPdsInterval {

  private static void assertStatType(@Nullable Variable v, String expected) {
    Attribute grib2StatType = v.findAttribute("Grib2_Statistical_Process_Type");
    assertThat(grib2StatType.getStringValue()).isEqualTo(expected);
  }

  @Test
  public void testStatisticalProcessTypeAttributeExists() throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/gfs.t00z.pgrb2.1p00.f003")) {
      assertStatType(nc.findVariable("Albedo_surface_3_Hour_Average"), "Average");
      assertStatType(nc.findVariable("Maximum_temperature_height_above_ground_3_Hour_Maximum"), "Maximum");
      assertStatType(nc.findVariable("Minimum_temperature_height_above_ground_3_Hour_Minimum"), "Minimum");
      assertStatType(nc.findVariable("Total_precipitation_surface_3_Hour_Accumulation"), "Accumulation");
    }
  }

  @Test
  public void testStatisticalProcessTypeAttributeNotPresent() throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/gfs.t00z.pgrb2.1p00.f003")) {
      Variable v = nc.findVariable("Convective_available_potential_energy_surface");
      assertThat(v.findAttribute("Grib2_Statistical_Process_Type")).isNull();
    }
  }

}
