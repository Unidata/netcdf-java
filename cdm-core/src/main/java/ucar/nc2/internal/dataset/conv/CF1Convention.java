/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import static ucar.nc2.internal.dataset.CoordSystemFactory.breakupConventionNames;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ucar.array.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.Variable.Builder;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;

/**
 * CF-1 Convention.
 * <p/>
 * <i>
 * "The CF conventions for climate and forecast metadata are designed to promote the processing and sharing of files
 * created with the netCDF
 * API. The conventions define metadata that provide a definitive description of what the data in each variable
 * represents, and of the
 * spatial and temporal properties of the data. This enables users of data from different sources to decide which
 * quantities are comparable,
 * and facilitates building applications with powerful extraction, regridding, and display capabilities."
 * </i>
 */
public class CF1Convention extends CSMConvention {
  private static final String CONVENTION_NAME = "CF-1.X";

  private static final String convName = "CF-1."; // start with

  /**
   * Get which CF version this is, ie CF-1.x
   *
   * @param hasConvName extract from convention name or list of names
   * @return version, or -1 if not CF
   */
  public static int getVersion(String hasConvName) {
    int result = extractVersion(hasConvName);
    if (result >= 0) {
      return result;
    }
    List<String> names = breakupConventionNames(hasConvName);
    for (String name : names) {
      result = extractVersion(name);
      if (result >= 0) {
        return result;
      }
    }
    return -1;
  }

  private static int extractVersion(String hasConvName) {
    if (!hasConvName.startsWith(convName)) {
      return -1;
    }
    String versionS = hasConvName.substring(convName.length());
    try {
      return Integer.parseInt(versionS);
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Guess the value of ZisPositive based on z axis name and units
   *
   * @param zaxisName z coordinate axis name
   * @param vertCoordUnits z coordinate axis name
   * @return CF.POSITIVE_UP or CF.POSITIVE_DOWN
   */
  public static String getZisPositive(String zaxisName, String vertCoordUnits) {
    if (vertCoordUnits == null) {
      return CF.POSITIVE_UP;
    }
    if (vertCoordUnits.isEmpty()) {
      return CF.POSITIVE_UP;
    }

    if (SimpleUnit.isCompatible("millibar", vertCoordUnits)) {
      return CF.POSITIVE_DOWN;
    }

    if (SimpleUnit.isCompatible("m", vertCoordUnits)) {
      return CF.POSITIVE_UP;
    }

    // dunno - make it up
    return CF.POSITIVE_UP;
  }

  private static final String[] vertical_coords = {"atmosphere_ln_pressure_coordinate", "atmosphere_sigma_coordinate",
      "atmosphere_hybrid_sigma_pressure_coordinate", "atmosphere_hybrid_height_coordinate",
      "atmosphere_sleve_coordinate", "ocean_sigma_coordinate", "ocean_s_coordinate", "ocean_sigma_z_coordinate",
      "ocean_double_sigma_coordinate", "ocean_s_coordinate_g1", // -sachin 03/25/09
      "ocean_s_coordinate_g2"};

  private int cfVersion = 0;

  protected CF1Convention(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
    String conv = rootGroup.getAttributeContainer().findAttributeString(CF.CONVENTIONS, null);
    if (conv != null) {
      this.cfVersion = getVersion(conv);
    }
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    augmentDataset(rootGroup);
    augmentSimpleGeometry();

    for (Group.Builder nested : rootGroup.gbuilders) {
      augmentDataset(nested);
    }

    // TODO remove this
    // make corrections for specific datasets
    String src = rootGroup.getAttributeContainer().findAttributeString("Source", "");
    if (src.equals("NOAA/National Climatic Data Center")) {
      String title = rootGroup.getAttributeContainer().findAttributeString("title", "");
      avhrr_oiv2 = title.indexOf("OI-V2") > 0;
    }
  }

  private void augmentDataset(Group.Builder group) {
    boolean got_grid_mapping = false;

    // look for transforms
    for (Variable.Builder<?> vb : group.vbuilders) {
      // look for special standard_names
      String sname = vb.getAttributeContainer().findAttributeString(CF.STANDARD_NAME, null);
      if (sname != null) {
        sname = sname.trim();

        if (sname.equalsIgnoreCase(CF.TIME_REFERENCE)) {
          vb.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
          continue;
        }

        if (sname.equalsIgnoreCase(CF.TIME_OFFSET)) {
          vb.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));
          continue;
        }

        if (sname.equalsIgnoreCase(CF.TIME)) {
          vb.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
        }

        if (sname.equalsIgnoreCase("ensemble") || sname.equalsIgnoreCase("realization")) {
          vb.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));
          continue;
        }

        for (String vertical_coord : vertical_coords) {
          if (sname.equalsIgnoreCase(vertical_coord)) {
            vb.addAttribute(new Attribute(_Coordinate.TransformType, CDM.Vertical));
            if (vb.getAttributeContainer().findAttribute(_Coordinate.Axes) == null) {
              vb.addAttribute(new Attribute(_Coordinate.Axes, vb.shortName));
            }
          }
        }

        // look for time variables and check to see if they have a calendar attribute. if not, add the default
        checkTimeVarForCalendar((VariableDS.Builder<?>) vb);
      }

      // look for horiz transforms. only ones that are referenced by another variable.
      String grid_mapping = vb.getAttributeContainer().findAttributeString(CF.GRID_MAPPING, null);
      if (grid_mapping != null) {
        Optional<Variable.Builder<?>> gridMapOpt = group.findVariableLocal(grid_mapping);
        if (gridMapOpt.isPresent()) {
          // TODO might be group relative - CF does not specify - see original version
          Variable.Builder<?> gridMap = gridMapOpt.get();
          gridMap.addAttribute(new Attribute(_Coordinate.TransformType, CDM.Projection));

          String grid_mapping_name = gridMap.getAttributeContainer().findAttributeString(CF.GRID_MAPPING_NAME, null);
          if (CF.LATITUDE_LONGITUDE.equals(grid_mapping_name)) {
            // "grid_mapping_name == latitude_longitude" is special in CF: it's applied to variables that describe
            // properties of lat/lon CRSes.
            gridMap.addAttribute(new Attribute(_Coordinate.AxisTypes, AxisType.Lat + " " + AxisType.Lon));
          } else {
            gridMap.addAttribute(new Attribute(_Coordinate.AxisTypes, AxisType.GeoX + " " + AxisType.GeoY));
          }

          // TODO remove this
          if (group == rootGroup) {
            // check for CF-ish GOES-16/17 grid mappings
            Attribute productionLocation =
                rootGroup.getAttributeContainer().findAttributeIgnoreCase("production_location");
            Attribute icdVersion = rootGroup.getAttributeContainer().findAttributeIgnoreCase("ICD_version");
            if (productionLocation != null && icdVersion != null) {
              // the fact that those two global attributes are not null means we should check to see
              // if the grid mapping variable has attributes that need corrected.
              correctGoes16(productionLocation, icdVersion, gridMap);
            }
          }
          got_grid_mapping = true;
        }
      }
    }

    if (!got_grid_mapping) { // see if there are any grid mappings anyway
      for (Variable.Builder<?> vds : group.vbuilders) {
        String grid_mapping_name = vds.getAttributeContainer().findAttributeString(CF.GRID_MAPPING_NAME, null);
        if (grid_mapping_name != null) {
          vds.addAttribute(new Attribute(_Coordinate.TransformType, CDM.Projection));

          if (grid_mapping_name.equals(CF.LATITUDE_LONGITUDE)) {
            vds.addAttribute(new Attribute(_Coordinate.AxisTypes, AxisType.Lat + " " + AxisType.Lon));
          } else {
            vds.addAttribute(new Attribute(_Coordinate.AxisTypes, AxisType.GeoX + " " + AxisType.GeoY));
          }
        }
      }
    }
  }

  private void augmentSimpleGeometry() {
    for (Variable.Builder<?> vb : rootGroup.vbuilders) {

      // TODO simple geometry
      if (cfVersion >= 8) { // only acknowledge simple geometry standard extension if CF-1.8 or higher
        if (vb.getAttributeContainer().findAttribute(CF.GEOMETRY) != null) {
          String geomValue = vb.getAttributeContainer().findAttributeString(CF.GEOMETRY, null);
          rootGroup.findVariableLocal(geomValue).ifPresent(coordsvar -> {
            vb.addAttribute(findAttributeIn(coordsvar, CF.GEOMETRY_TYPE));
            vb.addAttribute(findAttributeIn(coordsvar, CF.NODE_COORDINATES));
            vb.addAttribute(findAttributeIn(coordsvar, CF.PART_NODE_COUNT));

            // Only add attribute if present, sometimes optional
            addOptionalAttributeIn(coordsvar, vb, CF.NODES);
            addOptionalAttributeIn(coordsvar, vb, CF.NODE_COUNT);

            if (CF.POLYGON
                .equalsIgnoreCase(coordsvar.getAttributeContainer().findAttributeString(CF.GEOMETRY_TYPE, ""))) {
              addOptionalAttributeIn(coordsvar, vb, CF.INTERIOR_RING);
            }

            if (vb.getAttributeContainer().findAttribute(CF.NODE_COORDINATES) != null) {

              String nodeCoords = coordsvar.getAttributeContainer().findAttributeString(CF.NODE_COORDINATES, "");
              String[] coords = nodeCoords.split(" ");
              final StringBuilder cds = new StringBuilder();
              for (String coord : coords) {
                rootGroup.findVariableLocal(coord).ifPresent(temp -> {
                  Attribute axis = temp.getAttributeContainer().findAttribute(CF.AXIS);
                  if (axis != null) {
                    if ("x".equalsIgnoreCase(axis.getStringValue())) {
                      temp.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.SimpleGeometryX.toString()));
                    }
                    if ("y".equalsIgnoreCase(axis.getStringValue())) {
                      temp.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.SimpleGeometryY.toString()));
                    }
                    if ("z".equalsIgnoreCase(axis.getStringValue())) {
                      temp.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.SimpleGeometryZ.toString()));
                    }
                    cds.append(coord);
                    cds.append(" ");
                  }
                });
              }

              List<String> dimNames = ImmutableList.copyOf(vb.getDimensionNames());
              // Append any geometry dimensions as axis
              final StringBuilder pre = new StringBuilder();
              // must go backwards for some reason.
              for (int i = dimNames.size() - 1; i >= 0; i--) {
                String dimName = dimNames.get(i);
                if (!dimName.equals("time")) {
                  rootGroup.findVariableLocal(dimName).ifPresent(coordvar -> coordvar.getAttributeContainer()
                      .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.SimpleGeometryID.toString())));
                  // handle else case as malformed CF NetCDF
                }

                pre.append(dimName);
                pre.append(" ");
              }

              vb.addAttribute(new Attribute(_Coordinate.Axes, pre + cds.toString().trim()));
            }
          });
        }
      }
    }
  }

  Attribute findAttributeIn(Variable.Builder<?> coordsvar, String attName) {
    return new Attribute(attName, coordsvar.getAttributeContainer().findAttributeString(attName, ""));
  }

  void addOptionalAttributeIn(Variable.Builder<?> src, Variable.Builder<?> dest, String attName) {
    Attribute att = src.getAttributeContainer().findAttribute(attName);
    if (att != null) {
      dest.addAttribute(att); // ok to share Immutable objects
    }
  }

  private void correctGoes16(Attribute productionLocation, Attribute icdVersion, Builder<?> gridMappingVar) {
    String prodLoc = productionLocation.getStringValue();
    String icdVer = icdVersion.getStringValue();
    if (prodLoc != null && icdVer != null) {
      prodLoc = prodLoc.toLowerCase().trim();
      icdVer = icdVer.toLowerCase().trim();
      boolean mightNeedCorrected = prodLoc.contains("wcdas");
      mightNeedCorrected = mightNeedCorrected && icdVer.contains("ground segment");
      mightNeedCorrected = mightNeedCorrected && icdVer.contains("awips");
      if (mightNeedCorrected) {
        Map<String, String> possibleCorrections =
            ImmutableMap.of("semi_minor", CF.SEMI_MINOR_AXIS, "semi_major", CF.SEMI_MAJOR_AXIS);
        possibleCorrections.forEach((incorrect, correct) -> {
          Attribute attr = gridMappingVar.getAttributeContainer().findAttributeIgnoreCase(incorrect);
          if (attr != null) {
            Array<?> vals = attr.getArrayValues();
            if (vals != null) {
              gridMappingVar.getAttributeContainer().replace(attr, correct);
              log.debug("Renamed {} attribute {} to {}", gridMappingVar, incorrect, correct);
            }
          }
        });
      }
    }
  }

  private boolean avhrr_oiv2;

  /*
   * this is here because it doesnt fit into the 3D array thing.
   * private void makeAtmLnCoordinate(VariableDS.Builder<?> vb) {
   * // get the formula attribute
   * String formula = vb.getAttributeContainer().findAttValueIgnoreCase(CF.formula_terms, null);
   * if (null == formula) {
   * String msg = " Need attribute 'formula_terms' on Variable " + vb.getFullName() + "\n";
   * parseInfo.format(msg);
   * userAdvice.format(msg);
   * return;
   * }
   * 
   * // parse the formula string
   * Variable p0Var = null, levelVar = null;
   * StringTokenizer stoke = new StringTokenizer(formula, " :");
   * while (stoke.hasMoreTokens()) {
   * String toke = stoke.nextToken();
   * if (toke.equalsIgnoreCase("p0")) {
   * String name = stoke.nextToken();
   * p0Var = rootGroup.findVariable(name);
   * } else if (toke.equalsIgnoreCase("lev")) {
   * String name = stoke.nextToken();
   * levelVar = rootGroup.findVariable(name);
   * }
   * }
   * 
   * if (null == p0Var) {
   * String msg = " Need p0:varName on Variable " + v.getFullName() + " formula_terms\n";
   * parseInfo.format(msg);
   * userAdvice.format(msg);
   * return;
   * }
   * 
   * if (null == levelVar) {
   * String msg = " Need lev:varName on Variable " + v.getFullName() + " formula_terms\n";
   * parseInfo.format(msg);
   * userAdvice.format(msg);
   * return;
   * }
   * 
   * String units = p0Var.findAttValueIgnoreCase(CDM.UNITS, "hPa");
   * 
   * // create the data and the variable
   * try { // p(k) = p0 * exp(-lev(k))
   * double p0 = p0Var.readScalarDouble();
   * Array levelData = levelVar.read();
   * Array pressureData = Array.factory(DataType.DOUBLE, levelData.getShape());
   * IndexIterator ii = levelData.getIndexIterator();
   * IndexIterator iip = pressureData.getIndexIterator();
   * while (ii.hasNext()) {
   * double val = p0 * Math.exp(-1.0 * ii.getDoubleNext());
   * iip.setDoubleNext(val);
   * }
   * 
   * CoordinateAxis1D p = new CoordinateAxis1D(ds, null, v.getShortName() + "_pressure", DataType.DOUBLE,
   * levelVar.getDimensionsString(), units,
   * "Vertical Pressure coordinate synthesized from atmosphere_ln_pressure_coordinate formula");
   * p.setCachedData(pressureData, false);
   * p.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
   * p.addAttribute(new Attribute(_Coordinate.AliasForDimension, p.getDimensionsString()));
   * ds.addVariable(null, p);
   * parseInfo.format(" added Vertical Pressure coordinate %s from CF-1 %s%n", p.getFullName(),
   * CF.atmosphere_ln_pressure_coordinate);
   * 
   * } catch (IOException e) {
   * String msg = " Unable to read variables from " + v.getFullName() + " formula_terms\n";
   * parseInfo.format(msg);
   * userAdvice.format(msg);
   * }
   * 
   * }
   */

  /*
   * vertical coordinate will be identifiable by:
   * 1. units of pressure; or
   * 2. the presence of the positive attribute with a value of up or down (case insensitive).
   * 3. Optionally, the vertical type may be indicated additionally by providing the standard_name attribute with an
   * appropriate value, and/or the axis attribute with the value Z.
   */

  // we assume that coordinate axes get identified by
  // 1) being coordinate variables or
  // 2) being listed in coordinates attribute.

  /**
   * Augment COARDS axis type identification with Standard names (including dimensionless vertical coordinates) and
   * CF.AXIS attributes
   */
  @Override
  public AxisType getAxisType(VariableDS.Builder<?> vb) {
    // standard names for unitless vertical coords
    String sname = vb.getAttributeContainer().findAttributeString(CF.STANDARD_NAME, null);
    if (sname != null) {
      sname = sname.trim();

      for (String vertical_coord : vertical_coords) {
        if (sname.equalsIgnoreCase(vertical_coord)) {
          return AxisType.GeoZ;
        }
      }
    }

    // COARDS - check units
    AxisType at = super.getAxisType(vb);
    if (at != null) {
      return at;
    }

    // standard names for X, Y : bug in CDO putting wrong standard name, so check units first (!)
    if (sname != null) {
      if (sname.equalsIgnoreCase(CF.ENSEMBLE)) {
        return AxisType.Ensemble;
      }

      if (sname.equalsIgnoreCase(CF.LATITUDE)) {
        return AxisType.Lat;
      }

      if (sname.equalsIgnoreCase(CF.LONGITUDE)) {
        return AxisType.Lon;
      }

      if (sname.equalsIgnoreCase(CF.PROJECTION_X_COORDINATE) || sname.equalsIgnoreCase(CF.GRID_LONGITUDE)
          || sname.equalsIgnoreCase("rotated_longitude")) {
        return AxisType.GeoX;
      }

      if (sname.equalsIgnoreCase(CF.PROJECTION_Y_COORDINATE) || sname.equalsIgnoreCase(CF.GRID_LATITUDE)
          || sname.equalsIgnoreCase("rotated_latitude")) {
        return AxisType.GeoY;
      }

      if (sname.equalsIgnoreCase(CF.TIME_REFERENCE)) {
        return AxisType.RunTime;
      }

      if (sname.equalsIgnoreCase(CF.TIME_OFFSET)) {
        return AxisType.TimeOffset;
      }
    }

    // check axis attribute - only for X, Y, Z
    String axis = vb.getAttributeContainer().findAttributeString(CF.AXIS, null);
    if (axis != null) {
      axis = axis.trim();
      String unit = vb.getUnits();

      if (axis.equalsIgnoreCase("X")) {
        if (SimpleUnit.isCompatible("m", unit)) {
          return AxisType.GeoX;
        }

      } else if (axis.equalsIgnoreCase("Y")) {
        if (SimpleUnit.isCompatible("m", unit)) {
          return AxisType.GeoY;
        }

      } else if (axis.equalsIgnoreCase("Z")) {
        if (unit == null) {
          return AxisType.GeoZ;
        }
        if (SimpleUnit.isCompatible("m", unit)) {
          return AxisType.Height;
        } else if (SimpleUnit.isCompatible("mbar", unit)) {
          return AxisType.Pressure;
        } else {
          return AxisType.GeoZ;
        }
      }
    }

    if (avhrr_oiv2) {
      if (vb.shortName.equals("zlev")) {
        return AxisType.Height;
      }
    }

    // Check time units
    String units = vb.getUnits();
    // parsed successfully, what could go wrong?
    if (CalendarDateUnit.fromUdunitString(null, units).isPresent()) {
      return AxisType.Time;
    }

    // dunno
    return null;
  }

  public static class Factory implements CoordSystemBuilderFactory {

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new CF1Convention(datasetBuilder);
    }
  }

}

