/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataComposite;
import ucar.ma2.StructureDataFromMember;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.VariableSimpleBuilder;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time2.CalendarDate;
import ucar.nc2.time2.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;

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
 */
class WriterCFPointCollection extends WriterCFPointAbstract {

  WriterCFPointCollection(String fileOut, AttributeContainer globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writerb.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.point.name()));
    writerb.addAttribute(new Attribute(CF.DSG_REPRESENTATION, "Point Data, H.1"));
  }

  void writeHeader(PointFeature pf) throws IOException {
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleBuilder.makeScalar(timeName, "time of measurement", timeUnit.toString(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    coords.add(
        VariableSimpleBuilder.makeScalar(latName, "latitude of measurement", CDM.LAT_UNITS, DataType.DOUBLE).build());
    coords.add(
        VariableSimpleBuilder.makeScalar(lonName, "longitude of measurement", CDM.LON_UNITS, DataType.DOUBLE).build());
    Formatter coordNames = new Formatter().format("%s %s %s", timeName, latName, lonName);
    if (altUnits != null) {
      coords.add(VariableSimpleBuilder.makeScalar(altName, "altitude of measurement", altUnits, DataType.DOUBLE)
          .addAttribute(CF.POSITIVE, getZisPositive(altName, altUnits)).build());
      coordNames.format(" %s", altName);
    }

    super.writeHeader(coords, null, null, pf.getDataAll(), coordNames.toString());
  }

  @Override
  void makeFeatureVariables(StructureData featureData, boolean isExtended) {
    // NOOP
  }

  /////////////////////////////////////////////////////////
  // writing data
  private int obsRecno;

  void writeRecord(PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sobs.getLocation(), sdata);
  }

  private void writeRecord(double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata)
      throws IOException {
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
