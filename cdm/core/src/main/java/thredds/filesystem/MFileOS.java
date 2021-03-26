/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.filesystem;

import java.nio.file.Files;
import javax.annotation.Nullable;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;

/**
 * Implements thredds.inventory.MFile using regular OS files.
 *
 * @author caron
 * @since Jun 30, 2009
 */
@ThreadSafe
public class MFileOS implements MFile {

  /**
   * Make MFileOS if file exists, otherwise return null
   * 
   * @param filename name of the existing file.
   * @return MFileOS or null
   */
  @Nullable
  public static MFileOS getExistingFile(String filename) {
    if (filename == null)
      return null;
    File file = new File(filename);
    if (file.exists())
      return new MFileOS(file);
    return null;
  }

  private final File file;
  private final long lastModified;
  private Object auxInfo;

  public MFileOS(java.io.File file) {
    this.file = file;
    this.lastModified = file.lastModified();
  }

  public MFileOS(String filename) {
    this.file = new File(filename);
    this.lastModified = file.lastModified();
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public long getLength() {
    return file.length();
  }

  @Override
  public boolean isDirectory() {
    return file.isDirectory();
  }

  @Override
  public boolean isReadable() {
    return Files.isReadable(file.toPath());
  }

  @Override
  public String getPath() {
    // no microsnot
    return StringUtil2.replace(file.getPath(), '\\', "/");
  }

  @Override
  public String getName() {
    return file.getName();
  }

  @Override
  public MFile getParent() {
    return new MFileOS(file.getParentFile());
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
  public void setAuxInfo(Object auxInfo) {
    this.auxInfo = auxInfo;
  }

  @Override
  public String toString() {
    return "MFileOS{" + "file=" + file.getPath() + ", lastModified=" + lastModified + '}';
  }

  public File getFile() {
    return file;
  }
}
