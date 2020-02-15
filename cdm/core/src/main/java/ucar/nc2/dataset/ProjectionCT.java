/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.Parameter;
import javax.annotation.concurrent.Immutable;

/** A Projection CoordinateTransform has a Projection bijection (GeoX, GeoY) <-> (Lat, Lon). */
@Immutable
public class ProjectionCT extends CoordinateTransform {

  /**
   * Create a Projection Coordinate Transform.
   * 
   * @param name name of transform, must be unique within the NcML.
   * @param authority naming authority.
   * @param proj projection function.
   */
  public ProjectionCT(String name, String authority, ProjectionImpl proj) {
    super(name, authority, TransformType.Projection);
    this.projection = proj;

    for (Parameter p : proj.getProjectionParameters()) {
      addParameter(p);
    }
  }

  /**
   * Get the Projection function.
   * 
   * @return the Projection
   * @deprecated this will return Projection, not ProjectionImpl in 6.
   */
  public ProjectionImpl getProjection() {
    return projection;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final ProjectionImpl projection;

  // not needed?
  protected ProjectionCT(Builder<?> builder, NetcdfDataset ncd) {
    super(builder, ncd);
    this.projection = builder.projection;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setProjection(this.projection);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends CoordinateTransform.Builder<T> {
    public ProjectionImpl projection;
    private boolean built;

    protected abstract T self();

    public Builder<?> setProjection(ProjectionImpl projection) {
      this.projection = projection;
      return self();
    }

    public ProjectionCT build(NetcdfDataset ncd) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new ProjectionCT(this, ncd);
    }
  }
}
