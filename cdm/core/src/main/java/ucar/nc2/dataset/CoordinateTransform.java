/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.unidata.util.Parameter;

import javax.annotation.concurrent.Immutable;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem.
 *
 * CoordinateTransform is the superclass for ProjectionCT and VerticalCT.
 * It contains the Attributes/Parameters needed to make a "Coordinate Transform Variable".
 */
@Immutable
public abstract class CoordinateTransform implements Comparable<CoordinateTransform> {

  /** The CoordinateTransform's name. */
  public String getName() {
    return name;
  }

  /** The naming authority. */
  // TODO is this needed?
  public String getAuthority() {
    return authority;
  }

  /** The CoordinateTransform's type. */
  public TransformType getTransformType() {
    return transformType;
  }

  /** @deprecated use getCtvAttributes() */
  @Deprecated
  public ImmutableList<Parameter> getParameters() {
    ImmutableList.Builder<Parameter> params = ImmutableList.builder();
    for (Attribute a : ctvAttributes) {
      params.add(Attribute.toParameter(a));
    }
    return params.build();
  }

  /** The attributes in the corresponding CoordinateTransform's Variable. */
  public AttributeContainer getCtvAttributes() {
    return ctvAttributes;
  }

  /** @deprecated use getCtvAttributes() */
  @Deprecated
  public Parameter findParameterIgnoreCase(String name) {
    for (Attribute a : ctvAttributes) {
      if (name.equalsIgnoreCase(a.getName()))
        return Attribute.toParameter(a);
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CoordinateTransform that = (CoordinateTransform) o;
    return Objects.equal(name, that.name) && Objects.equal(authority, that.authority)
        && transformType == that.transformType && Objects.equal(ctvAttributes, that.ctvAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, authority, transformType, ctvAttributes);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(CoordinateTransform oct) {
    return name.compareTo(oct.getName());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  final String name, authority;
  final TransformType transformType;
  final AttributeContainer ctvAttributes;

  protected CoordinateTransform(Builder<?> builder) {
    this.name = builder.name;
    this.authority = builder.authority;
    this.transformType = builder.transformType;
    this.ctvAttributes = builder.ctvAttributes.setName(this.name).toImmutable();
  }

  /**
   * Create a Coordinate Transform.
   *
   * @param name name of transform, must be unique within the Coordinate System.
   * @param authority naming authority
   * @param transformType type of transform.
   * @param params list of Parameters.
   */
  protected CoordinateTransform(String name, String authority, TransformType transformType, AttributeContainer params) {
    this.name = name;
    this.authority = authority;
    this.transformType = transformType;
    AttributeContainerMutable atts = new AttributeContainerMutable(this.name);
    atts.addAll(params);
    this.ctvAttributes = atts.toImmutable();
  }

  /** Convert to the mutable Builder. */
  public abstract Builder<?> toBuilder();

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return b.setName(this.name).setAuthority(this.authority).setTransformType(this.transformType)
        .setCtvAttributes(this.ctvAttributes);
  }

  public static abstract class Builder<T extends Builder<T>> {
    public String name;
    private String authority;
    private TransformType transformType;
    private final AttributeContainerMutable ctvAttributes = new AttributeContainerMutable(".");

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    public T setAuthority(String authority) {
      this.authority = authority;
      return self();
    }

    public T setTransformType(TransformType transformType) {
      this.transformType = transformType;
      return self();
    }

    public T setCtvAttributes(AttributeContainer attributeContainer) {
      this.ctvAttributes.addAll(attributeContainer);
      return self();
    }

    /** @deprecated use addParameter(Attribute param) */
    @Deprecated
    public T addParameter(Parameter param) {
      this.ctvAttributes.addAttribute(Attribute.fromParameter(param));
      return self();
    }

    public T addParameter(Attribute param) {
      this.ctvAttributes.addAttribute(param);
      return self();
    }

    public abstract CoordinateTransform build();
  }

}
