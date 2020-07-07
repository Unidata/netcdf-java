/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test writing structure data into netcdf4.
 *
 * @author caron
 * @since 5/12/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestNc4Structures {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  public void writeStructureFromNids() throws IOException, InvalidRangeException {
    // String datasetIn = TestDir.cdmUnitTestDir + "formats/nexrad/level3/KBMX_SDUS64_NTVBMX_201104272341";
    String datasetIn = TestDir.cdmUnitTestDir + "formats/nexrad/level3/NVW_20041117_1657";
    String datasetOut = tempFolder.newFile().getAbsolutePath();
    writeStructure(datasetIn, datasetOut);
  }

  private void writeStructure(String datasetIn, String datasetOut) throws IOException {
    System.out.printf("NetcdfDatataset read from %s write to %s %n", datasetIn, datasetOut);

    CancelTask cancel = CancelTask.create();
    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDatasets.openFile(datasetIn, cancel)) {
      NetcdfFormatWriter.Builder builder =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, datasetOut, null);
      NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder);

      try (NetcdfFile ncfileOut = copier.write(cancel)) {
        // empty
      } finally {
        cancel.setDone(true);
        System.out.printf("%s%n", cancel);
      }

    } catch (Exception ex) {
      System.out.printf("%s = %s %n", ex.getClass().getName(), ex.getMessage());
    }

    cancel.setDone(true);
    System.out.printf("%s%n", cancel);
  }
}
