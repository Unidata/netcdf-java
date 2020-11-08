/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.unidata.util.Parameter;

import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem.
 *
 * CoordinateTransform is the superclass for ProjectionCT and VerticalCT.
 * It contains the Attributes/Parameters needed to make a "Coordinate Transform Variable".
 */
@Immutable
public abstract class CoordinateTransform implements Comparable<CoordinateTransform> {

  public String getName() {
    return name;
  }

  // TODO is this needed?
  public String getAuthority() {
    return authority;
  }

  public TransformType getTransformType() {
    return transformType;
  }

  public ImmutableList<Parameter> getParameters() {
    return params;
  }

  public AttributeContainer getCtvAttributes() {
    return ctvAttributes;
  }


  /**
   * Convenience function; look up Parameter by name, ignoring case.
   *
   * @param name the name of the attribute
   * @return the Attribute, or null if not found
   */
  public Parameter findParameterIgnoreCase(String name) {
    for (Parameter a : params) {
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  @Override
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof CoordinateTransform))
      return false;

    CoordinateTransform o = (CoordinateTransform) oo;
    if (!getName().equals(o.getName()))
      return false;
    if (!getAuthority().equals(o.getAuthority()))
      return false;
    if (!(getTransformType() == o.getTransformType()))
      return false;

    List<Parameter> oparams = o.getParameters();
    if (params.size() != oparams.size())
      return false;

    for (int i = 0; i < params.size(); i++) {
      Parameter att = params.get(i);
      Parameter oatt = oparams.get(i);
      if (!att.getName().equals(oatt.getName()))
        return false;
      // if (!att.getValue().equals(oatt.getValue())) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getAuthority().hashCode();
      result = 37 * result + getTransformType().hashCode();
      for (Parameter att : params) {
        result = 37 * result + att.getName().hashCode();
        // result = 37*result + att.getValue().hashCode(); // why not?
      }
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode;

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
  final ImmutableList<Parameter> params; // what is difference with attributes ??
  final AttributeContainer ctvAttributes;

  protected CoordinateTransform(Builder<?> builder) {
    this.name = builder.name;
    this.authority = builder.authority;
    this.transformType = builder.transformType;
    this.params = ImmutableList.copyOf(builder.params);
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
  protected CoordinateTransform(String name, String authority, TransformType transformType, List<Parameter> params) {
    this.name = name;
    this.authority = authority;
    this.transformType = transformType;
    this.params = ImmutableList.copyOf(params);
    this.ctvAttributes = null; // WTF??
  }

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
    private AttributeContainerMutable ctvAttributes = new AttributeContainerMutable(".");
    private List<Parameter> params = new ArrayList<>();

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

    public T addParameter(Parameter param) {
      params.add(param);
      return self();
    }

    public abstract CoordinateTransform build();
  }

}
