/* Copyright */
package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;

/** A Builder of Projection CoordinateTransform. */
public interface ProjectionBuilder {

  /**
   * Make a ProjectionCT from a Coordinate Transform Variable.
   * A ProjectionCT is just a container for the metadata, the real work is in the Projection function.
   *
   * @param ctv the coordinate transform variable.
   * @param geoCoordinateUnits the geo X/Y coordinate units, or null.
   * @return ProjectionCT.Builder or null if not able to make one.
   */
  @Nullable
  Projection makeProjection(AttributeContainer ctv, @Nullable String geoCoordinateUnits);

  /** Get the Transform name. */
  String getTransformName();

}
