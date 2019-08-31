/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

/**
 * Type-safe enumeration of THREDDS Service types.
 *
 * @author john caron
 */

public final class ServiceType {
  private static java.util.List<ServiceType> members = new java.util.ArrayList<ServiceType>(20);

  public static final ServiceType NONE = new ServiceType("");

  public static final ServiceType ADDE = new ServiceType("ADDE");
  public static final ServiceType DODS = new ServiceType("DODS");
  public static final ServiceType OPENDAP = new ServiceType("OPENDAP");
  public static final ServiceType DAP4 = new ServiceType("DAP4");
  public static final ServiceType OPENDAPG = new ServiceType("OPENDAP-G");

  public static final ServiceType HTTPServer = new ServiceType("HTTPServer");
  public static final ServiceType FTP = new ServiceType("FTP");
  public static final ServiceType GRIDFTP = new ServiceType("GridFTP");
  public static final ServiceType FILE = new ServiceType("File");

  public static final ServiceType LAS = new ServiceType("LAS");
  public static final ServiceType WMS = new ServiceType("WMS");
  public static final ServiceType WFS = new ServiceType("WFS");
  public static final ServiceType WCS = new ServiceType("WCS");
  public static final ServiceType WSDL = new ServiceType("WSDL");

  // NGDC addition 5/10/2011
  public static final ServiceType NCML = new ServiceType("NCML");
  public static final ServiceType UDDC = new ServiceType("UDDC");
  public static final ServiceType ISO = new ServiceType("ISO");
  public static final ServiceType ncJSON = new ServiceType("ncJSON");
  public static final ServiceType H5Service = new ServiceType("H5Service");

  public static final ServiceType WebForm = new ServiceType("WebForm");

  public static final ServiceType CATALOG = new ServiceType("Catalog");
  public static final ServiceType QC = new ServiceType("QueryCapability");
  public static final ServiceType RESOLVER = new ServiceType("Resolver");
  public static final ServiceType COMPOUND = new ServiceType("Compound");
  public static final ServiceType THREDDS = new ServiceType("THREDDS");

  // experimental
  public static final ServiceType NetcdfSubset = new ServiceType("NetcdfSubset");
  public static final ServiceType CdmRemote = new ServiceType("CdmRemote");
  public static final ServiceType CdmrFeature = new ServiceType("CdmrFeature");

  // deprecated - do not use
  public static final ServiceType NETCDF = new ServiceType("NetCDF"); // deprecated - use dataFormatType = NetCDF
  public static final ServiceType HTTP = new ServiceType("HTTP"); // deprecated - use HTTPServer
  public static final ServiceType NetcdfServer = new ServiceType("NetcdfServer"); // deprecated - use CdmRemote

  private String name;

  private ServiceType(String s) {
    this.name = s;
    members.add(this);
  }

  private ServiceType(String name, boolean fake) {
    this.name = name;
  }

  /**
   * Get all ServiceType objects
   *
   * @return all ServiceType objects
   */
  public static java.util.Collection<ServiceType> getAllTypes() {
    return members;
  }

  /**
   * Return the known ServiceType that matches the given name (ignoring case)
   * or null if the name is unknown.
   *
   * @param name name of the desired ServiceType.
   * @return ServiceType or null if no match.
   */
  public static ServiceType findType(String name) {
    if (name == null)
      return null;
    for (ServiceType serviceType : members) {
      if (serviceType.name.equalsIgnoreCase(name))
        return serviceType;
    }
    return null;
  }

  /**
   * Return a ServiceType that matches the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired ServiceType
   * @return the named ServiceType or null if given name is null.
   */
  public static ServiceType getType(String name) {
    if (name == null)
      return null;
    ServiceType type = findType(name);
    return type != null ? type : new ServiceType(name, false);
  }

  /**
   * Return the ServiceType name.
   */
  public String toString() {
    return name;
  }

  /**
   * Override Object.hashCode() to be consistent with this equals.
   */
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * ServiceType with same name are equal.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ServiceType))
      return false;
    return o.hashCode() == this.hashCode();
  }
}
