package tests;

import examples.ZarrExamples;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;

public class TestZarrExamples {

  @Test
  public void testReadZarrExample() {
      Assert.assertThrows(FileNotFoundException.class, () -> {
        ZarrExamples.readZarrStores();
      });
  }

  @Test
  public void testImplementFilterAndProvider() {
    // just verify the example code compiles with no deprecations
    ZarrExamples.implementFilter();
    ZarrExamples.implementFilterProvider();
    ZarrExamples.implementFilterProvider2();
  }
}
