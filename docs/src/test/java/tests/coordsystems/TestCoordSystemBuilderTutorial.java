package tests.coordsystems;

import examples.coordsystems.coordSystemBuilderTutorial;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.test.TestDir;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestCoordSystemBuilderTutorial {

  @Test
  public void testOpenDataset() throws IOException {
    // test open fail
    Assert.assertThrows(FileNotFoundException.class, () -> {
      examples.coordsystems.coordSystemBuilderTutorial.openDataset("", true, null);
    });
  }

  @Test
  public void testisMineEx() {
    coordSystemBuilderTutorial.isMineEx();
  }

  @Test
  public void testAugmentDataset1() {
    coordSystemBuilderTutorial.augmentDataset1();
  }

  @Test
  public void testAugmentDataset2() throws IOException {
    coordSystemBuilderTutorial.augmentDataset2();
  }

  @Test
  public void testWrapNcmlExample() throws IOException {
    Assert.assertThrows(NullPointerException.class, () -> {
      coordSystemBuilderTutorial.wrapNcmlExample(null, null);
    });
  }

  @Test
  public void testRegisterNcml() {
    coordSystemBuilderTutorial.registerNcml("", "");
  }

  @Test
  public void testAugmentDataset3() {
    coordSystemBuilderTutorial.augmentDataset3(null, null);
  }

  @Test
  public void testGetAxisType() {
    coordSystemBuilderTutorial.getAxisType();
  }

  @Test
  public void testArgumentDataset4() throws IOException {
    coordSystemBuilderTutorial.argumentDataset4();
  }

  @Test
  public void testArgumentDataset5() {
    coordSystemBuilderTutorial.argumentDataset5(null);
  }
}
