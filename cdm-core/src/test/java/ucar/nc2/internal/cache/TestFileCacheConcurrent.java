/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

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
import java.util.concurrent.atomic.AtomicInteger;

/** Test concurrent use of FileCache */
public class TestFileCacheConcurrent {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static void makeFileList(File dir, String suffix, List<String> result) {
    File[] files = dir.listFiles();
    if (files == null) {
      assert false;
      return;
    }
    for (File f : files) {
      if (f.isDirectory() && !f.getName().equals("exclude")) {
        makeFileList(f, suffix, result);

      } else if (f.getPath().endsWith(suffix) && f.length() > 0) {
        String want = StringUtil2.replace(f.getPath(), '\\', "/");
        result.add(want);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  private final int COUNT = 1000;
  private final int PRINT_EVERY = 100;
  private final int CLIENT_THREADS = 50;
  private final int WAIT_MAX = 25; // msecs
  private final int MAX_TASKS = 100; // bounded queue
  private final int NSAME = 3; // submit same file n consecutive

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    System.out.printf("TestFileCacheConcurrent%n");
    // load some files into the cache
    List<String> fileList = new ArrayList<>(100);
    makeFileList(new File(TestDir.cdmLocalTestDataDir), "nc", fileList);
    int nfiles = fileList.size();
    System.out.printf(" loaded %d files%n", nfiles);

    ThreadPoolExecutor pool = null;
    try {
      Random r = new Random();
      ArrayBlockingQueue<Runnable> q = new ArrayBlockingQueue<>(MAX_TASKS);
      pool = new ThreadPoolExecutor(CLIENT_THREADS, CLIENT_THREADS, 100, TimeUnit.SECONDS, q);

      int count = 0;
      while (count < COUNT) {
        if (q.remainingCapacity() > NSAME) {
          // pick a file at random
          String location = fileList.get(r.nextInt(nfiles));

          for (int i = 0; i < NSAME; i++) {
            count++;
            pool.submit(new CallAcquire(location, r.nextInt(WAIT_MAX)));

            if (count % PRINT_EVERY == 0) {
              Formatter f = new Formatter();
              cache.showStats(f);
              System.out.printf(" submit %d queue size %d cache: {%s}%n", count, q.size(), f);
            }
          }
        } else {
          Thread.sleep(100);
        }
      }
    } finally {
      if (pool != null)
        pool.shutdownNow();
      FileCache.shutdown();
    }
  }

  private AtomicInteger done = new AtomicInteger();

  private FileCacheIF cache = new FileCache(50, 100, 30);
  private FileFactory factory = new MyFileFactory();

  class MyFileFactory implements FileFactory {
    public FileCacheable open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return NetcdfDatasets.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  // as files are acquired, check that they are locked and then release them
  class CallAcquire implements Runnable {
    String location;
    int wait;

    CallAcquire(String location, int wait) {
      this.location = location;
      this.wait = wait;
    }

    public void run() {
      try {
        DatasetUrl durl = DatasetUrl.create(null, location);
        FileCacheable fc = cache.acquire(factory, durl);
        NetcdfFile ncfile = (NetcdfFile) fc;
        // assert ncfile.isLocked();
        assertThat(ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_GET_IOSP)).isNotNull();
        Thread.sleep(wait);
        ncfile.close();
        int d = done.incrementAndGet();
        if (d % PRINT_EVERY == 0)
          System.out.printf(" done %d%n", d);

      } catch (InterruptedException e) {
        System.out.println(" InterruptedException=" + e.getMessage());

      } catch (Throwable e) {
        System.out.println(" fail=" + e.getMessage());
        e.printStackTrace();
        fail();
      }

    }
  }
}
