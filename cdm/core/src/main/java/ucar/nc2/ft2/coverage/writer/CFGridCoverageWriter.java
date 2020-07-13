/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.writer;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageTransform;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LatLonProjection;

/**
 * Write CF Compliant Grid file from a Coverage.
 * First, single coverage only.
 * - The idea is to subset the coordsys, use that for the file's metadata.
 * - Then subset the grid, and write out the data. Check that the grid's metadata matches.
 */
public class CFGridCoverageWriter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CFGridCoverageWriter.class);
  private static final boolean show = false;

  private static final String BOUNDS = "_bounds";
  private static final String BOUNDS_DIM = "bounds_dim"; // dimension of length 2, can be used by any bounds coordinate

  /** A value class holding information about the write() */
  public static class Result {
    private final long sizeToBeWritten;
    private final boolean wasWritten;
    @Nullable
    private final String errorMessage;

    private Result(long sizeToBeWritten, boolean wasWritten, @Nullable String errorMessage) {
      this.sizeToBeWritten = sizeToBeWritten;
      this.wasWritten = wasWritten;
      this.errorMessage = errorMessage;
    }

    /**
     * Estimated number of bytes the file will take. This is NOT exactly the size of the the whole output file, but
     * it's close.
     */
    public long sizeToBeWritten() {
      return sizeToBeWritten;
    }

    /** Whether the file was created or not. */
    public boolean wasWritten() {
      return wasWritten;
    }

    @Nullable
    public String getErrorMessage() {
      return errorMessage;
    }

    public static Result create(long sizeToBeWritten, boolean wasWritten, @Nullable String errorMessage) {
      return new Result(sizeToBeWritten, wasWritten, errorMessage);
    }
  }

  /**
   * Write a netcdf/CF file from a CoverageDataset
   *
   * @param gdsOrg the CoverageDataset
   * @param gridNames the list of coverage names to be written, or null for all
   * @param subset defines the requested subset, or null to include everything in gdsOrg
   * @param tryToAddLatLon2D add 2D lat/lon coordinates, if possible
   * @param writer this does the actual writing, must not be null
   * @param maxBytes if > 0, only create the file if sizeToBeWritten < maxBytes.
   * @return the result of the write.
   */
  public static Result write(CoverageCollection gdsOrg, List<String> gridNames, SubsetParams subset,
      boolean tryToAddLatLon2D, NetcdfFormatWriter.Builder writer, long maxBytes)
      throws IOException, InvalidRangeException {
    Preconditions.checkNotNull(writer);
    CFGridCoverageWriter writer2 = new CFGridCoverageWriter();
    return writer2.writeFile(gdsOrg, gridNames, subset, tryToAddLatLon2D, writer, maxBytes);
  }

  private Result writeFile(CoverageCollection gdsOrg, List<String> gridNames, SubsetParams subsetParams,
      boolean tryToAddLatLon2D, NetcdfFormatWriter.Builder writer, long maxBytes)
      throws IOException, InvalidRangeException {
    if (gridNames == null) { // want all of them
      gridNames = new LinkedList<>();

      for (Coverage coverage : gdsOrg.getCoverages()) {
        gridNames.add(coverage.getName());
      }
    }

    if (subsetParams == null) {
      subsetParams = new SubsetParams();
    }

    // We need global attributes, subsetted axes, transforms, and the coverages with attributes and referencing
    // subsetted axes.
    Formatter errLog = new Formatter();
    java.util.Optional<CoverageCollection> opt =
        CoverageSubsetter2.makeCoverageDatasetSubset(gdsOrg, gridNames, subsetParams, errLog);
    if (!opt.isPresent()) {
      return Result.create(0, false, errLog.toString());
    }

    CoverageCollection subsetDataset = opt.get();

    ////////////////////////////////////////////////////////////////////

    Group.Builder rootGroup = writer.getRootGroup();
    addGlobalAttributes(subsetDataset, rootGroup);
    addDimensions(subsetDataset, rootGroup);
    addCoordinateAxes(subsetDataset, rootGroup);
    addCoverages(subsetDataset, rootGroup);
    addCoordTransforms(subsetDataset, rootGroup);

    boolean shouldAddLatLon2D = shouldAddLatLon2D(tryToAddLatLon2D, subsetDataset);
    if (shouldAddLatLon2D) {
      addLatLon2D(subsetDataset, rootGroup);
    }
    addCFAnnotations(subsetDataset, rootGroup, shouldAddLatLon2D);

    // Actually create file and write variable data to it.
    try (NetcdfFormatWriter ncwriter = writer.build()) {
      // test if its too large
      long totalSizeOfVars = ncwriter.calcSize();
      if (maxBytes > 0 && totalSizeOfVars > maxBytes) {
        return Result.create(totalSizeOfVars, false, String.format("Too large, max size = %d", maxBytes));
      }

      writeCoordinateData(subsetDataset, ncwriter);
      writeCoverageData(gdsOrg, subsetParams, subsetDataset, ncwriter);

      if (shouldAddLatLon2D) {
        writeLatLon2D(subsetDataset, ncwriter);
      }
    }

    return Result.create(0, true, null);
  }

  /**
   * Returns {@code true} if we should add 2D latitude & longitude variables to the output file.
   * This method could return {@code false} for several reasons:
   *
   * <ul>
   * <li>{@code !tryToAddLatLon2D}</li>
   * <li>{@code subsetDataset.getHorizCoordSys().isLatLon2D()}</li>
   * <li>{@code !subsetDataset.getHorizCoordSys().isProjection()}</li>
   * <li>{@code subsetDataset.getHorizCoordSys() instanceof LatLonProjection}</li>
   * </ul>
   *
   * @param tryToAddLatLon2D attempt to add 2D lat/lon vars to output file.
   * @param subsetDataset subsetted version of the original CoverageCollection.
   * @return {@code true} if we should add 2D latitude & longitude variables to the output file.
   */
  private boolean shouldAddLatLon2D(boolean tryToAddLatLon2D, CoverageCollection subsetDataset) {
    if (!tryToAddLatLon2D) { // We don't even want 2D lat/lon vars.
      return false;
    }

    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    if (horizCoordSys.isLatLon2D()) { // We already have 2D lat/lon vars.
      return false;
    }
    if (!horizCoordSys.isProjection()) { // CRS doesn't contain a projection, meaning we can't calc 2D lat/lon vars.
      return false;
    }

    Projection proj = horizCoordSys.getTransform().getProjection();
    // Projection is a "fake"; we already have lat/lon.
    return !(proj instanceof LatLonProjection);

  }

  private void addGlobalAttributes(CoverageCollection gds, Group.Builder group) {
    // global attributes
    for (Attribute att : gds.getGlobalAttributes()) {
      if (att.getShortName().equals(CDM.FILE_FORMAT))
        continue;
      if (att.getShortName().equals(_Coordinate._CoordSysBuilder))
        continue;
      group.addAttribute(att);
    }

    Attribute att = gds.findAttributeIgnoreCase(CDM.CONVENTIONS);
    if (att == null || !att.getStringValue().startsWith("CF-")) // preserve prev version of CF Convention if exists
      group.addAttribute(new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    group.addAttribute(
        new Attribute("History", "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridCoverageWriter)\n"
            + "Original Dataset = " + gds.getName() + "; Translation Date = " + CalendarDate.present()));

    LatLonRect llbb = gds.getLatlonBoundingBox();
    if (llbb != null) {
      // this will replace any existing
      group.addAttribute(new Attribute(ACDD.LAT_MIN, llbb.getLatMin()));
      group.addAttribute(new Attribute(ACDD.LAT_MAX, llbb.getLatMax()));
      group.addAttribute(new Attribute(ACDD.LON_MIN, llbb.getLonMin()));
      group.addAttribute(new Attribute(ACDD.LON_MAX, llbb.getLonMax()));
    }
  }

  private void addDimensions(CoverageCollection subsetDataset, Group.Builder group) {
    // each independent coordinate is a dimension
    Map<String, Dimension> dimHash = new HashMap<>();
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        Dimension d = Dimension.builder(axis.getName(), axis.getNcoords()).build();
        group.addDimension(d);
        dimHash.put(axis.getName(), d);
      }

      if (axis.isInterval()) {
        if (null == dimHash.get(BOUNDS_DIM)) {
          Dimension d = Dimension.builder(BOUNDS_DIM, 2).build();
          group.addDimension(d);
          dimHash.put(BOUNDS_DIM, d);
        }
      }
    }
  }

  private void addCoordinateAxes(CoverageCollection subsetDataset, Group.Builder parent) {
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      String dims;

      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        dims = axis.getName();
      } else if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.scalar) {
        dims = "";
      } else {
        dims = axis.getDependsOn();
      }

      boolean hasBounds = false;
      if (axis.isInterval()) {
        Variable.Builder vb = Variable.builder().setName(axis.getName() + BOUNDS).setDataType(axis.getDataType())
            .setParentGroupBuilder(parent).setDimensionsByName(dims + " " + BOUNDS_DIM);
        vb.addAttribute(new Attribute(CDM.UNITS, axis.getUnits()));
        parent.addVariable(vb);
        hasBounds = true;
      }

      Variable.Builder vb = Variable.builder().setName(axis.getName()).setDataType(axis.getDataType())
          .setParentGroupBuilder(parent).setDimensionsByName(dims);
      addVariableAttributes(vb, axis.getAttributeContainer());
      vb.addAttribute(new Attribute(CDM.UNITS, axis.getUnits())); // override what was in att list
      if (hasBounds)
        vb.addAttribute(new Attribute(CF.BOUNDS, axis.getName() + BOUNDS));
      if (axis.getAxisType() == AxisType.TimeOffset)
        vb.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
      parent.addVariable(vb);
    }
  }

  private void addCoverages(CoverageCollection subsetDataset, Group.Builder parent) {
    for (Coverage grid : subsetDataset.getCoverages()) {
      Variable.Builder vb = Variable.builder().setName(grid.getName()).setDataType(grid.getDataType())
          .setParentGroupBuilder(parent).setDimensionsByName(grid.getIndependentAxisNamesOrdered());
      addVariableAttributes(vb, grid.attributes());
      parent.addVariable(vb);
    }
  }

  private void addVariableAttributes(Variable.Builder vb, AttributeContainer atts) {
    AttributeContainer modified = AttributeContainer.filter(atts, "_Coordinate", "_Chunk");
    modified.forEach(vb::addAttribute);
  }

  private void addCoordTransforms(CoverageCollection subsetDataset, Group.Builder group) {
    for (CoverageTransform ct : subsetDataset.getCoordTransforms()) {
      // scalar coordinate transform variable - container for transform info
      Variable.Builder ctv = Variable.builder().setName(ct.getName()).setDataType(DataType.INT);
      group.addVariable(ctv);
      ctv.addAttributes(ct.attributes());
    }
  }

  private void addLatLon2D(CoverageCollection subsetDataset, Group.Builder group) {
    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    CoverageCoordAxis1D xAxis = horizCoordSys.getXAxis();
    CoverageCoordAxis1D yAxis = horizCoordSys.getYAxis();

    Dimension xDim = group.findDimension(xAxis.getName())
        .orElseThrow(() -> new IllegalStateException("We should've added X dimension in addDimensions()."));
    Dimension yDim = group.findDimension(yAxis.getName())
        .orElseThrow(() -> new IllegalStateException("We should've added Y dimension in addDimensions()."));

    List<Dimension> dims = Arrays.asList(yDim, xDim);

    Variable.Builder latVar = Variable.builder().setName("lat").setDataType(DataType.DOUBLE).setDimensions(dims);
    latVar.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    latVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
    latVar.addAttribute(new Attribute(CDM.LONG_NAME, "latitude coordinate"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    group.addVariable(latVar);

    Variable.Builder lonVar = Variable.builder().setName("lon").setDataType(DataType.DOUBLE).setDimensions(dims);
    lonVar.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    lonVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
    lonVar.addAttribute(new Attribute(CDM.LONG_NAME, "longitude coordinate"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    group.addVariable(lonVar);
  }

  private void addCFAnnotations(CoverageCollection gds, Group.Builder group, boolean shouldAddLatLon2D) {
    for (Coverage grid : gds.getCoverages()) {
      CoverageCoordSys gcs = grid.getCoordSys();

      Optional<Variable.Builder<?>> newVopt = group.findVariableLocal(grid.getName());
      if (!newVopt.isPresent()) {
        logger.error("CFGridCoverageWriter cant find " + grid.getName() + " in writer ");
        continue;
      }
      Variable.Builder<?> newV = newVopt.get();

      // annotate Variable for CF
      Formatter coordsAttribValFormatter = new Formatter();
      for (String axisName : grid.getCoordSys().getAxisNames()) {
        coordsAttribValFormatter.format("%s ", axisName);
      }

      if (shouldAddLatLon2D) {
        group.findVariableLocal("lat")
            .orElseThrow(() -> new IllegalStateException("We should've added lat variable in addLatLon2D()"));
        group.findVariableLocal("lon")
            .orElseThrow(() -> new IllegalStateException("We should've added lon variable in addLatLon2D()"));
        coordsAttribValFormatter.format("lat lon");
      }

      newV.addAttribute(new Attribute(CF.COORDINATES, coordsAttribValFormatter.toString()));

      // add reference to coordinate transform variables
      CoverageTransform ct = gcs.getHorizTransform();
      if (ct != null && ct.isHoriz())
        newV.addAttribute(new Attribute(CF.GRID_MAPPING, ct.getName()));

      // LOOK what about vertical ?
    }

    for (CoverageCoordAxis axis : gds.getCoordAxes()) {
      Optional<Variable.Builder<?>> newVopt = group.findVariableLocal(axis.getName());
      if (!newVopt.isPresent()) {
        logger.error("CFGridCoverageWriter cant find " + axis.getName() + " in writer ");
        continue;
      }
      Variable.Builder<?> newV = newVopt.get();

      // LOOK: Commented out because CoverageCoordAxis doesn't have any info about "positive" wrt vertical axes.
      // To fix, we'd need to add that metadata when building the CRS.
      /*
       * if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) ||
       * (axis.getAxisType() == AxisType.GeoZ)) {
       * if (null != axis.getPositive())
       * newV.addAttribute(new Attribute(CF.POSITIVE, axis.getPositive()));
       * }
       */

      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
      }
      if (axis.getAxisType() == AxisType.GeoX) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.GeoY) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.Ensemble) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.ENSEMBLE));
      }
    }
  }

  private void writeCoordinateData(CoverageCollection subsetDataset, NetcdfFormatWriter writer)
      throws IOException, InvalidRangeException {
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      Variable v = writer.findVariable(axis.getName());
      if (v != null) {
        if (show)
          System.out.printf("CFGridCoverageWriter write axis %s%n", v.getNameAndDimensions());
        writer.write(v, axis.getCoordsAsArray());
      } else {
        logger.error("CFGridCoverageWriter No variable for {}", axis.getName());
      }

      if (axis.isInterval()) {
        Variable vb = writer.findVariable(axis.getName() + BOUNDS);
        writer.write(vb, axis.getCoordBoundsAsArray());
      }
    }
  }

  private void writeCoverageData(CoverageCollection gdsOrg, SubsetParams subsetParams, CoverageCollection subsetDataset,
      NetcdfFormatWriter writer) throws IOException, InvalidRangeException {
    for (Coverage coverage : subsetDataset.getCoverages()) {
      // we need to call readData on the original
      Coverage coverageOrg = gdsOrg.findCoverage(coverage.getName());
      GeoReferencedArray array = coverageOrg.readData(subsetParams);

      // test conform to whatever axis.getCoordsAsArray() returns
      checkConformance(coverage, array, gdsOrg.getName());

      Variable v = writer.findVariable(coverage.getName());
      if (show)
        System.out.printf("CFGridCoverageWriter write coverage %s%n", v.getNameAndDimensions());
      writer.write(v, array.getData());
    }
  }

  private void writeLatLon2D(CoverageCollection subsetDataset, NetcdfFormatWriter writer)
      throws IOException, InvalidRangeException {
    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    CoverageCoordAxis1D xAxis = horizCoordSys.getXAxis();
    CoverageCoordAxis1D yAxis = horizCoordSys.getYAxis();

    Projection proj = horizCoordSys.getTransform().getProjection();

    double[] xData = (double[]) xAxis.getCoordsAsArray().get1DJavaArray(DataType.DOUBLE);
    double[] yData = (double[]) yAxis.getCoordsAsArray().get1DJavaArray(DataType.DOUBLE);

    int numX = xData.length;
    int numY = yData.length;

    double[] latData = new double[numX * numY];
    double[] lonData = new double[numX * numY];

    // create the data
    for (int i = 0; i < numY; i++) {
      for (int j = 0; j < numX; j++) {
        ProjectionPoint projPoint = ProjectionPoint.create(xData[j], yData[i]);
        LatLonPoint latlonPoint = proj.projToLatLon(projPoint);
        latData[i * numX + j] = latlonPoint.getLatitude();
        lonData[i * numX + j] = latlonPoint.getLongitude();
      }
    }

    Variable latVar = writer.findVariable("lat");
    assert latVar != null : "We should have added lat var in addLatLon2D().";
    Array latDataArray = Array.factory(DataType.DOUBLE, new int[] {numY, numX}, latData);
    writer.write(latVar, latDataArray);

    Variable lonVar = writer.findVariable("lon");
    assert lonVar != null : "We should have added lon var in addLatLon2D().";
    Array lonDataArray = Array.factory(DataType.DOUBLE, new int[] {numY, numX}, lonData);
    writer.write(lonVar, lonDataArray);
  }

  private void checkConformance(Coverage gridSubset, GeoReferencedArray geo, String where) {
    CoverageCoordSys csys = gridSubset.getCoordSys();

    CoverageCoordSys csysData = geo.getCoordSysForData();

    Section s = new Section(csys.getShape());
    Section so = new Section(csysData.getShape());

    boolean ok = s.conformal(so);

    int[] dataShape = geo.getData().getShape();
    Section sdata = new Section(dataShape);
    boolean ok2 = s.conformal(sdata);

    if (!ok || !ok2)
      logger.warn("CFGridCoverageWriter checkConformance fails " + where);
  }
}
