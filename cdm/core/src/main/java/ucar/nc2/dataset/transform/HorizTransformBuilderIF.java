/* Copyright */
package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;

import javax.annotation.Nullable;
import java.util.Formatter;

/** A Builder of Projection CoordinateTransform. */
public interface HorizTransformBuilderIF {

  /**
   * Make a ProjectionCT from a Coordinate Transform Variable.
   * A ProjectionCT is just a container for the metadata, the real work is in the Projection function.
   *
   * @param ctv the coordinate transform variable.
   * @param geoCoordinateUnits the geo X/Y coordinate units, or null.
   * @return ProjectionCT.Builder or null if not able to make one.
   */
  @Nullable
  ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, @Nullable String geoCoordinateUnits);

  /** Get the Transform name. */
  String getTransformName();

  /*** Pass in a Formatter where error messages can be appended. */
  void setErrorBuffer(Formatter sb);

}
