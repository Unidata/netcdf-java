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
 * A collection defined by the collection spec (not directory sensitive)
 * May have by regexp: or glob: (experimental)
 * 
 * @author caron
 * @since 12/23/2014
 */
public class CollectionPathMatcher extends CollectionAbstract {
  protected final FeatureCollectionConfig config;
  private final boolean wantSubdirs;
  private final long olderThanMillis;
  private final MFileFilter matcher;

  public CollectionPathMatcher(FeatureCollectionConfig config, CollectionSpecParserAbstract specp, Logger logger) {
    super(config.collectionName, logger);
    this.config = config;
    this.wantSubdirs = specp.wantSubdirs();
    setRoot(specp.getRootDir());
    DateExtractor extract = config.getDateExtractor();
    if (extract != null && !(extract instanceof DateExtractorNone))
      setDateExtractor(extract);

    putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    matcher = specp.getMFileFilter(); // LOOK still need to decide what you are matching on name, path, etc

    this.olderThanMillis = parseOlderThanString(config.olderThan);
  }

  @Override
  public void close() {}

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new AllFilesIterator();
  }

  // could also use Files.walkFileTree
  // returns everything defined by specp, checking olderThanMillis, descends into subdirs as needed
  private class AllFilesIterator implements CloseableIterator<MFile> {
    Queue<OneDirIterator> subdirs = new LinkedList<>();
    OneDirIterator current;

    AllFilesIterator() throws IOException {
      current = new OneDirIterator(root, subdirs);
    }

    public boolean hasNext() {
      if (!current.hasNext()) {
        try {
          current.close();
        } catch (IOException e) {
          logger.error("Error closing dirStream", e);
        }
        current = subdirs.poll();
        return current != null && hasNext();
      }
      return true;
    }

    public MFile next() {
      if (current == null)
        throw new NoSuchElementException();
      return current.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
      if (current != null)
        current.close();
      current = null;
    }
  }

  private class OneDirIterator implements CloseableIterator<MFile> {
    Queue<OneDirIterator> subdirs;
    DirectoryStream<MFile> dirStream;
    Iterator<MFile> dirStreamIterator;
    MFile nextMFile;
    long now;

    OneDirIterator(String dir, Queue<OneDirIterator> subdirs) throws IOException {
      this.subdirs = subdirs;
      dirStream = MControllers.newDirectoryStream(dir);
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

          if (wantSubdirs && nextFile.isDirectory()) { // dont filter subdirectories
            subdirs.add(new OneDirIterator(nextFile.getPath(), subdirs));
            continue;
          }

          if (!matcher.accept(nextFile)) // otherwise apply the filter specified by the specp
            continue;

          if (olderThanMillis > 0) {
            long last = nextFile.getLastModified();
            long millisSinceModified = now - last;
            if (millisSinceModified < olderThanMillis)
              continue;
          }
          nextMFile = nextFile;
          return true;

        } catch (IOException e) {
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

    public void close() throws IOException {
      dirStream.close();
    }
  }
}
