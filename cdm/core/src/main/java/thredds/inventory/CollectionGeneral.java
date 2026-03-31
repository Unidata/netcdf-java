/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.util.CloseableIterator;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.*;

/**
 * All files are read in at once.
 * timePartition=none
 *
 * @author caron
 * @since 2/7/14
 */
public class CollectionGeneral extends CollectionAbstract {
  private final long olderThanMillis;

  public CollectionGeneral(FeatureCollectionConfig config, CollectionSpecParser specp, Logger logger) {
    super(config.collectionName, logger);
    this.root = specp.getRootDir();
    this.olderThanMillis = parseOlderThanString(config.olderThan);
    this.sfilter = specp.getMFileFilter();
  }

  @Override
  public void close() {}

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(root);
  }

  // returns everything defined by specp, checking olderThanMillis
  private class MyFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<MFile> dirStream;
    Iterator<MFile> dirStreamIterator;
    MFile nextMFile;
    long now;

    MyFileIterator(String dir) throws IOException {
      MController controller = MControllers.create(dir);
      CollectionConfig config = new CollectionConfig(collectionName, dir, false, (MFileFilter) sfilter, null);
      dirStream = controller.getInventoryTop(config, true);
      dirStreamIterator = dirStream.iterator();
      now = System.currentTimeMillis();
    }

    public boolean hasNext() {

      while (true) {
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        try {
          MFile nextFile = dirStreamIterator.next();
          if (nextFile.isDirectory())
            continue; // LOOK fix this

          long last = nextFile.getLastModified();
          long millisSinceModified = now - last;
          if (millisSinceModified < olderThanMillis)
            continue;
          nextMFile = nextFile;
          return true;

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }


    public MFile next() {
      if (nextMFile == null)
        throw new NoSuchElementException();
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      dirStream.close();
    }
  }
}
