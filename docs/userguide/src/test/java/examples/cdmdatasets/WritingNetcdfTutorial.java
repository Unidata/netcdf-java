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
    double[] a = new double[shape[0] * shape[1]];
    for (int i = 0; i < a.length; i++) {
      a[i] = (i * 1000000);
    }

    // 2) Create an immutable Array<Double> from the primitive array
    Array<Double> A = Arrays.factory(ArrayType.DOUBLE, shape, a);

    // 3) Or create an evenly spaced Array of doubles
    // public Array<T> makeArray(ArrayType type, int npts, double start, double incr, int... shape)
    Array<Double> A2 = Arrays.makeArray(ArrayType.DOUBLE, 20, 0, 5, 4, 5);

    // 4) Write the data to the temperature Variable, with origin all zeros.
    // origin array is converted to an immutable Index with `Index.of`
    int[] origin = new int[2]; // initialized to zeros
    try {
      writer.write(v, Index.of(origin), A);
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

    // 1) Create an immutable Array<char>> from primitive strings
    Array<Character> ac = Arrays.factory(ArrayType.CHAR, new int[] {someStringValue.length()},
        someStringValue.toCharArray());

    // 2) Write the data. The origin parameter is initilized with zeros using the rank of the variable
    try {
      writer.write(v, Index.ofRank(v.getRank()), ac);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourWriteNetcdfFileErrorMsgTxt);
    }
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
