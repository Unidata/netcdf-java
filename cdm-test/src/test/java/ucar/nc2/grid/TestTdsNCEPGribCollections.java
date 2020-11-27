/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Compare reading TDS Grib Collections with old and new GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestTdsNCEPGribCollections {
  private static final String topDir = TestDir.cdmUnitTestDir + "tds_index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".gbx9");
    List<Object[]> result = new ArrayList<>(500);
    try {
      /// NCEP
      // gefs
      result.add(new Object[] {
          topDir + "NCEP/GEFS/Global_1p0deg_Ensemble/member/GEFS-Global_1p0deg_Ensemble-members.ncx4", 36, 12, 15});
      result.add(new Object[] {
          topDir + "NCEP/GEFS/Global_1p0deg_Ensemble/derived/GEFS-Global_1p0deg_Ensemble-derived_products.ncx4", 72, 12,
          14});
      result.add(new Object[] {
          topDir + "NCEP/GEFS/Global_1p0deg_Ensemble/member_analysis/GEFS-Global_1p0deg_Ensemble-members-analysis.ncx4",
          12, 6, 10});

      // gfs
      result.add(new Object[] {topDir + "NCEP/GFS/Alaska_20km/GFS-Alaska_20km.ncx4", 52, 12, 14});
      result.add(new Object[] {topDir + "NCEP/GFS/CONUS_20km/GFS-CONUS_20km.ncx4", 52, 12, 14});
      result.add(new Object[] {topDir + "NCEP/GFS/CONUS_80km/GFS-CONUS_80km.ncx4", 31, 12, 16});
      result.add(new Object[] {topDir + "NCEP/GFS/Global_0p5deg/GFS-Global_0p5deg.ncx4", 157, 30, 32});
      result.add(
          new Object[] {topDir + "NCEP/GFS/Global_onedeg_noaaport/GFS-Global_onedegree_noaaport.ncx4", 40, 12, 15});
      result.add(new Object[] {topDir + "NCEP/GFS/Pacific_20km/GFS-Pacific_20km.ncx4", 52, 14, 15});
      result.add(new Object[] {topDir + "NCEP/GFS/Puerto_Rico_0p25deg/GFS-Puerto_Rico_0p25deg.ncx4", 52, 12, 14});
      result.add(new Object[] {topDir + "NCEP/GFS/Global_0p25deg/GFS-Global_0p25deg.ncx4", 157, 30, 32});
      result.add(new Object[] {topDir + "NCEP/GFS/Global_onedeg/GFS-Global_onedeg.ncx4", 157, 30, 32});

      // hrrrs
      result.add(new Object[] {topDir + "NCEP/HRRR/CONUS_2p5km/NCEP_HRRR_CONUS_2p5km.ncx4", 55, 20, 22});
      result.add(
          new Object[] {topDir + "NCEP/HRRR/CONUS_2p5km_Analysis/NCEP_HRRR_CONUS_2p5km_Analysis.ncx4", 53, 19, 22});

      // mrms
      result.add(new Object[] {topDir + "NCEP/MRMS/BaseRef/MRMS-BaseRef.ncx4", 1, 1, 5});
      result.add(new Object[] {topDir + "NCEP/MRMS/Model/MRMS-Model.ncx4", 2, 2, 10});
      result.add(new Object[] {topDir + "NCEP/MRMS/NLDN/MRMS-NLDN.ncx4", 6, 6, 16});
      result.add(new Object[] {topDir + "NCEP/MRMS/Precip/MRMS-Precip.ncx4", 39, 20, 43});
      result.add(new Object[] {topDir + "NCEP/MRMS/Radar/MRMS-Radar.ncx4", 19, 19, 41});
      result.add(new Object[] {topDir + "NCEP/MRMS/RotationTrackML/MRMS-RotationTracksML.ncx4", 2, 2, 7});
      result.add(new Object[] {topDir + "NCEP/MRMS/Radar/MRMS-Radar.ncx4", 19, 19, 41}); // */

      // nam
      result.add(new Object[] {topDir + "NCEP/NAM/Alaska_45km/noaaport/NAM-Alaska_45km-noaaport.ncx4", 21, 6, 9});
      result.add(new Object[] {topDir + "NCEP/NAM/Alaska_95km/NAM-Alaska_95km.ncx4", 29, 12, 15});
      result.add(new Object[] {topDir + "NCEP/NAM/CONUS_12km/NAM-CONUS_12km-noaaport.ncx4", 59, 18, 21});
      result.add(new Object[] {topDir + "NCEP/NAM/CONUS_80km/NAM-CONUS_80km.ncx4", 41, 11, 14});
      result.add(new Object[] {topDir + "NCEP/NAM/Alaska_11km/NAM-Alaska_11km.ncx4", 59, 18, 21});
      result.add(new Object[] {topDir + "NCEP/NAM/CONUS_20km/noaaport/NAM-CONUS_20km-noaaport.ncx4", 12, 5, 8});

      // Individual runtimes are SRC, and are fine
      result.add(new Object[] {topDir + "NCEP/NAM/Firewxnest/NAM_Firewxnest_20201010_0000.grib2.ncx4", 214, 38, 38});

      // No projection found
      // Firewxnest partition has moving grids (3).
      // see testFirewxnest()
      // result.add(new Object[]{topDir + "NCEP/NAM/Firewxnest/NAM-Firewxnest.ncx4", });

      // non-orthogonal regular time offset
      result.add(new Object[] {topDir + "NCEP/NAM/Alaska_45km/conduit/NAM-Alaska_45km-conduit.ncx4", 157, 35, 38});
      result.add(new Object[] {topDir + "NCEP/NAM/CONUS_12km_conduit/NAM-CONUS_12km-conduit.ncx4", 159, 31, 32});
      result.add(new Object[] {topDir + "NCEP/NAM/CONUS_40km/conduit/NAM-CONUS_40km-conduit.ncx4", 179, 31, 34});
      result.add(new Object[] {topDir + "NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4", 133, 29, 32}); // */

      // nbm
      // Individual runtimes are SRC, and are fine
      result.add(new Object[] {topDir + "NCEP/NBM/Alaska/National_Blend_Alaska_20201010_0000.grib2.ncx4", 26, 16, 18});
      result.add(new Object[] {topDir + "NCEP/NBM/CONUS/National_Blend_CONUS_20201010_0000.grib2.ncx4", 26, 16, 19});
      result.add(new Object[] {topDir + "NCEP/NBM/Hawaii/National_Blend_Hawaii_20201010_1200.grib2.ncx4", 16, 12, 15});
      result.add(
          new Object[] {topDir + "NCEP/NBM/PuertoRico/National_Blend_PuertoRico_20201010_0600.grib2.ncx4", 12, 4, 7});

      // nbm collection regular timeOffset
      result.add(new Object[] {topDir + "NCEP/NBM/Hawaii/NCEP_HAWAII_MODEL_BLEND.ncx4", 18, 17, 27}); // TODO fishy
      result.add(new Object[] {topDir + "NCEP/NBM/CONUS/NCEP_CONUS_MODEL_BLEND.ncx4", 28, 20, 31});
      result.add(new Object[] {topDir + "NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4", 28, 22, 33});
      result.add(new Object[] {topDir + "NCEP/NBM/PuertoRico/NCEP_PUERTORICO_MODEL_BLEND.ncx4", 17, 16, 25});

      // Variable 'Wind_direction_from_which_blowing_height_above_ground' already exists
      // result.add(new Object[]{topDir + "NCEP/NBM/Ocean/National_Blend_Ocean_20201010_0000.grib2.ncx4", });
      // result.add(new Object[]{topDir + "NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4", });
      // See testNbmOcean.

      // ndfd
      // Individual runtimes are SRC, and are fine
      result.add(new Object[] {topDir + "NCEP/NDFD/CPC/NDFD_CPC_CONUS_2p5km_20201013_2200.grib2.ncx4", 4, 1, 4});
      result.add(
          new Object[] {topDir + "NCEP/NDFD/NWS/NDFD_NWS_CONUS_conduit_2p5km_20201010_0200.grib2.ncx4", 15, 7, 10});
      result
          .add(new Object[] {topDir + "NCEP/NDFD/NWS_noaaport/NDFD_NWS_CONUS_2p5km_20201015_0000.grib2.ncx4", 8, 6, 9});
      result.add(new Object[] {topDir + "NCEP/NDFD/SPC/NDFD_SPC_CONUS_2p5km_20201010_2000.grib2.ncx4", 2, 2, 5});

      // ndfd collections: non-orth regular
      result.add(new Object[] {topDir + "NCEP/NDFD/NWS_noaaport/NDFD_NWS_CONUS_NOAAPORT.ncx4", 8, 6, 11});
      result.add(new Object[] {topDir + "NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4", 4, 4, 10});

      // TODO What if you didnt collect them, just leave the SRC's
      // CPC has a single timeOffset interval that varies in an irregular way.
      // result.add(new Object[]{topDir + "NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4", });
      // NWS has a 2D time, non orth, non regular.
      // result.add(new Object[]{topDir + "NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4", });

      // rr
      result.add(new Object[] {topDir + "NCEP/RR/CONUS_13km/RAP-CONUS_13km.ncx4", 53, 14, 16});
      result.add(new Object[] {topDir + "NCEP/RR/CONUS_40km/RAP-CONUS_40km.ncx4", 91, 19, 22});
      result.add(new Object[] {topDir + "NCEP/RR/CONUS_20km/RAP-CONUS_20km.ncx4", 91, 19, 22});

      // rtma
      result.add(new Object[] {topDir + "NCEP/RTMA/CONUS_2p5km/RTMA-CONUS_2p5km.ncx4", 20, 10, 15});
      result.add(new Object[] {topDir + "NCEP/RTMA/GUAM_2p5km/RTMA-GUAM_2p5km.ncx4", 17, 4, 8});

      // sref
      result.add(new Object[] {
          topDir + "NCEP/SREF/CONUS_40km/ensprod_biasc/SREF-CONUS_40km_biasCorrected_Ensemble-derived_products.ncx4",
          34, 8, 12});
      result.add(new Object[] {topDir + "NCEP/SREF/Alaska_45km/ensprod/SREF_Alaska_45km_Ensemble_Derived_Products.ncx4",
          54, 11, 14});
      result.add(new Object[] {topDir + "NCEP/SREF/CONUS_40km/ensprod/SREF-CONUS_40km_Ensemble-derived_products.ncx4",
          54, 10, 13});
      result.add(new Object[] {
          topDir + "NCEP/SREF/PacificNE_0p4/ensprod/SREF_Pacific_North_East_0.4_Degree_Ensemble_Derived_Products.ncx4",
          49, 9, 12});

      // ww3
      result.add(new Object[] {topDir + "NCEP/WW3/Coastal_Alaska/WW3-Coastal_Alaska.ncx4", 14, 2, 5});
      result.add(new Object[] {topDir + "NCEP/WW3/Coastal_US_East_Coast/WW3-Coastal_US_East_Coast.ncx4", 14, 2, 5});
      result.add(new Object[] {topDir + "NCEP/WW3/Coastal_US_West_Coast/WW3-Coastal_US_West_Coast.ncx4", 14, 2, 5});
      result.add(new Object[] {topDir + "NCEP/WW3/Global/WW3-Global.ncx4", 14, 3, 6});
      result.add(new Object[] {topDir + "NCEP/WW3/Regional_Alaska/WW3-Regional_Alaska.ncx4", 14, 2, 5});
      result
          .add(new Object[] {topDir + "NCEP/WW3/Regional_Eastern_Pacific/WW3-Regional_Eastern_Pacific.ncx4", 14, 2, 5});
      result.add(new Object[] {topDir + "NCEP/WW3/Regional_US_East_Coast/WW3-Regional_US_East_Coast.ncx4", 14, 2, 5});
      result.add(new Object[] {topDir + "NCEP/WW3/Regional_US_West_Coast/WW3-Regional_US_West_Coast.ncx4", 14, 2, 5});

      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "ft/grid/", ff, result);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  @Parameterized.Parameter(0)
  public String filename;
  @Parameterized.Parameter(1)
  public int ngrids;
  @Parameterized.Parameter(2)
  public int ncoordSys;
  @Parameterized.Parameter(3)
  public int nAxes;

  @Test
  public void checkGridDataset() throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("checkGridDataset: %s%n", gridDataset.getLocation());
      assertThat(gridDataset.getCoordSystems()).hasSize(ncoordSys);
      assertThat(gridDataset.getCoordAxes()).hasSize(nAxes);
      assertThat(gridDataset.getGrids()).hasSize(ngrids);

      HashSet<GridCoordinateSystem> csysSet = new HashSet<>();
      HashSet<GridAxis> axisSet = new HashSet<>();
      for (Grid grid : gridDataset.getGrids()) {
        csysSet.add(grid.getCoordinateSystem());
        for (GridAxis axis : grid.getCoordinateSystem().getGridAxes()) {
          axisSet.add(axis);
        }
      }
      assertThat(csysSet).hasSize(ncoordSys);
      assertThat(axisSet).hasSize(nAxes);
    }
  }

  @Test
  public void compareGridDataset() throws Exception {
    boolean ok = true;
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        fail();
      }
      System.out.printf("compareGridDataset: %s%n", newDataset.getLocation());
      System.out.printf(" NewGrid: %d %d %d %n", newDataset.getGrids().size(), newDataset.getCoordSystems().size(),
          newDataset.getCoordAxes().size());
      System.out.printf(" OldGrid: %d %d %n", dataset.getGrids().size(), dataset.getGridsets().size());
      // assertThat(dataset.getGrids()).hasSize(2 * ngrids);
      // assertThat(dataset.getGridsets()).hasSize(2 * ncoordSys);

      for (Grid grid : newDataset.getGrids()) {
        GridDatatype geogrid = findInOld(grid.getName(), dataset);
        if (geogrid == null) {
          System.out.printf(" GeoGrid %s not in OldGrid%n", grid.getName());
          ok = false;
        } else {
          if (!compareCoordinateNames(grid.getCoordinateSystem().getName(), geogrid.getCoordinateSystem().getName())) {
            System.out.printf("    Grid %s: %s%n", grid.getName(), grid.getCoordinateSystem().getName());
            System.out.printf(" GeoGrid %s: %s%n%n", geogrid.getName(), geogrid.getCoordinateSystem().getName());
          }
        }
      }
      for (GridDatatype geogrid : dataset.getGrids()) {
        if (geogrid.getName().startsWith("Best")) {
          continue;
        }
        String name = removeGroup(geogrid.getName());
        if (!newDataset.findGrid(name).isPresent()) {
          System.out.printf(" GeoGrid %s not in NewGrid%n", name);
          ok = false;
        }
      }
    }
    assertThat(ok).isTrue();
  }

  private GridDatatype findInOld(String want, ucar.nc2.dt.grid.GridDataset dataset) {
    for (GridDatatype geogrid : dataset.getGrids()) {
      if (removeGroup(geogrid.getName()).equals(want)) {
        return geogrid;
      }
    }
    return null;
  }

  private String removeGroup(String name) {
    int pos = name.indexOf("/");
    return (pos < 0) ? name : name.substring(pos + 1);
  }

  private boolean compareCoordinateNames(String newName, String oldName) {
    boolean result = true;
    Iterable<String> oldNames = StringUtil2.split(oldName);
    Iterable<String> newNames = StringUtil2.split(newName);
    if (Iterables.size(oldNames) != Iterables.size(newNames)) {
      System.out.printf(" Old size = %d != %d%n", Iterables.size(oldNames), Iterables.size(newNames));
      result = false;
    }
    Iterator<String> oldIter = oldNames.iterator();
    Iterator<String> newIter = newNames.iterator();
    while (oldIter.hasNext() && newIter.hasNext()) {
      String old = StringUtil2.remove(oldIter.next(), "TwoD/");
      String nnn = StringUtil2.remove(newIter.next(), "Offset");
      if (!old.equals(nnn)) {
        result = false;
      }
    }
    return result;
  }
}

