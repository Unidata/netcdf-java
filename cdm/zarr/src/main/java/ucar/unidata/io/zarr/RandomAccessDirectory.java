/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.zarr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.*;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.cache.FileCacheable;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;

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

  protected List<RandomAccessDirectoryItem> children; // all files within the store

  private RandomAccessFile currentFile; // file currently containing the file pointer

  private long currentFileStartPos = -1; // start position of current file, relative to the directory/store

  protected int bufferSize;

  private static final String WRITES_NOT_IMPLEMENTED_MESSAGE =
      "Method not implemented: writes are not implemented in RandomAccessDirectory";

  public RandomAccessDirectory(String location) throws IOException {
    this(location, RandomAccessFile.defaultBufferSize);
  }

  public RandomAccessDirectory(String location, int bufferSize) throws IOException {
    super(bufferSize);
    this.bufferSize = bufferSize;
    this.location = location.replace("\\", "/"); // standardize path
    this.readonly = true; // RandomAccessDirectory does not support writes

    // build children list
    this.children = new ArrayList<>();
    MController controller = MControllers.create(location);
    CollectionConfig cc = new CollectionConfig("children", location, false, null, null);
    Iterator<MFile> files = sortIterator(controller.getInventoryAll(cc, false)); // standardize order
    if (files != null) {
      files.forEachRemaining(mfile -> {
        this.children.add(new VirtualRandomAccessFile(mfile.getPath().replace("\\", "/"), mfile.getLength(),
            mfile.getLastModified()));
      });
    }
  }

  /**
   * Sorts items in an iterator
   * 
   * @param mfiles
   * @return sorted iterator
   */
  private static Iterator<MFile> sortIterator(Iterator<MFile> mfiles) {
    List list = new ArrayList();
    while (mfiles.hasNext()) {
      list.add(mfiles.next());
    }
    Collections.sort(list);
    return list.iterator();
  }

  /**
   * @return RandomAccessFile containing the current file pointer
   */
  public RandomAccessFile getCurrentFile() {
    return this.currentFile;
  }

  /**
   * Find all files in the store that fall under the given path
   * 
   * @param path
   * @return list of files in path
   * @throws IOException
   */
  public List<RandomAccessFile> getFilesByName(String path) throws IOException {
    List<RandomAccessFile> files = new ArrayList<>();
    for (RandomAccessDirectoryItem item : this.children) {
      String location = item.getLocation();
      if (location.contains(path)) {
        RandomAccessFile raf = item.getRaf();
        files.add(raf == null ? NetcdfFiles.getRaf(location, this.bufferSize) : raf);
      }
    }
    return files;
  }

  /**
   * sets current RandomAccessFile to that containing pos
   * saves start position on current RAF
   * 
   * @param pos
   * @throws IOException
   */
  protected void setFileToPos(long pos) throws IOException {
    long tempPos = 0;
    for (RandomAccessDirectoryItem item : this.children) {
      long rafLength = item.length();
      if (tempPos + rafLength > pos) {
        RandomAccessFile raf = item.getRaf();
        if (raf == null) {
          raf = NetcdfFiles.getRaf(item.getLocation(), this.bufferSize);
          item.setRaf(raf);
        }
        this.currentFile = raf;
        this.currentFileStartPos = tempPos;
        return;
      }
      tempPos += rafLength;
    }
    // pos past EOF
    this.currentFile = null;
    this.currentFileStartPos = -1;
  }

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
      // find new file if offset is outside current file
      if (this.currentFile == null || offset + n < this.currentFileStartPos
          || offset + n >= this.currentFileStartPos + this.currentFile.length()) {
        setFileToPos(offset + n);
        if (this.currentFile == null) {
          break;
        }
      }

      // read from current file
      long count = this.currentFile.readToByteChannel(dest, offset + n - this.currentFileStartPos, nbytes - n);
      n += count;
    }
    return n;
  }

  @Override
  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
    int n = 0;
    while (n < len) {
      // find new file if offset is outside current file
      if (this.currentFile == null || pos < this.currentFileStartPos
          || pos >= this.currentFileStartPos + this.currentFile.length()) {
        setFileToPos(pos);
        if (this.currentFile == null) {
          break;
        }
      }

      // rad from current file
      this.currentFile.seek(pos - this.currentFileStartPos);
      int count = this.currentFile.read(b, offset + n, len - n);
      if (count < 0) {
        break;
      }

      // update position
      n += count;
      pos += count;
    }
    return n;
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
   * e.g. getCurrentFile().write()
   */
  @Override
  public void write(int b) {
    // RandomAccessDirectory.write would not know whether to append or prepend on writes at the end of a file
    // Look - possible to create a new write signature that includes a append/prepend parameter
    logger.error(WRITES_NOT_IMPLEMENTED_MESSAGE);
  }

  /**
   * Not implemented - use write methods on the leaf RandomAccessFile
   * e.g. getCurrentFile().write()
   */
  @Override
  public void writeBytes(byte[] b, int off, int len) {
    // RandomAccessDirectory.write would not know whether to append or prepend on writes at the end of a file
    // Look - possible to create a new write signature that includes a append/prepend parameter
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
