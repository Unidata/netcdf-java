/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.filesystem;

import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MFile;
import thredds.inventory.MFileDirectoryStream;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Use Java 7 NIO for scanning the file system
 *
 * @author caron
 * @since 11/8/13
 */
@ThreadSafe
public class ControllerOS7 implements MController {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerOS7.class);

  ////////////////////////////////////////

  @Override
  public DirectoryStream<MFile> getInventoryAll(CollectionConfig mc, boolean recheck) {
    return null;
  }

  @Override
  public DirectoryStream<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    Path cd = Paths.get(path);
    if (!Files.exists(cd))
      return null;
    MFileDirectoryStream mfileDirStream = null;
    try {
      mfileDirStream = new MFileDirectoryStream(new MFileIterator(cd, new CollectionFilter(mc))); // removes subdirs
    } catch (IOException ioe) {
      logger.warn(ioe.getMessage(), ioe);
    }
    return mfileDirStream;
  }

  @Override
  public DirectoryStream<MFile> getSubdirs(CollectionConfig mc, boolean recheck) {
    return null;
  }


  public void close() {} // NOOP


  ////////////////////////////////////////////////////////////

  private static class CollectionFilter implements DirectoryStream.Filter<Path> {
    CollectionConfig mc; // LOOK not used yet

    private CollectionFilter(CollectionConfig mc) {
      this.mc = mc;
    }

    @Override
    public boolean accept(Path entry) {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }

  // returns everything in the current directory
  private static class MFileIterator implements Iterator<MFile>, AutoCloseable {
    private final DirectoryStream<Path> dirStream;
    private final Iterator<Path> pathIterator;

    MFileIterator(Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
      if (filter != null)
        dirStream = Files.newDirectoryStream(dir, filter);
      else
        dirStream = Files.newDirectoryStream(dir);
      pathIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      return pathIterator.hasNext();
    }

    public MFile next() {
      try {
        return new MFileOS7(pathIterator.next());
      } catch (IOException e) {
        throw new NoSuchElementException(e.getMessage());
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
      dirStream.close();
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
      String sb = "PrintFiles{" + "countFiles=" + countFiles + ", countDirs=" + countDirs + ", countOther=" + countOther
          + ", countSyms=" + countSyms + '}';
      return sb;
    }
  }

}
