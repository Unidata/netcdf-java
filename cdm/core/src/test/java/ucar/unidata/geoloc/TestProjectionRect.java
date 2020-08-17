/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

/** Test {@link ucar.unidata.geoloc.ProjectionRect} */
public class TestProjectionRect {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testDefaultConstructor() {
    ProjectionRect rect = new ProjectionRect();
    assertThat(rect.getMinX()).isEqualTo(-5000);
    assertThat(rect.getMinY()).isEqualTo(-5000);
    assertThat(rect.getWidth()).isEqualTo(10000);
    assertThat(rect.getHeight()).isEqualTo(10000);
    assertThat(rect.isEmpty()).isFalse();
    assertThat(rect.getCenterX()).isEqualTo(0);
    assertThat(rect.getCenterY()).isEqualTo(0);
  }

  @Test
  public void testCornerConstructor() {
    ProjectionPoint pt1 = ProjectionPoint.create(100, 200);
    ProjectionPoint pt2 = ProjectionPoint.create(-100, -200);
    ProjectionRect rect = new ProjectionRect(pt1, pt2);
    assertThat(rect.getMinX()).isEqualTo(-100);
    assertThat(rect.getMinY()).isEqualTo(-200);
    assertThat(rect.getWidth()).isEqualTo(200);
    assertThat(rect.getHeight()).isEqualTo(400);
  }

  @Test
  public void testSpecConstructor() {
    ProjectionRect rect = new ProjectionRect("-4.502221, -4.570379, 3925.936303, 2148.077947");
    ProjectionRect rect2 = ProjectionRect.builder().setRect(-4.502221, -4.570379, 3925.936303, 2148.077947).build();
    assertThat(rect).isEqualTo(rect2);
    assertThat(rect.hashCode()).isEqualTo(rect2.hashCode());
  }

  @Test
  public void testIntersect() {
    ProjectionRect rect = new ProjectionRect();
    assertThat(
        rect.intersects(ProjectionRect.builder().setRect(-4.502221, -4.570379, 3925.936303, 2148.077947).build()))
            .isTrue();
    assertThat(rect.intersects(ProjectionRect.builder().setRect(-6000, -6000, 500, 500).build())).isFalse();
    assertThat(rect.intersects(ProjectionRect.builder().setRect(-6000, -500, 500, 1000).build())).isFalse();
    assertThat(rect.intersects(ProjectionRect.builder().setRect(-500, -6000, 5000, 500).build())).isFalse();

    ProjectionRect rect2 = new ProjectionRect().toBuilder().setX(-6000).setY(-6000).build();
    assertThat(ProjectionRect.intersect(rect, rect2).isEmpty()).isFalse();
    assertThat(ProjectionRect.intersect(rect, rect2)).isEqualTo(new ProjectionRect("-5000, -5000, 9000, 9000"));

    ProjectionRect rect3 = new ProjectionRect().toBuilder().setX(-6000).setY(6000).build();
    assertThat(ProjectionRect.intersect(rect, rect3).isEmpty()).isTrue();
    assertThat(ProjectionRect.intersect(rect, rect3)).isEqualTo(new ProjectionRect("-5000, 6000, 9000, -1000"));
  }

  @Test
  public void testAdd() {
    ProjectionRect rect = new ProjectionRect().toBuilder().add(0, 0).build();
    assertThat(rect).isEqualTo(new ProjectionRect());

    ProjectionRect rect3 = new ProjectionRect().toBuilder().add(ProjectionPoint.create(0, 6000)).build();
    assertThat(rect3).isEqualTo(new ProjectionRect("-5000, -5000, 10000, 11000"));

    ProjectionRect rect2 = new ProjectionRect().toBuilder().add(10000, 1000).build();
    assertThat(rect2).isEqualTo(new ProjectionRect("-5000, -5000, 15000, 10000"));
  }

}
