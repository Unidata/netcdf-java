/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/** Test {@link NcStreamWriter} */
@RunWith(Parameterized.class)
public class TestNcStreamWriter {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    result.add(new Object[] {TestDir.cdmLocalTestDataDir + "testWrite.nc"});
    result.add(new Object[] {TestDir.cdmLocalTestDataDir + "testStructures.nc"});
    result.add(new Object[] {TestDir.cdmLocalTestDataDir + "testNestedGroups.ncml"});
    return result;
  }

  private final String filename;

  public TestNcStreamWriter(String filename) {
    this.filename = filename;
  }

  @Test
  public void writeNcStream() throws IOException, InvalidRangeException {
    String outFile = tempFolder.newFile().getAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);
      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile), 50 * 1000)) {
        writer.streamAll(fos);
      }
      // read it back in and compare to original.
      try (NetcdfFile copy = NetcdfDatasets.openFile(outFile, null)) {
        Formatter errs = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(errs, true, true, true);
        boolean ok = compare.compare(ncfile, copy);
        if (!ok) {
          System.out.printf("FAIL %s%n", errs);
          fail();
        }
      }
    }
  }

}
