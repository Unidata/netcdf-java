package ucar.nc2.jni.netcdf;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import java.io.IOException;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;

public class TestNc4Iosp {

  @Before
  public void checkLibrary() {
    Assume.assumeTrue("Netcdf-4 C library not present", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  public void flushInDefineMode() throws IOException {
    Nc4Iosp nc4Iosp = new Nc4Iosp();
    nc4Iosp.flush();
  }

  @Test
  public void updateAttribute() throws IOException {
    Nc4Iosp nc4Iosp = new Nc4Iosp();
    nc4Iosp.updateAttribute(null, new Attribute("foo", "bar"));
  }
}
