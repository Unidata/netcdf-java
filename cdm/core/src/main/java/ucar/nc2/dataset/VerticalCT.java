/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.Dimension;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.transform.VertTransformBuilderIF;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.Parameter;


/**
 * A VerticalCT is a CoordinateTransform function CT: (GeoZ) -> Height or Pressure.
 * Typically it may be dependent also on X,Y and/or Time. CT: (X,Y,GeoZ,Time) -> Height or Pressure.
 * This class just records the transformation parameters. The mathematical transformation itself is
 * delegated to a class implementing ucar.unidata.geoloc.VerticalTransform.
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
    Existing3DField("atmosphere_sigma"), //
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

  protected VerticalCT(String name, String authority, VerticalCT.Type type, List<Parameter> params) {
    super(name, authority, TransformType.Vertical, params);
    this.type = type;
    this.transformBuilder = null;
  }

  /**
   * Create a Vertical Coordinate Transform.
   *
   * @param name name of transform, must be unique within the dataset.
   * @param authority naming authority.
   * @param type type of vertical transform
   * @param builder creates the VerticalTransform
   */
  public VerticalCT(String name, String authority, VerticalCT.Type type, VertTransformBuilderIF builder) {
    super(name, authority, TransformType.Vertical);
    this.type = type;
    this.transformBuilder = builder;
  }

  /**
   * Copy Constructor
   *
   * @param from copy from this one
   * @deprecated use builder
   */
  @Deprecated
  public VerticalCT(VerticalCT from) {
    super(from.getName(), from.getAuthority(), from.getTransformType());
    this.type = from.getVerticalTransformType();
    this.transformBuilder = from.getTransformBuilder();
  }

  /**
   * get the Vertical Transform type
   *
   * @return the Vertical Transform Type
   */
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
    // LOOK This is a VerticalTransform.Builder, not a VerticalCT.builder
    return transformBuilder.makeMathTransform(ds, timeDim, this);
  }

  /**
   * get the CoordTransBuilderIF
   *
   * @return builder
   * @deprecated do not use
   */
  @Deprecated
  public VertTransformBuilderIF getTransformBuilder() {
    return transformBuilder;
  }

  @Override
  public String toString() {
    String builderName = transformBuilder == null ? " none" : transformBuilder.getTransformName();
    return "VerticalCT {" + "type=" + type + ", builder=" + builderName + '}';
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO Make these final and immutable
  private final VerticalCT.Type type;
  private final VertTransformBuilderIF transformBuilder;

  // not needed?
  protected VerticalCT(Builder<?> builder, NetcdfDataset ncd) {
    super(builder, ncd);
    this.type = builder.type;
    this.transformBuilder = null; // placeholder
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
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
    public VerticalCT.Type type;
    private boolean built;

    protected abstract T self();

    public Builder<?> setType(Type type) {
      this.type = type;
      return self();
    }

    public VerticalCT build(NetcdfDataset ncd) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new VerticalCT(this, ncd);
    }
  }

}
