package ucar.unidata.util.test;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Set up d4ts and dts servers using TestContainers to use for testing opendap, dap4, and httpservices.
 **/
public abstract class DapTestContainer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final GenericContainer<?> CONTAINER;

  public static final String NAME = "D4TS and DTS TestContainer";
  public static final String D4TS_TEST_PATH = "d4ts/testfiles";
  public static final String DTS_TEST_PATH = "dts";

  public static final String HOST;
  public static final Integer PORT;
  public static final String SERVER;
  public static final String D4TS_PATH;
  public static final String DTS_PATH;

  static {
    CONTAINER = new GenericContainer<>(
        new ImageFromDockerfile().withFileFromClasspath("Dockerfile", "/ucar/unidata/util/test/Dockerfile"))
            .withExposedPorts(8080);
    CONTAINER.start();

    HOST = CONTAINER.getHost();
    PORT = CONTAINER.getFirstMappedPort();
    SERVER = HOST + ":" + PORT;
    D4TS_PATH = SERVER + "/" + D4TS_TEST_PATH;
    DTS_PATH = SERVER + "/" + DTS_TEST_PATH;

    logger.info("Starting d4ts and dts using docker TestContainer at {}", SERVER);
  }
}
