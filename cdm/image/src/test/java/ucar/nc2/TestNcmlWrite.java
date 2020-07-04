/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.unidata.util.test.CompareNcml;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Read dataset, write NcML, compare results. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNcmlWrite {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    String datadir = TestDir.cdmUnitTestDir;

    List<Object[]> result = new ArrayList<>(500);
    result.add(new Object[] {datadir + "formats/gini/n0r_20041013_1852-compress", false}); //
    result.add(new Object[] {datadir + "formats/gini/ntp_20041206_2154", true}); //
    return result;
  }

  /////////////////////////////////////////////////////////////
  private final boolean showFiles = true;
  private final boolean compareData;
  private final DatasetUrl durl;

  public TestNcmlWrite(String location, boolean compareData) throws IOException {
    this.durl = DatasetUrl.findDatasetUrl(location);
    this.compareData = compareData;
  }

  @Test
  public void compareNcML() throws IOException {
    new CompareNcml(tempFolder, durl, compareData, showFiles);
  }

}

