package ucar.nc2.grib.coord;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test SparseArray in cdmUnitTestDir test files. */
public class TestSparseArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final FeatureCollectionConfig config = new FeatureCollectionConfig();

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testShowFromPartition() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4";
    String vname = "Maximum_relative_humidity_height_above_ground_12_Hour_Maximum";
    String sa = testShowSparseArray(filename, vname);
    assertThat(sa).isNotNull();
    // TODO should start with 10 XXXXXXXXXX instead of 36. ??
    assertThat(sa).contains("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testShowFromFirstCollection() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/National_Blend_Alaska_20201027_0100.grib2.ncx4";
    String vname = "Maximum_relative_humidity_height_above_ground_12_Hour_Maximum";
    String sa = testShowSparseArray(filename, vname);
    assertThat(sa).isNotNull();
    assertThat(sa).isEqualTo("XXXXXXXXXX\n");
  }

  private String testShowSparseArray(String filename, String vname) throws IOException {
    System.out.printf("testShowSparseArray %s%n", filename);
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(filename, config, false, logger)) {
      assertThat(gc).isNotNull();
      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        if (ds.getType() == GribCollectionImmutable.Type.Best) {
          continue;
        }
        GribCollectionImmutable.GroupGC g = Iterables.getOnlyElement(ds.getGroups());
        GribCollectionImmutable.VariableIndex vi =
            g.getVariables().stream().filter(v -> v.makeVariableName().equals(vname)).findFirst().orElseThrow();
        return showSparseArray(vi);
      }
    }
    return null;
  }

  private String showSparseArray(GribCollectionImmutable.VariableIndex v) {
    Formatter f = new Formatter();
    try {
      if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
        PartitionCollectionImmutable.VariableIndexPartitioned vip =
            (PartitionCollectionImmutable.VariableIndexPartitioned) v;

        vip.readRecords();
        vip.showSparseArray(f);

      } else {
        v.readRecords();
        if (v.getSparseArray() != null) {
          SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
          sa.showMissing(f);
        }
      }
    } catch (IOException e) {
      f.format("Failed to showSparseArray for variable %s%n", v);
    }
    System.out.printf("%s%n", f);
    return f.toString();
  }
}
