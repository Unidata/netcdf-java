/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Scanner;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;

public class WRFProcessingUtils {

  private WRFProcessingUtils(){};

  public static final String METGRID_NAME     = "Metgrid_Name";
  public static final String METGRID_UNIT     = "Metgrid_Unit";
  public static final String METGRID_DESC     = "Metgrid_Desc";

  static final String GRIB_1_FILE_VER  = "GRIB-1";
  static final String GRIB1_PARAMETER  = "Grib1_Parameter";
  static final String GRIB1_LEVEL_TYPE = "Grib1_Level_Type";
  static final String GRIB_2_FILE_VER  = "GRIB-2";
  static final String GRIB2_PARAMETER_NAME = "Grib2_Parameter_Name";


  static final String GRIB_VAR_ID = "Grib_Variable_Id";

  /**
   *  Required Field Names,  these fields are taken from:
   *  http://www2.mmm.ucar.edu/wrf/users/docs/user_guide_V3/users_guide_chap3.htm#_Required_Meteorological_Fields
   *
   *  Rules per the WRF documentation:
   *
   *  0.  The first 5 in this list are required twice, once for 3-d and once for 2-meter data.  Two sets of TT,RH (or SPECHUMD),
   *      UU and VV are required, with different Discipline|Category|Param|Level components.
   *      We do not have invariants to determine which is which, but we assume that
   *      "Level Type" (GRIB1) and "GRIB2 Level" for GRIB2 datasets is different for both
   *      of these so we use these values to differentiate between 3-d and 2 meter
   *  1.  Either RH or SPECHUMD is required, NOT BOTH.
   *  2.  For the SMtttbbb, STtttbbb SOILMmmm, and SOILTmmm fields:
   *      ttt represents layer top depth and bbb bottom depth, mm is the level depth.  This layer information is found in a Coverage,
   *
   *      so this code will strip off any data after the "SM" or "ST".
   *  3.  If an SM record exists, SOILM is not required
   *  4.  If an ST record exists, SOILT is not required
   *  5.  Pressure is only requried for non-isobaric datasets, we could only determine this by looking for special
   *      a normalized pressure level.
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


  public static ArrayList<String> readSingleVarsetXMLFile( File f) throws Exception{

    ArrayList<String> varset = new ArrayList<>();
    org.jdom2.Document doc;

    try {
      SAXBuilder builder = new SAXBuilder();

      doc = builder.build(f);
      Element rootNode = doc.getRootElement();
      List<Element> nodes = rootNode.getChildren("variable");

      for( Element e : nodes ){
        varset.add(e.getAttributeValue("name"));
      }

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    return varset;
  }

  public static HashMap<String,ArrayList<String>> readTDSVarsetXMLFile( File f) throws Exception{

    HashMap<String,ArrayList<String>> varset = new HashMap<>();
    org.jdom2.Document doc;

    try {
      SAXBuilder builder = new SAXBuilder();

      doc = builder.build(f);
      Element rootNode = doc.getRootElement();
      List<Element> varsetNodes = rootNode.getChildren("varset");

        for( int i = 0; i < varsetNodes.size(); i ++){

          Element e = varsetNodes.get(i);
          String name = e.getAttributeValue("name");
          ArrayList<String> theVars = new ArrayList<>();
          List<Element> variableElements = e.getChildren("variable");

          for ( Element var : variableElements ) {
            theVars.add(var.getAttributeValue("name"));
          }
          varset.put(name, theVars);
      }

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    return varset;
  }

  public static ArrayList<String> readVariablesFromfile( File f) throws Exception{

    ArrayList<String> varset = new ArrayList<>();
    try {
      Scanner sc = new Scanner(f);

      while (sc.hasNextLine())
        varset.add(sc.nextLine());

    } catch (Exception e) {
      throw new IOException(e.getMessage());
      }
    return varset;
  }

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

  private static String cropSOILField(String metname){
    if (metname.startsWith("SM") || metname.startsWith("ST"))
      return metname.substring(0,2);
    else
      return metname;
  }

  public static ArrayList<Coverage> filterCoverageListWithVarset( ArrayList<Coverage> cv, ArrayList<String> varset){

    ArrayList<Coverage> theCoverages = new ArrayList<>();

    cv.forEach( c -> {

//    for(Coverage cov: cv.getCoverages()) {
      String name = c.getFullName();
      //    Attribute ab = cov.findAttributeIgnoreCase(GRIB2_PARAMETER_NAME);

      if (varset.contains(name)){
        theCoverages.add(c);
        //     varset.remove(name);
      }
    });

    return theCoverages;
  }

  public static ArrayList<Coverage> createCoverageListFromVarset( CoverageCollection cv, ArrayList<String> varset){

    ArrayList<Coverage> theCoverages = new ArrayList<>();

    for(Coverage cov: cv.getCoverages()) {
      String name = cov.getFullName();
  //    Attribute ab = cov.findAttributeIgnoreCase(GRIB2_PARAMETER_NAME);

      if (varset.contains(name)){
        theCoverages.add(cov);
        Attribute att = cov.findAttributeIgnoreCase(GRIB2_PARAMETER_NAME);
        if ( att != null ){

        }
   //     varset.remove(name);
      }
    }

    return theCoverages;
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
                cov.getAttributes().add(new Attribute(METGRID_NAME, cropSOILField(v.getMetgridName())));
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

    ArrayList<Coverage> theCoverages = new ArrayList<>();
    boolean variableFound = false;

    for (VtableEntry v : vt) {

      String varId = v.getGRIB2Var();

      for (Coverage cov : cv.getCoverages()) {
        variableFound = false;
        for (Attribute att : cov.getAttributes()) {

          if (GRIB_VAR_ID.equals(att.getShortName()) && (att.getStringValue().startsWith(varId))) {
            variableFound = true;
            break;
          }
        }
        if( variableFound ){

          cov.getAttributes().add(new Attribute(METGRID_NAME, cropSOILField(v.getMetgridName())));
          cov.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
          cov.getAttributes().add(new Attribute(METGRID_DESC, v.getMetgridDesc()));
          theCoverages.add(cov);

        }
      }
    }
    return theCoverages;
  }
}
