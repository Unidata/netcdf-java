/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/** Compare NetcdfCopier with old NetcdfFileWriter */
public class TestNetcdfCopierCompare {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  private String filename = "file:" + TestDir.cdmLocalFromTestDataDir + "point/stationData2Levels.ncml";

  @Test
  public void doOne() throws IOException {
    File fin = new File(filename);
    File foutOrg = tempFolder.newFile();
    File foutNew = tempFolder.newFile();

    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDatasets.openFile(fin.getPath(), null)) {
      FileWriter2 fileWriter = new FileWriter2(ncfileIn, foutOrg.getPath(), NetcdfFileWriter.Version.netcdf3, null);
      try (NetcdfFile ncfileOut = fileWriter.write()) {
      }

      NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(foutNew.getPath());
      NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder);
      try (NetcdfFile ncfileOut = copier.write(null)) {
      }
    }

    try (NetcdfFile org = NetcdfFiles.open(foutOrg.getPath());
        NetcdfFile withBuilder = NetcdfFiles.open(foutNew.getPath())) {
      Formatter f = new Formatter();
      CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
      boolean ok = compare.compare(org, withBuilder);
      System.out.printf("Compare old and new copy of %s is %s%n", filename, ok);
      if (!ok) {
        System.out.printf("%s%n", f);
        assert false;
      }
    }
  }

}
