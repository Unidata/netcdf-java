/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.zarr;

import java.io.InputStream;
import java.io.OutputStream;
import thredds.filesystem.MFileOS;
import thredds.inventory.MFile;
import thredds.inventory.MFileProvider;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import ucar.nc2.util.IO;

/**
 * Implements thredds.inventory.MFile for ZipFiles and ZipEntries
 */
public class MFileZip implements MFile {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MFileZip.class);

  private final ZipFile root; // ZipFile container
  private final Path rootPath; // Path to the main Zip
  private final Path relativePath; // Path from main Zip to current ZipEntry
  private Object auxInfo;

  private final List<ZipEntry> leafEntries; // all entries contained in current path
  private ZipEntry entry; // current entry or null if not a leaf

  public MFileZip(ZipFile file) throws IOException {
    if (file == null) {
      throw new IOException("Could not create MFile: ZipFile is null");
    }
    this.root = file;
    this.rootPath = Paths.get(file.getName());
    this.relativePath = Paths.get(File.separator);
    this.leafEntries = getEntries();
  }

  public MFileZip(String filename) throws IOException {
    // Split filename into zipfile path and internal path
    int split = filename.indexOf(Provider.ext);
    if (split < 0) {
      throw new IOException(filename + " is not a zip file");
    }
    String location = filename.substring(0, split + Provider.ext.length());
    filename = filename.substring(split + Provider.ext.length());

    // create root zipfile object
    this.root = new ZipFile(location);
    this.rootPath = Paths.get(location);
    // set relative path
    this.relativePath = filename.isEmpty() ? Paths.get(File.separator) : Paths.get(filename);
    this.leafEntries = this.getEntries();
  }

  /**
   *
   * @return all ZipEntries that fall under this internal path
   */
  private List<ZipEntry> getEntries() {
    List<ZipEntry> entries = new ArrayList<>();
    try {
      ZipInputStream zipIn = new ZipInputStream(new FileInputStream(root.getName()));
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        // skip entries outside or equal to our current path
        Path entryPath = Paths.get(File.separator + entry.getName());
        if (!entryPath.startsWith(relativePath) || entryPath.equals(relativePath)) {
          // if equal, save as this.entry
          if (entryPath.equals(relativePath)) {
            this.entry = entry;
          }
          zipIn.closeEntry();
          entry = zipIn.getNextEntry();
          continue;
        }
        if (!entry.isDirectory()) {
          entries.add(entry);
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
      zipIn.close();
    } catch (IOException ioe) {
      logger.error(ioe.getMessage(), ioe);
    }
    return entries;
  }

  @Override
  public long getLastModified() {
    return this.entry == null ? 0 : this.entry.getLastModifiedTime().toMillis();
  }

  @Override
  public long getLength() {
    return this.entry == null ? 0 : this.entry.getSize();
  }

  @Override
  public boolean isDirectory() {
    return leafEntries.size() > 0;
  }

  @Override
  public boolean isReadable() {
    // readable if root is readable
    return Files.isReadable(Paths.get(root.getName()));
  }

  @Override
  public String getPath() {
    return rootPath.toString() + relativePath.toString();
  }

  @Override
  public String getName() {
    return relativePath.toString();
  }

  @Override
  public MFile getParent() {
    return MFileOS.getExistingFile(Paths.get(root.getName()).getParent().toString());
  }

  @Override
  public int compareTo(MFile o) {
    return getPath().compareTo(o.getPath());
  }

  @Override
  public Object getAuxInfo() {
    return auxInfo;
  }

  @Override
  public void setAuxInfo(Object info) {
    auxInfo = info;
  }

  @Override
  public boolean exists() {
    return Files.exists(Paths.get(root.getName()));
  }

  @Override
  public InputStream getInputStream() {
    throw new UnsupportedOperationException("MFileZip::getInputStream not implemented");
  }

  @Override
  public void writeToStream(OutputStream outputStream) throws IOException {
    for (ZipEntry entry : leafEntries) {
      final File file = new File(entry.getName());
      IO.copyFile(file, outputStream);
    }
  }

  @Override
  public void writeToStream(OutputStream outputStream, long offset, long maxBytes) throws IOException {
    throw new IOException("Writing MFileZip with a byte range to stream not implemented. Filename: " + getName());
  }

  public Path getRootPath() {
    return rootPath;
  }

  public Path getRelativePath() {
    return relativePath;
  }

  public List<ZipEntry> getLeafEntries() {
    return leafEntries;
  }

  public static class Provider implements MFileProvider {

    protected static final String ext = ".zip";

    @Override
    public String getProtocol() {
      return null;
    }

    @Override
    public boolean canProvide(String location) {
      return location.contains(ext);
    }

    @Nullable
    @Override
    public MFile create(String location) throws IOException {
      return new MFileZip(location);
    }
  }
}
