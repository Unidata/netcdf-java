---
title: Coordinate transforms
last_updated: 2019-07-23
sidebar: userguide_sidebar
permalink: coord_transform.html
toc: false
---

## Writing a Coordinate Transform: Projections and Vertical Transforms

### Overview

A <b>_Coordinate Transform_</b> represents a mathematical function that transforms a dataset's coordinates to a coordinates in a <b>_Reference Coordinate System_</b>. Currently the CDM has two kinds of transforms: <b>_Projections_</b> and <b>_Vertical Transforms_</b>. A <b>_Projection_</b> maps between cartesian x and y coordinates (called <b>_GeoX_</b> and <b>_GeoY_</b>) and latitude, longitude coordinates, by implementing the <b>_ucar.unidata.geoloc.Projection_</b> interface. A <b>_Vertical Transform_</b> takes a <b>_GeoZ_</b> coordinate and usually other data fields such as surface pressure, and produces a 3D <b>_height_</b> or <b>_pressure_</b> vertical coordinate field.

A <b>_Coordinate System_</b> may have 0 or more <b>_Coordinate Transforms_</b>, each of which is either a <b>_ProjectionCT_</b> containing a <b>_ucar.unidata.geoloc.Projection_</b> or a <b>_VerticalCT_</b> containing a <b>_ucar.unidata.geoloc.vertical.VerticalTransform_</b>:

{% include image.html file="coordsystems/CoordSys.png" alt="Coord Sys Object Model" caption="" %}

The Netcdf-Java library implements a standard set of <b>_ucar.unidata.geoloc.Projection_</b> and <b>_ucar.unidata.geoloc.vertical.VerticalTransform_</b> classes, following the specifications of the <a href="http://cfconventions.org/" target="_blank">CF-1.0 Conventions</a>.

## Implementing a Coordinate Transform
The steps to using your own CoordinateTransform in the Netcdf-Java library:

Write a class that implements <b>_ucar.unidata.geoloc.Projection_</b> or ucar.nc2.geoloc.vertical.VerticalTransform
Write a class that implements <b>_ucar.nc2.dataset.CoordTransBuilderIF_</b>, by subclassing ucar.nc2.dataset.transform.AbstractCoordTransBuilder.
Add these classes to your classpath.
From your application, call <b>_ucar.nc2.dataset.CoordTransBuilder.registerTransform( String transformName, Class c)_</b>.
The <a href="coord_system_builder.html#creating-coordinate-transformations">Coordinate System Builder</a> for your dataset must recognize the transform and add it to the coordinate system. If you use the CF-1.0 or the _Coordinate Conventions, this means that the dataset must contain a Coordinate Transform Variable that contains the parameters of the transform
The classes that you will use are shown in the following diagram, which has an example of both a Projection ( LambertConformal) and a Vertical Transform (OceanSigma).

{% include image.html file="coordsystems/CoordTransforms.png" alt="Coordinate Transforms" caption="" %}

### Implementing a Projection

You should implement the ucar.unidata.geoloc.Projection interface by subclassing the abstract class ucar.unidata.geoloc.projection.ProjectionImpl. The methods you need to implement are:

~~~
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint);
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint);
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);
  public ProjectionImpl constructCopy();
  public boolean equals(Object proj);
  public int hashCode()
  public String paramsToString();
~~~
  
The <b>_latLonToProj_</b> and <b>_projToLatLon_</b> methods are inverses of each other, and map between _lat_, _lon_ (in units of decimal degrees) to cartesian x,y, the coordinates that your dataset uses, usually in units of "km on the projection plane". The <b>_crossSeam_</b> method return true when a line between two points in projection coordinates would cross a seam in the projection plane, such as for a cylindrical or conic projections. This helps drawing routines to eliminate spurious lines. The <b>_constructCopy_</b> method constructs a new, equivalent Projection object, which avoids the problems with clone (see Bloch, <a href="http://java.sun.com/developer/Books/effectivejava/Chapter3.pdf" target="_blank">Effective Java</a>, item 10). The <b>_equals_</b> method should be overridden to make Projections equal that have the same parameters. You should also override <b>_hashCode_</b> to make it consistent with equals (see Bloch, <a href="http://java.sun.com/developer/Books/effectivejava/Chapter3.pdf" target="_blank">Effective Java</a>, item 8). The <b>_paramsToString_</b> returns a String representation of the Projection parameters. Examine the classes in <b>_ucar.unidata.geoloc.projection_</b> for implementation examples.

### Implementing a VerticalTransform

You should implement the <b>_ucar.unidata.geoloc.vertical.VerticalTransform_</b> interface by subclassing the abstract class <b>_ucar.unidata.geoloc.vertical.VerticalTransformImpl_</b>. The methods you need to implement are:
~~~
  public ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex);
  public String getUnitString();
~~~
  
The <b>_getCoordinateArray_</b> returns a 3D vertical coordinate for the given time step (the time step is ignored if <b>_isTimeDependent_</b> is false). The returned array must use dimensions in the order of z, y, x. The <b>_getUnitString_</b> returns the unit of the transformed vertical coordinate, which should be <b>_udunits_</b> compatible with <b>_height_</b> or <b>_pressure_</b>. Examine the classes in <b>_ucar.unidata.geoloc.vertical_</b> for implementation examples.

### Implementing and registering CoordTransBuilderIF

The Projection and VerticalTransform implement the mathematical transformation itself. Now we need to add the glue classes that allow runtime discovery and object instantiation. To do so, you must add a class that implements the ucar.nc2.dataset.CoordTransBuilderIF interface. You should subclass the abstract class ucar.nc2.dataset.transform.AbstractCoordTransBuilder, and implement the following methods:

~~~
  public String getTransformName();
  public TransformType getTransformType();
  public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv);
  public ucar.nc2.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT);
~~~
  
Give your transform a unique name, which is returned by the <b>_getTransformName_</b> method. The <b>_getTransformType_</b> method should return either <b>_ucar.nc2.dataset.TransformType.Projection_</b> or <b>_TransformType.Vertical_</b>. The <b>_makeCoordinateTransform_</b> method is the guts of the class, it takes as parameters the NetcdfDataset and the Coordinate Transform Variable that contains the transformation parameters. The <b>_makeMathTransform_</b> is used only for Vertical Transforms to defer the creation of the VerticalTransform until the Coordinate System has been fully constructed and, for example, the time dimension has been identified.

You then need to tell the Netcdf Java library about your transform class :
~~~
 CoordTransBuilder.registerTransform("MyTransformName", MyTransform.class);
The name is the same as getTransformType returns, and must be referenced in your dataset by the Coordinate Transform Variable. 
~~~

### Projection Example

Following is an example from the standard implementation classes in <b>_ucar.nc2.dataset.transform_</b>.

~~~
public class LambertConformalConic extends AbstractCoordTransBuilder {
 public String getTransformName() {
1)  return "lambert_conformal_conic";
 }
   
 public TransformType getTransformType() {
   return TransformType.Projection;
 }
  
 public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
2) double[] pars = readAttributeDouble2(ctv.findAttribute( "standard_parallel"));
   double lon0 = readAttributeDouble( ctv, "longitude_of_central_meridian");
   double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
   double false_easting = readAttributeDouble( ctv, "false_easting");
   double false_northing = readAttributeDouble( ctv, "false_northing");
   String units = ds.findAttValueIgnoreCase( ctv, "units", null);
   if (units == null)
     units = getUnits( ds);
     
3) ucar.unidata.geoloc.projection.LambertConformal lc = 
     new ucar.unidata.geoloc.projection.LambertConformal(lat0, lon0, pars[0], pars[1], false_easting, false_northing, units);

4) return new ProjectionCT(ctv.getShortName(), "FGDC", lc);
 }
}
~~~

1. The name of the transformation. This is referenced in your dataset.
2. Various parameters are read from the attributes of the Coordinate Transform Variable.
3. A Projection is created from the parameters
4. A ProjectionCT wraps the Projection.

### Vertical Transform Example

~~~
public class VAtmSigma extends AbstractCoordTransBuilder {
  private String sigma, ps, ptop;
    public String getTransformName() {
1)  return "atmosphere_sigma_coordinate";
  }
  public TransformType getTransformType() {
    return TransformType.Vertical;
  }
  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
2) String formula_terms = getFormula(ds, ctv);
   if (null == formula_terms) return null;
  
   // parse the formula string
3) StringTokenizer stoke = new StringTokenizer(formula_terms);
   while (stoke.hasMoreTokens()) {
    String toke = stoke.nextToken();
    if (toke.equalsIgnoreCase("sigma:"))
      sigma = stoke.nextToken();
    else if (toke.equalsIgnoreCase("ps:"))
      ps = stoke.nextToken();
    else if (toke.equalsIgnoreCase("ptop:"))
      ptop = stoke.nextToken();
   }

4) CoordinateTransform rs = new VerticalCT("AtmSigma_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.Sigma, this);
5) rs.addParameter(new Parameter("standard_name", getTransformName()));
   rs.addParameter(new Parameter("formula_terms", formula_terms));
   rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));
6) if (!addParameter( rs, AtmosSigma.PS, ds, ps, false)) return null;
   if (!addParameter( rs, AtmosSigma.SIGMA, ds, sigma, false)) return null;
   if (!addParameter( rs, AtmosSigma.PTOP, ds, ptop, true)) return null;
      return rs;
  }
 
  public ucar.nc2.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
7)  return new AtmosSigma(ds, timeDim, vCT);
  }
}
~~~

1. The name of the transformation. This is referenced in your dataset.
2. The CF vertical transforms rely on a formula terms string to describe the algorithm. You may choose to not use this method.
3. Parse the formula terms to get the names of the variables holding the transform information.
4. A VerticalCT is constructed that holds the transform parameters
5. The <b>_standard_name_</b> and <b>_formula_terms_</b> attributes are added to the VerticalCT, so that the Coordinate Transform Variable can be recreated from the VerticalCT if needed. The <b>_formula_</b> is not strictly needed, but makes the dataset metadata more self contained.
6. The parameters are added to the VerticalCT, this is where the AtmosSigma class will find them. Use public parameter names from the VerticalTransform class.
7. The creation of the VerticalTransform is deferred until this method is called by the VerticalCT object.

The corresponding VerticalTransform class looks like:

~~~
public class AtmosSigma extends VerticalTransformImpl {
1) public static final String PTOP = "Pressure at top";
   public static final String PS = "surfacePressure variable name";
   public static final String SIGMA = "sigma variable name";
   private Variable psVar; // surface pressue
   private double[] sigma; // The sigma array, function of z
   private double ptop;    // Top of the model
   
   public AtmosSigma(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
2)   super(timeDim);

     String psName = vCT.findParameterIgnoreCase(PS).getStringValue();
3)   this.psVar = ds.findStandardVariable(psName);

     String ptopName = vCT.findParameterIgnoreCase(PTOP).getStringValue();
     Variable ptopVar = ds.findStandardVariable(ptopName);
     try {
4)     this.ptop = ptopVar.readScalarDouble();
     } catch (IOException e) {
       throw new IllegalArgumentException("AtmosSigma failed to read " + ptopVar + " err= "+e.getMessage());
     }

     String sigmaName = vCT.findParameterIgnoreCase(SIGMA).getStringValue();
     Variable sigmaVar = ds.findStandardVariable(sigmaName) {
     try {
       Array data = sigmaVar.read();
5)     this.sigma = (double[]) data.get1DJavaArray(double.class);
     } catch (IOException e) {
       throw new IllegalArgumentException("AtmosSigma failed to read "+ sigmaName);
     }
     
6)   units = ds.findAttValueIgnoreCase(psVar, "units", "none");
   }
~~~   
   
The <b>_AtmosSigma_</b> class constructor:

1. The parameter names as public constant Strings
2.  The superclass keeps track of the time SImension
3. The Variable holding the surface pressure
4. The value of the ptop scalar variable
5. The value of the sigma[z] coordinate
6. The converted coordinates will be in the units of the surface pressure

~~~
 /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException problem reading data
   */
 public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
1) Array ps = readArray(psVar, timeIndex);
   Index psIndex = ps.getIndex();
   int nz = sigma.length;
   int[] shape2D = ps.getShape();
   int ny = shape2D[0];
   int nx = shape2D[1];
2) ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

   for (int y = 0; y < ny; y++) {
     for (int x = 0; x < nx; x++) {
3)    double psVal = ps.getDouble(psIndex.set(y, x));
      for (int z = 0; z < nz; z++) {
4)      result.set(z,y,x, ptop + sigma[z] * (psVal - ptop));
      }
    }
   }
   return result;
 }
 ~~~
 
1. Read the surface pressure variable at the given time step through a utility method in the superclass
2. Create the result array
3. Extract the surface pressure at the given x,y point
4. Looping over z, the converted coordinate = ptop + sigma(z)*(surfacePressure(x,y)-ptop), as advertised
