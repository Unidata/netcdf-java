/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.partition;

import java.util.Collections;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A PartitionManager from a list of index files.
 * Used in GribCollectionBuilder with multiple runtimes.
 * Index Files are already in the cache.
 *
 * @author John
 * @since 2/5/14                   `
 */
public class PartitionManagerFromIndexList extends CollectionAbstract implements PartitionManager {
  private List<MFile> partIndexFiles;
  private final FeatureCollectionConfig config;

  public PartitionManagerFromIndexList(MCollection dcm, List<MFile> partFiles, org.slf4j.Logger logger) {
    super(dcm.getCollectionName(), logger);
    this.config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);

    this.root = dcm.getRoot();
    this.partIndexFiles = partFiles;
  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) {
    return new PartIterator();
  }

  private class PartIterator implements Iterator<MCollection>, Iterable<MCollection> {
    Iterator<MFile> iter = partIndexFiles.iterator();

    @Override
    public Iterator<MCollection> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public MCollection next() {
      MFile nextFile = iter.next();

      MCollection result = new CollectionSingleIndexFile( nextFile, logger);
      result.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      return result;
    }

    @Override
    public void remove() {
    }
  }

  @Override
  public void close() { }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return Collections.emptyList() ;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() {
    return new CloseableIterator() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Object next() {
        return null;
      }

      @Override
      public void close() {

      }
    };
  }

  /////////////////////////////////////////////////////////////
  // partitions can be removed (!)

  public void removePartition( MCollection partition) {
    for (MFile mfile : partIndexFiles) {
      if (mfile.getName().equalsIgnoreCase(partition.getCollectionName())) {
        List<MFile> part = new ArrayList<>(partIndexFiles);
        part.remove(mfile);
        partIndexFiles = part;
        return;
      }
    }
  }
}
