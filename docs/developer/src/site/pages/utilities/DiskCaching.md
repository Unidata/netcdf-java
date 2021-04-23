---
title: Disk Caching
last_updated: 2018-10-10
sidebar: developer_sidebar
toc: false
permalink: disk_caching.html
---

## CDM Caching

### Disk Caching

#### Writing temporary files using DiskCache

There are a number of places where the CDM library needs to write temporary files to disk. If you end up using the file more than once, its useful to save these files. The CDM uses static methods in <b>_ucar.nc2.util.DiskCache_</b> to manage how the temporary files are managed.

Before the CDM writes the temporary file, it looks to see if it already exists.

1. If a filename ends with <b>_".Z", ".zip", ".gzip", ".gz", or ".bz2", NetcdfFile.open_</b> will write an uncompressed file of the same name, but without the suffix.

2. <b>_Nexrad2_</b>, <b>_Cinrad2_</b> files that are compressed will be uncompressed to a file with an <b>_.uncompress_</b> prefix.
By default, DiskCache prefers to place the temporary file in the same directory as the original file. If it does not have write permission in that directory, by default it will use the directory <b>_${user_home}/.unidata/cache/_</b>. You can change the directory by calling

<b>_ucar.nc2.util.DiskCache.setRootDirectory(rootDirectory)._</b>

You might want to always write temporary files to the cache directory, in order to manage them in a central place. To do so, call

<b>_ucar.nc2.util.DiskCache.setCachePolicy( boolean alwaysInCache)_</b> with parameter <b>_alwaysInCache = true_</b>.

You may want to limit the amount of space the disk cache uses (unless you always have data in writeable directories, so that the disk cache is never used). To scour the cache, call <b>_DiskCache.cleanCache()_</b>. There are several variations of the cleanup:

* <b>_DiskCache.cleanCache(Date cutoff, StringBuilder sbuff)_</b> will delete files older than the cutoff date.
* <b>_DiskCache.cleanCache(long maxBytes, StringBuilder sbuff)_</b> will retain maxBytes bytes, deleting oldest files first.
* <b>_DiskCache.cleanCache(long maxBytes, Comparator<File> fileComparator, StringBuilder sbuff)_</b> will retain maxBytes bytes, deleting files in the order defined by your Comparator.

For long running application, you might want to do this periodically in a background timer thread, as in the following example.

~~~
1) Calendar c = Calendar.getInstance(); // contains current startup time
   c.add( Calendar.MINUTE, 30); // add 30 minutes to current time     // run task every 60 minutes, starting 30 minutes from now
2) java.util.Timer timer = new Timer();
    timer.scheduleAtFixedRate( new CacheScourTask(), c.getTime(), (long) 1000 * 60 * 60 );

3) private class CacheScourTask extends java.util.TimerTask {   
    public void run() {
     StringBuffer sbuff = new StringBuffer();
4) DiskCache.cleanCache(100 * 1000 * 1000, sbuff); // 100 Mbytes
     sbuff.append("----------------------\n");
5)   log.info(sbuff.toString());
    }
   }
   ...
   // upon exiting
6) timer.cancel();
~~~

1. Get the current time and add 30 minutes to it
2. Start up a timer that executes every 60 minutes, starting in 30 minutes
3. Your class must extend TimerTask, the run method is called by the Timer
4. Scour the cache, allowing 100 Mbytes of space to be retained
5. Optionally log a message with the results of the scour.
6. Make sure you cancel the timer before your application exits, or else the process will not terminate.

#### Writing temporary files using DiskCache2

In a number of places, the <b>_ucar.nc2.util.DiskCache2_</b> class is used to control caching. This does not use static methods, so can be configured for each individual use.

The default constructor mimics DiskCache, using <b>_${user_home}/.unidata/cache/</b>_ as the root directory:

<b>_DiskCache2 dc2 = new DiskCache2();_</b>

You can change the root directory by calling

<b>_dc2.setRootDirectory(rootDirectory)._</b>

You can tell the class to scour itself in a background timer by using the constructor:

<b>_DiskCache2 dc2 = new DiskCache2(rootDirectory, false, 24 * 60, 60);_</b>

~~~
/**
 * Create a cache on disk.
 * @param root the root directory of the cache. Must be writeable.
 * @param reletiveToHome if the root directory is reletive to the cache home directory.
 * @param persistMinutes  a file is deleted if its last modified time is greater than persistMinutes
 * @param scourEveryMinutes how often to run the scour process. If <= 0, dont scour.
 */
 public DiskCache2(String root, boolean reletiveToHome, int persistMinutes, int scourEveryMinutes);
~~~
       
You can change the cache policy from the default CachePathPolicy.OneDirectory by (eg):

~~~
dc2.setCachePathPolicy(CachePathPolicy.NestedTruncate, null).

  /**
   * Set the cache path policy
   * @param cachePathPolicy one of:
   *   OneDirectory (default) : replace "/" with "-", so all files are in one directory.
   *   NestedDirectory: cache files are in nested directories under the root.
   *   NestedTruncate: eliminate leading directories
   *
   * @param cachePathPolicyParam for NestedTruncate, eliminate this string
   */
  public void setCachePathPolicy(CachePathPolicy cachePathPolicy, String cachePathPolicyParam);
~~~
  
You can ensure that the cache is always used with:

<b>_dc2.setCacheAlwaysUsed(true);_</b>

Otherwise, the cache will try to write the temporary file in the same directory as the data file, and only use the cache if that directory is not writeable.

### GRIB Indexing and Caching

In 4.3 and above, for each GRIB file the CDM writes a _grib index file_ using the filename plus suffix <b>_.gbx9_</b>. So a file named <b>_filename.grib1_</b> will have an index file <b>_filename.grib1.gbx9_</b> created for it the first time that its read. Usually a _cdm index file_ is also created, using the filename plus suffix <b>_.ncx_</b>. So a file named filename.grib1 will have an index file filename.grib1.ncx created for it the first time. When a GRIB file is only part of a collection of GRIB files, then the ncx file may be created only for the collection.

The location of these index files is controlled by a caching strategy. The default strategy is to try to place the index files in the same directory as the data file. If that directory is not writeable, then the default strategy is to write the index files in the default caching directory. In a client application using the CDM, that default will be

<b>_${user_home}/.unidata/cache/._</b>

On the TDS it will be
<b>_${tomcat_home}/content/thredds/cache/cdm_</b>

Clients of the CDM can change the GRIB caching behavior by configuring a DiskCache2 and calling:

<b>_ucar.nc2.grib.GribCollection.setDiskCache2(DiskCache2 dc);_</b>

### Object Caching

#### NetcdfFileCache
NetcdfFile objects are cached in memory for performance. When acquired, the object is locked so another thread cannot use. When closed, the lock is removed. When the cache is full, older objects are removed from the cache, and all resources released.

Note that typically a <b>_java.io.RandomAccessFile_</b> object, holding an OS file handle, is open while its in the cache. You must make sure that your cache size is not so large such that you run out of file handles due to NetcdfFile object caching. Most aggregations do not hold more than one file handle open, no matter how many files are in the aggregation. The exception to that is a Union aggregation, which holds each of the files in the union open for the duration of the NetcdfFile object.

Holding a file handle open also creates a read lock on some operating systems, which will prevent the file from being opened in write mode.

To enable caching, you must first call

~~~
 NetcdfDataset.initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period);
~~~

where _minElementsInMemory_ are the number of objects to keep in the cache when cleaning up, maxElementsInMemory triggers a cleanup if the cache size goes over it, and period specifies the time in seconds to do periodic cleanups.

After enabling, you can disable with:

~~~
NetcdfDataset.disableNetcdfFileCache();
~~~

However, you cant reenable after disabling.

Setting <b>_minElementsInMemory_</b> to zero will remove all files not currently in use every <b>_period_</b> seconds.

Normally the cleanup is done is a background thread to not interferre with your application, and the maximum elements is approximate. When resources such as file handles must be carefully managed, you can set a hard limit with this call:

~~~
   NetcdfDataset.initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int hardLimit, int period);
~~~

so that as soon as the number of NetcdfFile objects exceeds hardLimit , a cleanup is done immediately in the calling thread.