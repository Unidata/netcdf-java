/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.internal.dataset.CoordTransformFactory;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.Projection;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;

/**
 * Coverage Coordinate Transform.
 * Immutable with lazy instantiation of projection
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoverageTransform {

  private final String name;
  private final AttributeContainer attributes;
  private final boolean isHoriz;
  private Projection projection; // lazy instantiation

  public CoverageTransform(String name, AttributeContainer attributes, boolean isHoriz) {
    this.name = name;
    this.attributes = attributes;
    this.isHoriz = isHoriz;
  }

  public boolean isHoriz() {
    return isHoriz;
  }

  public Projection getProjection() {
    synchronized (this) {
      if (projection == null && isHoriz) {
        projection = CoordTransformFactory.makeProjection(this, new Formatter());
      }
      return projection;
    }
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordTransform '%s'", indent, name);
    f.format(" isHoriz: %s%n", isHoriz());
    if (projection != null)
      f.format(" projection: %s%n", projection);
    for (Attribute att : attributes)
      f.format("%s     %s%n", indent, att);
    f.format("%n");

    indent.decr();
  }

  /** The attributes contained by this CoverageTransform. */
  public AttributeContainer attributes() {
    return attributes;
  }

}
