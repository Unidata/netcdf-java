package tests.featuretypes;

import examples.cdmdatasets.ReadingCdmTutorial;
import examples.featuretypes.GridDatasetsTutorial;
import examples.runtime.RunTimeLoadingTutorial;
import org.junit.Assert;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.grid.Grid;
import ucar.nc2.util.CancelTask;

import java.io.FileNotFoundException;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestGridDatasetsTutorial {

    @Test
    public void testGridDatasetFormat()  {
        // test open success
        Assert.assertThrows(FileNotFoundException.class, () -> {
            GridDatasetsTutorial.gridDatasetFormat(null,  "yourLocationAsString", null);
        });
    }

    @Test
    public void testGridFormat()  {
        // test open success
        Assert.assertThrows(FileNotFoundException.class, () -> {
            GridDatasetsTutorial.gridFormat("yourLocationAsString",   null);
        });
    }

    @Test
    public void testUsingGridDataset()  {
        // test open success
        Assert.assertThrows(FileNotFoundException.class, () -> {
            GridDatasetsTutorial.usingGridDataset("yourLocationAsString",   null);
        });
    }

    @Test
    public void testFindLatLonVal()  {
        // test open success
        Assert.assertThrows(FileNotFoundException.class, () -> {
            GridDatasetsTutorial.findLatLonVal("yourLocationAsString",   null, 0, 0);
        });
    }

    @Test
    public void testReadingData() throws InvalidRangeException, IOException {
        // test open success
        Assert.assertThrows(NullPointerException.class, () -> {
            GridDatasetsTutorial.readingData(null,   null);
        });
    }

    @Test
    public void testCallMakeSubset() throws IOException {
        // test open success
        Assert.assertThrows(NullPointerException.class, () -> {
            GridDatasetsTutorial.CallMakeSubset(null);
        });
    }

}
