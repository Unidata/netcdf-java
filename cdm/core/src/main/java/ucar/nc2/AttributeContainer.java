/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An immutable Container of Attributes.
 * Use AttributeContainerMutable if you want a mutable container.
 */
public interface AttributeContainer extends Iterable<Attribute> {

  /**
   * Create a new AttributeContainer, removing any whose name starts with one in the given list.
   *
   * @param atts Start with this set of Attributes.
   * @param remove Remove any whose name starts with one of these.
   * @return new AttributeContainer with attributes removed.
   */
  static AttributeContainer filter(AttributeContainer atts, String... remove) {
    List<Attribute> result = new ArrayList<>();
    for (Attribute att : atts) {
      boolean ok = true;
      for (String s : remove) {
        if (att.getShortName().startsWith(s)) {
          ok = false;
          break;
        }
      }
      if (ok) {
        result.add(att);
      }
    }
    return new AttributeContainerMutable(atts.getName(), result).toImmutable();
  }

  /** Find an Attribute by exact match on name. */
  @Nullable
  Attribute findAttribute(String attName);

  /** Determine if named attribute exists (exact match). */
  default boolean hasAttribute(String attName) {
    return null != findAttribute(attName);
  }

  /** Find an Attribute by name, first doing an exact match, then ignoring case. */
  @Nullable
  Attribute findAttributeIgnoreCase(String attName);

  /** Determine if named attribute exists, ignoring case. */
  default boolean hasAttributeIgnoreCase(String attName) {
    return null != findAttributeIgnoreCase(attName);
  }

  /**
   * Find a Numeric Attribute by name (ignore case), return the double value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  double findAttributeDouble(String attName, double defaultValue);

  /**
   * Find a Numeric Attribute by name (ignore case), return the integer value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  int findAttributeInteger(String attName, int defaultValue);

  /**
   * Find a String Attribute by name (ignore case), return the String value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  String findAttributeString(String attName, String defaultValue);

  /** True is there are no attributes in the container. */
  boolean isEmpty();

  /** Get the (optional) name of the AttributeContainer. */
  @Nullable
  String getName();

  /** An unordered iterator over the contained attributes. */
  @Override
  Iterator<Attribute> iterator();
}
