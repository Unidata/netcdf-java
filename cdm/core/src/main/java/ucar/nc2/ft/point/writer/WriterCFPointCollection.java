/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import com.google.common.collect.ImmutableList;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.*;
import ucar.nc2.ft.*;
import ucar.unidata.geoloc.EarthLocation;
import ucar.ma2.*;
import java.util.*;
import java.io.IOException;

/**
 * Write a CF 1.6 "Discrete Sample" point file.
 *
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8294224"
 * @author caron
 * @since Nov 23, 2010
 */
public class WriterCFPointCollection extends CFPointWriter {
  // private Map<String, Variable> varMap = new HashMap<>();

  public WriterCFPointCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.point.name()));
    writer.addGroupAttribute(null, new Attribute(CF.DSG_REPRESENTATION, "Point Data, H.1"));
  }

  public void writeHeader(PointFeature pf) throws IOException {
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleBuilder.makeScalar(pf.getFeatureCollection().getTimeName(), "time of measurement",
        timeUnit.getUdUnit(), DataType.DOUBLE).addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    coords.add(
        VariableSimpleBuilder.makeScalar(latName, "latitude of measurement", CDM.LAT_UNITS, DataType.DOUBLE).build());
    coords.add(
        VariableSimpleBuilder.makeScalar(lonName, "longitude of measurement", CDM.LON_UNITS, DataType.DOUBLE).build());
    Formatter coordNames =
        new Formatter().format("%s %s %s", pf.getFeatureCollection().getTimeName(), latName, lonName);
    if (altUnits != null) {
      coords.add(VariableSimpleBuilder
          .makeScalar(pf.getFeatureCollection().getAltName(), "altitude of measurement", altUnits, DataType.DOUBLE)
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(pf.getFeatureCollection().getAltName(), altUnits))
          .build());
      coordNames.format(" %s", pf.getFeatureCollection().getAltName());
    }

    super.writeHeader(coords, null, pf.getDataAll(), coordNames.toString());
  }

  public void writeHeader(PointFeatureCollection pfc) throws IOException {
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleBuilder
        .makeScalar(pfc.getTimeName(), "time of measurement", timeUnit.getUdUnit(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    coords.add(
        VariableSimpleBuilder.makeScalar(latName, "latitude of measurement", CDM.LAT_UNITS, DataType.DOUBLE).build());
    coords.add(
        VariableSimpleBuilder.makeScalar(lonName, "longitude of measurement", CDM.LON_UNITS, DataType.DOUBLE).build());
    if (altUnits != null) {
      coords
          .add(VariableSimpleBuilder.makeScalar(pfc.getAltName(), "altitude of measurement", altUnits, DataType.DOUBLE)
              .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits)).build());
    }

    super.writeHeader(coords, Arrays.asList(pfc), null, null);
  }

  protected void makeFeatureVariables(List<StructureData> featureData, boolean isExtended) {
    // NOOP
  }

  /////////////////////////////////////////////////////////
  // writing data

  public void writeRecord(PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(sobs.getFeatureCollection().getTimeName(), sobs.getObservationTime(),
        sobs.getObservationTimeAsCalendarDate(), sobs.getFeatureCollection().getAltName(), sobs.getLocation(), sdata);
  }

  private int obsRecno;

  public void writeRecord(double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata)
      throws IOException {
    writeRecord(timeName, timeCoordValue, obsDate, altName, loc, sdata);
  }

  private void writeRecord(String timeName, double timeCoordValue, CalendarDate obsDate, String altName,
      EarthLocation loc, StructureData sdata) throws IOException {
    trackBB(loc.getLatLon(), obsDate);

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(timeName, null, null, DataType.DOUBLE, timeCoordValue);
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, loc.getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, loc.getLongitude());
    if (altUnits != null)
      smb.addMemberScalar(altName, null, null, DataType.DOUBLE, loc.getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, sdata));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);

  }
}
