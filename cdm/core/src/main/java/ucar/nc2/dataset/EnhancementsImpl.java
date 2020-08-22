/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import java.util.*;

/**
 * Implementation of Enhancements for coordinate systems and standard attribute handling.
 * Factored out so that it can be used as a 'mixin' in VariablesDS and StructureDS.
 * TODO make immutable
 */
class EnhancementsImpl {
  private final Variable forVar;
  private final String desc;
  private final String units;
  private ImmutableList<CoordinateSystem> coordSys; // LOOK not immutable

  /**
   * Constructor.
   * 
   * @param forVar the Variable to decorate.
   * @param units set unit string.
   * @param desc set description.
   */
  EnhancementsImpl(Variable forVar, String units, String desc) {
    Preconditions.checkNotNull(forVar);
    this.forVar = forVar;
    this.units = makeUnits(units);
    this.desc = makeDescription(desc);
  }

  /**
   * Get the description of the Variable.
   * May be set explicitly, or for attributes: CDM.LONG_NAME, "description", CDM.TITLE, CF.STANDARD_NAME.
   */
  public String getDescription() {
    return desc;
  }

  /**
   * Get the Unit String for the Variable. May be set explicitly, else look for attribute CDM.UNITS.
   */
  public String getUnitsString() {
    return units;
  }

  /**
   * Get the list of Coordinate Systems for this variable.
   * Normally this is empty unless you use ucar.nc2.dataset.NetcdfDataset.
   * 
   * @return list of type ucar.nc2.dataset.CoordinateSystem; may be empty not null.
   */
  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return (coordSys == null) ? ImmutableList.of() : coordSys;
  }

  /** Backdoor, do not use. */
  void setCoordinateSystem(ImmutableList<CoordinateSystem> cs) {
    this.coordSys = cs;
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    if (coordSys != null)
      coordSys.remove(p0);
  }

  /**
   * Make the description of the Variable.
   * Default is to look for attributes in this order: CDM.LONG_NAME, "description", "title", "standard_name".
   */
  private String makeDescription(String desc) {
    if (desc == null) {
      Attribute att = forVar.attributes().findAttributeIgnoreCase(CDM.LONG_NAME);
      if ((att != null) && att.isString())
        desc = att.getStringValue();

      if (desc == null) {
        att = forVar.attributes().findAttributeIgnoreCase("description");
        if ((att != null) && att.isString())
          desc = att.getStringValue();
      }

      if (desc == null) {
        att = forVar.attributes().findAttributeIgnoreCase(CDM.TITLE);
        if ((att != null) && att.isString())
          desc = att.getStringValue();
      }

      if (desc == null) {
        att = forVar.attributes().findAttributeIgnoreCase(CF.STANDARD_NAME);
        if ((att != null) && att.isString())
          desc = att.getStringValue();
      }
    }
    return (desc == null) ? null : desc.trim();
  }

  /** Make the Unit String for this Variable. Default is to use the CDM.UNITS attribute. */
  private String makeUnits(String units) {
    if (units == null) {
      Attribute att = forVar.attributes().findAttributeIgnoreCase(CDM.UNITS);
      if ((att != null) && att.isString()) {
        units = att.getStringValue().trim();
      }
    }
    return units;
    // LOOK forVar.addAttribute(new Attribute(CDM.UNITS, units));
  }
}
