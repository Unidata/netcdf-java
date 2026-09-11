/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import com.google.common.io.BaseEncoding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.grib2.Grib2Pds.PdsAerosol;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test data taken from a sample RRFS 2dfld 3km conus product, filtered to contain only aerosol records.
 */
@RunWith(JUnit4.class)
public class TestPdsAerosol {

  private static byte[] decode(String chars) {
    return BaseEncoding.base16().decode(chars);
  }

  private static byte[] newPds46() {
    // string-encoded byte[] using octets from rrfs.t00z.2dfld.3km.f003.conus.grib2, modified to have no intervals
    return decode("00000047040000002E1400F230FF000000000000000000000200860000000100000002670000000008FF000000000007EA09"
        + "08030000010000000000020100000001FF00000000");
  }

  private static byte[] newPds48() {
    // string-encoded byte[] using octets from rrfs.t00z.2dfld.3km.f003.conus.grib2, modified to have no intervals
    return decode("0000003A04000000301400F231FF00000000000000000000FF00000000000000000000020086000000010000000367000000"
        + "0008FF0000000000");
  }

  private static Grib2Pds makePds(byte[] data) {
    return new Grib2SectionProductDefinition(data).getPDS();
  }

  private static String aerosolRange(byte[] data) {
    return ((PdsAerosol) makePds(data)).getAerosolRange();
  }

  private static byte[] withInterval(byte[] data, int offset, int intervalType, int firstLimitScale,
      int firstLimitValue, int secondLimitScale, int secondLimitValue) {
    data[offset] = (byte) intervalType;
    data[offset + 1] = (byte) firstLimitScale;
    data[offset + 5] = (byte) firstLimitValue;
    data[offset + 6] = (byte) secondLimitScale;
    data[offset + 10] = (byte) secondLimitValue;
    return data;
  }

  @Test
  public void testPds46SizeInterval() {
    // 13: byte offset of size interval in Pds46
    assertThat(aerosolRange(withInterval(newPds46(), 13, 0, 7, 25, 5, 1))).isEqualTo("<2.5um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 1, 7, 25, 5, 1))).isEqualTo(">10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 2, 7, 25, 5, 1))).isEqualTo(">=2.5um,<10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 3, 7, 25, 5, 1))).isEqualTo(">2.5um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 4, 7, 25, 5, 1))).isEqualTo("<10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 5, 7, 25, 5, 1))).isEqualTo("<=2.5um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 6, 7, 25, 5, 1))).isEqualTo(">=10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 7, 7, 25, 5, 1))).isEqualTo(">=2.5um,<=10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 8, 7, 25, 5, 1))).isEqualTo(">=2.5um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 9, 7, 25, 5, 1))).isEqualTo("<=10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 10, 7, 25, 5, 1))).isEqualTo(">2.5um,<=10um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 11, 7, 25, 5, 1))).isEqualTo("2.5um");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 12, 7, 25, 5, 1))).isEqualTo("");
    assertThat(aerosolRange(withInterval(newPds46(), 13, 255, 7, 25, 5, 1))).isEqualTo("");
  }

  @Test
  public void testPds48SizeInterval() {
    // 13: byte offset of size interval in Pds48
    assertThat(aerosolRange(withInterval(newPds48(), 13, 0, 7, 25, 5, 1))).isEqualTo("<2.5um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 1, 7, 25, 5, 1))).isEqualTo(">10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 2, 7, 25, 5, 1))).isEqualTo(">=2.5um,<10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 3, 7, 25, 5, 1))).isEqualTo(">2.5um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 4, 7, 25, 5, 1))).isEqualTo("<10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 5, 7, 25, 5, 1))).isEqualTo("<=2.5um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 6, 7, 25, 5, 1))).isEqualTo(">=10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 7, 7, 25, 5, 1))).isEqualTo(">=2.5um,<=10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 8, 7, 25, 5, 1))).isEqualTo(">=2.5um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 9, 7, 25, 5, 1))).isEqualTo("<=10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 10, 7, 25, 5, 1))).isEqualTo(">2.5um,<=10um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 11, 7, 25, 5, 1))).isEqualTo("2.5um");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 12, 7, 25, 5, 1))).isEqualTo("");
    assertThat(aerosolRange(withInterval(newPds48(), 13, 255, 7, 25, 5, 1))).isEqualTo("");
  }

  @Test
  public void testPds48WavelengthInterval() {
    // 24: byte offset of wavelength interval in Pds48
    assertThat(aerosolRange(withInterval(newPds48(), 24, 0, 8, 55, 8, 124))).isEqualTo("<550nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 1, 8, 55, 8, 124))).isEqualTo(">1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 2, 8, 55, 8, 124))).isEqualTo(">=550nm,<1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 3, 8, 55, 8, 124))).isEqualTo(">550nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 4, 8, 55, 8, 124))).isEqualTo("<1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 5, 8, 55, 8, 124))).isEqualTo("<=550nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 6, 8, 55, 8, 124))).isEqualTo(">=1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 7, 8, 55, 8, 124))).isEqualTo(">=550nm,<=1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 8, 8, 55, 8, 124))).isEqualTo(">=550nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 9, 8, 55, 8, 124))).isEqualTo("<=1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 10, 8, 55, 8, 124))).isEqualTo(">550nm,<=1240nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 11, 8, 55, 8, 124))).isEqualTo("550nm");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 12, 8, 55, 8, 124))).isEqualTo("");
    assertThat(aerosolRange(withInterval(newPds48(), 24, 255, 8, 55, 8, 124))).isEqualTo("");
  }

  @Test
  public void testPds48AerosolSizeAndWavelengthIntervals() {
    // theoretically possible to have both intervals at the same time...
    byte[] data = newPds48();

    data = withInterval(data, 13, 7, 7, 25, 5, 1);
    data = withInterval(data, 24, 7, 8, 55, 8, 124);

    assertThat(aerosolRange(data)).isEqualTo(">=2.5um,<=10um >=550nm,<=1240nm");
  }

  @Test
  public void testDistinctAerosolVariableNames() throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/rrfs.t00z.2dfld.3km.f003.conus.grib2")) {

      List<Variable> variables = nc.getVariables().stream().filter(e -> e.getFullName().toLowerCase().contains("mass"))
          .collect(Collectors.toList());

      List<String> varNames = variables.stream().map(Variable::getFullName).collect(Collectors.toList());
      assertThat(varNames).containsExactly(
          "Atmosphere_emission_mass_flux_entire_atmosphere_single_layer_Dust_dry_lt_10um",
          "Atmosphere_emission_mass_flux_entire_atmosphere_single_layer_Particulate_organic_matter_dry_lt_2p5um",
          "Column-integrated_mass_density_entire_atmosphere_single_layer_Dust_dry_ge_2p5um_lt_10um",
          "Column-integrated_mass_density_entire_atmosphere_single_layer_Dust_dry_lt_10um",
          "Column-integrated_mass_density_entire_atmosphere_single_layer_Dust_dry_lt_2p5um",
          "Column-integrated_mass_density_entire_atmosphere_single_layer_Particulate_organic_matter_dry_lt_2p5um",
          "Mass_density_concentration_height_above_ground_1_Hour_Average_Total_aerosol_lt_10um",
          "Mass_density_concentration_height_above_ground_1_Hour_Average_Total_aerosol_lt_2p5um",
          "Mass_density_concentration_height_above_ground_Dust_dry_ge_2p5um_lt_10um",
          "Mass_density_concentration_height_above_ground_Dust_dry_lt_2p5um",
          "Mass_density_concentration_height_above_ground_Particulate_organic_matter_dry_lt_2p5um");
    }
  }

  @Test
  public void testDistinctAerosolVariableDescriptions() throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/rrfs.t00z.2dfld.3km.f003.conus.grib2")) {

      List<Variable> variables = nc.getVariables().stream().filter(e -> e.getFullName().toLowerCase().contains("mass"))
          .collect(Collectors.toList());

      List<String> varDescriptions = variables.stream().map(Variable::getDescription).collect(Collectors.toList());
      assertThat(varDescriptions).containsExactly(
          "Atmosphere emission mass flux (Dust dry <10um) @ Entire atmosphere layer",
          "Atmosphere emission mass flux (Particulate organic matter dry <2.5um) @ Entire atmosphere layer",
          "Column-integrated mass density (Dust dry >=2.5um,<10um) @ Entire atmosphere layer",
          "Column-integrated mass density (Dust dry <10um) @ Entire atmosphere layer",
          "Column-integrated mass density (Dust dry <2.5um) @ Entire atmosphere layer",
          "Column-integrated mass density (Particulate organic matter dry <2.5um) @ Entire atmosphere layer",
          "Mass density (concentration) (1_Hour Average) (Total aerosol <10um) @ Specified height level above ground",
          "Mass density (concentration) (1_Hour Average) (Total aerosol <2.5um) @ Specified height level above ground",
          "Mass density (concentration) (Dust dry >=2.5um,<10um) @ Specified height level above ground",
          "Mass density (concentration) (Dust dry <2.5um) @ Specified height level above ground",
          "Mass density (concentration) (Particulate organic matter dry <2.5um) @ Specified height level above ground");
    }
  }

  @Test
  public void testDistinctAerosolVariableGribIds() throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/rrfs.t00z.2dfld.3km.f003.conus.grib2")) {

      List<Variable> variables = nc.getVariables().stream().filter(e -> e.getFullName().toLowerCase().contains("mass"))
          .collect(Collectors.toList());

      List<String> varIds = variables.stream()
          .map(v -> Objects.requireNonNull(v.findAttribute(Grib.VARIABLE_ID_ATTNAME)).getStringValue())
          .collect(Collectors.toList());
      assertThat(varIds).containsExactly("VAR_0-20-0_L103_A62001_ge_2p5um_lt_10um", "VAR_0-20-0_L103_A62001_lt_2p5um",
          "VAR_0-20-0_L103_A62010_lt_2p5um", "VAR_0-20-0_L103_I1_Hour_S0_A62000_lt_10um",
          "VAR_0-20-0_L103_I1_Hour_S0_A62000_lt_2p5um", "VAR_0-20-1_L200_A62001_ge_2p5um_lt_10um",
          "VAR_0-20-1_L200_A62001_lt_10um", "VAR_0-20-1_L200_A62001_lt_2p5um", "VAR_0-20-1_L200_A62010_lt_2p5um",
          "VAR_0-20-3_L200_A62001_lt_10um", "VAR_0-20-3_L200_A62010_lt_2p5um");
    }
  }
}
