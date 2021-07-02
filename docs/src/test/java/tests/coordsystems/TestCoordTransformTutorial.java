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
  public void testVertTransEx() {
    coordTransformTutorial.vertTransEx();
  }


  @Test
  public void testVertTransClass() {
    coordTransformTutorial.vertTransClass();
  }
}
