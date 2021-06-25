package tests.coordsystems;

import examples.coordsystems.coordTransformTutorial;
import org.junit.Assert;
import org.junit.Test;

public class TestCoordTransformTutorial {

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
    public void testImplementMakeVerticalCT() {
        Assert.assertThrows(NullPointerException.class, () -> {
            coordTransformTutorial.implementMakeVerticalCT(null, null, "", "", "");
        });
    }

    @Test
    public void testVertTransClass() {
        coordTransformTutorial.vertTransClass();
    }


}
