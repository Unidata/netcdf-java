/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

public class TestSpecialAttributes {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadAll() throws IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testSpecialAttributes.nc4");
    // Iterate over all top-level attributes and see if it is special
    for (Attribute a : ncfile.getRootGroup().attributes()) {
      Assert.assertFalse("Attribute iteration found special attribute: " + a.getShortName(), Nc4Iosp.isspecial(a));
    }
    ncfile.close();
  }

  @Test
  public void testReadByName() throws IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testSpecialAttributes.nc4");
    // Attempt to read special attributes by name
    for (String name : new String[] {CDM.NCPROPERTIES}) {
      Attribute special = ncfile.getRootGroup().findAttribute(name);
      Assert.assertTrue("Could not access special attribute: " + name, special != null && Nc4Iosp.isspecial(special));
    }
    ncfile.close();
  }

}