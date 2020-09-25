package examples;

import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.internal.util.DiskCache;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.*;

public class ReadingCdmTutorial {
  // logs error/info message in memory and can be accessed from test functions
  public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

  ////////////////////////////
  // constant string messages for tests
  public static final String yourOpenNetCdfFileErrorMsgTxt = "exception while opening Netcdf file";
  public static final String yourReadVarErrorMsgTxt = "invalid Range for variable";

  ////////////////////////////
  // Netcdf tutorial functions
  // NOTE: these functions are used in the Netcdf tutorial docs, so formatting matters!

  /**
   * Tutorial code snippet to open a netcdf file and log exceptions
   * @param pathToYourFileAsStr: relative path to locally stored file
   */
  public static void openNCFile(String pathToYourFileAsStr) {
    try (NetcdfFile ncfile = NetcdfFiles.open(pathToYourFileAsStr)) {
      // Do cool stuff here
    } catch (IOException ioe) {
      // Handle less-cool exceptions here
      logger.log(yourOpenNetCdfFileErrorMsgTxt, ioe);
    }
  }

  /**
   * Tutorial code snippet equivalent to NCDump Data in ToolsUI
   * @param ncfile: pointer to an open NetcdfFile
   * @param varName: name of a variable to be read
   * @param sectionSpec: range of data to be read
   */
  public static void toolsUIDataDump(NetcdfFile ncfile, String varName, String sectionSpec) {
    // varName is a string with the name of a variable, e.g. "T"
    Variable v = ncfile.findVariable(varName);
    if (v == null) return;
    try {
      // sectionSpec is string specifying a range of data, eg ":,1:2,0:3"
      Array data = v.read(sectionSpec);
      String arrayStr = Ncdump.printArray(data, varName, null);
      logger.log(arrayStr);
    } catch (IOException|InvalidRangeException e) {
      logger.log(yourReadVarErrorMsgTxt, e);
    }
  }

  /**
   * Tutorial code snippet to read all data from a variable
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   */
  public static Array readAllVarData(Variable v) throws IOException {
    Array data = v.read();
    return data; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read subset of data by origin and size
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByOriginAndSize(Variable v) throws IOException, InvalidRangeException {
    int[] origin = new int[] {2, 0, 0};
    int[] size = new int[] {1, 3, 4};
    Array data3D = v.read(origin, size);
    // remove dimensions of length 1
    Array data2D = data3D.reduce();
    return data2D; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read data in a loop
   * @param v: variable to be read
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static void readInLoop(Variable v) throws IOException, InvalidRangeException {
    int[] varShape = v.getShape();
    int[] origin = new int[3];

    int[] size = new int[] {1, varShape[1], varShape[2]};
    // read each time step, one at a time
    for (int i = 0; i < varShape[0]; i++) {
      origin[0] = i;
      Array data2D = v.read(origin, size).reduce(0);
      logger.log(Ncdump.printArray(data2D, "T", null));
    }
  }

  /**
   * Tutorial code snippet to read a subset of data by indices
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readSubset(Variable v) throws IOException, InvalidRangeException {
    Array data = v.read("2,0:2,1:3");
    return data; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read a subset of data with strided indices
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByStride(Variable v) throws IOException, InvalidRangeException {
    Array data = v.read("2,0:2,0:3:2");
    return data; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read a subset using Ranges
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByRange(Variable v) throws IOException, InvalidRangeException {
    List ranges = new ArrayList();
    // List of Ranges equivalent to ("2,0:2,0:3:2")
    ranges.add(new Range(2,2));
    ranges.add(new Range(0,2));
    ranges.add(new Range(0,3,2));
    Array data = v.read(ranges);
    return data; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read a subset of using a loop and Ranges
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static void readInLoopRanges(Variable v) throws IOException, InvalidRangeException {
    // get variable shape
    int[] varShape = v.getShape();
    List ranges = new ArrayList();
    ranges.add(null);
    ranges.add(new Range(0, varShape[1]-1, 2));
    ranges.add(new Range(0, varShape[2]-1, 2));

    // loop time steps
    for (int i = 0; i < varShape[0]; i++) {
      ranges.set(0, new Range(i, i));
      Array data2D = v.read(ranges).reduce(0);
      logger.log(Ncdump.printArray(data2D, "T", null));
    }
  }

  /**
   * Tutorial code snippet to convert Range to origin and size
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static List<int[]> convertRangesToSection(Variable v, List<Range> ranges) throws IOException, InvalidRangeException {
    // a builder is used to create or modify a section
    Section.Builder builder = new Section.Builder();
    builder.appendRanges(ranges);
    Section section = builder.build();

    // convert section to equivalent origin, size arrays
    int[] origins = section.getOrigin();
    int[] shape = section.getShape();
    return Arrays.asList(origins, shape); /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read numeric scalar
   * @param v: variable to be read
   * @return array scalar values as double, float, and int types
   * @throws IOException
   */
  public static List<Object> readScalars(Variable v) throws IOException {
    double dval = v.readScalarDouble();
    float fval = v.readScalarFloat();
    int ival = v.readScalarInt();
    return Arrays.asList(dval, fval, ival); /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to read string scalar
   * @param v: variable to be read
   * @return string variable
   * @throws IOException
   */
  public static String readStringScalar(Variable v) throws IOException {
    String sval = v.readScalarString();
    return sval; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to iterate data of known rank
   * @param v: variable to be read
   * @return list of iterated values
   * @throws IOException
   */
  public static List iterateForLoop(Variable v) throws IOException {
    Array data = v.read();
    List list = new ArrayList<Double>(); /*DOCS-IGNORE*/

    int[] shape = data.getShape();
    Index index = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          double dval = data.getDouble(index.set(i, j, k));
          list.add(dval);  /*DOCS-IGNORE*/
        }
      }
    }
    return list;  /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to iterate data of any rank
   * @param v: variable to be read
   * @return sum of iterated data
   * @throws IOException
   */
  public static double indexIterator(Variable v) throws IOException, InvalidRangeException {
    Array data = v.read();
    double sum = 0.0;

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      sum += ii.getDoubleNext();
    }
    return sum; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to iterate data using a list of ranges
   * @param v: variable to be read
   * @return sum of iterated data
   * @throws IOException
   */
  public static double rangeIterator(Variable v) throws IOException, InvalidRangeException {
    Array data = v.read();
    int[] dataShape = data.getShape();
    List ranges = new ArrayList();
    for (int i = 0; i < dataShape.length; i++) {
      ranges.add(new Range(0, dataShape[i] - 1, 5));
    }

    double sum = 0.0;
    IndexIterator ii = data.getRangeIterator(ranges);
    while (ii.hasNext()) {
      sum += ii.getDoubleNext();
    }
    return sum; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to cast an array to subclass
   * @param v: variable to be read
   * @return list of iterated data
   * @throws IOException
   */
  public static List castDataArray(Variable v) throws IOException {
    ArrayDouble.D3 data = (ArrayDouble.D3) v.read();
    List list = new ArrayList<Double>(); /*DOCS-IGNORE*/

    int[] shape = data.getShape();
    Index index = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          double dval = data.get(i, j, k);
          list.add(dval);  /*DOCS-IGNORE*/
        }
      }
    }
    return list;  /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet reorder Array indices
   * @param data
   * @param origin
   * @param shape
   * @param stride
   * @throws InvalidRangeException
   */
  public static void indexManipulation(Array data, int[] origin, int[] shape, int[] stride) throws InvalidRangeException {
    // public Array flip(int dim);
    Array reversedData = data.flip(0); // reverse time dimension
    // public Array permute(int[] dims);
    Array permutedData = data.permute(new int[]{1, 2, 0});  // time becomes last dimension
    // public Array section(int[] origin, int[] shape, int[] stride);
    Array data1D = data.section(origin, shape, stride); // 1D array of measurements at first lat/lon
    // public Array sectionNoReduce(int[] origin, int[] , int[] stride);
    Array data3D = data.sectionNoReduce(origin, shape, stride); // 3D array of measurements
    // public Array reduce();
    Array reducedData = data3D.reduce(); // same as data1D
    // public Array slice(int dim, int val);
    Array slideData = data.slice(0, 10); // 2D array of measurements for 10th time step
    // public Array transpose( int dim1, int dim2);
    Array transposedData = data.transpose(1, 2); // transpose lat and lon dimensions
  }

  /**
   * Tutorial code snippet to get backing array of data
   * @param data
   * @return 1D java array
   */
  public static double[] get1DArray(Array data) {
    double[] javaArray = (double[]) data.get1DJavaArray(DataType.DOUBLE);
    return javaArray; /*DOCS-IGNORE*/
  }

  /**
   * Tutorial code snippet to periodically clear cache
   */
  public static void scourCache() {
    // 1) Get the current time and add 30 minutes to it
    Calendar c = Calendar.getInstance(); // contains current startup time
    c.add( Calendar.MINUTE, 30); // add 30 minutes to current time

    // 2) Make a class that extends TimerTask; the run method is called by the Timer
    class CacheScourTask extends java.util.TimerTask {
      public void run() {
        StringBuilder sbuff = new StringBuilder();
        // 3) Scour the cache, allowing 100 Mbytes of space to be used
        DiskCache.cleanCache(100 * 1000 * 1000, sbuff);
        sbuff.append("----------------------\n");
        // 4) Optionally log a message with the results of the scour.
        logger.log(sbuff.toString());
      }
    }

    // 5) Start up a timer that executes the cache scour task every 60 minutes, starting in 30 minutes
    java.util.Timer timer = new Timer();
    timer.scheduleAtFixedRate( new CacheScourTask(), c.getTime(), (long) 1000 * 60 * 60 );

    // 6) Make sure you cancel the time before you application exits, or else the process will not terminate.
    timer.cancel();
  }
}
