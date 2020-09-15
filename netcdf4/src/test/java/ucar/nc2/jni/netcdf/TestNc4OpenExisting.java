/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatUpdater;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;

/** Test miscellaneous netcdf4 writing */
public class TestNc4OpenExisting {

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
  @Ignore("not ready yet")
  public void testUnlimitedDimension() throws IOException, InvalidRangeException {
    String location = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, null);
    System.out.printf("write to file = %s%n", new File(location).getAbsolutePath());

    Dimension timeDim = writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", DataType.DOUBLE, ImmutableList.of(timeDim));

    Array data = Array.makeFromJavaArray(new double[] {0, 1, 2, 3});
    try (NetcdfFormatWriter writer = writerb.build()) {
      writer.write("time", data);
    }

    NetcdfFormatUpdater.Builder existingb = NetcdfFormatUpdater.openExisting(location);
    try (NetcdfFormatUpdater existing = existingb.build()) {
      Variable time = existing.findVariable("time");
      int[] origin = new int[1];
      origin[0] = (int) time.getSize();
      existing.write("time", origin, data);
    }

    try (NetcdfFile file = NetcdfFiles.open(location)) {
      Variable time = file.findVariable("time");
      assert time.getSize() == 8 : "failed to append to unlimited dimension";
    }
  }

  @Test
  @Ignore("doesnt work yet")
  public void testAttributeChangeNc4() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalFromTestDataDir + "dataset/testRename.nc4");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }

  @Test
  @Ignore("doesnt work yet")
  public void testAttributeChangeNc3() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalFromTestDataDir + "dataset/testRename.nc3");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }

  private void doRename(String filename) throws IOException {
    logger.debug("Rename {}", filename);
    // old and new name of variable
    String oldVarName = "Pressure_reduced_to_MSL_msl";
    String newVarName = "Pressure_MSL";
    // name and value of attribute to change
    String attrToChange = "long_name";
    String newAttrValue = "Long name changed!";
    Array orgData;

    NetcdfFormatUpdater.Builder writerb = NetcdfFormatUpdater.openExisting(filename).setFill(false);
    Optional<Variable.Builder<?>> newVar = writerb.renameVariable(oldVarName, newVarName);
    newVar.ifPresent(vb -> vb.addAttribute(new Attribute(attrToChange, newAttrValue)));

    // write the above changes to the file
    try (NetcdfFormatUpdater writer = writerb.build()) {
      Variable var = writer.findVariable(newVarName);
      orgData = var.read();
    }

    // check that it worked
    try (NetcdfFile ncd = NetcdfFiles.open(filename)) {
      Variable var = ncd.findVariable(newVarName);
      Assert.assertNotNull(var);
      String attValue = var.findAttributeString(attrToChange, "");
      Assert.assertEquals(attValue, newAttrValue);

      Array data = var.read();
      logger.debug("{}", data);
      orgData.resetLocalIterator();
      data.resetLocalIterator();
      while (data.hasNext() && orgData.hasNext()) {
        float val = data.nextFloat();
        float orgval = orgData.nextFloat();
        Assert2.assertNearlyEquals(orgval, val);
      }
    }
  }
}
