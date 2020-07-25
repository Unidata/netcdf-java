/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Look for missing data in Grib Collections.
 * <p/>
 * Indicates that coordinates are not matching, because DGEX_CONUS is dense (has data for each coordinate). Note that
 * not all grib
 * collections will be dense.
 *
 * @author John
 * @since 10/13/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionProblem {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  static public void before() throws IOException {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();

    readOldOutput();
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(new DebugFlagsImpl());
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n",
        GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }

  @Test
  public void testPofG_Grib2() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    // assert count.nread == 172166 : count.nread;
    // assert count.nmiss == 5023;
    // assert count.nerrs == 0;
  }

  private static Count read(String filename) {
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    Count allCount = new Count();
    try (GridDataset gds = GridDataset.open(filename)) {
      for (GridDatatype gdt : gds.getGrids()) {
        Count count = read(gdt);
        // System.out.printf("%80s == %d/%d%n", gdt.getFullName(), count.nmiss, count.nread);
        checkAgainstOld(gdt.getFullName(), String.format("%s/%s", count.nmiss, count.nread));
        allCount.add(count);
      }
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / allCount.nread;
      System.out.printf("%n%80s == %d/%d%n", "total", allCount.nmiss, allCount.nread);
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took / 1000, r);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      Formatter out = new Formatter(System.out);
      GribCdmIndex.gribCollectionCache.showCache(out);
    }

    for (Result result : oldResults.values()) {
      if (!result.checked) {
        System.out.printf("NOT PRESENT %s = %s%n", result.name, result.result);
      }
    }

    return allCount;
  }

  private static Count read(GridDatatype gdt) throws IOException {
    Dimension rtDim = gdt.getRunTimeDimension();
    Dimension tDim = gdt.getTimeDimension();
    Dimension zDim = gdt.getZDimension();

    Count count = new Count();
    if (rtDim != null) {
      for (int rt = 0; rt < rtDim.getLength(); rt++) {
        read(gdt, count, rt, tDim, zDim);
      }
    } else {
      read(gdt, count, -1, tDim, zDim);
    }
    return count;
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, Dimension timeDim, Dimension zDim)
      throws IOException {
    if (timeDim != null) {
      for (int t = 0; t < timeDim.getLength(); t++) {
        read(gdt, count, rtIndex, t, zDim);
      }
    } else {
      read(gdt, count, rtIndex, -1, zDim);
    }
  }


  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      for (int z = 0; z < zDim.getLength(); z++) {
        read(gdt, count, rtIndex, tIndex, z);
      }
    } else {
      read(gdt, count, rtIndex, tIndex, -1);
    }
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, int zIndex) throws IOException {
    // int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
    Array data = gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);
    /*
     * if (data.getSize() != 1 || data.getRank() != 0) {
     * System.out.printf("%s size=%d rank=%d%n", gdt.getFullName(), data.getSize(), data.getRank());
     * gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, 10, 10); // debug
     * }
     */

    // subset the array by striding x,y by 100
    Array dataSubset;
    try {
      Section s = new Section(data.getShape());
      List<Range> ranges = s.getRanges();
      int rank = ranges.size();
      assert rank >= 2;
      List<Range> subset = new ArrayList<>(rank);
      for (int i = 0; i < rank; i++) {
        if (i < rank - 2) {
          subset.add(ranges.get(i));
        } else {
          Range r = ranges.get(i);
          Range strided = new Range(r.first(), r.last(), 33);
          subset.add(strided);
        }
      }
      dataSubset = data.section(subset);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      return;
    }
    // System.out.printf("data=%d subset=%d%n", data.getSize(), dataSubset.getSize());

    // they all have to be missing values
    boolean isMissing = true;
    while (dataSubset.hasNext()) {
      float val = dataSubset.nextFloat();
      if (!Float.isNaN(val)) {
        isMissing = false;
      }
    }
    if (isMissing) {
      count.nmiss++;
    }
    count.nread++;
  }

  static public class Count {

    int nread;
    int nmiss;
    int nerrs;

    void add(Count c) {
      nread += c.nread;
      nmiss += c.nmiss;
      nerrs += c.nerrs;
    }
  }

  private static void checkAgainstOld(String name, String result) {
    Result oldResult = oldResults.get(name);
    if (oldResult == null) {
      System.out.printf("*** Cant find oldResult name=%s%n", name);
      return;
    }
    oldResult.checked = true;

    if (oldResult.result.equals(result)) {
      System.out.printf("OK name=%s %s%n", name, result);
      return;
    }

    System.out.printf("NOT OK name=%s '%s' '%s'%n", name, result, oldResult.result);
  }

  private static Map<String, Result> oldResults = new HashMap<>();

  private static void readOldOutput() throws IOException {
    try (BufferedReader dataIS = new BufferedReader(new StringReader(oldOutput))) {

      while (true) {
        String line = dataIS.readLine();
        if (line == null) {
          break;
        }
        Iterator<String> tokens = Splitter.on("==").trimResults().omitEmptyStrings().split(line).iterator();
        String name = tokens.next();
        String result = tokens.next();
        System.out.printf("%s = %s%n", name, result);
        oldResults.put(name, new Result(name, result));
      }
      System.out.printf("done%n");
    }
  }

  private static class Result {
    String name;
    String result;
    boolean checked;

    public Result(String name, String result) {
      this.name = name;
      this.result = result;
    }
  }

  private static String oldOutput =
      "                                 TwoD/Total_ozone_entire_atmosphere_single_layer == 12/372\n"
          + "                                                TwoD/Ozone_Mixing_Ratio_isobaric == 191/4464\n"
          + "                TwoD/Total_cloud_cover_entire_atmosphere_Mixed_intervals_Average == 12/368\n"
          + "                          TwoD/Temperature_low_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                       TwoD/Temperature_middle_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                         TwoD/Temperature_high_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                                               TwoD/Surface_Lifted_Index_surface == 12/372\n"
          + "                                           TwoD/Pressure_convective_cloud_bottom == 12/368\n"
          + "                                              TwoD/Pressure_convective_cloud_top == 12/368\n"
          + "                                            TwoD/Vertical_Speed_Shear_tropopause == 12/372\n"
          + "                           TwoD/Vertical_Speed_Shear_potential_vorticity_surface == 22/744\n"
          + "                                        TwoD/Ventilation_Rate_planetary_boundary == 17/372\n"
          + "                                               TwoD/MSLP_Eta_model_reduction_msl == 13/372\n"
          + "   TwoD/Pressure_of_level_from_which_parcel_was_lifted_pressure_difference_layer == 11/372\n"
          + "                                                    TwoD/Wind_speed_gust_surface == 17/372\n"
          + "                         TwoD/Precipitation_rate_surface_Mixed_intervals_Average == 12/368\n"
          + "                                         TwoD/Potential_Evaporation_Rate_surface == 13/368\n"
          + "                 TwoD/Volumetric_Soil_Moisture_Content_depth_below_surface_layer == 52/1488\n"
          + "                                     TwoD/Albedo_surface_Mixed_intervals_Average == 11/368\n"
          + "                       TwoD/Latent_heat_net_flux_surface_Mixed_intervals_Average == 12/368\n"
          + "                     TwoD/Sensible_heat_net_flux_surface_Mixed_intervals_Average == 12/368\n"
          + "                                                          TwoD/Ice_cover_surface == 11/372\n"
          + "                                          TwoD/Land_cover_0__sea_1__land_surface == 11/372\n"
          + "                  TwoD/Momentum_flux_u-component_surface_Mixed_intervals_Average == 12/368\n"
          + "                                                           TwoD/Pressure_surface == 13/372\n"
          + "                                TwoD/Specific_humidity_pressure_difference_layer == 11/372\n"
          + "                                                        TwoD/Temperature_surface == 13/372\n"
          + "                                      TwoD/Temperature_pressure_difference_layer == 11/372\n"
          + "                                TwoD/Relative_humidity_pressure_difference_layer == 11/372\n"
          + "                                              TwoD/Relative_humidity_sigma_layer == 44/1488\n"
          + "                                                       TwoD/Haines_index_surface == 13/372\n"
          + "                                 TwoD/Soil_temperature_depth_below_surface_layer == 52/1488\n"
          + "                              TwoD/u-component_of_wind_pressure_difference_layer == 11/372\n"
          + "                  TwoD/Momentum_flux_v-component_surface_Mixed_intervals_Average == 12/368\n"
          + "                           TwoD/Categorical_Rain_surface_Mixed_intervals_Average == 12/368\n"
          + "                              TwoD/v-component_of_wind_pressure_difference_layer == 11/372\n"
          + "                                                TwoD/Geopotential_height_surface == 13/372\n"
          + "                              TwoD/Convective_available_potential_energy_surface == 12/372\n"
          + "            TwoD/Convective_available_potential_energy_pressure_difference_layer == 22/744\n"
          + "                                              TwoD/Convective_inhibition_surface == 12/372\n"
          + "                            TwoD/Convective_inhibition_pressure_difference_layer == 22/744\n"
          + "                          TwoD/Storm_relative_helicity_height_above_ground_layer == 12/372\n"
          + "                                                         TwoD/Snow_depth_surface == 13/372\n"
          + "                         TwoD/Water_equivalent_of_accumulated_snow_depth_surface == 13/372\n"
          + "                           TwoD/Ground_Heat_Flux_surface_Mixed_intervals_Average == 12/368\n"
          + "                                         TwoD/Total_cloud_cover_convective_cloud == 12/368\n"
          + "                                                  TwoD/Sunshine_Duration_surface == 12/372\n"
          + "                                          TwoD/Best_4_layer_Lifted_Index_surface == 11/372\n"
          + "                          TwoD/Water_runoff_surface_Mixed_intervals_Accumulation == 12/368\n"
          + "                                        TwoD/5-Wave_Geopotential_Height_isobaric == 11/372\n"
          + "                           TwoD/Relative_humidity_entire_atmosphere_single_layer == 12/372\n"
          + "                            TwoD/Relative_humidity_highest_tropospheric_freezing == 11/372\n"
          + "                                                      TwoD/Wilting_Point_surface == 12/372\n"
          + "                   TwoD/Total_precipitation_surface_Mixed_intervals_Accumulation == 12/368\n"
          + "              TwoD/Convective_precipitation_surface_Mixed_intervals_Accumulation == 12/368\n"
          + "                          TwoD/Pressure_low_cloud_bottom_Mixed_intervals_Average == 12/368\n"
          + "                       TwoD/Pressure_middle_cloud_bottom_Mixed_intervals_Average == 12/368\n"
          + "                         TwoD/Pressure_high_cloud_bottom_Mixed_intervals_Average == 12/368\n"
          + "                             TwoD/Pressure_low_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                          TwoD/Pressure_middle_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                            TwoD/Pressure_high_cloud_top_Mixed_intervals_Average == 12/368\n"
          + "                  TwoD/Categorical_Freezing_Rain_surface_Mixed_intervals_Average == 12/368\n"
          + "                                      TwoD/Per_cent_frozen_precipitation_surface == 12/372\n"
          + "                                                       TwoD/Temperature_isobaric == 396/9672\n"
          + "                                                 TwoD/Relative_humidity_isobaric == 395/9672\n"
          + "                                        TwoD/Relative_humidity_zeroDegC_isotherm == 11/372\n"
          + "                                               TwoD/u-component_of_wind_isobaric == 394/9672\n"
          + "                        TwoD/Total_cloud_cover_low_cloud_Mixed_intervals_Average == 12/368\n"
          + "                     TwoD/Total_cloud_cover_middle_cloud_Mixed_intervals_Average == 12/368\n"
          + "                       TwoD/Total_cloud_cover_high_cloud_Mixed_intervals_Average == 12/368\n"
          + "             TwoD/Total_cloud_cover_boundary_layer_cloud_Mixed_intervals_Average == 12/368\n"
          + "         TwoD/Downward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average == 12/368\n"
          + " TwoD/Cloud_Work_Function_entire_atmosphere_single_layer_Mixed_intervals_Average == 12/368\n"
          + "                                               TwoD/v-component_of_wind_isobaric == 392/9672\n"
          + "            TwoD/Maximum_temperature_height_above_ground_Mixed_intervals_Maximum == 12/368\n"
          + "                                               TwoD/Geopotential_height_isobaric == 396/9672\n"
          + "                                      TwoD/Geopotential_height_zeroDegC_isotherm == 11/372\n"
          + "                         TwoD/U-Component_Storm_Motion_height_above_ground_layer == 12/372\n"
          + "                                     TwoD/u-component_of_wind_planetary_boundary == 17/372\n"
          + "                                        TwoD/Vertical_velocity_pressure_isobaric == 311/7812\n"
          + "                                                TwoD/Absolute_vorticity_isobaric == 392/9672\n"
          + "                                                TwoD/Pressure_reduced_to_MSL_msl == 11/372\n"
          + "                    TwoD/Categorical_Ice_Pellets_surface_Mixed_intervals_Average == 12/368\n"
          + "     TwoD/Meridional_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average == 12/368\n"
          + "               TwoD/Downward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average == 12/368\n"
          + "                                                TwoD/Cloud_mixing_ratio_isobaric == 309/7812\n"
          + "                                                      TwoD/Pressure_maximum_wind == 12/372\n"
          + "                                                   TwoD/Temperature_maximum_wind == 11/372\n"
          + "                                             TwoD/Temperature_altitude_above_msl == 33/1116\n"
          + "                                           TwoD/u-component_of_wind_maximum_wind == 11/372\n"
          + "                                     TwoD/u-component_of_wind_altitude_above_msl == 33/1116\n"
          + "                     TwoD/ICAO_Standard_Atmosphere_Reference_Height_maximum_wind == 12/372\n"
          + "           TwoD/Upward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average == 12/368\n"
          + "    TwoD/Upward_Short-Wave_Radiation_Flux_atmosphere_top_Mixed_intervals_Average == 12/368\n"
          + "                                           TwoD/v-component_of_wind_maximum_wind == 11/372\n"
          + "                                     TwoD/v-component_of_wind_altitude_above_msl == 33/1116\n"
          + "                                   TwoD/Apparent_temperature_height_above_ground == 12/372\n"
          + "                                           TwoD/Geopotential_height_maximum_wind == 11/372\n"
          + "            TwoD/Minimum_temperature_height_above_ground_Mixed_intervals_Minimum == 12/368\n"
          + "                         TwoD/V-Component_Storm_Motion_height_above_ground_layer == 12/372\n"
          + "                                                     TwoD/Field_Capacity_surface == 12/372\n"
          + "                                     TwoD/v-component_of_wind_planetary_boundary == 17/372\n"
          + "                          TwoD/Precipitable_water_entire_atmosphere_single_layer == 12/372\n"
          + "                                                        TwoD/Pressure_tropopause == 12/372\n"
          + "                                               TwoD/Pressure_height_above_ground == 11/372\n"
          + "                                      TwoD/Specific_humidity_height_above_ground == 24/744\n"
          + "                                            TwoD/Temperature_height_above_ground == 35/1116\n"
          + "                                                     TwoD/Temperature_tropopause == 12/372\n"
          + "                                      TwoD/Relative_humidity_height_above_ground == 12/372\n"
          + "                                    TwoD/u-component_of_wind_height_above_ground == 34/1116\n"
          + "                                             TwoD/u-component_of_wind_tropopause == 12/372\n"
          + "                       TwoD/ICAO_Standard_Atmosphere_Reference_Height_tropopause == 12/372\n"
          + "                           TwoD/Categorical_Snow_surface_Mixed_intervals_Average == 12/368\n"
          + "          TwoD/Zonal_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average == 12/368\n"
          + "                 TwoD/Upward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average == 12/368\n"
          + "          TwoD/Upward_Long-Wave_Radp_Flux_atmosphere_top_Mixed_intervals_Average == 12/368\n"
          + "                                    TwoD/v-component_of_wind_height_above_ground == 34/1116\n"
          + "                                             TwoD/v-component_of_wind_tropopause == 12/372\n"
          + "                                             TwoD/Geopotential_height_tropopause == 12/372\n"
          + "                                   TwoD/Dewpoint_temperature_height_above_ground == 13/372\n"
          + "                                                          TwoD/Temperature_sigma == 11/372\n"
          + "                                                    TwoD/Relative_humidity_sigma == 11/372\n"
          + "                                                TwoD/Potential_temperature_sigma == 11/372\n"
          + "                                                  TwoD/u-component_of_wind_sigma == 11/372\n"
          + "                                                  TwoD/v-component_of_wind_sigma == 11/372\n"
          + "                                    TwoD/Planetary_Boundary_Layer_Height_surface == 11/372\n"
          + "                                           TwoD/Vertical_velocity_pressure_sigma == 11/372\n"
          + "              TwoD/Convective_Precipitation_Rate_surface_Mixed_intervals_Average == 12/368\n"
          + "                          TwoD/Geopotential_height_highest_tropospheric_freezing == 11/372\n"
          + "                                 TwoD/Cloud_water_entire_atmosphere_single_layer == 12/372\n"
          + "                                       TwoD/Pressure_potential_vorticity_surface == 22/744\n"
          + "                                    TwoD/Temperature_potential_vorticity_surface == 22/744\n"
          + "                            TwoD/u-component_of_wind_potential_vorticity_surface == 22/744\n"
          + "                            TwoD/v-component_of_wind_potential_vorticity_surface == 22/744\n"
          + "                            TwoD/Geopotential_height_potential_vorticity_surface == 22/744\n"
          + "                                 Best/Total_ozone_entire_atmosphere_single_layer == 0/99\n"
          + "                                                Best/Ozone_Mixing_Ratio_isobaric == 0/1188\n"
          + "                Best/Total_cloud_cover_entire_atmosphere_Mixed_intervals_Average == 0/98\n"
          + "                          Best/Temperature_low_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                       Best/Temperature_middle_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                         Best/Temperature_high_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                                               Best/Surface_Lifted_Index_surface == 0/99\n"
          + "                                           Best/Pressure_convective_cloud_bottom == 0/98\n"
          + "                                              Best/Pressure_convective_cloud_top == 0/98\n"
          + "                                            Best/Vertical_Speed_Shear_tropopause == 0/99\n"
          + "                           Best/Vertical_Speed_Shear_potential_vorticity_surface == 0/198\n"
          + "                                        Best/Ventilation_Rate_planetary_boundary == 0/99\n"
          + "                                               Best/MSLP_Eta_model_reduction_msl == 0/99\n"
          + "   Best/Pressure_of_level_from_which_parcel_was_lifted_pressure_difference_layer == 0/99\n"
          + "                                                    Best/Wind_speed_gust_surface == 0/99\n"
          + "                         Best/Precipitation_rate_surface_Mixed_intervals_Average == 0/98\n"
          + "                                         Best/Potential_Evaporation_Rate_surface == 0/98\n"
          + "                 Best/Volumetric_Soil_Moisture_Content_depth_below_surface_layer == 0/396\n"
          + "                                     Best/Albedo_surface_Mixed_intervals_Average == 0/98\n"
          + "                       Best/Latent_heat_net_flux_surface_Mixed_intervals_Average == 0/98\n"
          + "                     Best/Sensible_heat_net_flux_surface_Mixed_intervals_Average == 0/98\n"
          + "                                                          Best/Ice_cover_surface == 0/99\n"
          + "                                          Best/Land_cover_0__sea_1__land_surface == 0/99\n"
          + "                  Best/Momentum_flux_u-component_surface_Mixed_intervals_Average == 0/98\n"
          + "                                                           Best/Pressure_surface == 0/99\n"
          + "                                Best/Specific_humidity_pressure_difference_layer == 0/99\n"
          + "                                                        Best/Temperature_surface == 0/99\n"
          + "                                      Best/Temperature_pressure_difference_layer == 0/99\n"
          + "                                Best/Relative_humidity_pressure_difference_layer == 0/99\n"
          + "                                              Best/Relative_humidity_sigma_layer == 0/396\n"
          + "                                                       Best/Haines_index_surface == 0/99\n"
          + "                                 Best/Soil_temperature_depth_below_surface_layer == 0/396\n"
          + "                              Best/u-component_of_wind_pressure_difference_layer == 0/99\n"
          + "                  Best/Momentum_flux_v-component_surface_Mixed_intervals_Average == 0/98\n"
          + "                           Best/Categorical_Rain_surface_Mixed_intervals_Average == 0/98\n"
          + "                              Best/v-component_of_wind_pressure_difference_layer == 0/99\n"
          + "                                                Best/Geopotential_height_surface == 0/99\n"
          + "                              Best/Convective_available_potential_energy_surface == 0/99\n"
          + "            Best/Convective_available_potential_energy_pressure_difference_layer == 0/198\n"
          + "                                              Best/Convective_inhibition_surface == 0/99\n"
          + "                            Best/Convective_inhibition_pressure_difference_layer == 0/198\n"
          + "                          Best/Storm_relative_helicity_height_above_ground_layer == 0/99\n"
          + "                                                         Best/Snow_depth_surface == 0/99\n"
          + "                         Best/Water_equivalent_of_accumulated_snow_depth_surface == 0/99\n"
          + "                           Best/Ground_Heat_Flux_surface_Mixed_intervals_Average == 0/98\n"
          + "                                         Best/Total_cloud_cover_convective_cloud == 0/98\n"
          + "                                                  Best/Sunshine_Duration_surface == 0/99\n"
          + "                                          Best/Best_4_layer_Lifted_Index_surface == 0/99\n"
          + "                          Best/Water_runoff_surface_Mixed_intervals_Accumulation == 0/98\n"
          + "                                        Best/5-Wave_Geopotential_Height_isobaric == 0/99\n"
          + "                           Best/Relative_humidity_entire_atmosphere_single_layer == 0/99\n"
          + "                            Best/Relative_humidity_highest_tropospheric_freezing == 0/99\n"
          + "                                                      Best/Wilting_Point_surface == 0/99\n"
          + "                   Best/Total_precipitation_surface_Mixed_intervals_Accumulation == 0/98\n"
          + "              Best/Convective_precipitation_surface_Mixed_intervals_Accumulation == 0/98\n"
          + "                          Best/Pressure_low_cloud_bottom_Mixed_intervals_Average == 0/98\n"
          + "                       Best/Pressure_middle_cloud_bottom_Mixed_intervals_Average == 0/98\n"
          + "                         Best/Pressure_high_cloud_bottom_Mixed_intervals_Average == 0/98\n"
          + "                             Best/Pressure_low_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                          Best/Pressure_middle_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                            Best/Pressure_high_cloud_top_Mixed_intervals_Average == 0/98\n"
          + "                  Best/Categorical_Freezing_Rain_surface_Mixed_intervals_Average == 0/98\n"
          + "                                      Best/Per_cent_frozen_precipitation_surface == 0/99\n"
          + "                                                       Best/Temperature_isobaric == 0/2574\n"
          + "                                                 Best/Relative_humidity_isobaric == 0/2574\n"
          + "                                        Best/Relative_humidity_zeroDegC_isotherm == 0/99\n"
          + "                                               Best/u-component_of_wind_isobaric == 0/2574\n"
          + "                        Best/Total_cloud_cover_low_cloud_Mixed_intervals_Average == 0/98\n"
          + "                     Best/Total_cloud_cover_middle_cloud_Mixed_intervals_Average == 0/98\n"
          + "                       Best/Total_cloud_cover_high_cloud_Mixed_intervals_Average == 0/98\n"
          + "             Best/Total_cloud_cover_boundary_layer_cloud_Mixed_intervals_Average == 0/98\n"
          + "         Best/Downward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average == 0/98\n"
          + " Best/Cloud_Work_Function_entire_atmosphere_single_layer_Mixed_intervals_Average == 0/98\n"
          + "                                               Best/v-component_of_wind_isobaric == 0/2574\n"
          + "            Best/Maximum_temperature_height_above_ground_Mixed_intervals_Maximum == 0/98\n"
          + "                                               Best/Geopotential_height_isobaric == 0/2574\n"
          + "                                      Best/Geopotential_height_zeroDegC_isotherm == 0/99\n"
          + "                         Best/U-Component_Storm_Motion_height_above_ground_layer == 0/99\n"
          + "                                     Best/u-component_of_wind_planetary_boundary == 0/99\n"
          + "                                        Best/Vertical_velocity_pressure_isobaric == 0/2079\n"
          + "                                                Best/Absolute_vorticity_isobaric == 0/2574\n"
          + "                                                Best/Pressure_reduced_to_MSL_msl == 0/99\n"
          + "                    Best/Categorical_Ice_Pellets_surface_Mixed_intervals_Average == 0/98\n"
          + "     Best/Meridional_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average == 0/98\n"
          + "               Best/Downward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average == 0/98\n"
          + "                                                Best/Cloud_mixing_ratio_isobaric == 0/2079\n"
          + "                                                      Best/Pressure_maximum_wind == 0/99\n"
          + "                                                   Best/Temperature_maximum_wind == 0/99\n"
          + "                                             Best/Temperature_altitude_above_msl == 0/297\n"
          + "                                           Best/u-component_of_wind_maximum_wind == 0/99\n"
          + "                                     Best/u-component_of_wind_altitude_above_msl == 0/297\n"
          + "                     Best/ICAO_Standard_Atmosphere_Reference_Height_maximum_wind == 0/99\n"
          + "           Best/Upward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average == 0/98\n"
          + "    Best/Upward_Short-Wave_Radiation_Flux_atmosphere_top_Mixed_intervals_Average == 0/98\n"
          + "                                           Best/v-component_of_wind_maximum_wind == 0/99\n"
          + "                                     Best/v-component_of_wind_altitude_above_msl == 0/297\n"
          + "                                   Best/Apparent_temperature_height_above_ground == 0/99\n"
          + "                                           Best/Geopotential_height_maximum_wind == 0/99\n"
          + "            Best/Minimum_temperature_height_above_ground_Mixed_intervals_Minimum == 0/98\n"
          + "                         Best/V-Component_Storm_Motion_height_above_ground_layer == 0/99\n"
          + "                                                     Best/Field_Capacity_surface == 0/99\n"
          + "                                     Best/v-component_of_wind_planetary_boundary == 0/99\n"
          + "                          Best/Precipitable_water_entire_atmosphere_single_layer == 0/99\n"
          + "                                                        Best/Pressure_tropopause == 0/99\n"
          + "                                               Best/Pressure_height_above_ground == 0/99\n"
          + "                                      Best/Specific_humidity_height_above_ground == 0/198\n"
          + "                                            Best/Temperature_height_above_ground == 0/297\n"
          + "                                                     Best/Temperature_tropopause == 0/99\n"
          + "                                      Best/Relative_humidity_height_above_ground == 0/99\n"
          + "                                    Best/u-component_of_wind_height_above_ground == 0/297\n"
          + "                                             Best/u-component_of_wind_tropopause == 0/99\n"
          + "                       Best/ICAO_Standard_Atmosphere_Reference_Height_tropopause == 0/99\n"
          + "                           Best/Categorical_Snow_surface_Mixed_intervals_Average == 0/98\n"
          + "          Best/Zonal_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average == 0/98\n"
          + "                 Best/Upward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average == 0/98\n"
          + "          Best/Upward_Long-Wave_Radp_Flux_atmosphere_top_Mixed_intervals_Average == 0/98\n"
          + "                                    Best/v-component_of_wind_height_above_ground == 0/297\n"
          + "                                             Best/v-component_of_wind_tropopause == 0/99\n"
          + "                                             Best/Geopotential_height_tropopause == 0/99\n"
          + "                                   Best/Dewpoint_temperature_height_above_ground == 0/99\n"
          + "                                                          Best/Temperature_sigma == 0/99\n"
          + "                                                    Best/Relative_humidity_sigma == 0/99\n"
          + "                                                Best/Potential_temperature_sigma == 0/99\n"
          + "                                                  Best/u-component_of_wind_sigma == 0/99\n"
          + "                                                  Best/v-component_of_wind_sigma == 0/99\n"
          + "                                    Best/Planetary_Boundary_Layer_Height_surface == 0/99\n"
          + "                                           Best/Vertical_velocity_pressure_sigma == 0/99\n"
          + "              Best/Convective_Precipitation_Rate_surface_Mixed_intervals_Average == 0/98\n"
          + "                          Best/Geopotential_height_highest_tropospheric_freezing == 0/99\n"
          + "                                 Best/Cloud_water_entire_atmosphere_single_layer == 0/99\n"
          + "                                       Best/Pressure_potential_vorticity_surface == 0/198\n"
          + "                                    Best/Temperature_potential_vorticity_surface == 0/198\n"
          + "                            Best/u-component_of_wind_potential_vorticity_surface == 0/198\n"
          + "                            Best/v-component_of_wind_potential_vorticity_surface == 0/198\n"
          + "                            Best/Geopotential_height_potential_vorticity_surface == 0/198\n";
}
