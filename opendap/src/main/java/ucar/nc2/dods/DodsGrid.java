/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import java.util.*;

/**
 * A DODS Grid.
 * A Grid has a Variable and associated coordinate variables, called maps.
 */
class DodsGrid extends DodsVariable {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsGrid.class);

  static DodsGrid.Builder builder(Group.Builder parentGroup, String dodsShortName, DodsV dodsV) {
    DodsGrid.Builder<?> builder = builder().setName(DodsNetcdfFiles.makeShortName(dodsShortName))
        .setDataType(dodsV.getDataType()).setSPobject(dodsV);

    DodsV array = dodsV.children.get(0);
    // the common case is that the map vectors already exist as top level variables
    List<Dimension> dims = new ArrayList<>();
    Formatter sbuff = new Formatter();
    for (int i = 1; i < dodsV.children.size(); i++) {
      DodsV map = dodsV.children.get(i);
      String name = DodsNetcdfFiles.makeShortName(map.bt.getEncodedName());
      Dimension dim = parentGroup.findDimension(name).orElse(null);
      if (dim == null) {
        logger.warn("DODSGrid cant find dimension = <" + name + ">");
      } else {
        dims.add(dim);
        sbuff.format("%s ", name);
      }
    }

    builder.setDimensions(dims);
    builder.setDataType(array.getDataType());
    Attribute att = new Attribute(_Coordinate.Axes, sbuff.toString());
    builder.addAttribute(att);

    return builder;
  }

  ////////////////////////////////////////////////////////

  protected DodsGrid(DodsGrid.Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  @Override
  public DodsGrid.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected DodsGrid.Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (DodsGrid.Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static DodsGrid.Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends DodsGrid.Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A builder of Structures. */
  public static abstract class Builder<T extends DodsGrid.Builder<T>> extends DodsVariable.Builder<T> {
    private boolean built;

    /** Normally this is only called by Group.build() */
    public DodsGrid build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new DodsGrid(this, parentGroup);
    }
  }

}
