package tests.netcdfJava;

import examples.netcdfJava.netcdf4ClibraryTutorial;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

public class Testnetcdf4ClibraryTutorial {

  @Test
  public void testWritingcdf() {
    // test open success
    Assert.assertThrows(NullPointerException.class, () -> {
      netcdf4ClibraryTutorial.writingcdf(null, 0, false, null, "locationAsString", null, null);
    });
  }

  @Test
  public void testChunkingOverride() {
    netcdf4ClibraryTutorial.chunkingOverride(0, false);
  }

}
