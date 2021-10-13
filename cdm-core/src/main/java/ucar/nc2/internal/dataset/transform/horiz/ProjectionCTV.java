/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.transform.horiz;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import ucar.nc2.AttributeContainer;
import ucar.unidata.geoloc.Projection;

import java.util.Objects;

/**
 * A Projection Coordinate Transform Variable has the metadata needed to construct a Projection.
 */
@Immutable
public class ProjectionCTV {
  private final String name;
  private final AttributeContainer metadata;
  private final String geoUnits;
  private final Projection preComputed;

  /**
   * Create a Projection Coordinate Transform from metadata.
   * 
   * @param name name of transform, must be unique within the dataset.
   * @param metadata projection metadata.
   */
  public ProjectionCTV(String name, AttributeContainer metadata, String geoUnits) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(metadata);
    this.name = name;
    this.metadata = metadata;
    this.geoUnits = geoUnits;
    this.preComputed = null;
  }

  /**
   * Create a Projection Coordinate Transform from a precomputed Projection.
   *
   * @param name name of transform, must be unique within the dataset.
   * @param preComputed projection.
   */
  public ProjectionCTV(String name, Projection preComputed) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(preComputed);
    this.name = name;
    this.geoUnits = null;
    this.metadata = preComputed.getProjectionAttributes();
    this.preComputed = preComputed;
  }

  /** The CoordinateTransform's name. */
  public String getName() {
    return name;
  }

  /** The expected units of the x, y axis. */
  @Nullable
  public String getGeounits() {
    return geoUnits;
  }

  /** The attributes in the corresponding CoordinateTransform's Variable. */
  public AttributeContainer getCtvAttributes() {
    return this.metadata;
  }

  /** The precomputed Projection, or null. */
  @Nullable
  public Projection getPrecomputedProjection() {
    return this.preComputed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProjectionCTV that = (ProjectionCTV) o;
    return name.equals(that.name) && metadata.equals(that.metadata) && Objects.equals(preComputed, that.preComputed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, metadata, preComputed);
  }
}
