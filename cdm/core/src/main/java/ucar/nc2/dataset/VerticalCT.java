/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import javax.annotation.concurrent.Immutable;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.constants.CF;
import ucar.nc2.internal.dataset.transform.vertical.VerticalTransformBuilder;
import ucar.unidata.geoloc.VerticalTransform;

/**
 * A VerticalCT is a CoordinateTransform function CT: (GeoZ) -> Height or Pressure.
 * It may be dependent also on X,Y and/or Time. CT: (X,Y,GeoZ,Time) -> Height or Pressure.
 * This class just records the transformation parameters. The mathematical transformation itself is
 * delegated to a class implementing {@link ucar.unidata.geoloc.VerticalTransform}.
 */
@Immutable
public class VerticalCT extends CoordinateTransform {

  /** Enumeration of known Vertical transformations. */
  public enum Type {
    // These are from CF-1.0: not all are implemented because we dont have an example to test
    HybridSigmaPressure(CF.atmosphere_hybrid_sigma_pressure_coordinate), //
    HybridHeight(CF.atmosphere_hybrid_height_coordinate), //
    LnPressure(CF.atmosphere_ln_pressure_coordinate), //
    OceanSigma(CF.ocean_sigma_coordinate), //
    OceanS(CF.ocean_s_coordinate), //
    Sleve(CF.atmosphere_sleve_coordinate), //
    Sigma(CF.atmosphere_sigma_coordinate), //

    // -Sachin 03/25/09
    OceanSG1("ocean_s_g1"), //
    OceanSG2("ocean_s_g2"), //

    // others
    Existing3DField("Existing3DField"), //
    WRFEta("WRFEta"); //

    private final String name;

    Type(String name) {
      this.name = name;
    }

    /**
     * Find the VerticalCT.Type that matches this name.
     *
     * @param name find this name
     * @return VerticalCT.Type or null if no match.
     */
    public static Type getType(String name) {
      for (Type t : Type.values()) {
        if (t.name.equalsIgnoreCase(name))
          return t;
      }
      return null;
    }

    public String toString() {
      return name;
    }
  }

  protected VerticalCT(String name, String authority, VerticalCT.Type type, AttributeContainer params) {
    super(name, authority, TransformType.Vertical, params);
    this.type = type;
    this.transformBuilder = null;
  }

  /** get the Vertical Transform type */
  public VerticalCT.Type getVerticalTransformType() {
    return type;
  }

  /**
   * Make the Vertical Transform function
   *
   * @param ds containing dataset
   * @param timeDim time Dimension
   * @return VerticalTransform
   */
  public VerticalTransform makeVerticalTransform(NetcdfDataset ds, Dimension timeDim) {
    return transformBuilder.makeMathTransform(ds, timeDim, this);
  }

  @Override
  public String toString() {
    return "VerticalCT {" + "type=" + type + ", builder=" + transformBuilder + '}';
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final VerticalCT.Type type;
  private final VerticalTransformBuilder transformBuilder;

  protected VerticalCT(Builder<?> builder) {
    super(builder);
    this.type = builder.type;
    this.transformBuilder = builder.transformBuilder;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setVerticalType(this.type).setTransformBuilder(this.transformBuilder);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** Get a Builder for VerticalCT. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A Builder of VerticalCT. */
  public static abstract class Builder<T extends Builder<T>> extends CoordinateTransform.Builder<T> {
    public VerticalCT.Type type;
    private VerticalTransformBuilder transformBuilder;
    private boolean built;

    protected abstract T self();

    public Builder<?> setVerticalType(Type type) {
      this.type = type;
      return self();
    }

    public Builder<?> setTransformBuilder(VerticalTransformBuilder transformBuilder) {
      this.transformBuilder = transformBuilder;
      return self();
    }

    public VerticalCT build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new VerticalCT(this);
    }
  }

}
