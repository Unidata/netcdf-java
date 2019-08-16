package thredds.ui.widget;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.junit.Test;

public class TestScaledPanel {

  @Test
  public void testStuff() {

    Rectangle2D w = new Rectangle2D.Double(1.0, 1.0, 10.0, 10.0);
    Rectangle2D s = new Rectangle2D.Double(0.0, 0.0, 200, 100);

    double xs = s.getWidth() / w.getWidth();
    double ys = s.getHeight() / w.getHeight();

    AffineTransform cat = new AffineTransform();
    cat.setToScale(xs, -ys);
    cat.translate(-w.getX(), -w.getY() - w.getHeight());

    Point2D src = new Point2D.Double(1.0, 1.0);
    Point2D dst = new Point2D.Double(0.0, 0.0);

    System.out.println("  screen = " + s);
    System.out.println("  world = " + w);

    System.out.println("  pt = " + src);
    System.out.println("  transform = " + cat.transform(src, dst));

    src = new Point2D.Double(11.0, 11.0);
    System.out.println("  pt = " + src);
    System.out.println("  transform = " + cat.transform(src, dst));
  }


}
