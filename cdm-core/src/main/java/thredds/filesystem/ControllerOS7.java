/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.filesystem;

import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MFile;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

/**
 * Use Java 7 NIO for scanning the file system
 *
 * @author caron
 * @since 11/8/13
 */
@ThreadSafe
public class ControllerOS7 implements MController {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerOS7.class);

  ////////////////////////////////////////

  @Override
  public Iterator<MFile> getInventoryAll(CollectionConfig mc, boolean recheck) {
    return null;
  }

  @Override
  public Iterator<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) throws IOException {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    Path cd = Paths.get(path);
    if (!Files.exists(cd))
      return null;
    return new MFileIterator(cd, new CollectionFilter(mc)); // removes subdirs
  }

  public Iterator<MFile> getSubdirs(CollectionConfig mc, boolean recheck) {
    return null;
  }


  public void close() {} // NOOP


  ////////////////////////////////////////////////////////////

  private static class CollectionFilter implements DirectoryStream.Filter<Path> {
    CollectionConfig mc;

    private CollectionFilter(CollectionConfig mc) {
      this.mc = mc;
    }

    @Override
    public boolean accept(Path entry) {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }

  // returns everything in the current directory
  private static class MFileIterator implements Iterator<MFile> {
    Iterator<Path> dirStream;

    MFileIterator(Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
      if (filter != null)
        dirStream = Files.newDirectoryStream(dir, filter).iterator();
      else
        dirStream = Files.newDirectoryStream(dir).iterator();
    }

    public boolean hasNext() {
      return dirStream.hasNext();
    }

    public MFile next() {
      try {
        return new MFileOS7(dirStream.next());
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  //////////////////////////////////////////////////////////////////
  // playing around with NIO

  public static class PrintFiles extends SimpleFileVisitor<Path> {
    private int countFiles;
    private int countDirs;
    private int countOther;
    private int countSyms;
    long start = System.currentTimeMillis();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      if (attr.isSymbolicLink()) {
        countSyms++;
      } else if (attr.isRegularFile()) {
        countFiles++;
      } else {
        countOther++;
      }
      if (countFiles % 10000 == 0) {
        double took = (System.currentTimeMillis() - start);
        double rate = countFiles / took;
        double drate = countDirs / took;
      }
      return FileVisitResult.CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      countDirs++;
      return FileVisitResult.CONTINUE;
    }

    // If there is some error accessing the file, let the user know.
    // If you don't override this method and an error occurs, an IOException is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public String toString() {
      return "PrintFiles{" + "countFiles=" + countFiles + ", countDirs=" + countDirs + ", countOther=" + countOther
          + ", countSyms=" + countSyms + '}';
    }
  }

}
