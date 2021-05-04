/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import java.util.Formatter;
import java.util.Optional;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A Client CatalogRef
 *
 * @author caron
 * @since 1/7/2015
 */
public class CatalogRef extends Dataset {
  private final String xlink;
  private boolean isRead;

  public CatalogRef(DatasetNode parent, String name, String xlink, Map<String, Object> flds,
      List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders) {
    super(parent, name, flds, accessBuilders, datasetBuilders);
    this.xlink = xlink;
  }

  public String getXlinkHref() {
    return xlink;
  }

  @Deprecated
  public boolean isRead() {
    return isRead;
  }

  @Deprecated
  public void setRead(boolean isRead) {
    this.isRead = isRead;
  }

  // only present for server catalogs, put here as convenience
  // return Boolean, so can tell if its been set or not.
  public Boolean useRemoteCatalogService() {
    return (Boolean) flds.get(UseRemoteCatalogService);
  }

  /**
   * @return Xlink reference as a URI, resolved
   */
  public URI getURI() {
    try {
      Catalog parent = getParentCatalog();
      if (parent != null)
        return parent.resolveUri(xlink);
    } catch (java.net.URISyntaxException e) {
      return null;
    }
    return null;
  }

  /////////////////////////////////////////////////

  protected String translatePathToReletiveLocation(String dsPath, String configPath) {
    if (dsPath == null)
      return null;
    if (dsPath.isEmpty())
      return null;

    if (dsPath.startsWith("/"))
      dsPath = dsPath.substring(1);

    if (!dsPath.startsWith(configPath))
      return null;

    // remove the matching part, the rest is the "reletive location"
    String dataDir = dsPath.substring(configPath.length());
    if (dataDir.startsWith("/"))
      dataDir = dataDir.substring(1);

    return dataDir;
  }

  //////////////////////////////////////////////////////////////////
  private Catalog proxy;

  @Override
  public boolean hasNestedDatasets() {
    return true;
  }

  @Override
  public List<Dataset> getDatasets() {
    if (proxy != null)
      return proxy.getDatasets();
    return super.getDatasets();
  }

  @Override
  public List<Dataset> getDatasetsLogical() {
    Formatter errlog = new Formatter();
    java.util.Optional<DatasetNode> opt = readCatref(errlog);
    if (!opt.isPresent()) {
      throw new RuntimeException(errlog.toString());
    }

    DatasetNode proxy = opt.get();
    return proxy.getDatasets();
  }

  public synchronized Optional<DatasetNode> readCatref(Formatter errlog) {
    if (proxy != null) {
      return Optional.of(proxy);
    }

    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromCatref(this);
    if (builder.hasFatalError() || cat == null) {
      errlog.format("Error reading catref " + getURI() + " err=" + builder.getErrorMessage());
      return Optional.empty();
    }
    this.proxy = cat;
    return Optional.of(proxy);
  }


}
