package ucar.nc2.iosp.zarr;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class TestZarrIosp {

  private static final Logger logger = LoggerFactory.getLogger(TestZarrIosp.class);

  @BeforeClass
  public static void setup() throws URISyntaxException, IOException {}

  @Test
  public void testReadZipStore() throws IOException {

  }

  @Test
  public void testReadDirectoryStore() throws IOException {}

  // @Test
  // public void testReadS3ObjectStore() throws IOException {
  // System.setProperty(ZarrTestsCommon.AWS_REGION_PROP_NAME, ZarrTestsCommon.AWS_REGION);
  // try (NetcdfFile ncfile = NetcdfFiles.open(ZARR_HIERARCHY_URI)) {
  // // Do stuff
  // } finally {
  // System.clearProperty(ZarrTestsCommon.AWS_REGION_PROP_NAME);
  // }
  // }
}
