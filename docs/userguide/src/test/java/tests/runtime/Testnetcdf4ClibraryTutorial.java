package tests.runtime;

import examples.runtime.netcdf4ClibraryTutorial;
import org.junit.Assert;
import org.junit.Test;
import ucar.array.InvalidRangeException;
import java.io.IOException;

public class Testnetcdf4ClibraryTutorial {

  @Test
  public void testWritingcdf() throws InvalidRangeException, IOException {
    // test open success
    Assert.assertThrows(NullPointerException.class, () -> {
      netcdf4ClibraryTutorial.writingcdf(null, 0, false, null, "locationAsString", null);
    });
  }

  @Test
  public void testChunkingOverride() {
    netcdf4ClibraryTutorial.chunkingOverride(0, false);
  }

}
