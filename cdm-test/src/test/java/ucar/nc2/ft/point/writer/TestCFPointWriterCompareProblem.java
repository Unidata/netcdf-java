/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.writer;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.ft.point.TestCFPointDatasets.CFpointObs_topdir;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import ucar.nc2.write.NetcdfFormatWriter;

public class TestCFPointWriterCompareProblem {
  String outDir = "C:/temp/";
  String location = CFpointObs_topdir + "stationData2Levels.ncml";
  String outOrg = outDir + "stationData2Levels.org.nc";
  String outNew = outDir + "stationData2Levels.new.nc";

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWrite() throws IOException {
    writeDataset(location, FeatureType.STATION, NetcdfFileWriter.Version.netcdf3, -1);
  }

  @Test
  public void testWrite4() throws IOException {
    writeDataset(location, FeatureType.STATION, Version.netcdf4_classic, -1);
  }

  private void writeDataset(String location, FeatureType ftype, NetcdfFileWriter.Version version, int countExpected)
      throws IOException {
    File fileIn = new File(location);
    long start = System.currentTimeMillis();
    outOrg = tempFolder.newFile().getPath();
    outNew = tempFolder.newFile().getPath();

    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s%n", fileIn.getAbsoluteFile(),
        fileIn.length(), outOrg);

    // open point dataset
    Formatter out = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(ftype, location, null, out)) {
      if (fdataset == null) {
        System.out.printf("**failed on %s %n --> %s %n", location, out);
        assert false;
      }
      assert fdataset instanceof FeatureDatasetPoint;
      FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

      CFPointWriterConfig configOrg = new CFPointWriterConfig(version);
      int count = CFPointWriter.writeFeatureCollection(fdpoint, outOrg, configOrg);
      long took = System.currentTimeMillis() - start;
      System.out.printf(" CFPointWriter nrecords written = %d took=%d msecs%n%n", count, took);
      if (countExpected > 0) {
        assert count == countExpected : "count =" + count + " expected " + countExpected;
      }
    }

    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(ftype, location, null, out)) {
      FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
      ucar.nc2.ft.point.writer2.CFPointWriterConfig configNew = ucar.nc2.ft.point.writer2.CFPointWriterConfig.builder()
          .setFormat(NetcdfFormatWriter.convertToNetcdfFileFormat(version)).build();
      int countNew = ucar.nc2.ft.point.writer2.CFPointWriter.writeFeatureCollection(fdpoint, outNew, configNew);
      long tookNew = System.currentTimeMillis() - start;
      System.out.printf(" CFPointWriterNew nrecords written = %d took=%d msecs%n%n", countNew, tookNew);
      if (countExpected > 0) {
        assert countNew == countExpected : "count =" + countNew + " expected " + countExpected;
      }

      compare(outOrg, outNew);
    }
  }

  private static void compare(String fileOrg, String fileNew) throws IOException {
    // Compare that the files are identical
    try (NetcdfFile org = NetcdfFile.open(fileOrg); NetcdfFile copy = NetcdfFiles.open(fileNew)) {
      Formatter f = new Formatter();
      CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
      boolean ok = compare.compare(org, copy, new FileWritingObjFilter());
      System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
      assertThat(ok).isTrue();
    }
  }

  public static class FileWritingObjFilter implements ObjFilter {
    public boolean attCheckOk(Variable v, Attribute att) {
      if (att.getName().equals("_ChunkSizes")) {
        return false;
      }
      if (!att.isString()) {
        return true;
      }
      String val = att.getStringValue();
      if (val == null) {
        return true;
      }
      return !val.contains("Translation Date");
    }
  }

}
