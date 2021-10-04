/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Test {@link GcdmNetcdfFile} */
@Category(NeedsCdmUnitTest.class)
public class TestGcdmGridDatasetProblems {

  @Test
  public void testTimeCoordRegular() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4";
    TestGcdmGridConverter.roundtrip(Paths.get(filename));
  }

  @Test
  public void testCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/stag/bora_feb.nc";
    new TestGcdmGridDataset(filename).doOne();
  }

  @Test
  public void testVerticalTransform() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/testCFwriter.nc";
    new TestGcdmGridDataset(filename).doOne();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testSanityCheck() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    Path path = Paths.get(filename);
    TestGcdmGridConverter.roundtrip(path);
    new TestGcdmGridDataset(filename).doOne();
  }

}
