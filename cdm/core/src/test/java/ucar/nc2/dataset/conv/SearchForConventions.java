/* Copyright Unidata */
package ucar.nc2.dataset.conv;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.CoordSysBuilderIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class SearchForConventions {
  private static final String tempDir = "~/tmp/";
  private static final List<String> testDirs =
      ImmutableList.of(TestDir.cdmUnitTestDir + "/conventions", TestDir.cdmUnitTestDir + "/ft");
  private static Multimap<String, String> convMap = ArrayListMultimap.create();
  private static Multimap<String, String> builderMap = ArrayListMultimap.create();

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      for (String dir : testDirs) {
        TestDir.actOnAllParameterized(dir, (file) -> file.getPath().endsWith(".nc"), filenames, true);
        // TestDir.actOnAllParameterized(dir, null, filenames, true);
      }
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  @AfterClass
  public static void showResults() throws IOException {
    FileWriter out = new FileWriter(tempDir + "conventions.txt");
    showResults(out, convMap);
    out.close();

    out = new FileWriter(tempDir + "builder.txt");
    showResults(out, builderMap);
    out.close();
  }

  private static void showResults(FileWriter out, Multimap<String, String> mmap) throws IOException {
    List<String> keys = mmap.asMap().keySet().stream().sorted().collect(Collectors.toList());
    for (String key : keys) {
      out.write(String.format("%n%s%n", key));
      int count = 0;
      for (String filename : mmap.get(key)) {
        out.write(String.format("   %s%n", filename));
        if (count > 5)
          break;
        count++;
      }
    }
  }

  private String filename;

  public SearchForConventions(String filename) {
    this.filename = filename;
  }

  @Test
  @Ignore("Not a test - really a utility program")
  public void findConventions() {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      System.out.printf("%s%n", filename);
      String convName = ncfile.getRootGroup().attributes().findAttValueIgnoreCase("Conventions", null);
      if (convName == null)
        convName = ncfile.getRootGroup().attributes().findAttValueIgnoreCase("Convention", null);
      if (convName != null) {
        convMap.put(convName, filename);
      }

      // Now let CoordSystemFactory have a try
      NetcdfDataset.Builder builder = NetcdfDataset.builder(ncfile);
      Optional<CoordSystemBuilder> hasNewBuilder = CoordSystemFactory.factory(builder, null);
      if (hasNewBuilder.isPresent()) {
        builderMap.put(hasNewBuilder.get().getClass().getName(), filename);
      } else {
        // If fail, let CoordSysBuilder have a try
        NetcdfDataset ds = NetcdfDataset.openDataset(ncfile.getLocation(), false, null); // fresh enhancement
        CoordSysBuilderIF old = ds.enhance();
        builderMap.put(old.getClass().getName(), filename);
      }

    } catch (Throwable t) {
      System.out.printf("****Failed on %s (%s)%n", filename, t.getMessage());
    }
  }
}

/*
 * CF-1.0
 * D:/cdmUnitTest/conventions/avhrr/20080509-NCDC-L4LRblend-GLOB-v01-fv01_0-AVHRR_AMSR_OI.nc
 * D:/cdmUnitTest/conventions/cf/bora_feb_001.nc
 * D:/cdmUnitTest/conventions/cf/bora_feb_002.nc
 * D:/cdmUnitTest/conventions/cf/ccsm2.nc
 * D:/cdmUnitTest/conventions/cf/cf1.nc
 * D:/cdmUnitTest/conventions/cf/cf1_rap.nc
 * D:/cdmUnitTest/conventions/cf/fcst_int.20030424.i1502.f0058.nc
 * D:/cdmUnitTest/conventions/cf/feb2003_short2.nc
 * D:/cdmUnitTest/conventions/cf/gomoos_cf.nc
 * D:/cdmUnitTest/conventions/cf/mississippi.nc
 * D:/cdmUnitTest/conventions/cf/roms_sample.nc
 * D:/cdmUnitTest/conventions/cf/sampleCurveGrid2.nc
 * D:/cdmUnitTest/conventions/cf/temperature.nc
 * D:/cdmUnitTest/conventions/cf/tomw.nc
 * D:/cdmUnitTest/conventions/cf/twoGridMaps.nc
 * D:/cdmUnitTest/conventions/cf/year0.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/cl_A1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/hfogo_O1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/pr_A1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/sftlf_A1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/so_O1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/stfmmc_O1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/ta_A1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/tas_A1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/tos_O1.nc
 * D:/cdmUnitTest/conventions/cf/ipcc/zobt_O1.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.control_Gmosf.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.control_ll10.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.cradth2o_ll20.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.fw0.0Sv.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.tradch2o_ll10.nc
 * D:/cdmUnitTest/conventions/cf/jonathan/fixed.transient_ll10.nc
 * D:/cdmUnitTest/conventions/cf/signell/feb2003_short2.nc
 * D:/cdmUnitTest/conventions/cf/signell/signell_bathy_fixed.nc
 * D:/cdmUnitTest/conventions/cf/signell/signell_fixed.nc
 * D:/cdmUnitTest/conventions/coards/sst.mnmean.nc
 * 
 * CF/Radial instrument_parameters radar_parameters radar_calibration
 * D:/cdmUnitTest/conventions/cfradial/cfrad.20140608_220305.809_to_20140608_220710.630_KFTG_v348_Surveillance_SUR.nc
 * D:/cdmUnitTest/conventions/cfradial/cfrad.20140717_003008.286_to_20140717_003049.699_SPOL_v140_rhi_sim_RHI.nc
 * D:/cdmUnitTest/conventions/cfradial/cfrad.20171127_202111.203_to_20171127_202123.085_DOW7_v275_s04_el7.00_SUR.nc
 * 
 * CF/Radial instrument_parameters radar_parameters radar_calibration geometry_correction
 * D:/cdmUnitTest/conventions/cfradial/cfrad.20080604_002217_000_SPOL_v36_SUR.nc
 * 
 * COARDS
 * D:/cdmUnitTest/conventions/coards/air.2001.nc
 * D:/cdmUnitTest/conventions/coards/cldc.mean.nc
 * D:/cdmUnitTest/conventions/coards/inittest24.QRIDV07200.nc
 * D:/cdmUnitTest/conventions/coards/lflx.mean.nc
 * D:/cdmUnitTest/conventions/coards/olr.day.mean.nc
 * D:/cdmUnitTest/conventions/coards/testUnion.air.nc
 * 
 * GDV
 * D:/cdmUnitTest/conventions/gdv/OceanDJF.nc
 * D:/cdmUnitTest/conventions/gdv/testGDV.nc
 * 
 * GIEF/GIEF-F
 * D:/cdmUnitTest/conventions/gief/coamps.wind_uv.nc
 * 
 * MARS
 * D:/cdmUnitTest/conventions/mars/temp_air_01082000.nc
 * 
 * NCAR-CSM
 * D:/cdmUnitTest/conventions/csm/atmos.tuv.monthly.nc
 * D:/cdmUnitTest/conventions/csm/ha0001.nc
 * D:/cdmUnitTest/conventions/csm/o3monthly.nc
 * 
 * NUWG
 * D:/cdmUnitTest/conventions/nuwg/03061219_ruc.nc
 * D:/cdmUnitTest/conventions/nuwg/050231700.lsxmod.nc
 * D:/cdmUnitTest/conventions/nuwg/2003021212_avn-x.nc
 * D:/cdmUnitTest/conventions/nuwg/avn-q.nc
 * D:/cdmUnitTest/conventions/nuwg/CMC-HGT.nc
 * D:/cdmUnitTest/conventions/nuwg/eta.nc
 * D:/cdmUnitTest/conventions/nuwg/ocean.nc
 * D:/cdmUnitTest/conventions/nuwg/ruc.nc
 * 
 * NUWG, CF, ADN
 * D:/cdmUnitTest/conventions/nuwg/avn-x.nc
 * 
 * Zebra lat-lon-alt convention
 * D:/cdmUnitTest/conventions/atd-radar/SPOL_3Volumes.nc
 * D:/cdmUnitTest/conventions/zebra/SPOL_3Volumes.nc
 * 
 * _Coordinates
 * D:/cdmUnitTest/conventions/cf/SUPER-NATIONAL_latlon_IR_20070222_1600.nc
 */
