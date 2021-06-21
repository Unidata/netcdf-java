# cdmUnitTest/tds_index
### /CMC/RDPS/NA_15km/CMC_RDPS_NA_15km.ncx4
  **TwoD** Group PolarStereographic_399X493 (Center 60.143, -90.946)
   Accum{nrecords=2866004, ndups=0 (0.0 %), nmissing=1713 (0.1 %), rectMiss=1987 (0.1 %)}

  **TwoD** ***Group PolarStereographic_190X245 (Center 56.763, -99.393)
   Accum{nrecords=12908, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=130 (1.0 %)}
   
1. There are two separate grids here, with disjunct variables. These should possibly be separated into two datasets, perhaps in the LDM feed?   

### /FNMOC/FAROP/Global_1p0deg/FNMOC_FAROP_Global_1p0deg.ncx4
  **MRUTP** Group LatLon_181X360 (Center 90.0, .49999)
   Accum{nrecords=976, ndups=480 (49.2 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}
   
1. 100% duplicates, no obvious differences. 16/16   

### /FNMOC/NAVGEM/Global_0p5deg/FNMOC_NAVGEM_Global_0p5deg.ncx4
  **TwoD** Group LatLon_361X720 (Center 90.0, .24999)
   Accum{nrecords=2982127, ndups=56244 (1.9 %), nmissing=145890 (4.9 %), rectMiss=245444 (8.2 %)}

1. Look like exact duplicates? Mabe IDD artifact? Regular missing pattern in isobaric levels.

### /FNMOC/NCODA/Global_Ocean/FNMOC_NCODA_Global_Ocean.ncx4
  **MRUTP** Group LatLon_721X1440 (Center 90.0, .12499)
   Accum{nrecords=551, ndups=123 (22.3 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

1. Duplicates in a few regular fields, may be deliberate?

### /NCEP/NAM/Firewxnest/NAM-Firewxnest.ncx4
  **TwoD** Group LambertConformal_368X518 (Center 41.000, -106.07)
   Accum{nrecords=611724, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

  **TwoD** ***Group LambertConformal_384X634 (Center 40.029, -122.23)
   Accum{nrecords=1191252, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

  **TwoD** ***Group LambertConformal_380X609 (Center 37.019, -120.71)
   Accum{nrecords=611724, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

  **TwoD** ***Group LambertConformal_507X496 (Center 32.026, -89.995)
   Accum{nrecords=708312, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

  **TwoD** ***Group LambertConformal_508X515 (Center 37.517, -90.986)
   Accum{nrecords=708312, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

  **TwoD** ***Group LambertConformal_379X576 (Center 34.498, -117.97)
   Accum{nrecords=128784, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}
   
1. Moving nested grid - we dont handle this.   

### /NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4
  **TwoD** Group LambertConformal_1377X2145 (Center 38.226, -95.434)
   Accum{nrecords=206, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}

1. time2D **Fixed #728**

### /NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4
  **TwoD** Group LambertConformal_1377X2145 (Center 38.229, -95.438)
   Accum{nrecords=743936, ndups=44 (0.0 %), nmissing=0 (0.0 %), rectMiss=271199 (36.5 %)}

1. time2D **Fixed #728**
   
### /NCEP/SREF/CONUS_40km/ensprod_biasc/SREF-CONUS_40km_biasCorrected_Ensemble-derived_products.ncx4
  **TwoD** Group LambertConformal_129X185 (Center 40.787, -100.33)
   Accum{nrecords=320131, ndups=913152 (285.2 %), nmissing=0 (0.0 %), rectMiss=0 (0.0 %)}   

1. All have many duplicates, in some cases 7 duplicates for every valid record.
2. Guess: theres a bug in our code or theirs. Best way to proceed is compare to other software.

### /NOAA_GSD/HRRR/CONUS_3km/surface/GSD_HRRR_CONUS_3km_surface.ncx4
  **TwoD** Group LambertConformal_1059X1799 (Center 38.510, -97.488)
   Accum{nrecords=658827, ndups=13687 (2.1 %), nmissing=0 (0.0 %), rectMiss=345307 (52.4 %)}

1. dups: Vegetation surface: 3X, different data sizes.  
2: In addition, seems like table might be wrong. Issue #730

### /NOAA_GSD/HRRR/CONUS_3km/wrfprs/GSD_HRRR_CONUS_3km_wrfprs.ncx4
  **TwoD** Group LambertConformal_1059X1799 (Center 38.510, -97.488)
   Accum{nrecords=1197246, ndups=0 (0.0 %), nmissing=0 (0.0 %), rectMiss=1245168 (104.0 %)}
   
1. time2d
2. float DZDT_P0_L100_GLC0_isobaric(reftime=82, time1=27, isobaric=1, y=1059, x=1799);
        :coordinates = "reftime validtime1 time1 isobaric y x "; // string
        :description = "Vertical Velocity (Geometric)";
    double reftime(reftime=82);
    double validtime1(reftime=82, time1=27);  
    
    *** time1 is missing in the file, should be I think??
    double time1(time1=27);  
    double time1(reftime=82, time1=27);  
     
   this is the only variable like this, all the rest (14) are orthogional. 

   *** probably the mistake is that it shouldnt be listed in the coordinates attriibute
        :coordinates = "reftime validtime1 time1 isobaric y x "; 
      should be:
         :coordinates = "reftime validtime1 isobaric y x "; 
       
         
        