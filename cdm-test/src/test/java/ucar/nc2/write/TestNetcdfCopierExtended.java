/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test NetcdfCopier, write copy, then read back and comparing to original. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestNetcdfCopierExtended {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // classic
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/netcdf3/longOffset.nc", true});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/grib1/radar_national.grib", true});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/grib2/200508041200.ngrid_gfs", true});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf5/dimScales.h5", true});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/f13_owsa_04010_09A.hdf", true});
    result.add(new Object[] {"file:" + TestDir.cdmLocalFromTestDataDir + "point/stationData2Levels.ncml", false});

    // extended
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/17766010.hdf", true});

    return result;
  }

  private final String filename;
  private final boolean same;

  public TestNetcdfCopierExtended(String filename, boolean same) {
    this.filename = filename;
    this.same = same;
  }

  @Test
  public void doOne() throws IOException {
    File fin = new File(filename);
    File fout = tempFolder.newFile();
    String fileout = tempFolder.newFile().getAbsolutePath();
    System.out.printf("Write %s %n   to %s (%s %s)%n", fin.getAbsolutePath(), fout.getAbsolutePath(), fout.exists(),
        fout.getParentFile().exists());

    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDatasets.openFile(fin.getPath(), null)) {
      NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf4(fileout);
      try (NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder)) {
        copier.write(null);
      }
      try (NetcdfFile ncfileOut = ucar.nc2.dataset.NetcdfDatasets.openFile(fileout, null)) {
        Formatter errs = new Formatter();
        CompareNetcdf2 tc = new CompareNetcdf2(errs, false, false, true);
        boolean ok = tc.compare(ncfileIn, ncfileOut, new CompareNetcdf2.Netcdf4ObjectFilter());
        System.out.printf(" %s compare %s to %s ok = %s%n", ok ? "" : "***", ncfileIn.getLocation(),
            ncfileOut.getLocation(), ok);
        if (!ok && same) {
          System.out.printf(" %s%n", errs);
          fail();
        }
      }
    }
    System.out.printf("%n");
  }

}
