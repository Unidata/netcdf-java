/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

import javax.annotation.concurrent.Immutable;

/**
 * Implementation of Enhancements for coordinate systems and standard attribute handling.
 * Factored out so that it can be used as a 'mixin' in VariablesDS and StructureDS.
 */
@Immutable
class EnhancementsImpl {
  private final Variable forVar;
  private final String desc;
  private final String units;

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
