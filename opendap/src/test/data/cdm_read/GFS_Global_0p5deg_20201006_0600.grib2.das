Attributes {
    LatLon_Projection {
        String grid_mapping_name "latitude_longitude";
        Float64 earth_radius 6371229.0;
        String _CoordinateTransformType "Projection";
        String _CoordinateAxisTypes "GeoX GeoY";
    }
    time_bounds {
        String units "Hour since 2020-10-06T06:00:00Z";
        String long_name "bounds for time";
    }
    time2_bounds {
        String units "Hour since 2020-10-06T06:00:00Z";
        String long_name "bounds for time2";
    }
    pressure_difference_layer_bounds {
        String units "Pa";
        String long_name "bounds for pressure_difference_layer";
    }
    pressure_difference_layer1_bounds {
        String units "Pa";
        String long_name "bounds for pressure_difference_layer1";
    }
    sigma_layer_bounds {
        String units "sigma";
        String long_name "bounds for sigma_layer";
    }
    height_above_ground_layer_bounds {
        String units "m";
        String long_name "bounds for height_above_ground_layer";
    }
    depth_below_surface_layer_bounds {
        String units "m";
        String long_name "bounds for depth_below_surface_layer";
    }
    pressure_difference_layer2_bounds {
        String units "Pa";
        String long_name "bounds for pressure_difference_layer2";
    }
    height_above_ground_layer1_bounds {
        String units "m";
        String long_name "bounds for height_above_ground_layer1";
    }
    Absolute_vorticity_isobaric {
        String long_name "Absolute vorticity @ Isobaric surface";
        String units "1/s";
        String abbreviation "ABSV";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric1 lat lon ";
        String Grib_Variable_Id "VAR_0-2-10_L100";
        Int32 Grib2_Parameter 0, 2, 10;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Absolute vorticity";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Albedo_surface_Mixed_intervals_Average {
        String long_name "Albedo (Mixed_intervals Average) @ Ground or water surface";
        String units "%";
        String abbreviation "ALBDO";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-19-1_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 19, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Physical atmospheric Properties";
        String Grib2_Parameter_Name "Albedo";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Apparent_temperature_height_above_ground {
        String long_name "Apparent temperature @ Specified height level above ground";
        String units "K";
        String abbreviation "APTMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground lat lon ";
        String Grib_Variable_Id "VAR_0-0-21_L103";
        Int32 Grib2_Parameter 0, 0, 21;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Apparent temperature";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Cloud_mixing_ratio_hybrid {
        String long_name "Cloud mixing ratio @ Hybrid level";
        String units "kg/kg";
        String abbreviation "CLWMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 hybrid lat lon ";
        String Grib_Variable_Id "VAR_0-1-22_L105";
        Int32 Grib2_Parameter 0, 1, 22;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Cloud mixing ratio";
        Int32 Grib2_Level_Type 105;
        String Grib2_Level_Desc "Hybrid level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Cloud_mixing_ratio_isobaric {
        String long_name "Cloud mixing ratio @ Isobaric surface";
        String units "kg/kg";
        String abbreviation "CLWMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-1-22_L100";
        Int32 Grib2_Parameter 0, 1, 22;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Cloud mixing ratio";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Cloud_water_entire_atmosphere_single_layer {
        String long_name "Cloud water @ Entire atmosphere layer";
        String units "kg.m-2";
        String abbreviation "CWAT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-6-6_L200";
        Int32 Grib2_Parameter 0, 6, 6;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Cloud water";
        Int32 Grib2_Level_Type 200;
        String Grib2_Level_Desc "Entire atmosphere layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_available_potential_energy_surface {
        String long_name "Convective available potential energy @ Ground or water surface";
        String units "J/kg";
        String abbreviation "CAPE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-7-6_L1";
        Int32 Grib2_Parameter 0, 7, 6;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Convective available potential energy";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_available_potential_energy_pressure_difference_layer {
        String long_name "Convective available potential energy @ Level at specified pressure difference from ground to level layer";
        String units "J/kg";
        String abbreviation "CAPE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer1 lat lon ";
        String Grib_Variable_Id "VAR_0-7-6_L108_layer";
        Int32 Grib2_Parameter 0, 7, 6;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Convective available potential energy";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_inhibition_surface {
        String long_name "Convective inhibition @ Ground or water surface";
        String units "J/kg";
        String abbreviation "CIN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-7-7_L1";
        Int32 Grib2_Parameter 0, 7, 7;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Convective inhibition";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_inhibition_pressure_difference_layer {
        String long_name "Convective inhibition @ Level at specified pressure difference from ground to level layer";
        String units "J/kg";
        String abbreviation "CIN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer1 lat lon ";
        String Grib_Variable_Id "VAR_0-7-7_L108_layer";
        Int32 Grib2_Parameter 0, 7, 7;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Convective inhibition";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_precipitation_surface_Mixed_intervals_Accumulation {
        String long_name "Convective precipitation (Mixed_intervals Accumulation) @ Ground or water surface";
        String units "kg.m-2";
        String abbreviation "ACPCP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time lat lon ";
        String Grib_Statistical_Interval_Type "Accumulation";
        String Grib_Variable_Id "VAR_0-1-10_L1_Imixed_S1";
        Int32 Grib2_Parameter 0, 1, 10;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Convective precipitation";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_precipitation_rate_surface {
        String long_name "Convective precipitation rate @ Ground or water surface";
        String units "kg.m-2.s-1";
        String abbreviation "CPRAT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 lat lon ";
        String Grib_Variable_Id "VAR_0-1-37_L1";
        Int32 Grib2_Parameter 0, 1, 37;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Convective precipitation rate";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Dewpoint_temperature_height_above_ground {
        String long_name "Dewpoint temperature @ Specified height level above ground";
        String units "K";
        String abbreviation "DPT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground lat lon ";
        String Grib_Variable_Id "VAR_0-0-6_L103";
        Int32 Grib2_Parameter 0, 0, 6;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Dewpoint temperature";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_potential_vorticity_surface {
        String long_name "Geopotential height @ Potential vorticity surface";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L109";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_surface {
        String long_name "Geopotential height @ Ground or water surface";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L1";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_isobaric {
        String long_name "Geopotential height @ Isobaric surface";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric6 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L100";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_zeroDegC_isotherm {
        String long_name "Geopotential height @ Level of 0 °C isotherm";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L4";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 4;
        String Grib2_Level_Desc "Level of 0 °C isotherm";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_maximum_wind {
        String long_name "Geopotential height @ Maximum wind level";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L6";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_tropopause {
        String long_name "Geopotential height @ Tropopause";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L7";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Geopotential_height_highest_tropospheric_freezing {
        String long_name "Geopotential height @ Highest tropospheric freezing level";
        String units "gpm";
        String abbreviation "HGT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-5_L204";
        Int32 Grib2_Parameter 0, 3, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Geopotential height";
        Int32 Grib2_Level_Type 204;
        String Grib2_Level_Desc "Highest tropospheric freezing level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Graupel_snow_pellets_hybrid {
        String long_name "Graupel (snow pellets) @ Hybrid level";
        String units "kg/kg";
        String abbreviation "GRLE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 hybrid lat lon ";
        String Grib_Variable_Id "VAR_0-1-32_L105";
        Int32 Grib2_Parameter 0, 1, 32;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Graupel (snow pellets)";
        Int32 Grib2_Level_Type 105;
        String Grib2_Level_Desc "Hybrid level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Graupel_snow_pellets_isobaric {
        String long_name "Graupel (snow pellets) @ Isobaric surface";
        String units "kg/kg";
        String abbreviation "GRLE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-1-32_L100";
        Int32 Grib2_Parameter 0, 1, 32;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Graupel (snow pellets)";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Haines_index_surface {
        String long_name "Haines index @ Ground or water surface";
        String units "";
        String abbreviation "HINDEX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_2-4-2_L1";
        Int32 Grib2_Parameter 2, 4, 2;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Fire Weather Products";
        String Grib2_Parameter_Name "Haines index";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    ICAO_Standard_Atmosphere_Reference_Height_maximum_wind {
        String long_name "ICAO Standard Atmosphere Reference Height @ Maximum wind level";
        String units "m";
        String abbreviation "ICAHT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-3_L6";
        Int32 Grib2_Parameter 0, 3, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "ICAO Standard Atmosphere Reference Height";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    ICAO_Standard_Atmosphere_Reference_Height_tropopause {
        String long_name "ICAO Standard Atmosphere Reference Height @ Tropopause";
        String units "m";
        String abbreviation "ICAHT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-3_L7";
        Int32 Grib2_Parameter 0, 3, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "ICAO Standard Atmosphere Reference Height";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ice_cover_surface {
        String long_name "Ice cover @ Ground or water surface";
        String units "";
        String abbreviation "ICEC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_10-2-0_L1";
        Int32 Grib2_Parameter 10, 2, 0;
        String Grib2_Parameter_Discipline "Oceanographic products";
        String Grib2_Parameter_Category "Ice";
        String Grib2_Parameter_Name "Ice cover";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ice_growth_rate_altitude_above_msl {
        String long_name "Ice growth rate @ Specific altitude above mean sea level";
        String units "m/s";
        String abbreviation "ICEG";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 altitude_above_msl1 lat lon ";
        String Grib_Variable_Id "VAR_10-2-6_L102";
        Int32 Grib2_Parameter 10, 2, 6;
        String Grib2_Parameter_Discipline "Oceanographic products";
        String Grib2_Parameter_Category "Ice";
        String Grib2_Parameter_Name "Ice growth rate";
        Int32 Grib2_Level_Type 102;
        String Grib2_Level_Desc "Specific altitude above mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ice_water_mixing_ratio_hybrid {
        String long_name "Ice water mixing ratio @ Hybrid level";
        String units "kg/kg";
        String abbreviation "ICMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 hybrid lat lon ";
        String Grib_Variable_Id "VAR_0-1-23_L105";
        Int32 Grib2_Parameter 0, 1, 23;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Ice water mixing ratio";
        Int32 Grib2_Level_Type 105;
        String Grib2_Level_Desc "Hybrid level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ice_water_mixing_ratio_isobaric {
        String long_name "Ice water mixing ratio @ Isobaric surface";
        String units "kg/kg";
        String abbreviation "ICMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-1-23_L100";
        Int32 Grib2_Parameter 0, 1, 23;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Ice water mixing ratio";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Land_cover_0__sea_1__land_surface {
        String long_name "Land cover (0 = sea, 1 = land) @ Ground or water surface";
        String units "";
        String abbreviation "LAND";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_2-0-0_L1";
        Int32 Grib2_Parameter 2, 0, 0;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Land cover (0 = sea, 1 = land)";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Latent_heat_net_flux_surface_Mixed_intervals_Average {
        String long_name "Latent heat net flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "LHTFL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-0-10_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 0, 10;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Latent heat net flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Maximum_temperature_height_above_ground_Mixed_intervals_Maximum {
        String long_name "Maximum temperature (Mixed_intervals Maximum) @ Specified height level above ground";
        String units "K";
        String abbreviation "TMAX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 height_above_ground lat lon ";
        String Grib_Statistical_Interval_Type "Maximum";
        String Grib_Variable_Id "VAR_0-0-4_L103_Imixed_S2";
        Int32 Grib2_Parameter 0, 0, 4;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Maximum temperature";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Minimum_temperature_height_above_ground_Mixed_intervals_Minimum {
        String long_name "Minimum temperature (Mixed_intervals Minimum) @ Specified height level above ground";
        String units "K";
        String abbreviation "TMIN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 height_above_ground lat lon ";
        String Grib_Statistical_Interval_Type "Minimum";
        String Grib_Variable_Id "VAR_0-0-5_L103_Imixed_S3";
        Int32 Grib2_Parameter 0, 0, 5;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Minimum temperature";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Momentum_flux_u-component_surface_Mixed_intervals_Average {
        String long_name "Momentum flux, u-component (Mixed_intervals Average) @ Ground or water surface";
        String units "N.m-2";
        String abbreviation "UFLX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-2-17_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 2, 17;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Momentum flux, u-component";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Momentum_flux_v-component_surface_Mixed_intervals_Average {
        String long_name "Momentum flux, v-component (Mixed_intervals Average) @ Ground or water surface";
        String units "N.m-2";
        String abbreviation "VFLX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-2-18_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 2, 18;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Momentum flux, v-component";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Per_cent_frozen_precipitation_surface {
        String long_name "Per cent frozen precipitation @ Ground or water surface";
        String units "%";
        String abbreviation "CPOFP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-39_L1";
        Int32 Grib2_Parameter 0, 1, 39;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Per cent frozen precipitation";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Potential_temperature_sigma {
        String long_name "Potential temperature @ Sigma level";
        String units "K";
        String abbreviation "POT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-0-2_L104";
        Int32 Grib2_Parameter 0, 0, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Potential temperature";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Precipitable_water_entire_atmosphere_single_layer {
        String long_name "Precipitable water @ Entire atmosphere layer";
        String units "kg.m-2";
        String abbreviation "PWAT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-3_L200";
        Int32 Grib2_Parameter 0, 1, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Precipitable water";
        Int32 Grib2_Level_Type 200;
        String Grib2_Level_Desc "Entire atmosphere layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Precipitation_rate_surface {
        String long_name "Precipitation rate @ Ground or water surface";
        String units "kg.m-2.s-1";
        String abbreviation "PRATE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-7_L1";
        Int32 Grib2_Parameter 0, 1, 7;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Precipitation rate";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Precipitation_rate_surface_Mixed_intervals_Average {
        String long_name "Precipitation rate (Mixed_intervals Average) @ Ground or water surface";
        String units "kg.m-2.s-1";
        String abbreviation "PRATE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-7_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 7;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Precipitation rate";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_potential_vorticity_surface {
        String long_name "Pressure @ Potential vorticity surface";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L109";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_high_cloud_bottom_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ High cloud bottom level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L232_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 232;
        String Grib2_Level_Desc "High cloud bottom level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_middle_cloud_bottom_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ Middle cloud bottom level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L222_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 222;
        String Grib2_Level_Desc "Middle cloud bottom level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_middle_cloud_top_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ Middle cloud top level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L223_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 223;
        String Grib2_Level_Desc "Middle cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_high_cloud_top_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ High cloud top level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L233_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 233;
        String Grib2_Level_Desc "High cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_convective_cloud_top {
        String long_name "Pressure @ Convective cloud top level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L243";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 243;
        String Grib2_Level_Desc "Convective cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_low_cloud_top_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ Low cloud top level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L213_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 213;
        String Grib2_Level_Desc "Low cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_low_cloud_bottom_Mixed_intervals_Average {
        String long_name "Pressure (Mixed_intervals Average) @ Low cloud bottom level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-0_L212_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 212;
        String Grib2_Level_Desc "Low cloud bottom level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_convective_cloud_bottom {
        String long_name "Pressure @ Convective cloud bottom level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L242";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 242;
        String Grib2_Level_Desc "Convective cloud bottom level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_surface {
        String long_name "Pressure @ Ground or water surface";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L1";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_maximum_wind {
        String long_name "Pressure @ Maximum wind level";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L6";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_tropopause {
        String long_name "Pressure @ Tropopause";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L7";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_height_above_ground {
        String long_name "Pressure @ Specified height level above ground";
        String units "Pa";
        String abbreviation "PRES";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-0_L103";
        Int32 Grib2_Parameter 0, 3, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_reduced_to_MSL_msl {
        String long_name "Pressure reduced to MSL @ Mean sea level";
        String units "Pa";
        String abbreviation "PRMSL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-1_L101";
        Int32 Grib2_Parameter 0, 3, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure reduced to MSL";
        Int32 Grib2_Level_Type 101;
        String Grib2_Level_Desc "Mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Rain_mixing_ratio_hybrid {
        String long_name "Rain mixing ratio @ Hybrid level";
        String units "kg/kg";
        String abbreviation "RWMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 hybrid lat lon ";
        String Grib_Variable_Id "VAR_0-1-24_L105";
        Int32 Grib2_Parameter 0, 1, 24;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Rain mixing ratio";
        Int32 Grib2_Level_Type 105;
        String Grib2_Level_Desc "Hybrid level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Rain_mixing_ratio_isobaric {
        String long_name "Rain mixing ratio @ Isobaric surface";
        String units "kg/kg";
        String abbreviation "RWMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-1-24_L100";
        Int32 Grib2_Parameter 0, 1, 24;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Rain mixing ratio";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_highest_tropospheric_freezing {
        String long_name "Relative humidity @ Highest tropospheric freezing level";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L204";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 204;
        String Grib2_Level_Desc "Highest tropospheric freezing level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_entire_atmosphere_single_layer {
        String long_name "Relative humidity @ Entire atmosphere layer";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L200";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 200;
        String Grib2_Level_Desc "Entire atmosphere layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_sigma_layer {
        String long_name "Relative humidity @ Sigma level layer";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma_layer lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L104_layer";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_pressure_difference_layer {
        String long_name "Relative humidity @ Level at specified pressure difference from ground to level layer";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer2 lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L108_layer";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_isobaric {
        String long_name "Relative humidity @ Isobaric surface";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L100";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_zeroDegC_isotherm {
        String long_name "Relative humidity @ Level of 0 °C isotherm";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L4";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 4;
        String Grib2_Level_Desc "Level of 0 °C isotherm";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_height_above_ground {
        String long_name "Relative humidity @ Specified height level above ground";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L103";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Relative_humidity_sigma {
        String long_name "Relative humidity @ Sigma level";
        String units "%";
        String abbreviation "RH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-1-1_L104";
        Int32 Grib2_Parameter 0, 1, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Relative humidity";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Sensible_heat_net_flux_surface_Mixed_intervals_Average {
        String long_name "Sensible heat net flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "SHTFL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-0-11_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 0, 11;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Sensible heat net flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Snow_depth_surface {
        String long_name "Snow depth @ Ground or water surface";
        String units "m";
        String abbreviation "SNOD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-11_L1";
        Int32 Grib2_Parameter 0, 1, 11;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Snow depth";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Snow_mixing_ratio_hybrid {
        String long_name "Snow mixing ratio @ Hybrid level";
        String units "kg/kg";
        String abbreviation "SNMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 hybrid lat lon ";
        String Grib_Variable_Id "VAR_0-1-25_L105";
        Int32 Grib2_Parameter 0, 1, 25;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Snow mixing ratio";
        Int32 Grib2_Level_Type 105;
        String Grib2_Level_Desc "Hybrid level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Snow_mixing_ratio_isobaric {
        String long_name "Snow mixing ratio @ Isobaric surface";
        String units "kg/kg";
        String abbreviation "SNMR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-1-25_L100";
        Int32 Grib2_Parameter 0, 1, 25;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Snow mixing ratio";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Soil_temperature_depth_below_surface_layer {
        String long_name "Soil temperature @ Depth below land surface layer";
        String units "K";
        String abbreviation "TSOIL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 depth_below_surface_layer lat lon ";
        String Grib_Variable_Id "VAR_2-0-2_L106_layer";
        Int32 Grib2_Parameter 2, 0, 2;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Soil temperature";
        Int32 Grib2_Level_Type 106;
        String Grib2_Level_Desc "Depth below land surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Specific_humidity_pressure_difference_layer {
        String long_name "Specific humidity @ Level at specified pressure difference from ground to level layer";
        String units "kg/kg";
        String abbreviation "SPFH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer2 lat lon ";
        String Grib_Variable_Id "VAR_0-1-0_L108_layer";
        Int32 Grib2_Parameter 0, 1, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Specific humidity";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Specific_humidity_height_above_ground {
        String long_name "Specific humidity @ Specified height level above ground";
        String units "kg/kg";
        String abbreviation "SPFH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground1 lat lon ";
        String Grib_Variable_Id "VAR_0-1-0_L103";
        Int32 Grib2_Parameter 0, 1, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Specific humidity";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Storm_relative_helicity_height_above_ground_layer {
        String long_name "Storm relative helicity @ Specified height level above ground layer";
        String units "J/kg";
        String abbreviation "HLCY";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground_layer lat lon ";
        String Grib_Variable_Id "VAR_0-7-8_L103_layer";
        Int32 Grib2_Parameter 0, 7, 8;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Storm relative helicity";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_potential_vorticity_surface {
        String long_name "Temperature @ Potential vorticity surface";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L109";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_low_cloud_top_Mixed_intervals_Average {
        String long_name "Temperature (Mixed_intervals Average) @ Low cloud top level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-0-0_L213_Imixed_S0";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 213;
        String Grib2_Level_Desc "Low cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_middle_cloud_top_Mixed_intervals_Average {
        String long_name "Temperature (Mixed_intervals Average) @ Middle cloud top level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-0-0_L223_Imixed_S0";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 223;
        String Grib2_Level_Desc "Middle cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_high_cloud_top_Mixed_intervals_Average {
        String long_name "Temperature (Mixed_intervals Average) @ High cloud top level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-0-0_L233_Imixed_S0";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 233;
        String Grib2_Level_Desc "High cloud top level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_surface {
        String long_name "Temperature @ Ground or water surface";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L1";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_pressure_difference_layer {
        String long_name "Temperature @ Level at specified pressure difference from ground to level layer";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer2 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L108_layer";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_isobaric {
        String long_name "Temperature @ Isobaric surface";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric6 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L100";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_maximum_wind {
        String long_name "Temperature @ Maximum wind level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L6";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_altitude_above_msl {
        String long_name "Temperature @ Specific altitude above mean sea level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 altitude_above_msl lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L102";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 102;
        String Grib2_Level_Desc "Specific altitude above mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_height_above_ground {
        String long_name "Temperature @ Specified height level above ground";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground2 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L103";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_tropopause {
        String long_name "Temperature @ Tropopause";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L7";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Temperature_sigma {
        String long_name "Temperature @ Sigma level";
        String units "K";
        String abbreviation "TMP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-0-0_L104";
        Int32 Grib2_Parameter 0, 0, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Temperature";
        String Grib2_Parameter_Name "Temperature";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_entire_atmosphere_Mixed_intervals_Average {
        String long_name "Total cloud cover (Mixed_intervals Average) @ Entire atmosphere";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-1_L10_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 10;
        String Grib2_Level_Desc "Entire atmosphere";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_convective_cloud {
        String long_name "Total cloud cover @ Convective cloud layer";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 lat lon ";
        String Grib_Variable_Id "VAR_0-6-1_L244";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 244;
        String Grib2_Level_Desc "Convective cloud layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_middle_cloud_Mixed_intervals_Average {
        String long_name "Total cloud cover (Mixed_intervals Average) @ Middle cloud layer";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-1_L224_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 224;
        String Grib2_Level_Desc "Middle cloud layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_low_cloud_Mixed_intervals_Average {
        String long_name "Total cloud cover (Mixed_intervals Average) @ Low cloud layer";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-1_L214_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 214;
        String Grib2_Level_Desc "Low cloud layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_boundary_layer_cloud_Mixed_intervals_Average {
        String long_name "Total cloud cover (Mixed_intervals Average) @ Boundary layer cloud layer";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-1_L211_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 211;
        String Grib2_Level_Desc "Boundary layer cloud layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_high_cloud_Mixed_intervals_Average {
        String long_name "Total cloud cover (Mixed_intervals Average) @ High cloud layer";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-1_L234_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 234;
        String Grib2_Level_Desc "High cloud layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_cloud_cover_isobaric {
        String long_name "Total cloud cover @ Isobaric surface";
        String units "%";
        String abbreviation "TCDC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric7 lat lon ";
        String Grib_Variable_Id "VAR_0-6-1_L100";
        Int32 Grib2_Parameter 0, 6, 1;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Total cloud cover";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_ozone_entire_atmosphere_single_layer {
        String long_name "Total ozone @ Entire atmosphere layer";
        String units "DU";
        String abbreviation "TOZNE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-14-0_L200";
        Int32 Grib2_Parameter 0, 14, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Trace gases";
        String Grib2_Parameter_Name "Total ozone";
        Int32 Grib2_Level_Type 200;
        String Grib2_Level_Desc "Entire atmosphere layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Total_precipitation_surface_Mixed_intervals_Accumulation {
        String long_name "Total precipitation (Mixed_intervals Accumulation) @ Ground or water surface";
        String units "kg.m-2";
        String abbreviation "APCP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time lat lon ";
        String Grib_Statistical_Interval_Type "Accumulation";
        String Grib_Variable_Id "VAR_0-1-8_L1_Imixed_S1";
        Int32 Grib2_Parameter 0, 1, 8;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Total precipitation";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Rain_surface {
        String long_name "Categorical Rain @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CRAIN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-192_L1";
        Int32 Grib2_Parameter 0, 1, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Rain";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Rain_surface_Mixed_intervals_Average {
        String long_name "Categorical Rain (Mixed_intervals Average) @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CRAIN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-192_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Rain";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Freezing_Rain_surface {
        String long_name "Categorical Freezing Rain @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CFRZR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-193_L1";
        Int32 Grib2_Parameter 0, 1, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Freezing Rain";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Freezing_Rain_surface_Mixed_intervals_Average {
        String long_name "Categorical Freezing Rain (Mixed_intervals Average) @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CFRZR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-193_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Freezing Rain";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Ice_Pellets_surface {
        String long_name "Categorical Ice Pellets @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CICEP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-194_L1";
        Int32 Grib2_Parameter 0, 1, 194;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Ice Pellets";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Ice_Pellets_surface_Mixed_intervals_Average {
        String long_name "Categorical Ice Pellets (Mixed_intervals Average) @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CICEP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-194_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 194;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Ice Pellets";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Snow_surface {
        String long_name "Categorical Snow @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CSNOW";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-195_L1";
        Int32 Grib2_Parameter 0, 1, 195;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Snow";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Categorical_Snow_surface_Mixed_intervals_Average {
        String long_name "Categorical Snow (Mixed_intervals Average) @ Ground or water surface";
        String units "Code table 4.222";
        String abbreviation "CSNOW";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-195_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 195;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Categorical Snow";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Convective_Precipitation_Rate_surface_Mixed_intervals_Average {
        String long_name "Convective Precipitation Rate (Mixed_intervals Average) @ Ground or water surface";
        String units "kg.m-2.s-1";
        String abbreviation "CPRAT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-1-196_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 1, 196;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Convective Precipitation Rate";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Potential_Evaporation_Rate_surface {
        String long_name "Potential Evaporation Rate @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "PEVPR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 lat lon ";
        String Grib_Variable_Id "VAR_0-1-200_L1";
        Int32 Grib2_Parameter 0, 1, 200;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Potential Evaporation Rate";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ozone_Mixing_Ratio_isobaric {
        String long_name "Ozone Mixing Ratio @ Isobaric surface";
        String units "kg.kg-1";
        String abbreviation "O3MR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric5 lat lon ";
        String Grib_Variable_Id "VAR_0-14-192_L100";
        Int32 Grib2_Parameter 0, 14, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Trace gases";
        String Grib2_Parameter_Name "Ozone Mixing Ratio";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Composite_reflectivity_entire_atmosphere {
        String long_name "Composite reflectivity @ Entire atmosphere";
        String units "dB";
        String abbreviation "REFC";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-16-196_L10";
        Int32 Grib2_Parameter 0, 16, 196;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Forecast Radar Imagery";
        String Grib2_Parameter_Name "Composite reflectivity";
        Int32 Grib2_Level_Type 10;
        String Grib2_Level_Desc "Entire atmosphere";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Icing_Severity_isobaric {
        String long_name "Icing Severity @ Isobaric surface";
        String units "non-dim";
        String abbreviation "ICSEV";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time1 isobaric2 lat lon ";
        String Grib_Variable_Id "VAR_0-19-234_L100";
        Int32 Grib2_Parameter 0, 19, 234;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Physical atmospheric Properties";
        String Grib2_Parameter_Name "Icing Severity";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Vertical_Speed_Shear_potential_vorticity_surface {
        String long_name "Vertical Speed Shear @ Potential vorticity surface";
        String units "s-1";
        String abbreviation "VWSH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-2-192_L109";
        Int32 Grib2_Parameter 0, 2, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Vertical Speed Shear";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Vertical_Speed_Shear_tropopause {
        String long_name "Vertical Speed Shear @ Tropopause";
        String units "s-1";
        String abbreviation "VWSH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-192_L7";
        Int32 Grib2_Parameter 0, 2, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Vertical Speed Shear";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    U-Component_Storm_Motion_height_above_ground_layer {
        String long_name "U-Component Storm Motion @ Specified height level above ground layer";
        String units "m.s-1";
        String abbreviation "USTM";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground_layer1 lat lon ";
        String Grib_Variable_Id "VAR_0-2-194_L103_layer";
        Int32 Grib2_Parameter 0, 2, 194;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "U-Component Storm Motion";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    V-Component_Storm_Motion_height_above_ground_layer {
        String long_name "V-Component Storm Motion @ Specified height level above ground layer";
        String units "m.s-1";
        String abbreviation "VSTM";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground_layer1 lat lon ";
        String Grib_Variable_Id "VAR_0-2-195_L103_layer";
        Int32 Grib2_Parameter 0, 2, 195;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "V-Component Storm Motion";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ventilation_Rate_planetary_boundary {
        String long_name "Ventilation Rate @ Planetary Boundary Layer";
        String units "m2.s-1";
        String abbreviation "VRATE";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-224_L220";
        Int32 Grib2_Parameter 0, 2, 224;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Ventilation Rate";
        Int32 Grib2_Level_Type 220;
        String Grib2_Level_Desc "Planetary Boundary Layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    MSLP_Eta_model_reduction_msl {
        String long_name "MSLP (Eta model reduction) @ Mean sea level";
        String units "Pa";
        String abbreviation "MSLET";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-192_L101";
        Int32 Grib2_Parameter 0, 3, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "MSLP (Eta model reduction)";
        Int32 Grib2_Level_Type 101;
        String Grib2_Level_Desc "Mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    5-Wave_Geopotential_Height_isobaric {
        String long_name "5-Wave Geopotential Height @ Isobaric surface";
        String units "gpm";
        String abbreviation "5WAVH";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-193_L100";
        Int32 Grib2_Parameter 0, 3, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "5-Wave Geopotential Height";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Zonal_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average {
        String long_name "Zonal Flux of Gravity Wave Stress (Mixed_intervals Average) @ Ground or water surface";
        String units "N.m-2";
        String abbreviation "U-GWD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-194_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 194;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Zonal Flux of Gravity Wave Stress";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Meridional_Flux_of_Gravity_Wave_Stress_surface_Mixed_intervals_Average {
        String long_name "Meridional Flux of Gravity Wave Stress (Mixed_intervals Average) @ Ground or water surface";
        String units "N.m-2";
        String abbreviation "V-GWD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-3-195_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 3, 195;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Meridional Flux of Gravity Wave Stress";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Planetary_Boundary_Layer_Height_surface {
        String long_name "Planetary Boundary Layer Height @ Ground or water surface";
        String units "m";
        String abbreviation "HPBL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-3-196_L1";
        Int32 Grib2_Parameter 0, 3, 196;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Planetary Boundary Layer Height";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Pressure_of_level_from_which_parcel_was_lifted_pressure_difference_layer {
        String long_name "Pressure of level from which parcel was lifted @ Level at specified pressure difference from ground to level layer";
        String units "Pa";
        String abbreviation "PLPL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer lat lon ";
        String Grib_Variable_Id "VAR_0-3-200_L108_layer";
        Int32 Grib2_Parameter 0, 3, 200;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Mass";
        String Grib2_Parameter_Name "Pressure of level from which parcel was lifted";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Downward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average {
        String long_name "Downward Short-Wave Radiation Flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "DSWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-4-192_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 4, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Short wave radiation";
        String Grib2_Parameter_Name "Downward Short-Wave Radiation Flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Upward_Short-Wave_Radiation_Flux_surface_Mixed_intervals_Average {
        String long_name "Upward Short-Wave Radiation Flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "USWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-4-193_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 4, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Short wave radiation";
        String Grib2_Parameter_Name "Upward Short-Wave Radiation Flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Upward_Short-Wave_Radiation_Flux_atmosphere_top_Mixed_intervals_Average {
        String long_name "Upward Short-Wave Radiation Flux (Mixed_intervals Average) @ Nominal top of the atmosphere";
        String units "W.m-2";
        String abbreviation "USWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-4-193_L8_Imixed_S0";
        Int32 Grib2_Parameter 0, 4, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Short wave radiation";
        String Grib2_Parameter_Name "Upward Short-Wave Radiation Flux";
        Int32 Grib2_Level_Type 8;
        String Grib2_Level_Desc "Nominal top of the atmosphere";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Downward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average {
        String long_name "Downward Long-Wave Rad. Flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "DLWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-5-192_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 5, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Long wave radiation";
        String Grib2_Parameter_Name "Downward Long-Wave Rad. Flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Upward_Long-Wave_Radp_Flux_surface_Mixed_intervals_Average {
        String long_name "Upward Long-Wave Rad. Flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "ULWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-5-193_L1_Imixed_S0";
        Int32 Grib2_Parameter 0, 5, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Long wave radiation";
        String Grib2_Parameter_Name "Upward Long-Wave Rad. Flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Upward_Long-Wave_Radp_Flux_atmosphere_top_Mixed_intervals_Average {
        String long_name "Upward Long-Wave Rad. Flux (Mixed_intervals Average) @ Nominal top of the atmosphere";
        String units "W.m-2";
        String abbreviation "ULWRF";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-5-193_L8_Imixed_S0";
        Int32 Grib2_Parameter 0, 5, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Long wave radiation";
        String Grib2_Parameter_Name "Upward Long-Wave Rad. Flux";
        Int32 Grib2_Level_Type 8;
        String Grib2_Level_Desc "Nominal top of the atmosphere";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Cloud_Work_Function_entire_atmosphere_single_layer_Mixed_intervals_Average {
        String long_name "Cloud Work Function (Mixed_intervals Average) @ Entire atmosphere layer";
        String units "J.kg-1";
        String abbreviation "CWORK";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_0-6-193_L200_Imixed_S0";
        Int32 Grib2_Parameter 0, 6, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Cloud Work Function";
        Int32 Grib2_Level_Type 200;
        String Grib2_Level_Desc "Entire atmosphere layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Sunshine_Duration_surface {
        String long_name "Sunshine Duration @ Ground or water surface";
        String units "s";
        String abbreviation "SUNSD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-6-201_L1";
        Int32 Grib2_Parameter 0, 6, 201;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Cloud";
        String Grib2_Parameter_Name "Sunshine Duration";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Surface_Lifted_Index_surface {
        String long_name "Surface Lifted Index @ Ground or water surface";
        String units "K";
        String abbreviation "LFT X";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-7-192_L1";
        Int32 Grib2_Parameter 0, 7, 192;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Surface Lifted Index";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Best_4_layer_Lifted_Index_surface {
        String long_name "Best (4 layer) Lifted Index @ Ground or water surface";
        String units "K";
        String abbreviation "4LFTX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-7-193_L1";
        Int32 Grib2_Parameter 0, 7, 193;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Thermodynamic stability indices";
        String Grib2_Parameter_Name "Best (4 layer) Lifted Index";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Volumetric_Soil_Moisture_Content_depth_below_surface_layer {
        String long_name "Volumetric Soil Moisture Content @ Depth below land surface layer";
        String units "Fraction";
        String abbreviation "SOILW";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 depth_below_surface_layer lat lon ";
        String Grib_Variable_Id "VAR_2-0-192_L106_layer";
        Int32 Grib2_Parameter 2, 0, 192;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Volumetric Soil Moisture Content";
        Int32 Grib2_Level_Type 106;
        String Grib2_Level_Desc "Depth below land surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Ground_Heat_Flux_surface_Mixed_intervals_Average {
        String long_name "Ground Heat Flux (Mixed_intervals Average) @ Ground or water surface";
        String units "W.m-2";
        String abbreviation "GFLUX";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Average";
        String Grib_Variable_Id "VAR_2-0-193_L1_Imixed_S0";
        Int32 Grib2_Parameter 2, 0, 193;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Ground Heat Flux";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Wilting_Point_surface {
        String long_name "Wilting Point @ Ground or water surface";
        String units "Fraction";
        String abbreviation "WILT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_2-0-201_L1";
        Int32 Grib2_Parameter 2, 0, 201;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Wilting Point";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Land-sea_coverage_nearest_neighbor_land1sea0_surface {
        String long_name "Land-sea coverage (nearest neighbor) [land=1,sea=0] @ Ground or water surface";
        String units "";
        String abbreviation "LANDN";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_2-0-218_L1";
        Int32 Grib2_Parameter 2, 0, 218;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Land-sea coverage (nearest neighbor) [land=1,sea=0]";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Field_Capacity_surface {
        String long_name "Field Capacity @ Ground or water surface";
        String units "Fraction";
        String abbreviation "FLDCP";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_2-3-203_L1";
        Int32 Grib2_Parameter 2, 3, 203;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Soil Products";
        String Grib2_Parameter_Name "Field Capacity";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Vertical_velocity_geometric_isobaric {
        String long_name "Vertical velocity (geometric) @ Isobaric surface";
        String units "m/s";
        String abbreviation "DZDT";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric4 lat lon ";
        String Grib_Variable_Id "VAR_0-2-9_L100";
        Int32 Grib2_Parameter 0, 2, 9;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Vertical velocity (geometric)";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Vertical_velocity_pressure_isobaric {
        String long_name "Vertical velocity (pressure) @ Isobaric surface";
        String units "Pa/s";
        String abbreviation "VVEL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric4 lat lon ";
        String Grib_Variable_Id "VAR_0-2-8_L100";
        Int32 Grib2_Parameter 0, 2, 8;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Vertical velocity (pressure)";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Vertical_velocity_pressure_sigma {
        String long_name "Vertical velocity (pressure) @ Sigma level";
        String units "Pa/s";
        String abbreviation "VVEL";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-2-8_L104";
        Int32 Grib2_Parameter 0, 2, 8;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Vertical velocity (pressure)";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Visibility_surface {
        String long_name "Visibility @ Ground or water surface";
        String units "m";
        String abbreviation "VIS";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-19-0_L1";
        Int32 Grib2_Parameter 0, 19, 0;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Physical atmospheric Properties";
        String Grib2_Parameter_Name "Visibility";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Water_equivalent_of_accumulated_snow_depth_surface {
        String long_name "Water equivalent of accumulated snow depth @ Ground or water surface";
        String units "kg.m-2";
        String abbreviation "WEASD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-1-13_L1";
        Int32 Grib2_Parameter 0, 1, 13;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Moisture";
        String Grib2_Parameter_Name "Water equivalent of accumulated snow depth";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Water_runoff_surface_Mixed_intervals_Accumulation {
        String long_name "Water runoff (Mixed_intervals Accumulation) @ Ground or water surface";
        String units "kg.m-2";
        String abbreviation "WATR";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time2 lat lon ";
        String Grib_Statistical_Interval_Type "Accumulation";
        String Grib_Variable_Id "VAR_2-0-5_L1_Imixed_S1";
        Int32 Grib2_Parameter 2, 0, 5;
        String Grib2_Parameter_Discipline "Land surface products";
        String Grib2_Parameter_Category "Vegetation/Biomass";
        String Grib2_Parameter_Name "Water runoff";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    Wind_speed_gust_surface {
        String long_name "Wind speed (gust) @ Ground or water surface";
        String units "m/s";
        String abbreviation "GUST";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-22_L1";
        Int32 Grib2_Parameter 0, 2, 22;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "Wind speed (gust)";
        Int32 Grib2_Level_Type 1;
        String Grib2_Level_Desc "Ground or water surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_planetary_boundary {
        String long_name "u-component of wind @ Planetary Boundary Layer";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L220";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 220;
        String Grib2_Level_Desc "Planetary Boundary Layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_potential_vorticity_surface {
        String long_name "u-component of wind @ Potential vorticity surface";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L109";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_pressure_difference_layer {
        String long_name "u-component of wind @ Level at specified pressure difference from ground to level layer";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer2 lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L108_layer";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_isobaric {
        String long_name "u-component of wind @ Isobaric surface";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L100";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_altitude_above_msl {
        String long_name "u-component of wind @ Specific altitude above mean sea level";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 altitude_above_msl lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L102";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 102;
        String Grib2_Level_Desc "Specific altitude above mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_maximum_wind {
        String long_name "u-component of wind @ Maximum wind level";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L6";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_height_above_ground {
        String long_name "u-component of wind @ Specified height level above ground";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground4 lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L103";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_tropopause {
        String long_name "u-component of wind @ Tropopause";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L7";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    u-component_of_wind_sigma {
        String long_name "u-component of wind @ Sigma level";
        String units "m/s";
        String abbreviation "UGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-2-2_L104";
        Int32 Grib2_Parameter 0, 2, 2;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "u-component of wind";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_planetary_boundary {
        String long_name "v-component of wind @ Planetary Boundary Layer";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L220";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 220;
        String Grib2_Level_Desc "Planetary Boundary Layer";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_potential_vorticity_surface {
        String long_name "v-component of wind @ Potential vorticity surface";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 potential_vorticity_surface lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L109";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 109;
        String Grib2_Level_Desc "Potential vorticity surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_pressure_difference_layer {
        String long_name "v-component of wind @ Level at specified pressure difference from ground to level layer";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 pressure_difference_layer2 lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L108_layer";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 108;
        String Grib2_Level_Desc "Level at specified pressure difference from ground to level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_isobaric {
        String long_name "v-component of wind @ Isobaric surface";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 isobaric lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L100";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 100;
        String Grib2_Level_Desc "Isobaric surface";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_maximum_wind {
        String long_name "v-component of wind @ Maximum wind level";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L6";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 6;
        String Grib2_Level_Desc "Maximum wind level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_altitude_above_msl {
        String long_name "v-component of wind @ Specific altitude above mean sea level";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 altitude_above_msl lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L102";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 102;
        String Grib2_Level_Desc "Specific altitude above mean sea level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_height_above_ground {
        String long_name "v-component of wind @ Specified height level above ground";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 height_above_ground4 lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L103";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 103;
        String Grib2_Level_Desc "Specified height level above ground";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_tropopause {
        String long_name "v-component of wind @ Tropopause";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L7";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 7;
        String Grib2_Level_Desc "Tropopause";
        String Grib2_Generating_Process_Type "Forecast";
    }
    v-component_of_wind_sigma {
        String long_name "v-component of wind @ Sigma level";
        String units "m/s";
        String abbreviation "VGRD";
        Float32 missing_value NaN;
        String grid_mapping "LatLon_Projection";
        String coordinates "reftime time3 sigma lat lon ";
        String Grib_Variable_Id "VAR_0-2-3_L104";
        Int32 Grib2_Parameter 0, 2, 3;
        String Grib2_Parameter_Discipline "Meteorological products";
        String Grib2_Parameter_Category "Momentum";
        String Grib2_Parameter_Name "v-component of wind";
        Int32 Grib2_Level_Type 104;
        String Grib2_Level_Desc "Sigma level";
        String Grib2_Generating_Process_Type "Forecast";
    }
    lat {
        String units "degrees_north";
        String _CoordinateAxisType "Lat";
    }
    lon {
        String units "degrees_east";
        String _CoordinateAxisType "Lon";
    }
    reftime {
        String units "Hour since 2020-10-06T06:00:00Z";
        String standard_name "forecast_reference_time";
        String long_name "GRIB reference time";
        String calendar "proleptic_gregorian";
        String _CoordinateAxisType "RunTime";
    }
    time {
        String units "Hour since 2020-10-06T06:00:00Z";
        String standard_name "time";
        String long_name "GRIB forecast or observation time";
        String calendar "proleptic_gregorian";
        String bounds "time_bounds";
        String _CoordinateAxisType "Time";
    }
    time1 {
        String units "Hour since 2020-10-06T06:00:00Z";
        String standard_name "time";
        String long_name "GRIB forecast or observation time";
        String calendar "proleptic_gregorian";
        String _CoordinateAxisType "Time";
    }
    time2 {
        String units "Hour since 2020-10-06T06:00:00Z";
        String standard_name "time";
        String long_name "GRIB forecast or observation time";
        String calendar "proleptic_gregorian";
        String bounds "time2_bounds";
        String _CoordinateAxisType "Time";
    }
    time3 {
        String units "Hour since 2020-10-06T06:00:00Z";
        String standard_name "time";
        String long_name "GRIB forecast or observation time";
        String calendar "proleptic_gregorian";
        String _CoordinateAxisType "Time";
    }
    isobaric {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    height_above_ground {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    sigma {
        String units "sigma";
        String long_name "Sigma level";
        String positive "down";
        Int32 Grib_level_type 104;
        String _CoordinateAxisType "GeoZ";
        String _CoordinateZisPositive "down";
    }
    isobaric1 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    isobaric2 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    isobaric3 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    height_above_ground1 {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    isobaric4 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    altitude_above_msl {
        String units "m";
        String long_name "Specific altitude above mean sea level";
        String positive "up";
        Int32 Grib_level_type 102;
        String datum "mean sea level";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    pressure_difference_layer {
        String units "Pa";
        String long_name "Level at specified pressure difference from ground to level";
        String positive "up";
        Int32 Grib_level_type 108;
        String datum "ground";
        String bounds "pressure_difference_layer_bounds";
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "up";
    }
    pressure_difference_layer1 {
        String units "Pa";
        String long_name "Level at specified pressure difference from ground to level";
        String positive "up";
        Int32 Grib_level_type 108;
        String datum "ground";
        String bounds "pressure_difference_layer1_bounds";
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "up";
    }
    sigma_layer {
        String units "sigma";
        String long_name "Sigma level";
        String positive "down";
        Int32 Grib_level_type 104;
        String bounds "sigma_layer_bounds";
        String _CoordinateAxisType "GeoZ";
        String _CoordinateZisPositive "down";
    }
    height_above_ground2 {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    isobaric5 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    potential_vorticity_surface {
        String units "K m2 kg-1 s-1";
        String long_name "Potential vorticity surface";
        String positive "up";
        Int32 Grib_level_type 109;
        String _CoordinateAxisType "GeoZ";
        String _CoordinateZisPositive "up";
    }
    height_above_ground_layer {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String bounds "height_above_ground_layer_bounds";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    depth_below_surface_layer {
        String units "m";
        String long_name "Depth below land surface";
        String positive "down";
        Int32 Grib_level_type 106;
        String datum "land surface";
        String bounds "depth_below_surface_layer_bounds";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "down";
    }
    pressure_difference_layer2 {
        String units "Pa";
        String long_name "Level at specified pressure difference from ground to level";
        String positive "up";
        Int32 Grib_level_type 108;
        String datum "ground";
        String bounds "pressure_difference_layer2_bounds";
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "up";
    }
    isobaric6 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    height_above_ground3 {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    isobaric7 {
        String units "Pa";
        String long_name "Isobaric surface";
        String positive "down";
        Int32 Grib_level_type 100;
        String _CoordinateAxisType "Pressure";
        String _CoordinateZisPositive "down";
    }
    height_above_ground_layer1 {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String bounds "height_above_ground_layer1_bounds";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    altitude_above_msl1 {
        String units "m";
        String long_name "Specific altitude above mean sea level";
        String positive "up";
        Int32 Grib_level_type 102;
        String datum "mean sea level";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    hybrid {
        String units "sigma";
        String long_name "Hybrid level";
        String positive "down";
        Int32 Grib_level_type 105;
        String _CoordinateAxisType "GeoZ";
        String _CoordinateZisPositive "down";
    }
    height_above_ground4 {
        String units "m";
        String long_name "Specified height level above ground";
        String positive "up";
        Int32 Grib_level_type 103;
        String datum "ground";
        String _CoordinateAxisType "Height";
        String _CoordinateZisPositive "up";
    }
    NC_GLOBAL {
        String Originating_or_generating_Center "US National Weather Service, National Centres for Environmental Prediction (NCEP)";
        String Originating_or_generating_Subcenter "0";
        String GRIB_table_version "2,1";
        String Type_of_generating_process "Forecast";
        String Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre "Analysis from GFS (Global Forecast System)";
        String file_format "GRIB-2";
        String Conventions "CF-1.6";
        String history "Read using CDM IOSP GribCollection v3";
        String featureType "GRID";
        String _CoordSysBuilder "ucar.nc2.dataset.conv.CF1Convention";
    }
}
