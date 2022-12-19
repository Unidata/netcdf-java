/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;
import java.io.IOException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.*;

/** Test FileCache. */
public class TestNetcdfFileCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static FileCache cache;
  private static FileFactory factory = new MyFileFactory();

  @BeforeClass
  static public void setUp() {
    cache = new FileCache(5, 100, 60 * 60);
  }

  static public class MyFileFactory implements FileFactory {
    public FileCacheable open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return NetcdfDatasets.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  private int count = 0;

  private void loadFilesIntoCache(File dir, FileCache cache) {
    File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File f : files) {
      if (f.isDirectory()) {
        loadFilesIntoCache(f, cache);
      } else if (f.getPath().endsWith(".nc") && f.length() > 0) {
        try {
          String want = StringUtil2.replace(f.getPath(), '\\', "/");
          DatasetUrl durl = DatasetUrl.create(null, want);
          cache.acquire(factory, durl);
          count++;
        } catch (IOException e) {
          fail(" *** failed on " + f.getPath());
        }
      }
    }
  }

  @Test
  public void testNetcdfFileCache() throws IOException {
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    cache.showCache(new Formatter(System.out));
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(count);

    // count cache size
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(count);

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);
    }

    // load same files again - should be added to the list, rather than creating a new elem
    // Note that the files are still open (therefore locked), so this won't increment hits.
    int saveCount = count;
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    cache.showCache(new Formatter(System.out));
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(2 * saveCount);

    assertThat(map.values().size()).isEqualTo(saveCount); // problem

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(2);
      checkAllSame(elem.list);
    }

    cache.clearCache(true);
    // clearing the cache doesn't reset the cache counters.
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(2 * saveCount);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(0);

    // load again (cached files are still locked, so no hits and all misses)
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(3 * saveCount);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(saveCount);

    // close all
    List<FileCacheable> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);
      for (FileCache.CacheElement.CacheFile file : elem.list) {
        files.add(file.ncfile);
      }
    }
    for (FileCacheable ncfile : files) {
      ncfile.close();
    }
    cache.clearCache(false);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(0);

    // load twice (files aren't unlocked, so it results in cache misses)
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(5 * saveCount);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(saveCount);

    // close 1 of 2
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(2);
      FileCache.CacheElement.CacheFile first = elem.list.get(0);
      first.ncfile.close();
      assertThat(first.isLocked.get()).isFalse();
      assertThat(elem.list.size()).isEqualTo(2);
    }

    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(saveCount);

    cache.clearCache(false);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(saveCount);

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);
    }

    cache.clearCache(true);

    // Verify that cache hits actually happen.
    // Load up the files and then close them (thus, unlocking them).
    // Then load them up again.
    // Since files are now unlocked, loading stuff up should result in hits.
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(6 * saveCount);
    map = cache.getCache();
    // close all
    files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);

      for (FileCache.CacheElement.CacheFile file : elem.list) {
        synchronized (file) {
          // Need to collect files to close instead of directly closing them
          // in this double-loop because closing the files changes the iterator
          // We are also explicitly doing synchronous unlocks so that when we
          // get to the next file loading stage, we are guaranteed that the
          // files in the cache are indeed released and capable of being
          // reacquired. Users won't need to do this normally.
          files.add(file.ncfile);
          file.isLocked.set(false);
        }
      }
    }
    for (FileCacheable ncfile : files) {
      ncfile.close();
    }
    logger.debug("Closed {} files", files.size());

    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    cache.showCache(new Formatter(System.out));
    assertThat(cache.hits.get()).isEqualTo(saveCount);
    assertThat(cache.miss.get()).isEqualTo(6 * saveCount);

    cache.clearCache(true);
  }

  void checkAllSame(List<FileCache.CacheElement.CacheFile> list) {
    FileCache.CacheElement.CacheFile first = null;
    for (FileCache.CacheElement.CacheFile file : list) {
      assertThat(file.isLocked.get()).isTrue();
      assertThat(file.countAccessed).isEqualTo(0); // countAccessed not incremented until its closed, so == 0
      assertThat(file.lastAccessed).isNotEqualTo(0);

      if (first == null)
        first = file;
      else {
        assertThat(first.ncfile.getLocation()).isEqualTo(file.ncfile.getLocation());
        assertThat(first.lastAccessed).isLessThan(file.lastAccessed);
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testPeriodicClear() throws IOException {
    FileCache cache = new FileCache(0, 10, 60 * 60);
    testPeriodicCleanup(cache);
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(0);

    cache = new FileCache(5, 10, 60 * 60);
    testPeriodicCleanup(cache);
    map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(5);
  }

  private void testPeriodicCleanup(FileCache cache) throws IOException {
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    logger.debug(" loaded " + count);

    // close all
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    List<FileCacheable> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);
      for (FileCache.CacheElement.CacheFile file : elem.list) {
        files.add(file.ncfile);
      }
    }
    logger.debug(" close " + files.size());

    for (FileCacheable ncfile : files) {
      ncfile.close();
    }

    cache.showCache(new Formatter(System.out));
    cache.cleanup(10);
  }


  //////////////////////////////////////////////////////////////////////////////////
  int N = 10000;
  int PROD_THREAD = 10;
  int CONS_THREAD = 10;
  int SKIP = 100;

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    List<String> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      for (FileCache.CacheElement.CacheFile file : elem.list)
        files.add(file.ncfile.getLocation());
    }

    Random r = new Random();
    int nfiles = files.size();

    ExecutorService qexec = null;
    ExecutorService exec = null;
    try {
      Formatter format = new Formatter(System.out);
      ConcurrentLinkedQueue<Future> q = new ConcurrentLinkedQueue<>();
      qexec = Executors.newFixedThreadPool(CONS_THREAD);
      qexec.submit(new Consumer(q, format));

      exec = Executors.newFixedThreadPool(PROD_THREAD);
      for (int i = 0; i < N; i++) {
        // pick a file at random
        int findex = r.nextInt(nfiles);
        String location = files.get(findex);
        q.add(exec.submit(new CallAcquire(location)));

        if (i % SKIP == 0) {
          format.format(" %3d qsize= %3d ", i, q.size());
          cache.showStats(format);
        }
      }

      format.format("awaitTermination 10 secs qsize= %3d%n", q.size());
      cache.showStats(format);
      exec.awaitTermination(10, TimeUnit.SECONDS);
      format.format("done qsize= %4d%n", q.size());
      cache.showStats(format);

      int total = 0;
      int total_locks = 0;
      HashSet<Object> checkUnique = new HashSet<>();
      map = cache.getCache();
      for (Object key : map.keySet()) {
        assertThat(checkUnique.contains(key)).isFalse();
        checkUnique.add(key);
        int locks = 0;
        FileCache.CacheElement elem = map.get(key);
        synchronized (elem) {
          for (FileCache.CacheElement.CacheFile file : elem.list)
            if (file.isLocked.get())
              locks++;
        }
        total_locks += locks;
        total += elem.list.size();
      }
      logger.debug(" total=" + total + " total_locks=" + total_locks);
      // assert total_locks == map.keySet().size();

      cache.clearCache(false);
      format.format("after cleanup qsize= %4d%n", q.size());
      cache.showStats(format);

      cache.clearCache(true);

    } finally {
      if (qexec != null)
        qexec.shutdownNow();
      if (exec != null)
        exec.shutdownNow();
    }
  }

  class Consumer implements Runnable {
    private final ConcurrentLinkedQueue<Future> queue;
    Formatter format;

    Consumer(ConcurrentLinkedQueue<Future> q, Formatter format) {
      queue = q;
      this.format = format;
    }


    public void run() {
      try {
        while (true) {
          consume(queue.poll());
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    void consume(Future x) throws ExecutionException, InterruptedException, IOException {
      if (x == null)
        return;

      if (x.isDone()) {
        NetcdfFile ncfile = (NetcdfFile) x.get();
        ncfile.close();
      } else {
        queue.add(x); // put it back
      }

    }
  }

  class CallAcquire implements Callable<FileCacheable> {
    String location;

    CallAcquire(String location) {
      this.location = location;
    }

    public FileCacheable call() throws Exception {
      DatasetUrl durl = DatasetUrl.create(null, location);
      return cache.acquire(factory, durl);
    }
  }

  class RunClose implements Runnable {
    NetcdfFile f;

    RunClose(NetcdfFile f) {
      this.f = f;
    }

    public void run() {
      try {
        f.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
