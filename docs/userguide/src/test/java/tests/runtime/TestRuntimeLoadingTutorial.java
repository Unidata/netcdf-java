package tests.runtime;

import examples.runtime.runtimeLoadingTutorial;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestRuntimeLoadingTutorial {

    @Test
    public void testRegisterIOSP() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Assert.assertThrows(ClassNotFoundException.class, () -> {
            runtimeLoadingTutorial.registerIOSP("");
        });
    }

    @Test
    public void testRegisterCoordSystemBuilder() throws ClassNotFoundException {
        Assert.assertThrows(ClassNotFoundException.class, () -> {
            runtimeLoadingTutorial.registerCoordSystemBuilder("","");
        });
    }

    @Test
    public void testRegisterCoordTransBuilder() throws ClassNotFoundException {
        Assert.assertThrows(ClassNotFoundException.class, () -> {
            runtimeLoadingTutorial.registerCoordTransBuilder("", "");
        });
    }

    @Test
    public void testRegisterFeatureDatasetFactory() {
        runtimeLoadingTutorial.registerFeatureDatasetFactory(null, "");
    }

    @Test
    public void testRegisterGRIBTable() {
        runtimeLoadingTutorial.registerGRIBTable(0, 0, 0,"");
    }

    @Test
    public void testRegisterGRIBLookupTable() throws IOException {
        Assert.assertThrows(NumberFormatException.class, () -> {
            runtimeLoadingTutorial.registerGRIBLookupTable("");
        });
    }

    @Test
    public void testRegisterBUFRTable() throws IOException {
        Assert.assertThrows(FileNotFoundException.class, () -> {
            runtimeLoadingTutorial.registerBUFRTable("");
        });
    }

    @Test
    public void testPassConfigurationToCDM() throws IOException {
        Assert.assertThrows(FileNotFoundException.class, () -> {
            runtimeLoadingTutorial.passConfigurationToCDM("");
        });
    }

    @Test
    public void testPassConfigurationToolsUI() {
        Assert.assertThrows(NullPointerException.class, () -> {
            runtimeLoadingTutorial.passConfigurationToolsUI();
        });
    }
}
