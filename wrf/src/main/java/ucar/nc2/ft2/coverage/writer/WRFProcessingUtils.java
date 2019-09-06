package ucar.nc2.ft2.coverage.writer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;

public class WRFProcessingUtils {

  private WRFProcessingUtils(){};

  // Processing Constants
  static final String METGRID_NAME     = "Metgrid_Name";
  static final String METGRID_UNIT     = "Metgrid_Unit";
  static final String METGRID_DESC     = "Metgrid_Desc";
  static final String GRIB_1_FILE_VER  = "GRIB-1";
  static final String GRIB1_PARAMETER  = "Grib1_Parameter";
  static final String GRIB1_LEVEL_TYPE = "Grib1_Level_Type";
  static final String GRIB_2_FILE_VER  = "GRIB-2";

  /**
   *   Required Field Names,  these fields are taken from:
   *   http://www2.mmm.ucar.edu/wrf/users/docs/user_guide_V3/users_guide_chap3.htm#_Required_Meteorological_Fields
   *
   *   The first 5 in this list are required twice, once for 3-d and once for 2-meter data.
   *   We do not have invariants to determine which is which, but we assume that
   *   "Level Type" (GRIB1) and "GRIB2 Level" for GRIB2 datasets is different for both
   *   of these so we use these values to differentiate between 3-d and 2 meter
   *
   *   Caveats:
   *   Only one of RH or SPECHUMD is required
   *   Pressure is only requried for non-isobaric datasets, we could only determine this by looking for special
   *   a normalized pressure level.
   *
   */
  public static final String TEMPERATURE      = "TT";
  public static final String REL_HUMD         = "RH";
  public static final String SPEC_HUMD        = "SPECHUMD";
  public static final String WIND_U           = "UU";
  public static final String WIND_V           = "VV";
  public static final String GEO_HEIGHT       = "HGT";    // test dataset has HGT not GHT, which is in one doc
  //public static final String PRESSURE_3D      = "PRESSURE";  // not dealing with this at this time, indicates a normalized custom level that
                                                             // would need translation "back to reality"
  public static final String PRESSURE_SURFACE = "PSFC";
  public static final String PRESSURE_SEA     = "PMSL";
  public static final String SKIN_TEMP        = "SKINTEMP";
  public static final String SOIL_HEIGHT      = "SOILHGT";
  public static final String LAND_SEA_MASK    = "LANDSEA";
  public static final String SOIL_MOIST_LAYER = "SM";
  public static final String SOIL_TEMP_LAYER  = "ST";
  public static final String SOIL_MOIST_LEVEL = "SOILM";
  public static final String SOIL_TEMP_LEVEL  = "SOILT";


  public static String validateRequiredElements(ArrayList<Coverage> cov){

    StringBuffer s = new StringBuffer();

    Map<String, Integer> required = new HashMap<>();
    required.put(TEMPERATURE,2);
    required.put(WIND_U,2);
    required.put(WIND_V,2);
    required.put(REL_HUMD,2);
    required.put(SPEC_HUMD,2);
    required.put(GEO_HEIGHT,1);
    required.put(PRESSURE_SURFACE,1);
    required.put(PRESSURE_SEA,1);
    required.put(SKIN_TEMP,1);
    required.put(SOIL_HEIGHT,1);
    required.put(LAND_SEA_MASK,1);
    required.put(SOIL_MOIST_LAYER,1);
    required.put(SOIL_TEMP_LAYER,1);
    required.put(SOIL_MOIST_LEVEL,1);
    required.put(SOIL_TEMP_LEVEL,1);

    int count = 0;
    String key = "";

    // processing phase
    for(Coverage c: cov) {

      Attribute att = c.findAttributeIgnoreCase(METGRID_NAME);

      if (att != null) {
        key = att.getStringValue();

        if (key.startsWith(SOIL_MOIST_LAYER) || key.startsWith(SOIL_TEMP_LAYER)) {
          String shortKey = key.substring(0, 2);
          count = required.get(shortKey);

          if ( count > 0 ) {

            required.replace(shortKey, count, count - 1);
            if (key.startsWith(SOIL_MOIST_LAYER)){
              required.put(SOIL_MOIST_LEVEL, count -1);
            }
            if ( key.startsWith(SOIL_TEMP_LAYER)){
              required.put(SOIL_TEMP_LEVEL, count -1);
            }
          }
        } else {
          if (required.containsKey(key)) {

            count = required.get(key);
            if (count > 0) {
              required.replace(key, count, count - 1);

              if (key.equals(REL_HUMD)) {
                required.put(SPEC_HUMD, count - 1);
              } else if (key.equals(SPEC_HUMD)) {
                required.put(REL_HUMD, count - 1);
              }
            }
          }
        }
      }
    }

    // testing phase
    for (Map.Entry<String, Integer> entry : required.entrySet()) {
      if( entry.getValue() != 0) {
        s.append(String.format("Field %s was not found in the dataset\n", entry.getKey()));
      }
    }

    if (s.length() > 0 ){
      s.insert(0,"\nFound the following issues:\n");
    }
  return s.toString();

  }

  public static ArrayList<Coverage> createCoverageListGrib1(CoverageCollection cv, ArrayList<VtableEntry> vt){

    ArrayList<Coverage> theCoverages = new ArrayList<>();

    for (VtableEntry v : vt) {

      int parm = Integer.parseInt(v.getParam());
      int levelType = Integer.parseInt(v.getLevelType());

      for(Coverage cov: cv.getCoverages()) {

        Attribute ab = cov.findAttributeIgnoreCase(GRIB1_PARAMETER);
        if (ab != null) {
          Number num = ab.getNumericValue();
          int n = num.intValue();
          if (n == parm) {
            Attribute level = cov.findAttributeIgnoreCase(GRIB1_LEVEL_TYPE);
            if (level != null) {
              Number nLevel = level.getNumericValue();
              int covLevel = nLevel.intValue();
              if (covLevel == levelType) {
                cov.getAttributes().add(new Attribute(METGRID_NAME, v.getMetgridName()));
                cov.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
                cov.getAttributes().add(new Attribute(METGRID_DESC, v.getMetgridDesc()));
                theCoverages.add(cov);
              }
            }
          }
        }
      }
    }
    return theCoverages;
  }

  public static ArrayList<Coverage> createCoverageListGrib2(CoverageCollection cv, ArrayList<VtableEntry> vt) {

    final String GRIB_VAR_ID = "Grib_Variable_Id";
    ArrayList<Coverage> theCoverages = new ArrayList<>();

    for (VtableEntry v : vt) {

      String varId = v.getGRIB2Var();
//      Coverage cov = cv.findCoverageByAttribute(GRIB_VAR_ID, varId);

//      public Coverage findCoverageByAttribute(String attName, String attValue) {
        for (Coverage cov : cv.getCoverages()) {
          boolean found = false;
          for (Attribute att : cov.getAttributes()) {
            // if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
            String nm = att.getShortName();
            String value = att.getStringValue();

            if (GRIB_VAR_ID.equals(att.getShortName()) && (att.getStringValue().startsWith(varId))) {
              found = true;
              break;
            }
          }
          if( found ){
            cov.getAttributes().add(new Attribute(METGRID_NAME, v.getMetgridName()));
            cov.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
            cov.getAttributes().add(new Attribute(METGRID_DESC, v.getMetgridDesc()));
            theCoverages.add(cov);
          }
        //return null;
      }
/******
      if (cov != null) {
        cov.getAttributes().add(new Attribute(METGRID_NAME, v.getMetgridName()));
        cov.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
        cov.getAttributes().add(new Attribute(METGRID_DESC, v.getMetgridDesc()));
        theCoverages.add(cov);
      }
 *********/
    }
    return theCoverages;
  }

}
