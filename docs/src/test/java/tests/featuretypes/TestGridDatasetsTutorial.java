package tests.featuretypes;

import examples.featuretypes.GridDatasetsTutorial;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ft.NoFactoryFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestGridDatasetsTutorial {

  @Test
  public void testGridDatasetFormat() {
    // test open success
    Assert.assertThrows(NoFactoryFoundException.class, () -> {
      GridDatasetsTutorial.gridDatasetFormat(null, "yourLocationAsString", null);
    });
  }

  @Test
  public void testGridFormat() {
    // test open success
    Assert.assertThrows(FileNotFoundException.class, () -> {
      GridDatasetsTutorial.gridFormat("yourLocationAsString");
    });
  }

  @Test
  public void testUsingGridDataset() {
    // test open success
    Assert.assertThrows(NullPointerException.class, () -> {
      GridDatasetsTutorial.usingGridDataset("yourLocationAsString", null);
    });
  }

  @Test
  public void testFindLatLonVal() {
    // test open success
    Assert.assertThrows(FileNotFoundException.class, () -> {
      GridDatasetsTutorial.findLatLonVal("yourLocationAsString", null, 0, 0);
    });
  }

  @Test
  public void testReadingData() throws InvalidRangeException, IOException {
    // test open success
    Assert.assertThrows(NullPointerException.class, () -> {
      GridDatasetsTutorial.readingData(null, 0, 0, 0, 0);
    });
  }

  @Test
  public void testCallMakeSubset() throws IOException {
    // test open success
    Assert.assertThrows(NullPointerException.class, () -> {
      GridDatasetsTutorial.CallMakeSubset(null, null, null, null, null, null, null);
    });
  }

}
