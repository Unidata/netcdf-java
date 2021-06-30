package examples.cdmdatasets;

import ucar.array.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.internal.util.DiskCache;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.*;
import java.util.Arrays;

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
   * 
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
   * 
   * @param ncfile: pointer to an open NetcdfFile
   * @param varName: name of a variable to be read
   * @param sectionSpec: range of data to be read
   */
  public static void toolsUIDataDump(NetcdfFile ncfile, String varName, String sectionSpec) {
    // varName is a string with the name of a variable, e.g. "T"
    Variable v = ncfile.findVariable(varName);
    if (v == null)
      return;
    try {
      // sectionSpec is string specifying a range of data, eg ":,1:2,0:3"
      Array data = v.readArray(new Section(sectionSpec));
      String arrayStr = NcdumpArray.printArray(data, varName, null);
      logger.log(arrayStr);
    } catch (IOException | InvalidRangeException e) {
      logger.log(yourReadVarErrorMsgTxt, e);
    }
  }

  /**
   * Tutorial code snippet to read all data from a variable
   * 
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   */
  public static Array readAllVarData(Variable v) throws IOException {
    Array data = v.readArray();
    return data; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read subset of data by origin and size
   * 
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByOriginAndSize(Variable v) throws IOException, InvalidRangeException {
    int[] origin = new int[] {2, 0, 0};
    int[] size = new int[] {1, 3, 4};
    Array data = v.readArray(new Section(origin, size));
    return data; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read data in a loop
   * 
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
      Array data = v.readArray(new Section(origin, size));
      logger.log(NcdumpArray.printArray(data, "T", null));
    }
  }

  /**
   * Tutorial code snippet to read a subset of data by indices
   * 
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readSubset(Variable v) throws IOException, InvalidRangeException {
    Array data = v.readArray(new Section("2,0:2,1:3"));
    return data; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read a subset of data with strided indices
   * 
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByStride(Variable v) throws IOException, InvalidRangeException {
    Array data = v.readArray(new Section("2,0:2,0:3:2"));
    return data; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read a subset using Ranges
   * 
   * @param v: variable to be read
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readByRange(Variable v) throws IOException, InvalidRangeException {
    List ranges = new ArrayList();
    // List of Ranges equivalent to ("2,0:2,0:3:2")
    ranges.add(new Range(2, 2));
    ranges.add(new Range(0, 2));
    ranges.add(new Range(0, 3, 2));
    Array data = v.readArray(new Section(ranges));
    return data; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read a subset of using a loop and Ranges
   * 
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
    ranges.add(new Range(0, varShape[1] - 1, 2));
    ranges.add(new Range(0, varShape[2] - 1, 2));

    // loop time steps
    for (int i = 0; i < varShape[0]; i++) {
      ranges.set(0, new Range(i, i));
      Array data = v.readArray(new Section(ranges));
      logger.log(NcdumpArray.printArray(data, "T", null));
    }
  }

  /**
   * Tutorial code snippet to convert Range to origin and size
   *
   * @return array of read data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static List<int[]> convertRangesToSection(List<Range> rangeList) {
    Section section = new Section(rangeList);

    // convert section to equivalent origin, size arrays
    int[] origins = section.getOrigin();
    int[] shape = section.getShape();
    return Arrays.asList(origins, shape); /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to read numeric scalar
   *
   * @param intVar
   * @param doubleVar
   * @return array scalar values as double, float, and int types
   * @throws IOException
   */
  public static List<Object> readScalars(Variable intVar, Variable doubleVar) throws IOException {
    int ival = ((Array<Integer>) intVar.readArray()).getScalar();
    double dval = ((Array<Double>) doubleVar.readArray()).getScalar();
    return Arrays.asList(ival, dval); /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to iterate data of known rank
   * 
   * @param v: variable to be read
   * @return list of iterated values
   * @throws IOException
   */
  public static List iterateForLoop(Variable v) throws IOException {
    Array data = v.readArray();
    List list = new ArrayList<Double>(); /* DOCS-IGNORE */

    int[] shape = data.getShape();
    Index index = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          double dval = (double) data.get(index.set(i, j, k));
          list.add(dval); /* DOCS-IGNORE */
        }
      }
    }
    return list; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to iterate data of any rank
   * 
   * @param v: variable to be read
   * @return sum of iterated data
   * @throws IOException
   */
  public static double dataIterator(Variable v) throws IOException, InvalidRangeException {
    Array data = v.readArray();
    double sum = 0.0;

    Iterator ii = data.iterator();
    while (ii.hasNext()) {
      sum += (double) ii.next();
    }
    return sum; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to cast an array to subclass
   * 
   * @param v: variable to be read
   * @return list of iterated data
   * @throws IOException
   */
  public static List castDataArray(Variable v) throws IOException {
    Array<Double> data = (Array<Double>) v.readArray();
    List list = new ArrayList<Double>(); /* DOCS-IGNORE */

    int[] shape = data.getShape();
    Index index = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          double dval = data.get(i, j, k);
          list.add(dval); /* DOCS-IGNORE */
        }
      }
    }
    return list; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to copy array data
   */
  public static double[] copyData(Variable v) throws IOException {
    Array<Double> dataSrc = (Array<Double>) v.readArray(); // data to be copied
    double[] dest = (double[]) ucar.array.Arrays.copyPrimitiveArray(dataSrc);
    // do something with copied data here
    // ...
    return dest; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet to periodically clear cache
   */
  public static void scourCache() {
    // 1) Get the current time and add 30 minutes to it
    Calendar c = Calendar.getInstance(); // contains current startup time
    c.add(Calendar.MINUTE, 30); // add 30 minutes to current time

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
    timer.scheduleAtFixedRate(new CacheScourTask(), c.getTime(), (long) 1000 * 60 * 60);

    // 6) Make sure you cancel the time before you application exits, or else the process will not terminate.
    timer.cancel();
  }
}
