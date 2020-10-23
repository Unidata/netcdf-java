/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestHyraxServer {
  // https://opendap.jpl.nasa.gov/opendap/hyrax/ (Hyrax 1.16.0)
  @Test
  public void testHyraxJpl() throws IOException {
    String url =
        "https://opendap.jpl.nasa.gov/opendap/hyrax/SeaIce/quikscat/preview/L3/byu_scp/sea_ice_age/arctic/v1/2009/001/qscat_seaice_age_0012009.nc.gz";
    try (DodsNetcdfFile dodsfile = TestDODSRead.openAbs(url)) {
      System.out.printf("%s%n", dodsfile);
    }
  }

  // https://aura.gesdisc.eosdis.nasa.gov/opendap/ (Hyrax 1.15.1)
  @Test
  @Ignore("permission failure")
  public void testHyraxGedsisc() throws IOException {
    String url =
        "https://aura.gesdisc.eosdis.nasa.gov/opendap/Aura_OMI_Level2/OMAERO.003/2020/001/OMI-Aura_L2-OMAERO_2020m0101t0117-o82246_v003-2020m0101t073900.he5";
    try (DodsNetcdfFile dodsfile = TestDODSRead.openAbs(url)) {
      System.out.printf("%s%n", dodsfile);
    }
  }

  // https://oceandata.sci.gsfc.nasa.gov/opendap/ (Hyrax 1.12.1)
  @Test
  @Ignore("permission failure")
  public void testHyraxGsfc() throws IOException {
    String url =
        "https://oceandata.sci.gsfc.nasa.gov/opendap/SeaWiFS/L3SMI/2010/001/S2010001.L3m_DAY_BIOS4_chlor_a_4km.nc";
    try (DodsNetcdfFile dodsfile = TestDODSRead.openAbs(url)) {
      System.out.printf("%s%n", dodsfile);
    }
  }

  // https://hydro1.gesdisc.eosdis.nasa.gov/dods/ (GrADS 2.0)
  @Test
  @Ignore("permission failure")
  public void testGradsHydro1() throws IOException {
    String url = "https://hydro1.gesdisc.eosdis.nasa.gov/dods/GLDAS_NOAH025_3H.2.0";
    try (DodsNetcdfFile dodsfile = TestDODSRead.openAbs(url)) {
      System.out.printf("%s%n", dodsfile);
    }
  }
}
