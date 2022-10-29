package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TestCollectionGlob {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {

        {"foo*.nc", "foo", 0},
        {"foo/*.nc", "foo/", 0},
        {"foo/*/bar.nc", "foo/", 1},
        {"**/bar.nc", "", 1},
        {"foo*/bar/baz.nc", "foo", 2},
        {"foo*/bar/**/baz.nc", "foo", Integer.MAX_VALUE},
    });
  }

  private final String spec;
  private final String root;
  private final int depth;

  public TestCollectionGlob(String spec, String root, int depth) {
    this.spec = spec;
    this.root = root;
    this.depth = depth;
  }

  @Test
  public void nominalGlobbing() {
    CollectionGlob globber;
    globber = new CollectionGlob("fooName", spec, logger);
    assertThat(globber.root).isEqualTo(root);
    assertThat(globber.depth).isEqualTo(depth);
  }
}
