/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Formatter;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.unidata.util.Parameter;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem, such as lat, lon.
 *
 * @author caron
 */

@ThreadSafe
public class CoordinateTransform implements Comparable<CoordinateTransform> {
  /**
   * Create a Coordinate Transform.
   *
   * @param name name of transform, must be unique within the NcML.
   * @param authority naming authority
   * @param transformType type of transform.
   * @deprecated Use CoordinateTransform.builder()
   */
  @Deprecated
  public CoordinateTransform(String name, String authority, TransformType transformType) {
    this.name = name;
    this.authority = authority;
    this.transformType = transformType;
    this.params = new ArrayList<>();
  }

  /**
   * add a parameter
   * 
   * @param param add this Parameter
   * @deprecated Use CoordinateTransform.builder()
   */
  @Deprecated
  public void addParameter(Parameter param) {
    params.add(param);
  }

  /**
   * get the name
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * get the naming authority
   * 
   * @return the naming authority
   */
  public String getAuthority() {
    return authority;
  }

  /**
   * get the transform type
   * 
   * @return the transform type
   */
  public TransformType getTransformType() {
    return transformType;
  }

  /**
   * get list of ProjectionParameter objects.
   * 
   * @return list of ProjectionParameter objects.
   */
  public List<Parameter> getParameters() {
    return params;
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

  /**
   * Instances which have same name, authority and parameters are equal.
   */
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

  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getAuthority().hashCode();
      result = 37 * result + getTransformType().hashCode();
      for (Parameter att : params) {
        result = 37 * result + att.getName().hashCode();
        // result = 37*result + att.getValue().hashCode();
      }
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode;

  public String toString() {
    return name;
  }

  @Override
  public int compareTo(CoordinateTransform oct) {
    return name.compareTo(oct.getName());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO make these final and immutable in 6.

  protected String name, authority;
  protected final TransformType transformType;
  protected List<Parameter> params;
  private AttributeContainerMutable attributeContainer;

  // not needed?
  protected CoordinateTransform(Builder<?> builder, NetcdfDataset ncd) {
    this.name = builder.name;
    this.authority = builder.authority;
    this.transformType = builder.transformType;
    this.attributeContainer = new AttributeContainerMutable(this.name);
    this.attributeContainer.addAll(builder.attributeContainer);

    CoordinateTransform ct =
        CoordTransBuilder.makeCoordinateTransform(ncd, builder.attributeContainer, new Formatter(), new Formatter());
    ct.attributeContainer = new AttributeContainerMutable(this.name);
    ct.attributeContainer.addAll(builder.attributeContainer);
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return b.setName(this.name).setAuthority(this.authority).setTransformType(this.transformType)
        .setAttributeContainer(this.attributeContainer);
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> {
    public String name;
    private String authority;
    private TransformType transformType;
    private AttributeContainer attributeContainer;
    private CoordinateTransform preBuilt;
    private boolean built;

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

    public T setAttributeContainer(AttributeContainer attributeContainer) {
      this.attributeContainer = attributeContainer;
      return self();
    }

    public T setPreBuilt(CoordinateTransform preBuilt) {
      this.preBuilt = preBuilt;
      this.name = preBuilt.name;
      return self();
    }

    public CoordinateTransform build(NetcdfDataset ncd) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;

      if (this.preBuilt != null) {
        return this.preBuilt;
      }

      // All this trouble because we need ncd before we can build.
      CoordinateTransform ct =
          CoordTransBuilder.makeCoordinateTransform(ncd, attributeContainer, new Formatter(), new Formatter());
      if (ct != null) {
        ct.name = this.name;
        ct.attributeContainer = new AttributeContainerMutable(this.name);
        ct.attributeContainer.addAll(attributeContainer);
      }

      return ct;
    }
  }

}
