/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

// Could use AutoValue
public class HttpNameValue {

  public static HttpNameValue create(String name, String value) {
    return new HttpNameValue(name, value);
  }

  private final String name;
  private final String value;

  private HttpNameValue(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
