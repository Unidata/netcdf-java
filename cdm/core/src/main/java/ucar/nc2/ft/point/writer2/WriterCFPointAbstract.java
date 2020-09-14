/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.writer2;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.internal.dataset.conv.CF1Convention;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

/** Abstract superclass for WriterCFPointXXXX */
abstract class WriterCFPointAbstract implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(WriterCFPointAbstract.class);

  static final String recordName = "obs";
  static final String recordDimName = "obs";
  static final String latName = "latitude";
  static final String lonName = "longitude";
  static final String altName = "altitude";
  static final String timeName = "time";

  static final String stationStructName = "station";
  static final String stationDimName = "station";
  static final String stationIdName = "station_id";
  static final String stationAltName = "stationAltitude";
  static final String descName = "station_description";
  static final String wmoName = "wmo_id";
  static final String stationIndexName = "stationIndex";

  static final String profileStructName = "profile";
  static final String profileDimName = "profile";
  static final String profileIdName = "profileId";
  static final String numberOfObsName = "nobs";
  static final String profileTimeName = "profileTime";

  static final String trajStructName = "trajectory";
  static final String trajDimName = "traj";
  static final String trajIdName = "trajectoryId";

  static final int idMissingValue = -9999;
  private static final int defaultStringLength = 20;

  // attributes with these names will not be copied to the output file
  private static final List<String> reservedGlobalAtts =
      Arrays.asList(CDM.CONVENTIONS, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.TIME_START,
          ACDD.TIME_END, _Coordinate._CoordSysBuilder, CF.featureTypeAtt2, CF.featureTypeAtt3);

  private static final List<String> reservedVariableAtts = Arrays.asList(CF.SAMPLE_DIMENSION, CF.INSTANCE_DIMENSION);

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final List<VariableSimpleIF> dataVars;
  final CalendarDateUnit timeUnit;
  final @Nullable String altUnits;
  private final CFPointWriterConfig config;
  private final boolean isExtendedModel;
  private final Map<String, Dimension> newDimensions = new HashMap<>(); // track dimensions by name
  final NetcdfFormatWriter.Builder writerb;

  private NetcdfFormatWriter writer;
  int nfeatures, id_strlen;

  boolean useAlt = true;
  String altitudeCoordinateName = altName;

  Structure record; // used for netcdf3 and netcdf4 extended
  HashSet<String> dataMap = new HashSet<>();
  private List<Variable> extra;

  LatLonRect.Builder llbb;
  private CalendarDate minDate;
  private CalendarDate maxDate;

  /**
   * Ctor
   *
   * @param fileOut name of the output file
   * @param atts global attributes to be added
   * @param dataVars the data variables to be added to the output file
   * @param timeUnit the unit of the time coordinate
   * @param altUnits the unit of the altitude coordinate, may be nullable
   * @param config configuration
   */
  WriterCFPointAbstract(String fileOut, AttributeContainer atts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, @Nullable String altUnits, CFPointWriterConfig config) {

    this.dataVars = dataVars;
    this.timeUnit = timeUnit;
    this.altUnits = altUnits;
    this.config = config;
    this.isExtendedModel = config.getFormat().isExtendedModel();
    this.writerb = NetcdfFormatWriter.builder().setFormat(config.getFormat()).setLocation(fileOut)
        .setChunker(config.getChunking()).setFill(false);

    addGlobalAtts(atts);
    addNetcdf3UnknownAtts(config.isNoTimeCoverage());
  }

  private void addGlobalAtts(AttributeContainer atts) {
    writerb.addAttribute(new Attribute(CDM.CONVENTIONS, isExtendedModel ? CDM.CF_EXTENDED : "CF-1.6"));
    writerb.addAttribute(new Attribute(CDM.HISTORY, "Written by CFPointWriter"));
    for (Attribute att : atts) {
      if (!reservedGlobalAtts.contains(att.getShortName()))
        writerb.addAttribute(att);
    }
  }

  // netcdf3 has to add attributes up front, but we dont know values until the end.
  // so we have this updateAttribute hack; values set in finish()
  private void addNetcdf3UnknownAtts(boolean noTimeCoverage) {
    // dummy values, update in finish()
    if (!noTimeCoverage) {
      CalendarDate now = CalendarDate.of(new Date());
      writerb.addAttribute(new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(now)));
      writerb.addAttribute(new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(now)));
    }
    writerb.addAttribute(new Attribute(ACDD.LAT_MIN, 0.0));
    writerb.addAttribute(new Attribute(ACDD.LAT_MAX, 0.0));
    writerb.addAttribute(new Attribute(ACDD.LON_MIN, 0.0));
    writerb.addAttribute(new Attribute(ACDD.LON_MAX, 0.0));
  }

  //////////////////////////////////////////////////////////////////////
  // These are set from CFPointWriter

  void setFeatureAuxInfo(int nfeatures, int id_strlen) {
    this.nfeatures = nfeatures;
    this.id_strlen = id_strlen;
  }

  void setExtraVariables(List<Variable> extra) {
    this.extra = extra;
    if (extra != null) {
      for (Variable v : extra) {
        if (v instanceof CoordinateAxis) {
          CoordinateAxis axis = (CoordinateAxis) v;
          if (axis.getAxisType() == AxisType.Height) {
            useAlt = false; // dont need another altitude variable
            altitudeCoordinateName = v.getFullName();
          }
        }
      }
    }
  }

  //////////////////////////////////////////////////////////////////////
  // These are called from subclasses

  @Nullable
  VariableSimpleIF findDataVar(String name) {
    return dataVars.stream().filter(v -> v.getShortName().equals(name)).findFirst().orElse(null);
  }

  // Always overridden
  abstract void makeFeatureVariables(StructureData featureData, boolean isExtended);

  // Supplied when its a two level feature (station profile, trajectory profile)
  void makeMiddleVariables(StructureData middleData, boolean isExtended) {
    // NOOP
  }

  void writeHeader(List<VariableSimpleIF> obsCoords, StructureData featureData, @Nullable StructureData middleData,
      StructureData obsData, String coordNames) throws IOException {

    Dimension recordDim = writerb.addUnlimitedDimension(recordDimName);
    addExtraVariables();

    if (isExtendedModel) {
      if (featureData != null) {
        makeFeatureVariables(featureData, true);
      }
      if (middleData != null) {
        makeMiddleVariables(middleData, true);
      }
      Structure.Builder<?> recordb = writerb.addStructure(recordName, recordDimName);
      addCoordinatesExtended(recordb, obsCoords);
      addDataVariablesExtended(recordb, obsData, coordNames);

    } else {
      if (featureData != null) {
        makeFeatureVariables(featureData, false);
      }
      if (middleData != null) {
        makeMiddleVariables(middleData, false);
      }
      addCoordinatesClassic(recordDim, obsCoords, dataMap);
      addDataVariablesClassic(recordDim, obsData, dataMap, coordNames);
      // record = writer.addRecordStructure(); // for netcdf3
    }

    // Create the NetcdfFile and write variable metadata to it.
    this.writer = writerb.build();
    /*
     * NetcdfFormatWriter.Result result = writer.create(netcdfBuilder.build(), 0);
     * if (!result.wasWritten()) {
     * throw new IOException(result.getErrorMessage());
     * }
     */

    writeExtraVariables();
    finishBuilding();
  }

  private void addExtraVariables() {
    if (extra == null)
      return;

    addDimensionsClassic(extra);

    for (VariableSimpleIF vs : extra) {
      List<Dimension> dims = makeDimensionList(vs.getDimensions());
      writerb.addVariable(vs.getShortName(), vs.getDataType(), dims).addAttributes(vs.attributes());
    }
  }

  // added as variables with the unlimited (record) dimension
  void addCoordinatesClassic(Dimension recordDim, List<VariableSimpleIF> coords, Set<String> varSet) {
    addDimensionsClassic(coords);

    for (VariableSimpleIF oldVar : coords) {
      List<Dimension> dims = makeDimensionList(oldVar.getDimensions());
      dims.add(0, recordDim);
      Variable.Builder<?> newVar;

      if (oldVar.getDataType() == DataType.STRING && !this.isExtendedModel) {
        // What should the string length be ?? Should read variable to find out....see old
        // writer.addStringVariable(null, (Variable) oldVar, dims)
        String name = oldVar.getShortName();
        Dimension strlen = new Dimension(name + "_strlen", defaultStringLength);
        newVar = Variable.builder().setName(name).setDataType(DataType.CHAR).setDimensions(dims).addDimension(strlen);
        writerb.getRootGroup().addDimensionIfNotExists(strlen);
      } else {
        newVar =
            Variable.builder().setName(oldVar.getShortName()).setDataType(oldVar.getDataType()).setDimensions(dims);
      }

      if (writerb.getRootGroup().replaceVariable(newVar)) {
        logger.info("Variable was already added =" + oldVar.getShortName());
      }

      newVar.addAttributes(oldVar.attributes());
      varSet.add(oldVar.getShortName());
    }

  }

  // added as members of the given structure
  void addCoordinatesExtended(Structure.Builder<?> parent, List<VariableSimpleIF> coords) {
    for (VariableSimpleIF vs : coords) {
      String dims = Dimensions.makeDimensionsString(vs.getDimensions());
      Variable.Builder<?> member = Variable.builder().setName(vs.getShortName()).setDataType(vs.getDataType())
          .setParentGroupBuilder(writerb.getRootGroup()).setDimensionsByName(dims);
      if (parent.replaceMemberVariable(member)) {
        logger.warn("Variable already exists =" + vs.getShortName()); // LOOK barf
      }
      member.addAttributes(vs.attributes());
    }
  }

  // added as variables with the unlimited (record) dimension
  private void addDataVariablesClassic(Dimension recordDim, StructureData stnData, HashSet<String> varSet,
      String coordVars) {
    addDimensionsClassic(dataVars);

    for (StructureMembers.Member m : stnData.getMembers()) {
      VariableSimpleIF oldVar = findDataVar(m.getName());
      if (oldVar == null)
        continue;

      List<Dimension> dims = makeDimensionList(oldVar.getDimensions());
      dims.add(0, recordDim);

      Variable.Builder<?> newVar;
      if (oldVar.getDataType() == DataType.STRING && !isExtendedModel) {
        // What should the string length be ??
        String name = oldVar.getShortName();
        Dimension strlen = new Dimension(name + "_strlen", defaultStringLength);
        newVar = Variable.builder().setName(name).setDataType(DataType.CHAR).setDimensions(dims).addDimension(strlen);
        writerb.getRootGroup().addDimensionIfNotExists(strlen);
      } else {
        newVar =
            Variable.builder().setName(oldVar.getShortName()).setDataType(oldVar.getDataType()).setDimensions(dims);
      }

      if (writerb.getRootGroup().replaceVariable(newVar)) {
        logger.warn("Variable was already added =" + oldVar.getShortName());
      }

      for (Attribute att : oldVar.attributes()) {
        String attName = att.getShortName();
        if (!reservedVariableAtts.contains(attName) && !attName.startsWith("_Coordinate"))
          newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordVars));
      varSet.add(oldVar.getShortName());
    }

  }

  // add variables to the record structure
  private void addDataVariablesExtended(Structure.Builder<?> recordb, StructureData obsData, String coordNames) {
    for (StructureMembers.Member m : obsData.getMembers()) {
      VariableSimpleIF oldVar = findDataVar(m.getName());
      if (oldVar == null)
        continue;

      // make dimension list
      StringBuilder dimNames = new StringBuilder();
      for (Dimension d : oldVar.getDimensions()) {
        if (d.isUnlimited())
          continue;
        if (d.getShortName() == null || !d.getShortName().equals(recordDimName))
          dimNames.append(" ").append(d.getLength()); // anonymous
      }

      Variable.Builder<?> newVar = Variable.builder().setName(oldVar.getShortName()).setDataType(oldVar.getDataType())
          .setParentGroupBuilder(writerb.getRootGroup()).setDimensionsByName(dimNames.toString());
      recordb.addMemberVariable(newVar);

      // TODO
      /*
       * Variable newVar =
       * writer.addStructureMember(record, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());
       * if (newVar == null) {
       * logger.warn("Variable already exists =" + oldVar.getShortName()); // LOOK barf
       * continue;
       * }
       */

      for (Attribute att : oldVar.attributes()) {
        String attName = att.getShortName();
        if (!reservedVariableAtts.contains(attName) && !attName.startsWith("_Coordinate"))
          newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordNames));
    }

  }

  // classic model: no private dimensions
  private void addDimensionsClassic(List<? extends VariableSimpleIF> vars) {
    Set<Dimension> oldDims = new HashSet<>(20);

    // find all dimensions needed by these variables
    for (VariableSimpleIF var : vars) {
      List<Dimension> dims = var.getDimensions();
      oldDims.addAll(dims);
    }

    // add them
    for (Dimension d : oldDims) {
      // The dimension we're creating below will be shared, so we need an appropriate name for it.
      String dimName = getSharedDimName(d);
      if (!writerb.getRootGroup().findDimension(dimName).isPresent()) {
        Dimension newDim = Dimension.builder(dimName, d.getLength()).setIsVariableLength(d.isVariableLength()).build();
        writerb.addDimension(newDim);
        newDimensions.put(dimName, newDim);
      }
    }
  }

  private List<Dimension> makeDimensionList(List<Dimension> oldDims) {
    List<Dimension> result = new ArrayList<>();

    // find all dimensions needed by the coord variables
    for (Dimension dim : oldDims) {
      Dimension newDim = newDimensions.get(getSharedDimName(dim));
      assert newDim != null : "Oops, we screwed up: dimMap doesn't contain " + getSharedDimName(dim);
      result.add(newDim);
    }

    return result;
  }

  /**
   * Returns a name for {@code dim} that is suitable for a shared dimension. If the dimension is anonymous, meaning
   * that its name is {@code null}, we return a default name: {@code "len" + dim.getLength()}. Otherwise, we return the
   * dimension's existing name.
   *
   * @param dim a dimension.
   * @return a name that is suitable for a shared dimension, i.e. not {@code null}.
   */
  private String getSharedDimName(Dimension dim) {
    if (dim.getShortName() == null) { // Dim is anonymous.
      return "len" + dim.getLength();
    } else {
      return dim.getShortName();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // called after the NetcdfFile has been created

  void finishBuilding() throws IOException {
    record = findStructure(recordName);
  }

  @Nullable
  Structure findStructure(String name) {
    NetcdfFile outputFile = writer.getOutputFile();
    Variable s = outputFile.getVariables().stream().filter(v -> v.getShortName().equals(name)).findFirst().orElse(null);
    return (s instanceof Structure) ? (Structure) s : null;
  }

  @Nullable
  private Variable findVariable(String name) {
    NetcdfFile outputFile = writer.getOutputFile();
    return outputFile.getVariables().stream().filter(v -> v.getShortName().equals(name)).findFirst().orElse(null);
  }

  private void writeExtraVariables() throws IOException {
    if (extra == null)
      return;

    for (Variable v : extra) {
      NetcdfFile ncfile = writer.getOutputFile();
      Variable mv = ncfile.findVariable(v.getFullName());
      if (mv == null)
        continue; // may be removed
      try {
        writer.write(mv, v.read());
      } catch (InvalidRangeException e) {
        e.printStackTrace(); // cant happen haha
      }
    }
  }

  int writeStructureData(int recno, Structure s, StructureData sdata, Set<String> varSet) throws IOException {

    // write the recno record
    int[] origin = new int[1];
    origin[0] = recno;
    try {
      if (isExtendedModel) {
        if (s.isUnlimited())
          return writer.appendStructureData(s, sdata); // can write it all at once along unlimited dimension
        else {
          ArrayStructureW as = new ArrayStructureW(sdata.getStructureMembers(), new int[] {1});
          as.setStructureData(sdata, 0);
          writer.write(s, origin, as); // can write it all at once along regular dimension
          return recno + 1;
        }

      } else {
        writeStructureDataClassic(origin, sdata, varSet);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    return recno + 1;
  }

  private void writeStructureDataClassic(int[] origin, StructureData sdata, Set<String> varSet)
      throws IOException, InvalidRangeException {
    for (StructureMembers.Member m : sdata.getMembers()) {
      Variable mv = findVariable(m.getName());
      if (!varSet.contains(m.getName()) || mv == null) {
        continue; // normal to fail here
      }

      Array org = sdata.getArray(m);
      if (m.getDataType() == DataType.STRING) { // convert to ArrayChar
        int strlen = mv.getDimension(mv.getDimensions().size() - 1).getLength();
        org = ArrayChar.makeFromStringArray((ArrayObject) org, strlen);
      }

      Array orgPlus1 = Array.makeArrayRankPlusOne(org); // add dimension on the left (slow)
      int[] useOrigin = origin;

      if (org.getRank() > 0) { // if rank 0 (common case, this is a nop, so skip)
        useOrigin = new int[org.getRank() + 1];
        useOrigin[0] = origin[0]; // the rest are 0
      }

      writer.write(mv, useOrigin, orgPlus1);
    }

  }


  // keep track of the bounding box
  void trackBB(LatLonPoint loc, CalendarDate obsDate) {
    if (loc != null) {
      if (llbb == null) {
        llbb = new LatLonRect.Builder(loc, .001, .001);
      } else {
        llbb.extend(loc);
      }
    }

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate))
      minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate))
      maxDate = obsDate;
  }

  public void finish() throws IOException {
    if (llbb != null) {
      LatLonRect rect = llbb.build();
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MIN, rect.getLowerLeftPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MAX, rect.getUpperRightPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MIN, rect.getLowerLeftPoint().getLongitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MAX, rect.getUpperRightPoint().getLongitude()));
    }

    if (!config.isNoTimeCoverage()) {
      if (minDate == null)
        minDate = CalendarDate.present();
      if (maxDate == null)
        maxDate = CalendarDate.present();
      writer.updateAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(minDate)));
      writer.updateAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(maxDate)));
    }
  }

  @Override
  public void close() throws IOException {
    if (writer != null) {
      writer.close();
    }
  }

  // cover CF1Convention
  static String getZisPositive(String zaxisName, String vertCoordUnits) {
    return CF1Convention.getZisPositive(zaxisName, vertCoordUnits);
  }
}
