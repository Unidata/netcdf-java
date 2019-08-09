/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.ListenerManager;

import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of CollectionManager.
 *
 * @author caron
 * @since Jan 19, 2010
 */
public abstract class CollectionManagerAbstract extends CollectionAbstract implements CollectionManager  {

    // called from Aggregation, Fmrc, FeatureDatasetFactoryManager
  static public CollectionManager open(String collectionName, String collectionSpec, String olderThan, Formatter errlog) {
    if (collectionSpec.startsWith(CATALOG))
      return new CollectionManagerCatalog(collectionName, collectionSpec, olderThan, errlog);
    else
      return MFileCollectionManager.open(collectionName, collectionSpec, olderThan, errlog);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  TimeDuration recheck;
  private ListenerManager lm; // lazy init
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
  public TimeDuration getRecheck() {
    return recheck;
  }

  // fake default implementation
  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator( getFilesSorted().iterator(), null);
  }

  @Override
  public void close() {}

  @Override
  public boolean scanIfNeeded() throws IOException {
    // if (map == null && !isStatic()) return true;
    return isScanNeeded() && scan(false);
  }

  /////////////////////////////////////////////////////////////////////

  void sendEvent(TriggerEvent event) {
    if (lm != null)
      lm.sendEvent(event);
  }

  @Override
  public void addEventListener(TriggerListener l) {
    if (lm == null) createListenerManager();
    lm.addListener(l);
  }

  @Override
  public void removeEventListener(TriggerListener l) {
    if (lm != null)
      lm.removeListener(l);
  }

  private void createListenerManager() {
    lm = new ListenerManager(
            "thredds.inventory.CollectionManager$TriggerListener",
            "thredds.inventory.CollectionManager$TriggerEvent",
            "handleCollectionEvent");
  }

}
