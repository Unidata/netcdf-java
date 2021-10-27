/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.internal.cache.FileCacheable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Updating aggregation
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggUpdating {

  private final String dir = TestDir.cdmUnitTestDir + "agg/updating";
  private final String location = dir + "agg/updating.ncml";
  private final File dirFile = new File(dir);
  private final String extraFile = dir + "/extra.nc";

  String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" //
      + "       <aggregation dimName='time' type='joinExisting' recheckEvery='1 msec'>\n" //
      + "         <scan location='" + dir + "' suffix='*.nc' />\n" //
      + "         <variable name='depth'>\n" //
      + "           <attribute name='coordinates' value='lon lat'/>\n" //
      + "         </variable>\n" //
      + "         <variable name='wvh'>\n" //
      + "           <attribute name='coordinates' value='lon lat'/>\n" //
      + "         </variable>\n" //
      + "       </aggregation>\n" //
      + "       <attribute name='Conventions' type='String' value='CF-1.0'/>\n" //
      + "</netcdf>";

  @Before
  public void setup() {
    assert dirFile.exists();
    assert dirFile.isDirectory();
    assert dirFile.listFiles() != null;
  }

  @Test
  public void testUpdateLastModified() throws IOException {
    // make sure that the extra file is not in the agg
    move(extraFile);

    // open the agg
    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), location, null)) {
      long start = ncfile.getLastModified();

      // now make sure that the extra file is in the agg
      moveBack(extraFile);

      // reread
      long end = ncfile.getLastModified();
      assert (end > start);

      // again
      long end2 = ncfile.getLastModified();
      assert (end == end2);

    }
  }

  private void check(NetcdfFile ncfile, int n) {
    Variable v = ncfile.findVariable("time");
    assert v != null;
    System.out.printf(" time= %s%n", v.getNameAndDimensions());
    assert v.getSize() == n : v.getSize();

    v = ncfile.findVariable("eta");
    assert v != null;
    assert v.getRank() == 3 : v.getRank();
  }

  private class NcmlStringFileFactory implements ucar.nc2.internal.cache.FileFactory {

    @Override
    public FileCacheable open(DatasetUrl durl, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return NetcdfDatasets.openNcmlDataset(new StringReader(ncml), durl.getTrueurl(), null);
    }
  }

  static boolean move(String filename) throws IOException {
    Path src = Paths.get(filename);
    if (!Files.exists(src))
      return false;
    Path dest = Paths.get(filename + ".save");
    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    return true;
  }


  static boolean moveBack(String filename) throws IOException {
    Path src = Paths.get(filename + ".save");
    if (!Files.exists(src))
      return false;
    Path dest = Paths.get(filename);
    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    return true;
  }
}

