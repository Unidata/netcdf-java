package ucar.nc2;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

public class TestFailingTest {

  @Test
  public void testWrite4Classic() throws IOException {
    String location = "..\\cdm\\core\\src\\test\\data\\point\\point.ncml";
    FeatureType ftype = FeatureType.POINT;
    int countExpected = 3;
    writeDataset(location, ftype, NetcdfFileWriter.Version.netcdf4_classic, countExpected);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void writeDataset(String location, FeatureType ftype, NetcdfFileWriter.Version version, int countExpected)
          throws IOException {

    TemporaryFolder tempFolder = new TemporaryFolder();
    tempFolder.create();
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

  public static class FileWritingObjFilter implements CompareNetcdf2.ObjFilter {
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
