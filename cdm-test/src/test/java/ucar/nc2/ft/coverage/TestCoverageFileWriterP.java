/* Copyright */
package ucar.nc2.ft.coverage;

import com.google.common.collect.Lists;
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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Test CFGridCoverageWriter */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCoverageFileWriterP {
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
        Lists.newArrayList("P_sfc", "P_trop"), null, NetcdfFileFormat.NETCDF4});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID,
        Lists.newArrayList("P_sfc", "P_trop", "T"), null, NetcdfFileFormat.NETCDF3});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", FeatureType.GRID,
        Lists.newArrayList("Surface_pressure_surface"), null, NetcdfFileFormat.NETCDF3}); // scalar runtime, ens
                                                                                          // coord
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID,
        Lists.newArrayList("PS", "Temperature"), null, NetcdfFileFormat.NETCDF3}); // both x,y and lat,lon

    // TwoD Best
    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
        Lists.newArrayList("Soil_temperature_depth_below_surface_layer"), null, NetcdfFileFormat.NETCDF4});

    return result;
  }

  private String endpoint;
  private List<String> covList;
  private NetcdfFileFormat version;
  private FeatureType type;
  private SubsetParams params;

  public TestCoverageFileWriterP(String endpoint, FeatureType type, List<String> covList, SubsetParams params,
      NetcdfFileFormat version) {
    this.endpoint = endpoint;
    this.type = type;
    this.covList = covList;
    this.version = version;
    this.params = (params != null) ? params : new SubsetParams();
  }

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void writeTestFile() throws IOException, InvalidRangeException {
    // skip test requiring netcdf4 if not present.
    if (version.isNetdf4format() && !NetcdfClibrary.isLibraryPresent()) {
      return;
    }
    System.out.printf("Test Dataset %s type %s%n", endpoint, type);
    File tempFile = tempFolder.newFile();
    System.out.printf(" write to %s%n", tempFile.getAbsolutePath());

    // write the file
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(type);

      for (String covName : covList)
        Assert.assertNotNull(covName, gcs.findCoverage(covName));

      NetcdfFormatWriter.Builder writer =
          NetcdfFormatWriter.builder().setNewFile(true).setLocation(tempFile.getPath()).setFormat(version);
      CFGridCoverageWriter.Result result = CFGridCoverageWriter.write(gcs, covList, params, false, writer, -1);
      if (!result.wasWritten())
        throw new InvalidRangeException("Request failed: " + result.getErrorMessage());
    }

    // open the new file as a Coverage. Since its a netcdf file, it will open through the DtAdapter (!)
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(tempFile.getPath())) {
      Assert.assertNotNull(endpoint, cc);
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gcs = cc.getCoverageCollections().get(0);

      for (String covName : covList) {
        Assert.assertNotNull(covName, gcs.findCoverage(covName));
      }
    }

    // open the new file as a Grid
    try (GridDataset gds = GridDataset.open(tempFile.getPath())) {
      Assert.assertNotNull(tempFile.getPath(), gds);

      for (String covName : covList) {
        Assert.assertNotNull(covName, gds.findGridByName(covName));
      }
    }

    // open the file as old style Grid
    try (NetcdfDataset nf = NetcdfDatasets.openDataset(tempFile.getPath())) {
      ucar.nc2.dt.grid.GridDataset dtDataset = new ucar.nc2.dt.grid.GridDataset(nf);
      for (String covName : covList)
        Assert.assertNotNull(covName, dtDataset.findGridByName(covName));
    }

  }

}
