/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.nc2.grid.CompareGridDataset;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GcdmGridConverter} by roundtripping and comparing with original. Metadata only. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmGridConverter {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "permuteTest.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/namExtract/20060926_0000.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/coards/inittest24.QRIDV07200.ncml"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/nuwg/avn-x.nc"});

      result.add(new Object[] {
          TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4"});
      result
          .add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4"});

      // Offset (orthogonal)
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"});

      // orth, reg
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4"});

      // OffsetRegular
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT_ver7.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4"});

      // OffsetIrregular
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4"});

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmGridConverter(String filename) throws IOException {
    this.filename = filename.replace("\\", "/");
    File file = new File(filename);
    System.out.printf("getAbsolutePath %s%n", file.getAbsolutePath());
    System.out.printf("getCanonicalPath %s%n", file.getCanonicalPath());

    // kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testExample() throws Exception {
    Path path = Paths.get(this.filename);
    roundtrip(path);
  }

  public static void roundtrip(Path path) throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(path.toString(), errlog)) {
      assertThat(gridDataset).isNotNull();

      GcdmGridProto.GridDataset proto = GcdmGridConverter.encodeGridDataset(gridDataset);
      GcdmGridDataset.Builder builder = GcdmGridDataset.builder();
      GcdmGridConverter.decodeGridDataset(proto, builder, errlog);
      GcdmGridDataset roundtrip = builder.build(false);

      new CompareGridDataset(roundtrip, gridDataset, false).compare();
    }
  }

}
