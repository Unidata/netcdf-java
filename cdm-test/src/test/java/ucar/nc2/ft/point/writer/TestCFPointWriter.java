package ucar.nc2.ft.point.writer;


import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.CompareNetcdf2;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static com.google.common.truth.Truth.*;

@Ignore("These tests target changes to CF Point Writer that haven't been completed/merged")
@RunWith(Parameterized.class)
public class TestCFPointWriter {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();
  private static final String COLLECTION_STRING = "Collection";
  private static final String EXT = ".ncml";


  @Parameterized.Parameters(name = "{0}/{1}/{2}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> paramMatrix = new ArrayList<>();

    // 6 feature types: point, profile, station, station profile, trajectory, trajectory profile
    Object[][] featureTypes = new Object[][] {{FeatureType.POINT, "point"}, {FeatureType.PROFILE, "profile"},
        {FeatureType.STATION, "station"}, {FeatureType.STATION_PROFILE, "stationProfile"},
        {FeatureType.TRAJECTORY, "traj"}, {FeatureType.TRAJECTORY_PROFILE, "trajProfile"}};

    // 4 ncml variations: single collection, multiple collections, unlimited dimension, extended model features
    String[] ncmlVars = new String[] {"", "Record", "Multiple", "Ext"};

    // 3 file versions: nc3, nc4, nc4ext
    NetcdfFileWriter.Version[] versions = new NetcdfFileWriter.Version[] {NetcdfFileWriter.Version.netcdf3,
        NetcdfFileWriter.Version.netcdf4_classic, NetcdfFileWriter.Version.netcdf4};

    for (int ft = 0; ft < featureTypes.length; ft++) {
      for (int var = 0; var < ncmlVars.length; var++) {
        for (int v = 0; v < versions.length; v++) {
          paramMatrix.add(new Object[] {featureTypes[ft][0],
              featureTypes[ft][1] + COLLECTION_STRING + ncmlVars[var] + EXT, versions[v]});
        }
      }
    }

    return paramMatrix;
  }

  private final FeatureType wantedType;
  private final String filePath;
  private final NetcdfFileWriter.Version version;

  public TestCFPointWriter(FeatureType wantedType, String filePath, NetcdfFileWriter.Version version) {
    this.wantedType = wantedType;
    this.filePath = filePath;
    this.version = version;
  }

  @Test
  public void testWritePointFeatureType() throws IOException, URISyntaxException {
    File datasetFile = new File(this.getClass().getResource("input/" + filePath).toURI());
    File outFile = tempFolder.newFile();
    FeatureDatasetPoint fdPoint = openPointDataset(wantedType, datasetFile);
    CFPointWriter.writeFeatureCollection(fdPoint, outFile.getAbsolutePath(), version);
    assertThat(compareNetCDF(datasetFile, outFile)).isTrue();
  }

  private static FeatureDatasetPoint openPointDataset(FeatureType wantedType, File datasetFile) throws IOException {
    Formatter errlog = new Formatter();
    FeatureDataset fDset = FeatureDatasetFactoryManager.open(wantedType, datasetFile.getAbsolutePath(), null, errlog);
    return (FeatureDatasetPoint) fDset;
  }

  private static boolean compareNetCDF(File expectedResultFile, File actualResultFile) throws IOException {
    try (NetcdfFile expectedNcFile = NetcdfDatasets.openDataset(expectedResultFile.getAbsolutePath());
        NetcdfFile actualNcFile = NetcdfDatasets.openDataset(actualResultFile.getAbsolutePath())) {
      Formatter formatter = new Formatter();
      CompareNetcdf2 compareNetcdf2 = new CompareNetcdf2(formatter, true, false, true, true);
      boolean contentsAreEqual = compareNetcdf2.compare(expectedNcFile, actualNcFile, new CFObjFilter());
      if (!contentsAreEqual) {
        System.err.println(formatter);
      }
      return contentsAreEqual;
    }
  }

  private static class CFObjFilter implements CompareNetcdf2.ObjFilter {
    private static Map<String, List<String>> ignore;
    static {
      ignore = new HashMap<>();
      ArrayList global = new ArrayList();
      global.add(CDM.HISTORY);
      global.add("time_coverage_start");
      global.add("time_coverage_end");
      global.add("geospatial_lat_min");
      global.add("geospatial_lon_min");
      global.add("geospatial_lat_max");
      global.add("geospatial_lon_max");
      global.add("DSG_representation");
      global.add("_NCProperties");
      global.add("Conventions");
      ignore.put("global", global);
      ArrayList time = new ArrayList();
      time.add("calendar");
      time.add("units");
      ignore.put("time", time);
    }

    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      String name = v == null ? "global" : v.getFullName();
      return ignore.getOrDefault(name, new ArrayList<>()).stream().noneMatch(s -> s.equals(att.getShortName()));
    }
  }

}
