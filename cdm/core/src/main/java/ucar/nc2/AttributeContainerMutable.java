/* Copyright */
package ucar.nc2;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/** A mutable collection of Attributes. */
public class AttributeContainerMutable implements AttributeContainer {
  private final String name;
  private List<Attribute> atts;

  public AttributeContainerMutable(String name) {
    this.name = name;
    this.atts = new ArrayList<>();
  }

  public AttributeContainerMutable(String name, Iterable<Attribute> from) {
    this(name);
    addAll(from);
  }

  @Override
  public String getName() {
    return name;
  }

  @Deprecated
  public void setImmutable() {
    this.atts = Collections.unmodifiableList(atts);
  }

  @Override
  public List<Attribute> getAttributes() {
    return atts;
  }

  @Override
  public Attribute addAttribute(Attribute att) {
    if (att == null)
      return null;
    for (int i = 0; i < atts.size(); i++) {
      Attribute a = atts.get(i);
      if (att.getShortName().equals(a.getShortName())) {
        atts.set(i, att); // replace
        return att;
      }
    }
    atts.add(att);
    return att;
  }

  /** Add Attribute; name and value must not be null. */
  public Attribute addAttribute(String name, String value) {
    if (name == null || value == null) {
      return null;
    }
    Attribute att = new Attribute(name, value);
    return addAttribute(att);
  }

  /** Add Attribute; name and value must not be null. */
  public Attribute addAttribute(String name, Number value) {
    if (name == null || value == null) {
      return null;
    }
    Attribute att = new Attribute(name, value);
    return addAttribute(att);
  }

  /** Add all; replace old if has same name. */
  public void addAll(Iterable<Attribute> atts) {
    for (Attribute att : atts)
      addAttribute(att);
  }

  @Override
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    String attValue = null;
    Attribute att = findAttributeIgnoreCase(attName);

    if ((att != null) && att.isString())
      attValue = att.getStringValue();

    if (null == attValue) // not found, use default
      attValue = defaultValue;

    return attValue;
  }

  @Override
  public Attribute findAttribute(String name) {
    for (Attribute a : atts) {
      if (name.equals(a.getShortName()))
        return a;
    }
    return null;
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    Attribute result = findAttribute(name);
    return (result != null) ? result
        : atts.stream().filter(a -> a.getShortName().equalsIgnoreCase(name)).findFirst().orElse(null);
  }

  @Override
  public double findAttributeDouble(String attName, double defaultValue) {
    Attribute att = findAttributeIgnoreCase(attName);
    if (att == null)
      return defaultValue;
    if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  @Override
  public int findAttributeInteger(String attName, int defaultValue) {
    Attribute att = findAttributeIgnoreCase(attName);
    if (att == null)
      return defaultValue;
    if (att.isString())
      return Integer.parseInt(att.getStringValue());
    else
      return att.getNumericValue().intValue();
  }

  /**
   * Remove an Attribute : uses the attribute hashCode to find it.
   *
   * @param a remove this attribute
   * @return true if was found and removed
   */
  public boolean remove(Attribute a) {
    return a != null && atts.remove(a);
  }

  /**
   * Replace an Attribute with a different name, same value.
   *
   * @param a remove this attribute
   */
  public void replace(Attribute a, String newName) {
    atts.remove(a);
    Attribute newAtt = a.toBuilder().setName(newName).build();
    addAttribute(newAtt);
  }

  /**
   * Remove an Attribute by name.
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  public boolean removeAttribute(String attName) {
    Attribute att = findAttribute(attName);
    return att != null && atts.remove(att);
  }

  /**
   * Remove an Attribute by name, ignoring case
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  public boolean removeAttributeIgnoreCase(String attName) {
    Attribute att = findAttributeIgnoreCase(attName);
    return att != null && atts.remove(att);
  }

  public AttributeContainer toImmutable() {
    return new AttributeContainerImmutable(name, atts);
  }

  @Immutable
  private static class AttributeContainerImmutable implements AttributeContainer {
    private final String name;
    private final ImmutableList<Attribute> atts;

    private AttributeContainerImmutable(String name, List<Attribute> atts) {
      this.name = name;
      this.atts = ImmutableList.copyOf(atts);
    }

    @Override
    public List<Attribute> getAttributes() {
      return atts;
    }

    @Override
    public String findAttValueIgnoreCase(String attName, String defaultValue) {
      return atts.stream().filter(a -> a.getShortName().equals(attName)).findFirst().map(Attribute::getStringValue)
          .orElse(defaultValue);
    }

    @Override
    public Attribute findAttribute(String attName) {
      return atts.stream().filter(a -> a.getShortName().equals(attName)).findFirst().orElse(null);
    }

    @Override
    public Attribute findAttributeIgnoreCase(String attName) {
      Attribute result = findAttribute(attName);
      return (result != null) ? result
          : atts.stream().filter(a -> a.getShortName().equalsIgnoreCase(attName)).findFirst().orElse(null);
    }

    @Override
    public double findAttributeDouble(String attName, double defaultValue) {
      Attribute att = findAttributeIgnoreCase(attName);
      if (att == null)
        return defaultValue;
      if (att.isString())
        return Double.parseDouble(att.getStringValue());
      else
        return att.getNumericValue().doubleValue();
    }

    @Override
    public int findAttributeInteger(String attName, int defaultValue) {
      Attribute att = findAttributeIgnoreCase(attName);
      if (att == null)
        return defaultValue;
      if (att.isString())
        return Integer.parseInt(att.getStringValue());
      else
        return att.getNumericValue().intValue();
    }

    @Override
    public String getName() {
      return name;
    }

    @Deprecated
    public void addAll(Iterable<Attribute> atts) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    public Attribute addAttribute(Attribute att) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    public boolean remove(Attribute a) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    public boolean removeAttribute(String attName) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    public boolean removeAttributeIgnoreCase(String attName) {
      throw new UnsupportedOperationException();
    }
  }
}
