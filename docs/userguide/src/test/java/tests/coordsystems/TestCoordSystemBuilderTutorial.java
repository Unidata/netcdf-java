package tests.coordsystems;

import examples.coordsystems.coordSystemBuilderTutorial;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestCoordSystemBuilderTutorial {

    @Test
    public void testOpenDataset() throws IOException {
        Assert.assertThrows(FileNotFoundException.class, () -> {
            examples.coordsystems.coordSystemBuilderTutorial.openDataset("", true, null);
        });
    }

    @Test
    public void testAugmentDataset1() throws IOException {
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
    public void testRegisterNcml() throws IOException {
        coordSystemBuilderTutorial.registerNcml("", "");
    }

    @Test
    public void testAugmentDataset3() throws IOException {
        coordSystemBuilderTutorial.augmentDataset3(null, null);
    }

    @Test
    public void testGetAxisType() throws IOException {
        coordSystemBuilderTutorial.getAxisType();
    }

    @Test
    public void testArgumentDataset4() throws IOException {
        coordSystemBuilderTutorial.argumentDataset4();
    }

    @Test
    public void testArgumentDataset5() throws IOException {
        coordSystemBuilderTutorial.argumentDataset5(null);
    }

}
