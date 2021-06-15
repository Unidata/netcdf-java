package tests.runtime;

import examples.runtime.runtimeloadingTutorial;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestRuntimeloadingTutorial {

  @Test
  public void testRegisterIOSP() {
    Assert.assertThrows(ClassNotFoundException.class, () -> {
      runtimeloadingTutorial.registerIOSP("");
    });
  }

  @Test
  public void testRegisterCoordSystemBuilder() throws ClassNotFoundException {
    runtimeloadingTutorial.registerCoordSystemBuilder("", null);
  }

  @Test
  public void testRegisterCoordTransBuilder() throws ClassNotFoundException {
    Assert.assertThrows(ClassNotFoundException.class, () -> {
      runtimeloadingTutorial.registerCoordTransBuilder("", "");
    });
  }

  @Test
  public void testRegisterFeatureDatasetFactory() {
    runtimeloadingTutorial.registerFeatureDatasetFactory(null, "");
  }

  @Test
  public void testRegisterGRIBTable() {
    runtimeloadingTutorial.registerGRIBTable(0, 0, 0, "");
  }

  @Test
  public void testRegisterGRIBLookupTable() throws IOException {
    Assert.assertThrows(NumberFormatException.class, () -> {
      runtimeloadingTutorial.registerGRIBLookupTable("");
    });
  }

  @Test
  public void testRegisterBUFRTable() throws IOException {
    Assert.assertThrows(FileNotFoundException.class, () -> {
      runtimeloadingTutorial.registerBUFRTable("");
    });
  }

  @Test
  public void testPassConfigurationToCDM() throws IOException {
    Assert.assertThrows(FileNotFoundException.class, () -> {
      runtimeloadingTutorial.passConfigurationToCDM("");
    });
  }

  @Test
  public void testPassConfigurationToolsUI() {
    Assert.assertThrows(NullPointerException.class, () -> {
      runtimeloadingTutorial.passConfigurationToolsUI();
    });
  }
}
