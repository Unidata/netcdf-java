/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** Factory for Projection Transforms. */
public class ProjectionFactory {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectionFactory.class);
  private static final List<Transform> transformList = new ArrayList<>();
  private static boolean userMode = false;

  private static final boolean loadWarnings = false;

  // search in the order added
  static {
    registerTransform(CF.ALBERS_CONICAL_EQUAL_AREA, AlbersEqualArea.class);
    registerTransform(CF.AZIMUTHAL_EQUIDISTANT, AzimuthalEquidistant.class);
    registerTransform("flat_earth", FlatEarth.class);
    registerTransform(CF.GEOSTATIONARY, Geostationary.class);
    registerTransform(CF.LAMBERT_AZIMUTHAL_EQUAL_AREA, LambertAzimuthal.class);
    registerTransform(CF.LAMBERT_CONFORMAL_CONIC, LambertConformalConic.class);
    registerTransform(CF.LAMBERT_CYLINDRICAL_EQUAL_AREA, LambertCylindricalEqualArea.class);
    registerTransform(CF.LATITUDE_LONGITUDE, LatLon.class);
    // optional - needs visad.jar
    registerTransformMaybe("mcidas_area", "ucar.nc2.iosp.mcidas.McIDASAreaTransformBuilder");
    registerTransform(CF.MERCATOR, Mercator.class);
    registerTransform("MSGnavigation", MSGnavigation.class);
    registerTransform(CF.ORTHOGRAPHIC, Orthographic.class);
    registerTransform(CF.POLAR_STEREOGRAPHIC, PolarStereographic.class);
    registerTransform("polyconic", PolyconicProjection.class); // ghansham@sac.isro.gov.in 1/8/2012
    registerTransform(CF.ROTATED_LATITUDE_LONGITUDE, RotatedPole.class);
    registerTransform("rotated_latlon_grib", RotatedLatLon.class);
    registerTransform(CF.SINUSOIDAL, Sinusoidal.class);
    registerTransform(CF.STEREOGRAPHIC, Stereographic.class);
    registerTransform(CF.TRANSVERSE_MERCATOR, TransverseMercator.class);
    registerTransform("UTM", UTM.class);
    registerTransform(CF.VERTICAL_PERSPECTIVE, VerticalPerspective.class);

    // further calls to registerTransform are by the user
    userMode = true;
  }

  /**
   * Register a class that implements HorizTransformBuilderIF
   * 
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param c class that implements CoordTransBuilderIF.
   */
  public static void registerTransform(String transformName, Class<?> c) {
    if (!(HorizTransformBuilderIF.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement HorizTransformBuilderIF");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException(
          "CoordTransBuilderIF Class " + c.getName() + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class " + c.getName() + " is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      transformList.add(0, new Transform(transformName, c));
    else
      transformList.add(new Transform(transformName, c));

  }

  /**
   * Register a class that implements HorizTransformBuilderIF.
   * 
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   * @throws ClassNotFoundException if Class.forName( className) fails
   */
  public static void registerTransform(String transformName, String className) throws ClassNotFoundException {
    Class<?> c = Class.forName(className);
    registerTransform(transformName, c);
  }

  /**
   * Register a class that implements HorizTransformBuilderIF.
   * 
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   */
  public static void registerTransformMaybe(String transformName, String className) {
    Class<?> c;
    try {
      c = Class.forName(className);
    } catch (ClassNotFoundException e) {
      if (loadWarnings)
        log.warn("Coordinate Transform Class " + className + " not found.");
      return;
    }
    registerTransform(transformName, c);
  }

  private static class Transform {
    String transName;
    Class<?> transClass;

    Transform(String transName, Class<?> transClass) {
      this.transName = transName;
      this.transClass = transClass;
    }
  }

  /**
   * Make a CoordinateTransform object from the parameters in a Coordinate Transform Variable, using an intrinsic or
   * registered CoordTransBuilder.
   * 
   * @param ctv the Coordinate Transform Variable - container for the transform parameters
   * @param parseInfo pass back information about the parsing.
   * @return CoordinateTransform, or null if failure.
   */
  @Nullable
  public static Projection makeProjection(AttributeContainer ctv, String geoUnits, Formatter parseInfo) {
    // standard name
    String transform_name = ctv.findAttributeString(CDM.TRANSFORM_NAME, null);
    if (null == transform_name)
      transform_name = ctv.findAttributeString("Projection_Name", null);

    // these names are from CF - dont want to have to duplicate
    if (null == transform_name)
      transform_name = ctv.findAttributeString(CF.GRID_MAPPING_NAME, null);
    if (null == transform_name)
      transform_name = ctv.findAttributeString(CF.STANDARD_NAME, null);

    // Finally check the units
    if (null == transform_name)
      transform_name = ctv.findAttributeString(CDM.UNITS, null);

    if (null == transform_name) {
      parseInfo.format("**Failed to find Coordinate Transform name from Variable= %s%n", ctv);
      return null;
    }

    transform_name = transform_name.trim();

    // do we have a transform registered for this ?
    Class<?> builderClass = null;
    for (Transform transform : transformList) {
      if (transform.transName.equals(transform_name)) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      parseInfo.format("**Failed to find CoordTransBuilder name= %s from Variable= %s%n", transform_name, ctv);
      return null;
    }

    // get an instance of that class
    Object builderObject;
    try {
      builderObject = builderClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      log.error("Cant create new instance " + builderClass.getName(), e);
      return null;
    }

    ProjectionCT.Builder<?> ct;
    HorizTransformBuilderIF horizBuilder = (HorizTransformBuilderIF) builderObject;
    horizBuilder.setErrorBuffer(parseInfo);
    ct = horizBuilder.makeCoordinateTransform(ctv, geoUnits);
    if (ct != null) {
      ct.setTransformType(TransformType.Projection);
    }

    if (ct != null) {
      parseInfo.format(" Made Coordinate transform %s from variable %s: %s%n", transform_name, ctv.getName(),
          builderObject.getClass().getName());
    } else {
      parseInfo.format(" Failed to make Coordinate transform %s from variable %s: %s%n", transform_name, ctv.getName(),
          builderObject.getClass().getName());
    }

    return ct.build().getProjection();
  }
}
