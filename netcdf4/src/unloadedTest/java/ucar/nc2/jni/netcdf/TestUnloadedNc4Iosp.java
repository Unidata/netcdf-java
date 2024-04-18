package ucar.nc2.jni.netcdf;

import java.io.IOException;
import org.junit.Test;
import ucar.nc2.Attribute;

public class TestUnloadedNc4Iosp {

  @Test
  public void shouldFlushInDefineModeWithoutCLib() throws IOException {
    Nc4Iosp nc4Iosp = new Nc4Iosp();
    nc4Iosp.flush();
  }

  @Test
  public void shouldUpdateAttributeInDefineModeWithoutCLib() throws IOException {
    Nc4Iosp nc4Iosp = new Nc4Iosp();
    nc4Iosp.updateAttribute(null, new Attribute("foo", "bar"));
  }
}
