/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.writer;

import static com.google.common.truth.Truth.assertThat;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ucar.nc2.ft.point.TestCFPointDatasets;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Test CFPointWriter, write into nc, nc4 and nc4c (classic) files
 * C:/dev/github/thredds/cdm/target/test/tmp/stationRaggedContig.ncml.nc4
 *
 * @author caron
 * @since 4/11/12
 */
@RunWith(Parameterized.class)
public class TestCFPointWriterCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(TestCFPointDatasets.getPointDatasets());
    result.addAll(TestCFPointDatasets.getStationDatasets());
    result.addAll(TestCFPointDatasets.getProfileDatasets());
    result.addAll(TestCFPointDatasets.getTrajectoryDatasets());
    result.addAll(TestCFPointDatasets.getStationProfileDatasets());
    result.addAll(TestCFPointDatasets.getSectionDatasets());

    return result;
  }

  String location;
  FeatureType ftype;
  int countExpected;
  boolean show = false;

  public TestCFPointWriterCompare(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  @Test
  public void testWrite3() throws IOException {
    writeDataset(location, ftype, NetcdfFileWriter.Version.netcdf3, countExpected);
  }

  @Test
  public void testWrite4Classic() throws IOException {
    // Ignore this test if NetCDF-4 isn't present.
    if (!Nc4Iosp.isClibraryPresent()) {
      return;
    }
    writeDataset(location, ftype, Version.netcdf4_classic, countExpected);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void writeDataset(String location, FeatureType ftype, NetcdfFileWriter.Version version, int countExpected)
      throws IOException {
    File fileIn = new File(location);
    File fileOrg = tempFolder.newFile();
    long start = System.currentTimeMillis();

    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s%n", fileIn.getAbsoluteFile(),
        fileIn.length(), fileOrg.getAbsoluteFile());

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
      int count = CFPointWriter.writeFeatureCollection(fdpoint, fileOrg.getPath(), configOrg);
      long took = System.currentTimeMillis() - start;
      System.out.printf(" CFPointWriter nrecords written = %d took=%d msecs %s%n", count, took,
          fdpoint.getFeatureType());
      assert count == countExpected : "count =" + count + " expected " + countExpected;
    }

    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(ftype, location, null, out)) {
      FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
      File fileNew = tempFolder.newFile();
      ucar.nc2.ft.point.writer2.CFPointWriterConfig configNew = ucar.nc2.ft.point.writer2.CFPointWriterConfig.builder()
          .setFormat(NetcdfFormatWriter.convertToNetcdfFileFormat(version)).build();
      int countNew =
          ucar.nc2.ft.point.writer2.CFPointWriter.writeFeatureCollection(fdpoint, fileNew.getPath(), configNew);
      long tookNew = System.currentTimeMillis() - start;
      System.out.printf(" CFPointWriterNew nrecords written = %d took=%d msecs%n", countNew, tookNew);
      assert countNew == countExpected : "count =" + countNew + " expected " + countExpected;

      compare(fileOrg.getPath(), fileNew.getPath());
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
