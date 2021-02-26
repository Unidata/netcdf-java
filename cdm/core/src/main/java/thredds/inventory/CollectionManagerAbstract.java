/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of CollectionManager.
 *
 * @author caron
 * @since Jan 19, 2010
 */
public abstract class CollectionManagerAbstract extends CollectionAbstract implements CollectionManager {

  // called from Aggregation, Fmrc, FeatureDatasetFactoryManager
  public static CollectionManager open(String collectionName, String collectionSpec, String olderThan,
      Formatter errlog) {
    if (collectionSpec.startsWith(CATALOG))
      return new CollectionManagerCatalog(collectionName, collectionSpec, olderThan, errlog);
    else
      return MFileCollectionManager.open(collectionName, collectionSpec, olderThan, errlog);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  ucar.nc2.units.TimeDuration recheck;
  thredds.client.catalog.TimeDuration recheckEvery;
  private boolean isStatic; // true if theres no update element. It means dont scan if index already exists

  CollectionManagerAbstract(String collectionName, org.slf4j.Logger logger) {
    super(collectionName, logger);
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean aStatic) {
    isStatic = aStatic;
  }

  @Override
  public ucar.nc2.units.TimeDuration getRecheck() {
    return recheck;
  }

  @Override
  public thredds.client.catalog.TimeDuration getRecheckEvery() {
    return recheckEvery;
  }

  // fake default implementation
  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(getFilesSorted().iterator(), null);
  }

  @Override
  public void close() {}

  @Override
  public boolean scanIfNeeded() throws IOException {
    // if (map == null && !isStatic()) return true;
    return isScanNeeded() && scan(false);
  }

  /////////////////////////////////////////////////////////////////////

  private final List<TriggerListener> listeners = new ArrayList<>();

  void sendEvent(TriggerEvent event) {
    for (TriggerListener listen : listeners) {
      listen.handleCollectionEvent(event);
    }
  }

  @Override
  public void addEventListener(TriggerListener l) {
    listeners.add(l);
  }

  @Override
  public void removeEventListener(TriggerListener l) {
    listeners.remove(l);
  }

}
