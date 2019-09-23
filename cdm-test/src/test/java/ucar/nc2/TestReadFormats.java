package ucar.nc2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Just open all the files in the formats directory. */
// @Category(NeedsCdmUnitTest.class)
public class TestReadFormats {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static int countGood = 0;
  private static int countFail = 0;
  private static int countTotal = 0;
  private static boolean verbose = true;

  private List<String> failFiles = new ArrayList<>();

  static class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      countTotal++;
      String name = pathname.getName();
      if (name.contains(".gbx"))
        return false;
      if (name.contains(".ncx"))
        return false;
      if (name.endsWith(".java"))
        return false;
      if (name.endsWith(".jpg"))
        return false;
      if (name.endsWith(".tiff"))
        return false;
      if (name.endsWith(".tif"))
        return false;
      if (name.endsWith(".TIF"))
        return false;
      if (name.endsWith(".txt"))
        return false;
      if (name.endsWith(".xml"))
        return false;

      if (!name.endsWith(".ctl") && pathname.getPath().contains("grads"))
        return false;
      if (name.endsWith(".HDR") && pathname.getPath().contains("gtopo"))
        return false;
      return true;
    }
  }

  @Test
  public void testAllFormat() {
    openAllInDir(TestDir.cdmUnitTestDir + "/formats", new MyFileFilter());
    int countExclude = countTotal - countGood - countFail;
    System.out.printf("Good=%d Fail=%d Exclude=%d%n", countGood, countFail, countExclude);
    for (String f : failFiles)
      System.out.printf("  %s%n", f);
    assert countFail == 0 : "Failed = " + countFail;
  }

  @Test
  public void problem() {
    openAllInDir(TestDir.cdmUnitTestDir + "/formats/grib1", new MyFileFilter());
    int countExclude = countTotal - countGood - countFail;
    System.out.printf("Good=%d Fail=%d Exclude=%d%n", countGood, countFail, countExclude);
    for (String f : failFiles)
      System.out.printf("  %s%n", f);
    assert countFail == 0 : "Failed = " + countFail;
  }

  // these are fairly complete hdf4 files from nsidc
  public void utestHdf4() {
    openAllInDir("F:/data/formats/hdf4", new MyFileFilter());
    int countExclude = countTotal - countGood - countFail;
    System.out.printf("Good=%d Fail=%d Exclude=%d%n", countGood, countFail, countExclude);
  }

  @Test
  public void readCinrad() {
    doOne(TestDir.cdmUnitTestDir + "formats/cinrad/CHGZ_2006071512.0300");
  }

  private void doOne(String name) {
    try (NetcdfFile ncfile = NetcdfDataset.openFile(name, null)) {
      if (verbose)
        System.out.printf("  GOOD on %s == %s%n", name, ncfile.getFileTypeId());
      countGood++;
    } catch (Throwable t) {
      System.out.printf("  FAIL on %s == %s%n", name, t.getMessage());
      t.printStackTrace();
    }
  }

  private void openAllInDir(String dirName, FileFilter ff) {
    if (verbose)
      System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f)) {
        if (verbose)
          System.out.printf("  Open %s%n", name);
        try (NetcdfFile ncfile = NetcdfDataset.openFile(name, null)) {
          if (verbose)
            System.out.printf("  GOOD on %s == %s%n", name, ncfile.getFileTypeId());
          countGood++;
        } catch (Throwable t) {
          System.out.printf("  FAIL on %s == %s%n", name, t.getMessage());
          t.printStackTrace();
          failFiles.add(name);
          countFail++;
        }
      }
    }

    for (File f : flist) {
      if (f.isDirectory() && !f.getName().equals("problem") && !f.getName().equals("exclude"))
        openAllInDir(f.getAbsolutePath(), ff);
    }

  }

}
