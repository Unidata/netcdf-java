/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;

public class TestSpecialAttributes {

  @Test
  public void testReadAll() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalFromTestDataDir + "testSpecialAttributes.nc4")) {
      // Iterate over all top-level attributes and see if it is special
      for (Attribute a : ncfile.getRootGroup().attributes()) {
        assertWithMessage("Attribute iteration found special attribute: ").that(Nc4reader.isspecial(a)).isTrue();
      }
    }
  }

  @Test
  public void testReadByName() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalFromTestDataDir + "testSpecialAttributes.nc4")) {
      // Attempt to read special attributes by name
      for (String name : new String[] {CDM.NCPROPERTIES}) {
        Attribute special = ncfile.getRootGroup().findAttribute(name);
        assertThat(special).isNotNull();
        assertThat(Nc4reader.isspecial(special)).isTrue();
      }
    }
  }

}
