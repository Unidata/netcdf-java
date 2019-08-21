/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.grid;

import java.util.Objects;
import ucar.nc2.wmo.Util;

/**
 * Class which represents a grid parameter.
 * A parameter consists of a number that can be used to look up in a table,
 * a name( ie Temperature), a description( ie Temperature at 2 meters),
 * and Units( ie K ).
 */

public class GridParameter {

  protected int number;
  protected String name;
  protected String description;
  protected String unit;
  protected String cf_name; // CF standard name, if it exists

  public GridParameter() {
    number = -1;
    name = "undefined";
    description = "undefined";
    unit = "undefined";
  }

  public GridParameter(int number, String name, String description, String unit) {
    this.number = number;
    this.name = name;
    setDescription(description);
    setUnit(unit);
  }

  // unknown param
  public GridParameter(int center, int subcenter, int version, int number) {
    this.number = number;
    name = "undefined";
    description = "Unknown-C"+ center + "-S"+ subcenter + "-V"+ version + ":"+number;
    unit = "undefined";
  }

  public GridParameter(int number, String name, String description, String unit, String cf_name) {
    this.number = number;
    this.name = name;
    setDescription(description);
    setUnit(unit);
    this.cf_name = cf_name;
  }

  public final int getNumber() {
    return number;
  }

  public final String getName() {
    return name;
  }

  /**
   * description of parameter.
   *
   * @return description
   */
  public final String getDescription() {
    return description;
  }

  /**
   * unit of parameter.
   *
   * @return unit
   */
  public final String getUnit() {
    return unit;
  }

  public String getCFname() {
    return cf_name;
  }

  /**
   * sets number of parameter.
   *
   * @param number of parameter
   */
  public final void setNumber(int number) {
    this.number = number;
  }

  /**
   * sets name of parameter.
   *
   * @param name of parameter
   */
  public final void setName(String name) {
    this.name = name;
  }

  /**
   * sets description of parameter.
   *
   * @param description of parameter
   */
  public final void setDescription(String description) {
    this.description = Util.cleanName(description);
  }

  /**
   * sets unit of parameter.
   *
   * @param unit of parameter
   */
  public final void setUnit(String unit) {
    this.unit = Util.cleanUnit(unit);
  }

  /**
   * Return a String representation of this object
   *
   * @return a String representation of this object
   */
  @Override
  public String toString() {
    return "GridParameter{" +
            "number=" + number +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", unit='" + unit + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GridParameter that = (GridParameter) o;

    if (number != that.number) return false;
    if (!Objects.equals(cf_name, that.cf_name)) return false;
    if (!Objects.equals(description, that.description)) return false;
    if (!Objects.equals(name, that.name)) return false;
    return Objects.equals(unit, that.unit);

  }

  @Override
  public int hashCode() {
    int result = number;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (unit != null ? unit.hashCode() : 0);
    result = 31 * result + (cf_name != null ? cf_name.hashCode() : 0);
    return result;
  }
}
