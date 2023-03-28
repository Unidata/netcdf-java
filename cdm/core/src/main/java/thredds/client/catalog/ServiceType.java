/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

/**
 * Service Type enums
 *
 * @author caron
 * @since 1/7/2015
 */
public enum ServiceType {
  ADDE, // not used
  Catalog("Provide subsetting and HTML conversion services for THREDDS catalogs.", AccessType.Catalog, null), //
  CdmRemote("Provides index subsetting on remote CDM datasets, using ncstream.", AccessType.DataAccess, "cdmremote"), //
  CdmrFeature("Provides coordinate subsetting on remote CDM Feature Datasets, using ncstream.", AccessType.DataAccess,
      "cdmrFeature"), //
  Compound, //
  DAP4("Access dataset through OPeNDAP using the DAP4 protocol.", AccessType.DataAccess, "dap4"), //
  DODS, // deprecated
  File, // deprecated
  FTP, //
  GRIDFTP, //
  H5Service, //
  HTTPServer("HTTP file download.", AccessType.DataAccess, "httpserver"), //
  JupyterNotebook("Generate a Jupyter Notebook that uses Siphon to access this dataset.", AccessType.DataViewer, null), //
  ISO("Provide ISO 19115 metadata representation of a dataset's structure and metadata.", AccessType.Metadata, null), //
  LAS, //
  NcJSON, //
  NCML("Provide NCML representation of a dataset.", AccessType.Metadata, "ncml"), //
  NetcdfSubset("A web service for subsetting CDM scientific datasets.", AccessType.DataAccess, null), //
  OPENDAP("Access dataset through OPeNDAP using the DAP2 protocol.", AccessType.DataAccess, "dods"), //
  OPENDAPG, //
  Resolver, //
  THREDDS("Access dataset through thredds catalog entry", AccessType.DataAccess, "thredds"), //
  UDDC("An evaluation of how well the metadata contained in the dataset"
      + " conforms to the NetCDF Attribute Convention for Data Discovery (NACDD)", AccessType.Metadata, null), //
  WebForm, // ??
  WCS("Supports access to geospatial data as 'coverages'.", AccessType.DataAccess, null), //
  WFS("Supports access to geospatial data as simple geometry objects (such as polygons and lines).",
      AccessType.DataAccess, null), //
  WMS("Supports access to georegistered map images from geoscience datasets.", AccessType.DataAccess, null), //
  WSDL, //
  ;

  private final String desc;
  private final String protocol;
  private final AccessType accessType;

  ServiceType() {
    this.desc = null;
    this.protocol = null;
    this.accessType = AccessType.Unknown;
  }

  ServiceType(String desc, AccessType accessType, String protocol) {
    this.desc = desc;
    this.accessType = accessType;
    this.protocol = protocol;
  }

  // ignore case
  public static ServiceType getServiceTypeIgnoreCase(String typeS) {
    for (ServiceType s : values()) {
      if (s.toString().equalsIgnoreCase(typeS))
        return s;
    }
    return null;
  }

  public boolean isStandardTdsService() {
    return this == Catalog || this == CdmRemote || this == CdmrFeature || this == DAP4 || this == DODS || this == File
        || this == HTTPServer || this == ISO || this == NCML || this == NetcdfSubset || this == OPENDAP || this == UDDC
        || this == WCS || this == WMS || this == WFS;
  }

  public String getDescription() {
    return this.desc;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getAccessType() {
    return this.accessType.name;
  }

  public enum AccessType {
    Catalog("Catalog"), Metadata("Metadata"), DataAccess("Data Access"), DataViewer("Data Viewer"), Unknown("Unknown");

    protected final String name;

    AccessType(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }
}

