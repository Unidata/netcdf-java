/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.unidata.geoloc.Projection;

/**
 * A Projection CoordinateTransform has a Projection bijection (GeoX, GeoY) to/from (Lat, Lon).
 * All the work is done in the {@link ucar.unidata.geoloc.Projection}.
 */
@Immutable
public class ProjectionCT extends CoordinateTransform {
  /**
   * Create a Projection Coordinate Transform.
   * 
   * @param name name of transform, must be unique within the NcML.
   * @param authority naming authority.
   * @param proj projection function.
   */
  public ProjectionCT(String name, String authority, Projection proj) {
    super(name, authority, TransformType.Projection, proj.getProjectionAttributes());
    this.projection = proj;
  }

  /**
   * Get the Projection function.
   * 
   * @return the Projection or null
   */
  @Nullable
  public Projection getProjection() {
    return projection;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO: No one needs the builder?

  private final Projection projection;

  protected ProjectionCT(Builder<?> builder) {
    super(builder);
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

  /** Get a Builder for ProjectionCT. */
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
    public Projection projection;
    private boolean built;

    protected abstract T self();

    public Builder<?> setProjection(Projection projection) {
      this.projection = projection;
      return self();
    }

    public ProjectionCT build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new ProjectionCT(this);
    }
  }
}
