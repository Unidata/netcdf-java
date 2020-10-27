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

  public RandomAccessDirectory(String location) throws IOException {
    this(location, ucar.unidata.io.RandomAccessFile.defaultBufferSize);
  }

  public RandomAccessDirectory(String location, int bufferSize) throws IOException {
    super(bufferSize);
    this.bufferSize = bufferSize;
    this.location = location;
    this.readonly = true; // RandomAccessDirectory does not support writes
    this.controller = MControllers.create(location);

    // build list of immediate children
    this.children = new ArrayList<>();
    CollectionConfig cc = new CollectionConfig("children", location, true, null, null);
    // add subdirs
    Iterator<MFile> subdirs = this.controller.getSubdirs(cc, false);
    if (subdirs != null) {
      subdirs.forEachRemaining(mfile -> {
        this.children.add(new VirtualRandomAccessFile(mfile.getPath(), mfile.getLength(), mfile.getLastModified()));
//        if (mfile.isDirectory()) {
//          try {
//            this.children.add(new RandomAccessDirectory(mfile.getPath(), this.bufferSize));
//          } catch (IOException ioe) {
//            logger.error("Could not create RandomAccessDirectory at " + mfile.getPath());
//          }
//        }
//        ucar.unidata.io.RandomAccessFile raf = null;
//        try {
//          raf = NetcdfFiles.getRaf(mfile.getPath(), bufferSize);
//        } catch (IOException ioe) {
//          logger.error("Could not create RandomAccessFile for " + mfile.getPath());
//        }
//        if (raf == null) {
//          logger.warn("Could not create RandomAccessFile for " + mfile.getPath());
//        } else {
//          this.children.add(raf);
//        }
      });
    }
    // add files
    Iterator<MFile> files = this.controller.getInventoryTop(cc, false);
    if (files != null) {
      files.forEachRemaining(mfile -> {
        this.children.add(new VirtualRandomAccessFile(mfile.getPath(), mfile.getLength(), mfile.getLastModified()));
//        ucar.unidata.io.RandomAccessFile raf = null;
//        try {
//          raf = NetcdfFiles.getRaf(mfile.getPath(), bufferSize);
//        } catch (IOException ioe) {
//          logger.error("Could not create RandomAccessFile for " + mfile.getPath());
//        }
//        if (raf == null) {
//          logger.warn("Could not create RandomAccessFile for " + mfile.getPath());
//        } else {
//          this.children.add(raf);
//        }
      });
    }
  }

//  public RandomAccessFile getRaf() { return this; }
//
//  public void setRaf(RandomAccessFile raf) {}; // noop

  // returns a RandomAccessFile for the child file or directory containing position pos
  protected RandomAccessFile getFileAtPos(long pos) throws IOException {
    long tempPos = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      long rafLength = item.length();
      if (tempPos + rafLength > pos) {
        RandomAccessFile raf = item.getRaf();
        if (raf != null) { return raf; }
        raf = NetcdfFiles.getRaf(item.getLocation(), this.bufferSize);
        item.setRaf(raf);
        return raf;
      }
      tempPos += rafLength;
    }
    return null;
  }

  // returns the position, relative to the directory structure, of the first byte of a file
  protected long getFileStartPos(RandomAccessFile child) throws IOException {
    long startPos = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      RandomAccessFile raf = item.getRaf();
      if (raf != null && raf.equals(child)) { break; }
      startPos += item.length();
    }
    return startPos;
  }

  /**
   * List all immediate subdirectories and files in the directory
   * @return a list of child RandomAccessFiles
   */
  public List<RandomAccessDirectoryItem> getChildren() {
    return this.children;
  }

  /**
   * Set the bufferSize of the directory and its children
   * @param bufferSize length in bytes
   */
  @Override
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    this.children.forEach(item -> {
      RandomAccessFile raf = item.getRaf();
      if (raf != null) { raf.setBufferSize(bufferSize); }
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
      if (raf != null) { raf.close(); }
    }
  }

  @Override
  public long getLastModified() {
    long lastModified = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      File f = new File(item.getLocation());
      if (f.lastModified() > lastModified) {
        lastModified = f.lastModified();
      }
    }
    return lastModified;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public long length() throws IOException {
    return getFileStartPos(null);
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
      n += count;
      pos += count;
    }
    return n;
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
   * @param b write this byte
   */
  @Override
  public void write(int b) {
    logger.error(WRITES_NOT_IMPLEMENTED_MESSAGE);
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
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
