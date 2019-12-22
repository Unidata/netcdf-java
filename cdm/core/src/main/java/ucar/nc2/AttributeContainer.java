/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import javax.annotation.Nullable;

/**
 * An immutable Container of Attributes.
 * Use AttributeContainerHelper if you want a mutable container.
 *
 * @author caron
 * @since 3/20/14
 */
public interface AttributeContainer {

  /**
   * Returns immutable list of attributes.
   * TODO will return ImmutableList<Attribute> in version 6
   */
  java.util.List<Attribute> getAttributes();

  /**
   * Find a String-valued Attribute by name (ignore case), return the String value of the Attribute.
   * @return the attribute value, or defaultValue if not found
   */
  String findAttValueIgnoreCase(String attName, String defaultValue);

  /**
   * Find a Numeric Attribute by name (ignore case), return the double value of the Attribute.
   * @return the attribute value, or defaultValue if not found
   */
  double findAttributeDouble(String attName, double defaultValue);

  /**
   * Find a Numeric Attribute by name (ignore case), return the integer value of the Attribute.
   * @return the attribute value, or defaultValue if not found
   */
  int findAttributeInteger(String attName, int defaultValue);

  /** Find an Attribute by name */
  @Nullable Attribute findAttribute(String attName);

  /** Find an Attribute by name, ignoring case. */
  @Nullable Attribute findAttributeIgnoreCase(String attName);

  /** Get the (optional) name of the AttributeContainer. */
  @Nullable String getName();

  ///// will be removed in version 6 to make AttributeContainer immutable

  /**
   * Add all; replace old if has same name
   *
   * @deprecated will be removed in version 6.
   */
  @Deprecated
  void addAll(Iterable<Attribute> atts);

  /**
   * Add new or replace old if has same name
   *
   * @param att add this Attribute
   * @return the added attribute
   * @deprecated will be removed in version 6.
   */
  @Deprecated
  Attribute addAttribute(Attribute att);

  /**
   * Remove an Attribute : uses the attribute hashCode to find it.
   *
   * @param a remove this attribute
   * @return true if was found and removed
   * @deprecated will be removed in version 6.
   */
  @Deprecated
  boolean remove(Attribute a);

  /**
   * Remove an Attribute by name.
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   * @deprecated will be removed in version 6.
   */
  @Deprecated
  boolean removeAttribute(String attName);

  /**
   * Remove an Attribute by name, ignoring case
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   * @deprecated will be removed in version 6.
   */
  @Deprecated
  boolean removeAttributeIgnoreCase(String attName);
}
