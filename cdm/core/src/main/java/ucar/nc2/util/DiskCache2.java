/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;

/**
 * Manages a place on disk to persistently cache files, which are deleted when the last modified date exceeds a certain
 * time.
 * This starts up a thread to periodically scour itself; be sure to call exit() to terminate the thread.
 *
 * <p>
 * Each DiskCache has a "root directory", which may be set as an absolute path, or reletive to the
 * DiskCache "home directory". The root directory must be writeable.
 *
 * The DiskCache home directory is set in the following order:
 * <ol>
 * <li>through the system property "nj22.cache" if it exists
 * <li>through the system property "user.home" if it exists
 * <li>through the system property "user.dir" if it exists
 * <li>to the current working directory
 * </ol>
 * 
 * TODO will move in ver 6
 */
public class DiskCache2 {
  private static final org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  private static final String lockExtension = ".reserve";
  private static Timer timer;

  /** Be sure to call this when your application exits, otherwise your process may not exit without being killed. */
  public static void exit() {
    if (timer != null) {
      timer.cancel();
      cacheLog.info("DiskCache2.exit()%n");
    }
    timer = null;
  }

  private static synchronized void startTimer() {
    if (timer == null)
      timer = new Timer("DiskCache2");
  }

  /////////////////////////////////////////////////////////////

  public enum CachePathPolicy {
    OneDirectory, NestedDirectory, NestedTruncate
  }

  private CachePathPolicy cachePathPolicy = CachePathPolicy.NestedDirectory;
  private boolean alwaysUseCache;
  private boolean neverUseCache;
  private String cachePathPolicyParam;

  private String root;
  private int persistMinutes, scourEveryMinutes;
  private boolean fail;

  /**
   * Default DiskCache2 strategy: use $user_home/.unidata/cache/, no scouring, alwaysUseCache = false
   * Mimics default DiskCache static class
   */
  static public DiskCache2 getDefault() {
    String root = System.getProperty("nj22.cache");

    if (root == null) {
      String home = System.getProperty("user.home");

      if (home == null)
        home = System.getProperty("user.dir");

      if (home == null)
        home = ".";

      root = home + "/.unidata/cache/";
    }

    DiskCache2 result = new DiskCache2();
    result.setRootDirectory(root);
    result.alwaysUseCache = false;
    return result;
  }

  // NOOP
  static public DiskCache2 getNoop() {
    DiskCache2 noop = new DiskCache2();
    noop.neverUseCache = true;
    return noop;
  }

  private DiskCache2() {}

  /**
   * Create a cache on disk. Use default policy (CachePathPolicy.NestedDirectory)
   * 
   * @param root the root directory of the cache. Must be writeable.
   * @param reletiveToHome if the root directory is relative to the cache home directory.
   * @param persistMinutes a file is deleted if its last modified time is greater than persistMinutes
   * @param scourEveryMinutes how often to run the scour process. If <= 0, don't scour.
   */
  public DiskCache2(String root, boolean reletiveToHome, int persistMinutes, int scourEveryMinutes) {
    this.persistMinutes = persistMinutes;
    this.scourEveryMinutes = scourEveryMinutes;

    if (reletiveToHome) {
      String home = System.getProperty("nj22.cachePersistRoot");

      if (home == null)
        home = System.getProperty("user.home");

      if (home == null)
        home = System.getProperty("user.dir");

      if (home == null)
        home = ".";

      if (!home.endsWith("/"))
        home = home + "/";

      root = home + root;
    }
    setRootDirectory(root);

    if (!fail && scourEveryMinutes > 0) {
      startTimer();
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.MINUTE, scourEveryMinutes);
      timer.scheduleAtFixedRate(new CacheScourTask(), c.getTime(), (long) 1000 * 60 * scourEveryMinutes);
      cacheLog.info("Started a DiskCache2 scour task on {} every {} minutes for files older than {} minutes", root,
          scourEveryMinutes, persistMinutes);
    }

  }

  /*
   * Set the cache root directory. Create it if it doesnt exist.
   * Normally this is set in the Constructor.
   * 
   * @param cacheDir the cache directory
   */
  public void setRootDirectory(String cacheDir) {
    if (!cacheDir.endsWith("/"))
      cacheDir = cacheDir + "/";
    root = StringUtil2.replace(cacheDir, '\\', "/"); // no nasty backslash

    File dir = new File(root);
    if (!dir.mkdirs()) {
      if (!dir.exists()) {
        cacheLog.warn("Failed to create DiskCache2 root at {}. Reason unknown.", dir);
      }
    } else {
      cacheLog.debug("DiskCache2 root created at {}.", dir);
    }

    if (!dir.exists()) {
      fail = true;
      cacheLog.error("DiskCache2 failed to create directory " + root);
    } else {
      cacheLog.debug("DiskCache2 create directory " + root);
    }
  }

  /**
   * Get the cache root directory.
   * 
   * @return the cache root directory.
   */
  public String getRootDirectory() {
    return root;
  }

  /**
   * Get a File in the cache, corresponding to the fileLocation.
   * File may or may not exist.
   * If fileLocation has "/" in it, and cachePathPolicy == NestedDirectory, the
   * nested directories will be created.
   *
   * @param fileLocation logical file location
   * @return physical File in the cache.
   */
  public File getCacheFile(String fileLocation) {
    if (neverUseCache)
      return null;

    if (!alwaysUseCache) {
      File f = new File(fileLocation);
      if (canWrite(f))
        return f;
    }

    File f = new File(makeCachePath(fileLocation));
    // if (f.exists())
    // f.setLastModified( System.currentTimeMillis());

    if (cachePathPolicy == CachePathPolicy.NestedDirectory) {
      File dir = f.getParentFile();
      if (!dir.exists()) {
        boolean ret = dir.mkdirs();
        if (!ret)
          cacheLog.warn("Error creating dir: " + dir);
      }
    }

    return f;
  }

  /**
   * Get the named File.
   * If exists or isWritable, return it.
   * Otherwise get corresponding file in the cache directory.
   *
   * If fileLocation has "/" in it, and cachePathPolicy == NestedDirectory, the
   * nested directories will be created.
   *
   * @param fileLocation logical file location
   * @return physical File as named, or in the cache.
   */
  public File getFile(String fileLocation) {
    if (!alwaysUseCache) {
      File f = new File(fileLocation);
      if (f.exists())
        return f;
      if (canWrite(f))
        return f;
    }

    if (neverUseCache) {
      throw new IllegalStateException(
          "neverUseCache=true, but file does not exist and directory is not writeable =" + fileLocation);
    }

    File f = new File(makeCachePath(fileLocation));
    if (cachePathPolicy == CachePathPolicy.NestedDirectory) {
      File dir = f.getParentFile();
      if (!dir.exists() && !dir.mkdirs())
        cacheLog.warn("Cant create directories for file " + dir.getPath());
    }

    return f;
  }

  /**
   * Returns {@code true} if we can write to the file.
   *
   * @param f a file. It may be a regular file or a directory. It may even be non-existent, in which case the
   *        writability of the file's parent dir is tested.
   * @return {@code true} if we can write to the file.
   */
  public static boolean canWrite(File f) {
    Path path = f.toPath().toAbsolutePath();

    try {
      if (Files.isDirectory(path)) {
        // Try to create a file within the directory to determine if it's writable.
        Files.delete(Files.createTempFile(path, "check", null));
      } else if (Files.isRegularFile(path)) {
        // Try to open the file for writing in append mode.
        Files.newOutputStream(path, StandardOpenOption.APPEND).close();
      } else {
        // File does not exist. See if it's parent directory exists and is writeable.
        Files.delete(Files.createTempFile(path.getParent(), "check", null));
      }
    } catch (IOException | SecurityException e) {
      return false;
    }

    return true;
  }

  /**
   * Looking for an existing file, in cache or no
   * 
   * @param fileLocation the original name
   * @return existing file if you can find it
   */
  public File getExistingFileOrCache(String fileLocation) {
    File f = new File(fileLocation);
    if (f.exists())
      return f;

    if (neverUseCache)
      return null;

    File fc = new File(makeCachePath(fileLocation));
    if (fc.exists())
      return fc;

    return null;
  }

  /**
   * Reserve a new, uniquely named file in the root directory.
   * Mimics File.createTempFile(), but does not actually create
   * the file. Instead, a 0 byte file of the same name with the
   * extension lock is created, thus "reserving" the name until
   * it is used. The filename must be used before persistMinutes,
   * or the lock will be scoured and the reservation will be lost.
   *
   * @param prefix The prefix string to be used in generating the file's
   *        name; must be at least three characters long
   * @param suffix The suffix string to be used in generating the file's
   *        name; may be <code>null</code>, in which case the
   *        suffix <code>".tmp"</code> will be used
   * @return unique, reserved file name
   */
  public synchronized File createUniqueFile(String prefix, String suffix) {
    if (suffix == null)
      suffix = ".tmp";

    Random random = new Random(System.currentTimeMillis());
    String lockName = prefix + random.nextInt() + suffix + lockExtension;
    File lock = new File(getRootDirectory(), lockName);
    while (lock.exists()) {
      lockName = prefix + random.nextInt() + suffix + lockExtension;
      lock = new File(getRootDirectory(), lockName);
    }

    // create a lock file to reserve the name
    try {
      lock.createNewFile();
      cacheLog.debug(String.format("Reserved filename %s for future use.", lockName.replace(lockExtension, "")));
    } catch (IOException e) {
      // should not happen, as DiskCache2 had to make the directory before we can even get here
      // ...but just in case...
      cacheLog.error(String.format("Error creating lock file: %s. May result in cache file collisions.", lock));
    }

    // reserved name will be the lock file name, minus the lock extension
    return new File(getRootDirectory(), lock.getName().replace(lockExtension, ""));
  }

  /**
   * Set the cache path policy
   * 
   * @param cachePathPolicy one of:
   *        OneDirectory (default) : replace "/" with "-", so all files are in one directory.
   *        NestedDirectory: cache files are in nested directories under the root.
   *        NestedTruncate: eliminate leading directories
   */
  public void setPolicy(CachePathPolicy cachePathPolicy) {
    this.cachePathPolicy = cachePathPolicy;
  }

  /**
   * Set the cache path policy
   * 
   * @param cachePathPolicy one of:
   *        OneDirectory (default) : replace "/" with "-", so all files are in one directory.
   *        NestedDirectory: cache files are in nested directories under the root.
   *        NestedTruncate: eliminate leading directories
   *
   * @param cachePathPolicyParam for NestedTruncate, eliminate this string
   */
  public void setCachePathPolicy(CachePathPolicy cachePathPolicy, String cachePathPolicyParam) {
    this.cachePathPolicy = cachePathPolicy;
    this.cachePathPolicyParam = cachePathPolicyParam;
  }

  public void setPolicy(String policy) {
    if (policy == null)
      return;
    if (policy.equalsIgnoreCase("oneDirectory"))
      setCachePathPolicy(CachePathPolicy.OneDirectory, null);
    else if (policy.equalsIgnoreCase("nestedDirectory"))
      setCachePathPolicy(CachePathPolicy.NestedDirectory, null);
  }

  /**
   * If true, always put the file in the cache. default false.
   * 
   * @param alwaysUseCache If true, always put the file in the cache
   */
  public void setAlwaysUseCache(boolean alwaysUseCache) {
    this.alwaysUseCache = alwaysUseCache;
  }


  /**
   * If true, never put the file in the cache. default false.
   * 
   * @param neverUseCache If true, never put the file in the cache
   */
  public void setNeverUseCache(boolean neverUseCache) {
    this.neverUseCache = neverUseCache;
  }

  /**
   * Make the cache filename
   * 
   * @param fileLocation normal file location
   * @return cache filename
   */
  private String makeCachePath(String fileLocation) {

    // remove ':', '?', '=', replace '\' with '/', leading or trailing '/'
    String cachePath = fileLocation;
    cachePath = StringUtil2.remove(cachePath, '?');
    cachePath = StringUtil2.remove(cachePath, '=');
    cachePath = StringUtil2.replace(cachePath, '\\', "/");
    if (cachePath.startsWith("/"))
      cachePath = cachePath.substring(1);
    if (cachePath.endsWith("/"))
      cachePath = cachePath.substring(0, cachePath.length() - 1);
    cachePath = StringUtil2.remove(cachePath, ':');

    // remove directories
    if (cachePathPolicy == CachePathPolicy.OneDirectory) {
      cachePath = StringUtil2.replace(cachePath, '/', "-");
    }

    // eliminate leading directories
    else if (cachePathPolicy == CachePathPolicy.NestedTruncate) {
      int pos = cachePath.indexOf(cachePathPolicyParam);
      if (pos >= 0)
        cachePath = cachePath.substring(pos + cachePathPolicyParam.length());
      if (cachePath.startsWith("/"))
        cachePath = cachePath.substring(1);
    }

    // make sure the parent directory exists
    if (cachePathPolicy != CachePathPolicy.OneDirectory) {
      File file = new File(root + cachePath);
      File parent = file.getParentFile();
      if (!parent.exists()) {
        if (root == null) { // LOOK shouldnt happen, remove soon
          cacheLog.error("makeCachePath has null root %{}", parent.getPath());
          new Throwable().printStackTrace();
        }
        boolean ret = parent.mkdirs();
        if (!ret)
          cacheLog.warn("Error creating parent: " + parent);
      }
    }

    return root + cachePath;
  }

  /**
   * Show cache contents, for debugging.
   * 
   * @param pw write to this PrintStream.
   */
  public void showCache(PrintStream pw) {
    pw.println("Cache files");
    pw.println("Size   LastModified       Filename");
    File dir = new File(root);
    File[] files = dir.listFiles();
    if (files != null)
      for (File file : files) {
        String org = null;
        try {
          org = URLDecoder.decode(file.getName(), "UTF8");
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

        pw.println(" " + file.length() + " " + new Date(file.lastModified()) + " " + org);
      }
  }

  /**
   * Remove any files or directories whose last modified time greater than persistMinutes
   * 
   * @param dir clean starting here
   * @param sbuff status messages here, may be null
   * @param isRoot delete empty directories, bit not root directory
   */
  public void cleanCache(File dir, Formatter sbuff, boolean isRoot) {
    long now = System.currentTimeMillis();
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IllegalStateException("DiskCache2: not a directory or I/O error on dir=" + dir.getAbsolutePath());
    }

    // check for empty directory
    if (!isRoot && (files.length == 0)) {
      purgeExpired(dir, sbuff, now);
      return;
    }

    // check for expired files
    for (File file : files) {
      if (file.isDirectory()) {
        cleanCache(file, sbuff, false);
      } else {
        purgeExpired(file, sbuff, now);
      }
    }
  }

  private void purgeExpired(File deletable, Formatter sbuff, long now) {
    long duration = now - deletable.lastModified();
    duration /= 1000 * 60; // minutes
    if (duration > persistMinutes) {
      boolean ok = deletable.delete();
      if (!ok) {
        String type = deletable.isDirectory() ? "directory" : "file";
        cacheLog.error("Unable to delete {} {}", type, deletable.getAbsolutePath());
      }
      if (sbuff != null)
        sbuff.format(" deleted %s %s lastModified= %s%n", ok, deletable.getPath(),
            CalendarDate.of(deletable.lastModified()));
    }
  }

  private class CacheScourTask extends TimerTask {
    @Override
    public void run() {
      Formatter sbuff = new Formatter();
      sbuff.format("DiskCache2 scour on directory= %s%n", root);
      cleanCache(new File(root), sbuff, true);
      if (cacheLog.isDebugEnabled())
        cacheLog.debug(sbuff.toString());
    }
  }

  @Override
  public String toString() {
    String sb = "DiskCache2" + "{cachePathPolicy=" + cachePathPolicy + ", alwaysUseCache=" + alwaysUseCache
        + ", cachePathPolicyParam='" + cachePathPolicyParam + '\'' + ", root='" + root + '\'' + ", scourEveryMinutes="
        + scourEveryMinutes + ", persistMinutes=" + persistMinutes + ", fail=" + fail + '}';
    return sb;
  }

}
