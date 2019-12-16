/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

/**
 * An Immutable Container of Attributes.
 * Use AttributeContainerHelper if you want a mutable container.
 *
 * @author caron
 * @since 3/20/14
 *        TODO will be Immutable in version 6
 */
public interface AttributeContainer {

  /**
   * Returns immutable list of attributes.
   * TODO will return ImmutableList<Attribute> in version 6
   */
  java.util.List<Attribute> getAttributes();

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
   * Find a String-valued Attribute by Attribute name (ignore case), return the (string) value of the Attribute.
   * 
   * @return the attribute value, or defaultValue if not found
   */
  String findAttValueIgnoreCase(String attName, String defaultValue);

  Attribute findAttribute(String attName);

  Attribute findAttributeIgnoreCase(String attName);

  String getName();

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
