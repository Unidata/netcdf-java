/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;

/** Compare reading netcdf with Array */
public class TestReadArrayCompare {

  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc .mip222k.oschp");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/tst/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/samples/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/support/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/complex/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/wrf/", ff, result);

      if (Boolean.getBoolean("runSlowTests")) {
        TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf4/", ff, result, false);
        TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/", ff, result, false);
        TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/xmdf/", ff, result);
      }

      result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/MYD29.A2009152.0000.005.2009153124331.hdf"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc"});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/grib1/SST_Global_5x2p5deg_20071119_0000.grib1"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/grib2/ds.wdir.bin"});
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadArrayCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  public void compareNetcdfFile() throws IOException {
    compareNetcdfFile(filename);
  }

  public static long compareNetcdfFile(String filename) throws IOException {
    long total = 0;
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("compareNetcdfFile: " + ncfile.getLocation());

      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        System.out.printf("  read variable %s %s", v.getDataType(), v.getShortName());
        com.google.common.base.Stopwatch stopwatch = Stopwatch.createStarted();
        ucar.ma2.Array org = v.read();
        try {
          Array<?> array = v.readArray();
          System.out.printf("  COMPARE%n");
          Formatter f = new Formatter();
          boolean ok1 = CompareArrayToMa2.compareData(f, v.getShortName(), org, array, false, true);
          if (!ok1) { // array not ok
            System.out.printf("%s%n", f);
          } else {
            stopwatch.stop();
            long size = array.length() + org.getSize();
            double rate = ((double) array.length()) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
            System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, stopwatch, rate);
            total += size;
          }
          ok &= ok1;
        } catch (Exception e) {
          System.out.printf(" BAD%n");
          e.printStackTrace();
          ok = false;
        }
      }
      assertThat(ok).isTrue();
    } catch (FileNotFoundException e) {
      File file = new File(filename);
      System.out.printf("File.getAbsolutePath = %s%n", file.getAbsolutePath());
      throw e;
    }
    return total;
  }

  public static void compareNetcdfDataset(String filename) throws IOException {
    try (NetcdfDataset ncfile =
        NetcdfDatasets.openDataset(filename, true, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("compareNetcdfDataset: " + ncfile.getLocation());

      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        ucar.ma2.Array org = v.read();
        Array<?> array = v.readArray();
        if (array != null) {
          System.out.printf("  check %s %s%n", v.getDataType(), v.getNameAndDimensions());
          Formatter f = new Formatter();
          boolean ok1 = CompareArrayToMa2.compareData(f, v.getShortName(), org, array, false, true);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      assertThat(ok).isTrue();
    }
  }

  private static boolean compareStructureData(Formatter f, StructureData org, ucar.array.StructureData array,
      boolean justOne) throws IOException {
    boolean ok = true;

    StructureMembers sm1 = org.getStructureMembers();
    ucar.array.StructureMembers sm2 = array.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" membersize %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      ucar.array.StructureMembers.Member m2 = sm2.findMember(m1.getName());
      if (m2 == null) {
        System.out.printf("Cant find %s in copy%n", m1.getName());
        continue;
      }
      ucar.ma2.Array data1 = org.getArray(m1);
      ucar.array.Array<?> data2 = array.getMemberData(m2);
      if (data1 != null && data2 != null) {
        f.format("    compare member %s %s%n", m1.getDataType(), m1.getName());
        ok &= CompareArrayToMa2.compareData(f, m1.getName(), data1, data2, justOne, false);
      } else {
        f.format("    %s data MISSING %s %s%n", m1.getName(), data1 != null, data2 != null);
      }
    }

    return ok;
  }

  static boolean compareSequence(Formatter f, String name, StructureDataIterator org,
      Iterator<ucar.array.StructureData> array) throws IOException {
    boolean ok = true;
    int obsrow = 0;
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

}

