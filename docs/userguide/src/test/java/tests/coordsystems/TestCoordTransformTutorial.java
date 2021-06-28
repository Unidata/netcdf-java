package tests.coordsystems;

import examples.coordsystems.coordTransformTutorial;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

public class TestCoordTransformTutorial {

    private static String exampleDataPathStr = TestDir.cdmLocalTestDataDir + "jan.nc";
    private static NetcdfFile exampleNcfile;

    @BeforeClass
    public static void setUpTests() throws Exception {
        exampleNcfile = NetcdfFiles.open(exampleDataPathStr);
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        exampleNcfile.close();
    }

    @Test
    public void testRegisterTransform() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            coordTransformTutorial.registerTransform();
        });
    }

    @Test
    public void testProjectionEx() {
        coordTransformTutorial.projectionEx();
    }

    @Test
    public void testImplementMakeCoordinateTransform() {
        Assert.assertThrows(NullPointerException.class, () -> {
            coordTransformTutorial.implementMakeCoordinateTransform(null, null);
        });
    }

    @Test
    public void testVertTransEx() {
        coordTransformTutorial.vertTransEx();
    }

    @Test
    public void testImplementMakeVerticalCT() throws IOException {

        Assert.assertThrows(NullPointerException.class, () -> {
            coordTransformTutorial.implementMakeVerticalCT(null, null, "", "", "");
        });

        // test return null
        NetcdfDataset ds = NetcdfDatasets.openDataset(exampleDataPathStr, true, CancelTask.create());
        AttributeContainerMutable ctv = new AttributeContainerMutable("container");
        //ctv.addAttribute(new Attribute("formula_terms", "value"));
        ctv.addAttribute(new Attribute("name2", "value2"));

        coordTransformTutorial.implementMakeVerticalCT(ds, ctv, "", "", "");
        Assert.assertTrue( coordTransformTutorial.implementMakeVerticalCT(ds, ctv, "", "", "") == null);

        // test return null
        //NetcdfDataset ds = NetcdfDatasets.openDataset(exampleDataPathStr, true, CancelTask.create());
        AttributeContainerMutable ctv2 = new AttributeContainerMutable("container");
        ctv2.addAttribute(new Attribute("formula_terms", "value"));
        ctv2.addAttribute(new Attribute("name2", "value2"));
    }

    @Test
    public void testVertTransClass() {
        coordTransformTutorial.vertTransClass();
    }
}
