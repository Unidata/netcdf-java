package thredds.filesystem.zarr;

import thredds.filesystem.ControllerOS;
import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MControllerProvider;
import thredds.inventory.MFile;
import thredds.inventory.zarr.MFileZip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;

public class ControllerZip extends ControllerOS implements MController {

  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerZip.class);

  private static final String prefix = "file:";

  @Override
  public Iterator<MFile> getInventoryAll(CollectionConfig mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }

    try {
      MFileZip mfile = new MFileZip(path);
      return new MFileIteratorLeaves(mfile);
    } catch (IOException ioe) {
      logger.warn(ioe.getMessage(), ioe);
      return null;
    }
  }

  @Override
  public Iterator<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }

    try {
      MFileZip mfile = new MFileZip(path);
      return new FilteredIterator(mfile, false);
    } catch (IOException ioe) {
      logger.warn(ioe.getMessage(), ioe);
      return null;
    }
  }

  @Override
  public Iterator<MFile> getSubdirs(CollectionConfig mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }

    try {
      MFileZip mfile = new MFileZip(path);
      return new FilteredIterator(mfile, true);
    } catch (IOException ioe) {
      logger.warn(ioe.getMessage(), ioe);
      return null;
    }
  }

  @Override
  public void close() {} // NOOP

  protected static class MFileIterator implements Iterator<MFile> {
    List<MFileZip> files = new ArrayList<MFileZip>();
    int count = 0;

    public MFile next() {
      return files.get(count++);
    }

    public boolean hasNext() {
      return count < files.size();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // returns everything in the current directory
  protected static class FilteredIterator extends MFileIterator implements Iterator<MFile> {

    FilteredIterator(MFileZip file, boolean wantDirs) throws IOException {
      Set<Path> fileNames = new HashSet<Path>();
      List<ZipEntry> entries = file.getLeafEntries();
      Path relativePath = file.getRelativePath();

      for (ZipEntry entry : entries) {
        Path entryPath = Paths.get(File.separator + entry.getName());
        if (!entryPath.startsWith(relativePath)) {
          logger.warn(entryPath.toString() + " is not an entry in " + relativePath.toString());
          continue;
        }
        // get truncate path to one level below current path
        Path childPath = entryPath.subpath(0, relativePath.getNameCount() + 1);
        fileNames.add(childPath);
      }

      // filter by wantDirs
      for (Path location : fileNames) {
        try {
          // get mfile
          MFileZip mfile = new MFileZip(file.getRootPath().toString() + File.separator + location.toString());
          // filter by want dirs
          if (mfile.isDirectory() == wantDirs) {
            files.add(mfile);
          }
        } catch (IOException ioe) {
          logger.error(ioe.getMessage(), ioe);
        }
      }
    }
  }

  // returns all leaf "files" under current path in zip file
  protected static class MFileIteratorLeaves extends MFileIterator implements Iterator<MFile> {
    MFileIteratorLeaves(MFileZip file) {
      List<ZipEntry> entries = file.getLeafEntries();
      for (ZipEntry entry : entries) {
        try {
          this.files.add(new MFileZip(file.getRootPath() + File.separator + entry.getName()));
        } catch (IOException ioe) {
          logger.error(ioe.getMessage(), ioe);
        }
      }
    }
  }

  public static class Provider implements MControllerProvider {

    private static final String ext = ".zip";

    @Override
    public String getProtocol() {
      return null;
    }

    @Override
    public boolean canScan(String location) {
      return location.contains(ext);
    }

    @Override
    public MController create() {
      return new ControllerZip();
    }
  }
}
