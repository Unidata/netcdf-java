package examples.cdmdatasets;

import ucar.array.*;
import ucar.nc2.*;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.*;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WritingNetcdfTutorial {
  // logs error/info message in memory and can be accessed from test functions
  public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

  ////////////////////////////
  // constant string messages for tests
  public static final String yourCreateNetcdfFileErrorMsgTxt =
      "exception while creating new Netcdf file";
  public static final String yourWriteNetcdfFileErrorMsgTxt =
      "exception while writing to Netcdf file";
  public static final String someStringValue = "This porridge is too hot!";
  public static final String anotherStringValue = "This porridge is too cold!";
  public static final String aThirdStringValue = "This porridge is just right!";

  ////////////////////////////
  // Netcdf tutorial functions
  // NOTE: these functions are used in the Netcdf tutorial docs, so formatting matters!

  /**
   * Tutorial code snippet to create a new netCDF3 file
   * 
   * @param pathAndFilenameStr
   * @return NetcdfFormatWriter
   */
  public static NetcdfFormatWriter createNCFile(String pathAndFilenameStr) {
    // 1) Create a new netCDF-3 file builder with the given path and file name
    NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(pathAndFilenameStr);

    // 2) Create two Dimensions, named lat and lon, of lengths 64 and 129 respectively, and add them to the root group
    Dimension latDim = builder.addDimension("lat", 64);
    Dimension lonDim = builder.addDimension("lon", 128);

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(latDim);
    dims.add(lonDim);

    // 3) Create a builder for a Variable named temperature, or type double, with shape (lat, lon), and add to the root
    // group
    Variable.Builder t = builder.addVariable("temperature", ArrayType.DOUBLE, dims);

    // 4) Add a string Attribute to the temperature Variable, with name units and value K
    t.addAttribute(new Attribute("units", "K"));

    // 5) Create a 1D integer Array Attribute using Attribute.Builder,with name scale and value (1,2,3)
    // and add to the temperature Variables
    Array data = Arrays.factory(ArrayType.INT, new int[] {3}, new int[] {1, 2, 3});
    t.addAttribute(Attribute.builder("scale").setArrayValues(data).build());

    // 6) Create a Variable named svar or type character with length 80
    Dimension svar_len = builder.addDimension("svar_len", 80);
    builder.addVariable("svar", ArrayType.CHAR, "svar_len");

    // 7) Create a 2D Variable names names of type character with length 80
    Dimension names = builder.addDimension("names", 3);
    builder.addVariable("names", ArrayType.CHAR, "names svar_len");

    // 8) Create a scalar Variable names scalar or type double.
    // Note that the empty ArrayList means that it is a scalar, i.e. has no dimensions
    builder.addVariable("scalar", ArrayType.DOUBLE, new ArrayList<Dimension>());

    // 9) Create various global Attributes of different types
    builder.addAttribute(new Attribute("versionStr", "v"));
    builder.addAttribute(new Attribute("versionD", 1.2));
    builder.addAttribute(new Attribute("versionF", (float) 1.2));
    builder.addAttribute(new Attribute("versionI", 1));
    builder.addAttribute(new Attribute("versionS", (short) 2));
    builder.addAttribute(new Attribute("versionB", (byte) 3));

    // 10) Now that the metadata (Dimensions, Variables, and Attributes) is added to the builder, build the writer
    // At this point, the (empty) file will be written to disk, and the metadata is fixed and cannot be changed or
    // added.
    try (NetcdfFormatWriter writer = builder.build()) {
      // write data
      return writer; /* DOCS-IGNORE */
    } catch (IOException e) {
      logger.log(yourCreateNetcdfFileErrorMsgTxt);
    }
    return null; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to set the fill option of a NetcdfFormatWriter
   * 
   * @param builder
   */
  public static void setFillOption(NetcdfFormatWriter.Builder builder) {
    builder.setFill(true);
  }

  /**
   * Tutorial code snippet to open an existing netCDF file
   * 
   * @param filePathStr
   * @return NetcdfFormatWriter
   * @throws IOException
   */
  public static NetcdfFormatWriter openNCFileForWrite(String filePathStr) throws IOException {
    NetcdfFormatWriter updater = NetcdfFormatWriter.openExisting(filePathStr).build();
    return updater; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to write an array of doubles
   * 
   * @param writer
   * @param varName
   */
  public static void writeDoubleData(NetcdfFormatWriter writer, String varName) {
    // 1) Create a primitive array of the same shape as temperature(lat, lon) and fill it with some values
    Variable v = writer.findVariable(varName);
    int[] shape = v.getShape();
    double[][] a = new double[shape[0]][shape[1]];
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        a[i][j] = (double) (i * 1000000 + j * 1000);
      }
    }

    // 2) Create an Array of doubles from the primitive array
    Array<Double> A = Arrays.factory(ArrayType.DOUBLE, shape, a);

    // 2) Write the data to the temperature Variable, with origin all zeros.
    // Shape is taken from the data Array.
    int[] origin = new int[2]; // initialized to zeros
    try {
      writer.write(v, new Index(origin), A);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
  }

  /**
   * Tutorial code snippet to write char data as a string
   * 
   * @param writer
   * @param varName
   */
  public static void writeCharData(NetcdfFormatWriter writer, String varName) {
    // write char variable as String
    Variable v = writer.findVariable(varName);
    int[] shape = v.getShape();
    int len = shape[0];

    // 1) The ArrayChar class has special methods to make it convenient to work with Strings.
    // Note that we use the type and rank specific constructor ArrayChar.D1.
    // The setString(String val) method is for rank one ArrayChar objects.
    ArrayByte ac = new ArrayByte(len);
    ac.setString(someStringValue);

    // 2) Write the data. Since we dont pass in an origin parameter, it is assumed to be all zeroes.
    try {
      writer.write(v, ac);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
  }

  /**
   * Tutorial code snippet to write a string array
   * 
   * @param writer
   * @param varName
   */
  public static void writeStringArray(NetcdfFormatWriter writer, String varName) {
    Variable v = writer.findVariable("names");
    int[] shape = v.getShape();

    // 1) The setString(int index, String val) method is for rank two ArrayChar objects.
    ArrayChar ac = new ArrayChar.D2(shape[0], shape[1]);
    Index ima = ac.getIndex();
    ac.setString(ima.set(0), someStringValue);
    ac.setString(ima.set(1), anotherStringValue);
    ac.setString(ima.set(2), aThirdStringValue);

    // 2) Write the data
    try {
      writer.write(v, ac);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
  }

  /**
   * Tutorial code snippet to write a scalar
   * 
   * @param writer
   * @param varName
   * @param val
   */
  public static void writeScalarData(NetcdfFormatWriter writer, String varName, double val) {
    Variable v = writer.findVariable("scalar");

    // 1) Working with type and rank specific Array objects provides convenient set() methods.
    // Here, we have a rank-0 (scalar) double Array, whose set() methods sets the scalar value.
    ArrayDouble.D0 datas = new ArrayDouble.D0();
    datas.set(val);

    // 2) Write the data
    try {
      writer.write(v, datas);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
  }

  /**
   * Tutorial code snippet to write data, one at a time, along a record dimension
   * 
   * @param pathAndFilenameStr
   * @return NetcdfFormatWriter
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static NetcdfFormatWriter writeRecordOneAtATime(String pathAndFilenameStr)
      throws IOException, InvalidRangeException {
    // 1) Create a new netCDF-3 file builder with the given path and file name
    NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(pathAndFilenameStr);

    // 2) Define the dimensions, variables, and attributes.
    // Note the use of NetcdfFileWriter.addUnlimitedDimension() to add a record dimension.
    Dimension latDim = builder.addDimension("lat", 3);
    Dimension lonDim = builder.addDimension("lon", 4);
    Dimension timeDim = builder.addUnlimitedDimension("time");

    // 3) Define Variables
    builder.addVariable("lat", DataType.FLOAT, "lat")
        .addAttribute(new Attribute("units", "degrees_north"));
    builder.addVariable("lon", DataType.FLOAT, "lon")
        .addAttribute(new Attribute("units", "degrees_east"));
    builder.addVariable("rh", DataType.INT, "time lat lon")
        .addAttribute(new Attribute("long_name", "relative humidity"))
        .addAttribute(new Attribute("units", "percent"));
    builder.addVariable("T", DataType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute("long_name", "surface temperature"))
        .addAttribute(new Attribute("units", "degC"));
    builder.addVariable("time", DataType.INT, "time")
        .addAttribute(new Attribute("units", "hours since 1990-01-01"));

    // 4) Create the file
    try (NetcdfFormatWriter writer = builder.build()) {
      // 5) Write the non-record Variables
      writer.write("lat", Array.makeFromJavaArray(new float[] {41, 40, 39}, false));
      writer.write("lon", Array.makeFromJavaArray(new float[] {-109, -107, -105, -103}, false));

      // 6) Write the record Variables (unlimited dimensions)
      // Create the arrays to hold the data.
      // Note that the outer dimension has shape of 1, since we will write only one record at a time.
      ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength(), true);
      ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      Array timeData = Array.factory(DataType.INT, new int[] {1});
      Index ima = rhData.getIndex();

      int[] origin = new int[] {0, 0, 0};
      int[] time_origin = new int[] {0};

      // 7) Loop over the unlimited (record) dimension. Each loop will write one record.
      for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
        // 8) Set the data for this record, using three different ways to fill the data arrays.
        // In all cases the first dimension has index = 0.

        // 8.1) Array.setInt(Index ima, int value) : timeData.getIndex() returns an Index initialized to zero.
        timeData.setInt(timeData.getIndex(), timeIdx * 12);

        for (int latIdx = 0; latIdx < latDim.getLength(); latIdx++) {
          for (int lonIdx = 0; lonIdx < lonDim.getLength(); lonIdx++) {
            // 8.2) Array.setInt(Index ima, int value) : ima.set(0, lat, lon) explicitly sets the dimension indices
            rhData.setInt(ima.set(0, latIdx, lonIdx), timeIdx * latIdx * lonIdx);
            // 8.3) ArrayDouble.D3.set(int i, int j, int k, double value):
            // by using a type and rank specific Array class (ArrayDouble.D3), we don’t need to use an Index.
            tempData.set(0, latIdx, lonIdx, timeIdx * latIdx * lonIdx / 3.14159);
          }
        }

        // 9) Set the origin to the current record number. The other dimensions have origin 0.
        time_origin[0] = timeIdx;
        origin[0] = timeIdx;
        // 10) Write the data at the specified origin.
        writer.write("rh", origin, rhData);
        writer.write("T", origin, tempData);
        writer.write("time", time_origin, timeData);
      }
      return writer; /* DOCS-IGNORE */
    } catch (IOException e) {
      logger.log(yourCreateNetcdfFileErrorMsgTxt);
    }
    return null; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to write a new netCDF-4 file with compression and chunking
   * 
   * @param inFile
   * @param outFilePath
   * @param strategyType
   * @param dl
   * @param shfl
   * @param nc4format
   * @return created netCDF-4 file
   */

  public static void writeWithCompression(NetcdfFile inFile, String outFilePath,
      Nc4Chunking.Strategy strategyType, int dl, boolean shfl, NetcdfFileFormat nc4format) {
    // 1) Create an Nc4Chunking object
    Nc4Chunking.Strategy type = strategyType;
    int deflateLevel = dl;
    boolean shuffle = shfl;
    Nc4Chunking chunker = Nc4ChunkingStrategy.factory(type, deflateLevel, shuffle);

    // 3) Create a new netCDF-4 file builder with the given path and file name and Nc4Chunking object
    NetcdfFileFormat format = nc4format;
    NetcdfFormatWriter.Builder builder =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, outFilePath, chunker);

    // 4) Create a NetcdfCopier and pass it the opened file and NetcdfFormatWriter.Builder
    NetcdfCopier copier = NetcdfCopier.create(inFile, builder);

    try {
      // 5) Write new file
      copier.write(null);
      // do stuff with newly create chunked and compressed file
    } catch (IOException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
  }
}
