/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test netcdf dataset in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggReadGridDataset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /*
   * <?xml version="1.0" encoding="UTF-8"?>
   * <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
   * 
   * <aggregation dimName="ocean_time" type="joinExisting">
   * 
   * <netcdf location="bora_feb_001.nc"/>
   * <netcdf location="bora_feb_002.nc"/>
   * </aggregation>
   * </netcdf>
   */
  @Test
  public void testNcmlGrid() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "conventions/cf/bora_test_agg.ncml";
    System.out.printf("TestNcmlAggExisting.openGrid %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(ncd).isNotNull();
      assertThat(ncd.getGrids()).hasSize(19);
    }
  }

}
