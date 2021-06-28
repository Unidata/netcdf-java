package examples.coordsystems;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.Objects;


public class coordSystemBuilderTutorial {

    // logs error/info message in memory and can be accessed from test functions
    public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

    public String conventionName;

    public static void openDataset(String locationAsString, boolean enhance, ucar.nc2.util.CancelTask cancelTask) throws IOException {
        NetcdfDatasets.openDataset(locationAsString, enhance, cancelTask);
    }

    public static void isMineEx() {
        class nestedClass { /* DOCS-IGNORE */
            public /* INSERT static */ boolean isMine(NetcdfFile ncfile) {
                String stringValue =  Objects.requireNonNull(ncfile.findAttribute("full_name")).getStringValue();
                assert stringValue != null;
                return stringValue.equalsIgnoreCase("CRAFT/NEXRAD");
            }
        }
    }


    public static void augmentDataset1() {
        class nestedClass { /* DOCS-IGNORE */ public String conventionName;
            protected void augmentDataset(NetcdfDataset ncDataset) {
                this.conventionName ="ATDRadar";
                Variable time = ncDataset.findVariable("time");
                time.toBuilder().addAttribute( new Attribute("_CoordinateAxisType", "Time"));
            }
        }
    }

    public static void augmentDataset2() throws IOException {
        class nestedClass { /* DOCS-IGNORE */ public String conventionName;
            protected void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
                NetcdfDataset.Builder<?> ncBuilder = ncDataset.toBuilder();
                this.conventionName = "ATDRadar";
                ucar.nc2.internal.ncml.NcmlReader.wrapNcml( ncBuilder, "file:/MyResource/ATDRadar.ncml", cancelTask);
            }
        }
    }

    public static void wrapNcmlExample(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
        ucar.nc2.internal.ncml.NcmlReader.wrapNcmlResource( ncDataset.toBuilder(), "ATDRadar.ncml", cancelTask);
    }

    public static void registerNcml(String conventionNameAsString, String ncmlLocationAsString){
        CoordSystemFactory.registerNcml( conventionNameAsString, ncmlLocationAsString);
    }

    public static void augmentDataset3( NetcdfFile netcdfFile, StringBuilder parseInfo) {
        class nestedClass { /* DOCS-IGNORE */ public String conventionName;
            protected void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
                this.conventionName = "Zebra";
                // The time coord variable is created in the NcML
                ucar.nc2.internal.ncml.NcmlReader.wrapNcmlResource(
                        ncDataset.toBuilder(), CoordSystemFactory.resourcesDir + "Zebra.ncml", cancelTask);

                Dimension timeDim = netcdfFile.findDimension("time");
                Variable base_time = netcdfFile.findVariable("base_time");
                Variable time_offset = netcdfFile.findVariable("time_offset");

                // The time coordinate is created in the NcML file, and we set its values here
                Variable time = netcdfFile.findVariable("time");

                Attribute att = base_time.findAttribute("units");
                String units = (att != null) ? att.getStringValue() : "seconds since 1970-01-01 00:00 UTC";

                // Time coordinate units are set equal to units on the base_time variable
                time.toBuilder().addAttribute( new Attribute("units", units));

                Array data;
                try {
                    // Read in the (scalar) base_time
                    Double baseValue = (Double) base_time.readArray().getScalar();
                    // Read in the time_offset array
                    data = (ucar.array.Array) time_offset.readArray();
                    IndexIterator iter = (IndexIterator) data.iterator();
                    while (iter.hasNext()) {
                        // Add the baseValue to each value of the time_offset
                        iter.setDoubleCurrent( iter.getDoubleNext() + baseValue);
                        // For long-running calculations, check to see if the user has cancelled, and return ASAP
                        if ((cancelTask != null) && cancelTask.isCancel()) return;
                    }
                } catch (java.io.IOException ioe) {
                    // Error message if there's an exception
                    parseInfo.append(
                            "ZebraConvention failed to create time Coord Axis for "+ netcdfFile.getLocation()+"\n"+ioe+"\n");
                    return;
                }
                // Set the data values of the time coordinate to the computed values
                Variable.Builder timeb = Variable.builder().setName("time").setArrayType(ArrayType.DOUBLE);;
                timeb.setSourceData( data);

                // When adding new variables to a dataset, you must call close() when all done
                netcdfFile.close();
            }
        }
    }

    public static void getAxisType() {
        class nestedClass { /* DOCS-IGNORE */
            protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {
                String unit = v.getUnitsString();
                if (unit == null)
                    return null;
                if (unit.equalsIgnoreCase("degrees_east") ||
                        unit.equalsIgnoreCase("degrees_E") ||
                        unit.equalsIgnoreCase("degreesE") ||
                        unit.equalsIgnoreCase("degree_east") ||
                        unit.equalsIgnoreCase("degree_E") ||
                        unit.equalsIgnoreCase("degreeE"))
                    return AxisType.Lon;

                if (unit.equalsIgnoreCase("degrees_north") ||
                        unit.equalsIgnoreCase("degrees_N") ||
                        unit.equalsIgnoreCase("degreesN") ||
                        unit.equalsIgnoreCase("degree_north") ||
                        unit.equalsIgnoreCase("degree_N") ||
                        unit.equalsIgnoreCase("degreeN"))
                    return AxisType.Lat;

                if (SimpleUnit.isDateUnit(unit) || SimpleUnit.isTimeUnit(unit))
                    return AxisType.Time;

                // look for other z coordinate
                if (SimpleUnit.isCompatible("m", unit))
                    return AxisType.Height;
                if (SimpleUnit.isCompatible("mbar", unit))
                    return AxisType.Pressure;
                if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
                    return AxisType.GeoZ;

                Variable var = ncDataset.findVariable("fullName");
                String positive = var.findAttributeString("attributeName", "positive");
                if (positive != null) {
                    if (SimpleUnit.isCompatible("m", unit))
                        return AxisType.Height;
                    else
                        return AxisType.GeoZ;
                }
                return null;
            }

        }
    }

    public static void argumentDataset4() throws IOException {
        class nestedClass { /* DOCS-IGNORE */
            protected void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
                // Read the projection values stored as non-standard global attributes in your dataset
                double lat_origin = (double) ncDataset.findVariable("varName").findAttribute("LAT0").getNumericValue();
                double lon_origin = (double) ncDataset.findVariable("varName").findAttribute("LON0").getNumericValue();
                double scale = (double) ncDataset.findVariable("varName").findAttribute("SCALE").getNumericValue();
                if (Double.isNaN(scale)) scale = 1.0;

                // A Coordinate Transform Variable is created, and the parameters are renamed according to the CF-1.0
                Variable.Builder v = Variable.builder().setName("ProjectionPS").setArrayType(ArrayType.CHAR);

                v.addAttribute( new Attribute("grid_mapping_name", "polar_stereographic"));
                v.addAttribute( new Attribute("straight_vertical_longitude_from_pole", lon_origin));
                v.addAttribute( new Attribute("latitude_of_projection_origin", lat_origin));
                v.addAttribute( new Attribute("scale_factor_at_projection_origin", scale));

                // The CoordinateTransformType identifies this variable unambiguously as a CoordinateTransform 
                v.addAttribute( new Attribute(ucar.nc2.constants._Coordinate.TransformType, TransformType.Projection.toString()));
                // See NOTE below
                v.addAttribute( new Attribute(ucar.nc2.constants._Coordinate.AxisTypes, "GeoX GeoY"));
                // Fake data is added, in case someone accidentally tries to read it
                Array data = Arrays.factory(ArrayType.CHAR, new int[] {}, new char[] {' '});
                v.setSourceData(data);
                // The Coordinate Transform Variable is added to the dataset
                ncDataset.close();
            }
        }
    }

    public static void argumentDataset5( CoordSystemBuilder youCoordSystemBuilder){
        class nestedClass { /* DOCS-IGNORE */
            protected void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
                // Read the projection values stored as non-standard global attributes in your dataset
                double lat_origin = (double) ncDataset.findVariable("varName").findAttribute("LAT0").getNumericValue();
                double lon_origin = (double) ncDataset.findVariable("varName").findAttribute("LON0").getNumericValue();
                double scale = (double) ncDataset.findVariable("varName").findAttribute("SCALE").getNumericValue();
                if (Double.isNaN(scale)) scale = 1.0;

                // A Projection is created out of those parameters
                Projection proj = new ucar.unidata.geoloc.projection.Stereographic( lat_origin, lon_origin, scale);
                // A ProjectionCT wraps the Projection
                ProjectionCT projCT = new ProjectionCT("ProjectionPS", "FGDC", proj);

                // The makeCoordinateTransformVariable method creates the CoordinateTransform
                // The Projection knows what the standard names of its parameters are
                VariableDS.Builder<?> vb = youCoordSystemBuilder.makeCoordinateTransformVariable( projCT);

                // The _CoordinateAxisTypes attribute indicates that the transform is used for all Coordinate Systems that have a GeoX and GeoY coordinate axis
                vb.addAttribute( new Attribute(ucar.nc2.constants._Coordinate.AxisTypes, "GeoX GeoY"));
                // The CoordinateTransform variable is added to the dataset
                NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.openExisting(ncDataset.getLocation());
                Variable v = (Variable) vb.build(Group.builder().build());
                builder.addVariable("time", v.getArrayType(), v.getDimensionsString());
                ncDataset.close();
            }
        }
    }
}
