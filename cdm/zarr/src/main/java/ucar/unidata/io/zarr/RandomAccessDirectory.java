package ucar.unidata.io.zarr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.*;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.nio.channels.WritableByteChannel;
import java.util.*;

/**
 * This class allows a directory structure to be read in memory as a single file.
 * RandomAccessDirectory implemented a tree structure with files as leaves.
 * It is read-only - writes should use the leaf RandomAccessFile write methods
 */
public class RandomAccessDirectory extends ucar.unidata.io.RandomAccessFile implements FileCacheable, Closeable {

  private static final Logger logger = LoggerFactory.getLogger(RandomAccessDirectory.class);

  protected MController controller;

  protected List<RandomAccessDirectoryItem> children;

  protected int bufferSize; // since no buffer is initialized for directories, this tracks the buffer size of files

  private static final String WRITES_NOT_IMPLEMENTED_MESSAGE =
      "Method not implemented: writes are not implemented in RandomAccessDirectory";

  private static RandomAccessFile currentFile = null;

  public RandomAccessDirectory(String location) throws IOException {
    this(location, RandomAccessFile.defaultBufferSize, new ArrayList<>());
  }

  public RandomAccessDirectory(String location, int bufferSize) throws IOException {
    this(location, bufferSize, null);
  }

  public RandomAccessDirectory(String location, int bufferSize, List<RandomAccessDirectoryItem> children) throws IOException {
    super(bufferSize);
    this.bufferSize = bufferSize;
    this.location = location;
    this.readonly = true; // RandomAccessDirectory does not support writes
    this.controller = MControllers.create(location);

    // build list of immediate children
    this.children = children == null ? buildVirtualSubDirs(location) : children;
  }

  private static List<RandomAccessDirectoryItem> buildVirtualSubDirs(String location) throws IOException {
    MController controller = MControllers.create(location);
    CollectionConfig cc = new CollectionConfig("children", location, true, null, null);

    List<RandomAccessDirectoryItem> items = new ArrayList<>();

    // add subdirs
    Iterator<MFile> subdirs = sortIterator(controller.getSubdirs(cc, false));
    if (subdirs != null) {
      subdirs.forEachRemaining(mfile -> {
        try {
          String childPath = mfile.getPath();
          items.add(new VirtualRandomAccessFile(childPath, buildVirtualSubDirs(childPath)));
        } catch (IOException ioe) {
          logger.error(ioe.getMessage());
        }
      });
    }
    // add files
    Iterator<MFile> files = sortIterator(controller.getInventoryTop(cc, false));
    if (files != null) {
      files.forEachRemaining(mfile -> {
        items.add(new VirtualRandomAccessFile(mfile.getPath(), mfile.getLength(), mfile.getLastModified()));
      });
    }

    return items;
  }

  private static Iterator<MFile> sortIterator(Iterator<MFile> mfiles) {
    List list = new ArrayList();
    while (mfiles.hasNext()) {
      list.add(mfiles.next());
    }
    Collections.sort(list);
    return list.iterator();
  }

  // returns a RandomAccessFile for the child file or directory containing position pos
  protected RandomAccessFile getFileAtPos(long pos) throws IOException {
    // TODO - speed up performance
    long tempPos = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      long rafLength = item.length();
      if (tempPos + rafLength > pos) {
        RandomAccessFile raf = item.getRaf();
        if (raf != null) {
          return raf;
        }
        if (item.isDirectory()) {
          raf = new RandomAccessDirectory(item.getLocation(), this.bufferSize, item.getChildren());
        } else {
          raf = NetcdfFiles.getRaf(item.getLocation(), this.bufferSize);
        }
        item.setRaf(raf);
        return raf;
      }
      tempPos += rafLength;
    }
    return null;
  }

  // returns the position, relative to the directory structure, of the first byte of a file
  protected long getFileStartPos(RandomAccessFile child) {
    // TODO - speed up performance
    long startPos = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      RandomAccessFile raf = item.getRaf();
      if (raf != null && raf.equals(child)) {
        break;
      }
      startPos += item.length();
    }
    return startPos;
  }

  /**
   * Set the bufferSize of the directory and its children
   * 
   * @param bufferSize length in bytes
   */
  @Override
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    this.children.forEach(item -> {
      RandomAccessFile raf = item.getRaf();
      if (raf != null) {
        raf.setBufferSize(bufferSize);
      }
    });
  }

  @Override
  public int getBufferSize() {
    return this.bufferSize;
  }

  @Override
  public synchronized void close() throws IOException {
    for (RandomAccessDirectoryItem item : this.children) {
      RandomAccessFile raf = item.getRaf();
      if (raf != null) {
        raf.close();
      }
    }
  }

  @Override
  public long getLastModified() {
    return children.stream().mapToLong(RandomAccessDirectoryItem::getLastModified).max().orElse(-1);
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public long length() {
    return children.stream().mapToLong(RandomAccessDirectoryItem::length).sum();
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    long n = 0;
    while (n < nbytes) {

      RandomAccessFile raf = getFileAtPos(offset + n);
      if (raf == null) {
        break;
      }
      long count = raf.readToByteChannel(dest, offset + n - getFileStartPos(raf), nbytes - n);
      n += count;
    }
    return n;
  }

  @Override
  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
    int n = 0;
    while (n < len) {
      RandomAccessFile raf = getFileAtPos(pos);
      if (raf == null) {
        break;
      }
      long startPos = getFileStartPos(raf);
      raf.seek(pos - startPos);
      int count = raf.read(b, offset + n, len - n);
      if (count < 0) {
        break;
      }
      n += count;
      pos += count;
    }
    return n;
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
   * 
   * @param b write this byte
   */
  @Override
  public void write(int b) {
    logger.error(WRITES_NOT_IMPLEMENTED_MESSAGE);
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
   * 
   * @param b the array containing the data.
   * @param off the offset in the array to the data.
   * @param len the length of the data.
   */
  @Override
  public void writeBytes(byte[] b, int off, int len) {
    logger.error(WRITES_NOT_IMPLEMENTED_MESSAGE);
  }

  /**
   * Hook into service provider interface to RandomAccessFileProvider. Register in
   * META-INF.services.ucar.unidata.io.spi.RandomAccessFileProvider
   */
  public static class Provider implements RandomAccessFileProvider {

    @Override
    public boolean isOwnerOf(String location) {
      try {
        return MFiles.create(location).isDirectory();
      } catch (Exception e) {
        return false;
      }
    }

    /**
     * Open a location that this Provider is the owner of.
     */
    @Override
    public RandomAccessFile open(String location) throws IOException {
      return new RandomAccessDirectory(location);
    }

    @Override
    public RandomAccessFile open(String location, int bufferSize) throws IOException {
      return new RandomAccessDirectory(location, bufferSize);
    }
  }
}
