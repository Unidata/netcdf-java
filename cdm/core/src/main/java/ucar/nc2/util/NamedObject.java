/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import com.google.auto.value.AutoValue;

/** An object that has a name and a description. */
public interface NamedObject {

  /** Get the object's name */
  String getName();

  /** Get the object's description. */
  String getDescription();

  /** Get the object itself */
  Object getValue();

  static NamedObject create(String name, String desc, Object value) {
    return NamedObject.Value.create(name, desc, value);
  }

  static NamedObject create(Object value, String desc) {
    return NamedObject.Value.create(value.toString(), desc, value);
  }

  @AutoValue
  abstract class Value implements NamedObject {
    public abstract String getName();

    public abstract String getDescription();

    public abstract Object getValue();

    private static NamedObject create(String name, String desc, Object value) {
      return new AutoValue_NamedObject_Value(name, desc, value);
    }
  }

}
