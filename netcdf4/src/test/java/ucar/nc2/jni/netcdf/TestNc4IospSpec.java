package ucar.nc2.jni.netcdf;

import org.junit.Test;
import ucar.nc2.Attribute;
import java.io.IOException;

public class TestNc4IospSpec {
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
