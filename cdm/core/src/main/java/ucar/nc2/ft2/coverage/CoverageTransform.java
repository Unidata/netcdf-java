/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.internal.dataset.CoordTransformFactory;
import ucar.nc2.internal.dataset.transform.horiz.HorizTransformBuilderIF;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;
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
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoverageTransform.class);

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
        projection = makeProjection(this, new Formatter());
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

  /**
   * Make a Projection object from the parameters in a CoverageTransform
   *
   * @param errInfo pass back error information.
   * @return CoordinateTransform, or null if failure.
   */
  @Nullable
  public static Projection makeProjection(CoverageTransform gct, Formatter errInfo) {
    // standard name
    String transformName = gct.attributes().findAttributeString(CF.GRID_MAPPING_NAME, null);

    if (null == transformName) {
      errInfo.format("**Failed to find Coordinate Transform name from GridCoordTransform= %s%n", gct);
      return null;
    }

    transformName = transformName.trim();

    // do we have a transform registered for this ?
    Class<?> builderClass = CoordTransformFactory.getBuilderClassFor(transformName);
    if (null == builderClass) {
      errInfo.format("**Failed to find CoordTransBuilder name= %s from GridCoordTransform= %s%n", transformName, gct);
      return null;
    }

    // get an instance of that class
    HorizTransformBuilderIF builder;
    try {
      builder = (HorizTransformBuilderIF) builderClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      log.error("Cant create new instance " + builderClass.getName(), e);
      return null;
    }

    String units = gct.attributes().findAttributeString(CDM.UNITS, null);
    builder.setErrorBuffer(errInfo);
    ProjectionCT.Builder<?> ct = builder.makeCoordinateTransform(gct.attributes(), units);
    if (ct == null) {
      return null;
    }

    return ct.build().getProjection();
  }

}
