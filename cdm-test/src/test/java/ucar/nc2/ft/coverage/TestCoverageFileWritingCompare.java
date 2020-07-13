/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import java.util.Optional;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Compare CFGridCoverageWriter and CFGridCoverageWriter2
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCoverageFileWritingCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // SRC
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1",
        FeatureType.GRID, Lists.newArrayList("Temperature_isobaric"),
        new SubsetParams().set(SubsetParams.timePresent, true), NetcdfFileFormat.NETCDF3});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID,
        Lists.newArrayList("P_sfc", "P_trop"), null, NetcdfFileFormat.NETCDF3});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID,
        Lists.newArrayList("P_sfc", "P_trop", "T"), null, NetcdfFileFormat.NETCDF3});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", FeatureType.GRID,
        Lists.newArrayList("Surface_pressure_surface"), null, NetcdfFileFormat.NETCDF3}); // scalar runtime, ens
    // coord
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID,
        Lists.newArrayList("PS", "Temperature"), null, NetcdfFileFormat.NETCDF3}); // both x,y and lat,lon

    /// netcdf4
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID,
        Lists.newArrayList("P_sfc", "P_trop"), null, NetcdfFileFormat.NETCDF4});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
        Lists.newArrayList("Soil_temperature_depth_below_surface_layer"), null, NetcdfFileFormat.NETCDF4}); // TwoD

    return result;
  }

  private final String endpoint;
  private final List<String> covList;
  private final NetcdfFileFormat format;
  private final FeatureType type;
  private final SubsetParams params;

  public TestCoverageFileWritingCompare(String endpoint, FeatureType type, List<String> covList, SubsetParams params,
      NetcdfFileFormat format) {
    this.endpoint = endpoint;
    this.type = type;
    this.covList = covList;
    this.format = format;
    this.params = (params != null) ? params : new SubsetParams();
  }

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void writeTestFile() throws IOException, InvalidRangeException {
    // skip test requiring netcdf4 if not present.
    NetcdfFileWriter.Version version = NetcdfFormatWriter.convertToNetcdfFileWriterVersion(format);
    if (version.useJniIosp() && !NetcdfClibrary.isLibraryPresent()) {
      return;
    }
    System.out.printf("Test Dataset %s type %s%n", endpoint, type);
    File tempFile = tempFolder.newFile();
    File tempFile2 = tempFolder.newFile();

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(type);

      for (String covName : covList) {
        Assert.assertNotNull(covName, gcs.findCoverage(covName));
      }

      // write the file using CFGridCoverageWriter2
      System.out.printf(" CFGridCoverageWriter2 write to %s%n", tempFile2.getAbsolutePath());
      Formatter errLog = new Formatter();
      try (NetcdfFileWriter writer = NetcdfFileWriter
          .createNew(NetcdfFormatWriter.convertToNetcdfFileWriterVersion(format), tempFile2.getPath(), null)) {
        Optional<Long> estimatedSizeo = CFGridCoverageWriter2.write(gcs, covList, params, false, writer, errLog);
        if (!estimatedSizeo.isPresent()) {
          throw new InvalidRangeException("Request contains no data: " + errLog.toString());
        }
      }

      // write the file using CFGridCoverageWriter
      System.out.printf(" CFGridCoverageWriter write to %s%n", tempFile.getAbsolutePath());
      NetcdfFormatWriter.Builder writerb =
          NetcdfFormatWriter.builder().setNewFile(true).setFormat(format).setLocation(tempFile.getPath());
      CFGridCoverageWriter.Result result = CFGridCoverageWriter.write(gcs, covList, params, false, writerb, 0);
      if (!result.wasWritten()) {
        throw new InvalidRangeException("Error writing: " + result.getErrorMessage());
      }
    }

    // Compare that the files are identical
    try (NetcdfFile org = NetcdfFile.open(tempFile2.getPath())) {
      try (NetcdfFile copy = NetcdfFiles.open(tempFile.getPath())) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, copy, new FileWritingObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  public static class FileWritingObjFilter implements ObjFilter {
    public boolean attCheckOk(Variable v, Attribute att) {
      if (att.getName().equals("_ChunkSizes"))
        return false;
      if (!att.isString())
        return true;
      String val = att.getStringValue();
      if (val == null)
        return true;
      return !val.contains("Translation Date");
    }
  }

}
