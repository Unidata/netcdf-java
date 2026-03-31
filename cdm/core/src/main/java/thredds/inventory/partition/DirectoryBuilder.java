/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MControllers;
import thredds.inventory.MFile;
import thredds.inventory.MFiles;
import ucar.nc2.util.Indent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A Builder of DirectoryPartitions and DirectoryCollections.
 * Each DirectoryBuilder is associated with one directory and one ncx index.
 * This may contain collections of files (MFiles in a DirectoryCollection), or subdirectories (MCollections in a
 * DirectoryPartition).
 *
 * @author caron
 * @since 11/10/13
 */
public class DirectoryBuilder {

  // returns a DirectoryPartition or DirectoryCollection
  public static MCollection factory(FeatureCollectionConfig config, String topDir, boolean isTop,
      IndexReader indexReader, String suffix, org.slf4j.Logger logger) throws IOException {
    DirectoryBuilder builder = new DirectoryBuilder(config.collectionName, topDir, suffix);

    DirectoryPartition dpart = new DirectoryPartition(config, topDir, isTop, indexReader, suffix, logger);
    if (!builder.isLeaf(indexReader)) { // its a partition
      return dpart;
    }

    // its a collection
    boolean hasIndex = builder.findIndex();
    if (hasIndex) {
      return dpart.makeChildCollection(builder);
    } else {
      DirectoryCollection result =
          new DirectoryCollection(config.collectionName, topDir, isTop, config.olderThan, logger); // no index file
      return result;
    }
  }

  private enum PartitionStatus {
    unknown, isDirectoryPartition, isLeaf
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private static final boolean debug = false;
  private final String suffix;
  private final String topCollectionName; // collection name
  private final String partitionName; // partition name
  private final String dir; // the directory
  private final long dirLastModified; // directory last modified
  private String index; // TimePartition index file (ncx2 with magic = TimePartition)
  private long indexLastModified; // index last modified
  private long indexSize; // index size

  private boolean childrenConstructed;
  private List<DirectoryBuilder> children = new ArrayList<>(25);
  private PartitionStatus partitionStatus = PartitionStatus.unknown;

  public DirectoryBuilder(String topCollectionName, String dirFilename, String suffix) throws IOException {
    this(topCollectionName, MControllers.create(dirFilename).getMFile(dirFilename), suffix);
  }

  /**
   * Create a DirectoryBuilder for the named directory
   * 
   * @param topCollectionName from config, name of the collection
   * @param mdir covers this directory
   */
  public DirectoryBuilder(String topCollectionName, MFile mdir, String suffix) throws IOException {
    this.topCollectionName = topCollectionName;
    this.dir = mdir.getPath();
    this.partitionName = DirectoryCollection.makeCollectionName(topCollectionName, dir);
    this.suffix = suffix;

    if (!mdir.isDirectory())
      throw new IllegalArgumentException("DirectoryPartitionBuilder needs a directory");
    dirLastModified = mdir.getLastModified();

    // see if we can find the index
    findIndex();
  }

  // public void setChildrenConstructed(boolean childrenConstructed) { this.childrenConstructed = childrenConstructed; }

  /**
   * Find the index file, using its canonical name
   * 
   * @return true if found
   */
  public boolean findIndex() throws IOException {
    String indexPath = dir + "/" + partitionName + suffix;
    MFile indexFile = MFiles.createIfExists(indexPath);
    if (indexFile != null) {
      this.index = indexPath;
      this.indexLastModified = indexFile.getLastModified();
      this.indexSize = indexFile.getLength();
      return true;
    }
    return false;
  }

  /**
   * Scans first 100 files to decide if its a leaf. If so, it becomes a DirectoryCollection, else a PartitionCollection.
   * 
   * @param indexReader reads the index
   * @return true if partition, false if file collection
   */
  private boolean isLeaf(IndexReader indexReader) throws IOException {
    if (partitionStatus == PartitionStatus.unknown) {

      int countDir = 0, countFile = 0, count = 0;
      try (DirectoryStream<MFile> dirStream = MControllers.newDirectoryStream(dir)) {
        for (MFile mfile : dirStream) {
          if (count++ >= 100)
            break;
          if (mfile.isDirectory())
            countDir++;
          else
            countFile++;
        }
      }
      partitionStatus = (countFile > countDir) ? PartitionStatus.isLeaf : PartitionStatus.isDirectoryPartition;
    }

    return partitionStatus == PartitionStatus.isLeaf;
  }

  /**
   * Find all children directories. Does not recurse.
   * We separate this from the constructor so it can be done on demand
   * Public for debugging.
   *
   * Look for children by:
   * <ol>
   * <li>If index exists , use the children inside there./li>
   * <li>(or) scan the directory for children partitions</li>
   * </ol>
   *
   * @param indexReader this reads the index, and calls AddChild.addchild() for each child
   * @return children, may be empty but not null
   */
  public List<DirectoryBuilder> constructChildren(IndexReader indexReader, CollectionUpdateType forceCollection)
      throws IOException {
    if (childrenConstructed)
      return children;

    if (index != null && forceCollection == CollectionUpdateType.nocheck) { // use index if it exists
      constructChildrenFromIndex(indexReader, false);

    } else {
      scanForChildren();
    }

    // once we have found children, we know that this is a time partition
    partitionStatus = (!children.isEmpty()) ? PartitionStatus.isDirectoryPartition : PartitionStatus.isLeaf;
    childrenConstructed = true; // otherwise we are good

    return children;
  }

  public List<DirectoryBuilder> constructChildrenFromIndex(IndexReader indexReader, boolean substituteParentDir)
      throws IOException {
    if (!indexReader.readChildren(index, new AddChildSub(substituteParentDir))) {
      partitionStatus = PartitionStatus.isLeaf;
    }
    return children;
  }

  private class AddChild implements IndexReader.AddChildCallback {
    public void addChild(String dirName, String indexFilename, long lastModified) throws IOException {
      String indexPath = dirName + "/" + indexFilename;
      DirectoryBuilder child = new DirectoryBuilder(topCollectionName, indexPath, lastModified, suffix);
      children.add(child);
    }
  }

  private class AddChildSub implements IndexReader.AddChildCallback {
    boolean substituteParentDir;

    AddChildSub(boolean substituteParentDir) {
      this.substituteParentDir = substituteParentDir;
    }

    public void addChild(String dirName, String indexFilename, long lastModified) throws IOException {
      String indexPath = dirName + "/" + indexFilename;
      if (substituteParentDir) {
        int pos = index.lastIndexOf('/');
        if (pos < 0)
          pos = index.lastIndexOf('\\');
        String parent = (pos >= 0) ? index.substring(0, pos) : ".";
        indexPath = parent + "/" + indexFilename;
      }
      DirectoryBuilder child = new DirectoryBuilder(topCollectionName, indexPath, lastModified, suffix);
      children.add(child);
    }
  }

  // coming in from the index reader
  private DirectoryBuilder(String topCollectionName, String indexFile, long lastModified, String suffix)
      throws IOException {
    this.topCollectionName = topCollectionName;
    MFile mIndexFile = MFiles.createIfExists(indexFile);
    if (mIndexFile != null) {
      this.index = indexFile;
      this.indexLastModified = mIndexFile.getLastModified();
    }

    int pos = indexFile.lastIndexOf('/');
    if (pos < 0)
      pos = indexFile.lastIndexOf('\\');
    this.dir = (pos >= 0) ? indexFile.substring(0, pos) : ".";
    this.partitionName = DirectoryCollection.makeCollectionName(topCollectionName, dir);

    MFile mdir = MFiles.create(this.dir);
    if (!mdir.isDirectory())
      throw new IllegalArgumentException("DirectoryPartition needs a directory");
    dirLastModified = mdir.getLastModified();

    this.suffix = suffix;
  }

  /**
   * Scan for subdirectories, make each into a DirectoryBuilder and add as a child
   */
  private void scanForChildren() {
    if (debug)
      System.out.printf("DirectoryBuilder.scanForChildren on %s ", dir);

    int count = 0;
    try (DirectoryStream<MFile> ds = MControllers.newSubdirStream(dir)) {
      for (MFile mfile : ds) {
        children.add(new DirectoryBuilder(topCollectionName, mfile, suffix));
        if (debug && (++count % 10 == 0))
          System.out.printf("%d ", count);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (debug)
      System.out.printf("done=%d%n", count);
    childrenConstructed = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // read the list of files from the index

  public List<MFile> readFilesFromIndex(IndexReader indexReader) throws IOException {
    List<MFile> result = new ArrayList<>(100);
    if (index == null)
      return result;

    indexReader.readMFiles(index, result);
    return result;
  }

  ////////////////////////////////////////////////////////

  /**
   * The directory that the partition covers
   * 
   * @return directory
   */
  public String getDir() {
    return dir;
  }

  /**
   * The ncx2 file
   * 
   * @return ncx2 file path
   */
  public String getIndex() {
    return index;
  }

  /**
   * May be null if constructChildren() was not called
   * 
   * @return children directories
   */
  public List<DirectoryBuilder> getChildren() {
    return children;
  }

  public String getPartitionName() {
    return partitionName;
  }

  public void show(Formatter out) {
    out.format("Collection %s%n", partitionName);
    toString(out, new Indent(2));
    out.format("%n%n");
  }

  private void toString(Formatter out, Indent indent) {
    out.format("%sDir '%s' (%s) index '%s' (%s)%n", indent, dir, dirLastModified, index, indexLastModified);
    indent.incr();
    for (DirectoryBuilder c : children)
      c.toString(out, indent);
    indent.decr();
  }

}
