/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GcdmNetcdfFile} */
public class TestGcdmGridDatasetProblems {

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
