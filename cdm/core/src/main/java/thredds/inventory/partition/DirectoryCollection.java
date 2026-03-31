/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.partition;

import thredds.inventory.CollectionAbstract;
import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MControllers;
import thredds.inventory.MFile;
import thredds.inventory.MFileFilter;
import ucar.nc2.util.CloseableIterator;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.*;

/**
 * Manage MFiles from one directory.
 * Doesnt know about parents or children.
 * Use getFileIterator() for best performance on large directories
 *
 * @author caron
 * @since 11/16/13
 */
public class DirectoryCollection extends CollectionAbstract {

  /**
   * Create standard name = topCollectionName + last directory
   * 
   * @param topCollectionName from config, name of the collection
   * @param dir directory for this
   * @return standard collection name, to name the index file
   */
  public static String makeCollectionName(String topCollectionName, String dir) {
    int pos = dir.lastIndexOf('/');
    if (pos < 0)
      pos = dir.lastIndexOf('\\');
    String lastDirName = (pos >= 0) ? dir.substring(pos + 1) : dir;
    return topCollectionName + "-" + lastDirName;
  }

  /**
   * Create standard name = topCollectionName + last directory
   * 
   * @param topCollectionName from config, name of the collection
   * @param dir directory for this
   * @return standard collection name, to name the index file
   */
  public static String makeCollectionIndexPath(String topCollectionName, String dir, String suffix) {
    String collectionName = makeCollectionName(topCollectionName, dir);
    return dir + "/" + collectionName + suffix;
  }

  ///////////////////////////////////////////////////////////////////////////////////

  final String topCollection;
  final String collectionDir; // directory for this collection
  final long olderThanMillis;
  final boolean isTop;

  public DirectoryCollection(String topCollectionName, String topDirS, boolean isTop, String olderThan,
      org.slf4j.Logger logger) {
    super(null, logger);
    this.topCollection = cleanName(topCollectionName);
    this.collectionDir = topDirS;
    this.collectionName = isTop ? this.topCollection : makeCollectionName(topCollection, collectionDir);
    this.isTop = isTop;

    this.olderThanMillis = parseOlderThanString(olderThan);
    if (debug)
      System.out.printf("Open DirectoryCollection %s%n", collectionName);
  }

  @Override
  public String getRoot() {
    return collectionDir;
  }

  @Override
  public String getIndexFilename(String suffix) {
    if (isTop)
      return super.getIndexFilename(suffix);
    return DirectoryCollection.makeCollectionIndexPath(topCollection, collectionDir, suffix);
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(collectionDir);
  }

  @Override
  public void close() {
    if (debug)
      System.out.printf("Close DirectoryCollection %s%n", collectionName);
  }

  // returns everything in the current directory, subject to sfilter
  private class MyFileIterator implements CloseableIterator<MFile> {
    int debugNum;
    DirectoryStream<MFile> dirStream;
    Iterator<MFile> dirStreamIterator;
    MFile nextMFile;
    int count;

    MyFileIterator(String dir) {
      if (debug) {
        debugNum = debugCount++;
        System.out.printf(" MyFileIterator %s (%d)", dir, debugNum);
      }
      try (MController controller = MControllers.create(dir)) {
        CollectionConfig config = new CollectionConfig(collectionName, dir, false, (MFileFilter) sfilter, null);
        dirStream = controller.getInventoryTop(config, true);
        dirStreamIterator = dirStream.iterator();
      }
    }

    public boolean hasNext() {
      while (true) {
        // if (debug && count % 100 == 0) System.out.printf("%d ", count);
        count++;
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        long now = System.currentTimeMillis();
        try {
          MFile nextFile = dirStreamIterator.next();
          if (nextFile.isDirectory())
            continue;
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
      if (debug)
        System.out.printf(" closed %d (%d)%n", count, debugNum);
      dirStream.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private static final boolean debug = false;
  private static int debugCount;

  // this idiom keeps the iterator from escaping so that we can use try-with-resource, and ensure DirectoryStream
  // closes. like++
  public void iterateOverMFileCollection(Visitor visit) {
    if (debug)
      System.out.printf(" iterateOverMFileCollection %s ", collectionDir);
    int count = 0;
    MController controller = MControllers.create(collectionDir);
    CollectionConfig config = new CollectionConfig(collectionName, collectionDir, false, (MFileFilter) sfilter, null);
    DirectoryStream<MFile> ds = controller.getInventoryTop(config, true);
    controller.close();
    if (ds != null) {
      for (MFile mfile : ds) {
        if (!mfile.isDirectory())
          visit.consume(mfile);
        if (debug)
          System.out.printf("%d ", count++);
      }
    }
    if (debug)
      System.out.printf("%d%n", count);
  }

  public interface Visitor {
    void consume(MFile mfile);
  }

}
