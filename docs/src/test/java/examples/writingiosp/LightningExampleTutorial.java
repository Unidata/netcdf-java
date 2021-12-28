package examples.writingiosp;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import java.text.ParseException;

import java.io.IOException;
import ucar.unidata.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

public class LightningExampleTutorial {

  private static final String token = "USPLN-LIGHTNING";

  // state vars
  public static ArrayInt.D1 dateArray;
  public static ArrayDouble.D1 latArray;
  public static ArrayDouble.D1 lonArray;
  public static ArrayDouble.D1 ampArray;
  public static ArrayInt.D1 nstrokesArray;

  /**
   * Code snippet for creating an IOSP
   */
  public static AbstractIOServiceProvider getIOSP() {
    /* INSERT public */class UspLightning extends AbstractIOServiceProvider {

      public boolean isValidFile(RandomAccessFile raf) throws IOException {
        // TO BE IMPLEMENTED
        return false; /* DOCS-IGNORE */
      }

      public boolean isBuilder() {
        return true;
      }

      public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask)
          throws IOException {
        // TO BE IMPLEMENTED
      }

      public Array readData(Variable v2, Section section)
          throws IOException, InvalidRangeException {
        // NOT IMPLEMENTED IN THIS EXAMPLE
        return null;
      }

      public String getFileTypeId() {
        return "USPLN-LIGHTNING";
      }

      public String getFileTypeDescription() {
        return "Data from lightning data test file";
      }
    }
    return new UspLightning(); /* DOCS-IGNORE */
  }

  /**
   * Code snippet to implement isValidFile in an IOSP
   * 
   * @param raf
   * @return
   * @throws IOException
   */
  public static boolean implementIsValidFile(RandomAccessFile raf) throws IOException {
    String token = "USPLN-LIGHTNING";
    // 1) Make sure you are at the start of the file. In general, we won't be, since some other IOSP has also been
    // reading from it.
    raf.seek(0);
    // 2) Read in the exact number of bytes of the desired String
    int n = token.length();
    byte[] b = new byte[n];
    raf.read(b);
    // 3) Turn it into a String and require an exact match.
    String got = new String(b);
    return got.equals(token);
  }

  /**
   * Code snippet to read all data in a file
   * 
   * @param raf
   * @return
   * @throws IOException
   * @throws NumberFormatException
   * @throws ParseException
   */
  public static int readALlData(RandomAccessFile raf)
      throws IOException, NumberFormatException, ParseException {
    class nested { /* DOCS-IGNORE */
      private int readAllData(RandomAccessFile raf)
          throws IOException, NumberFormatException, ParseException {
        ArrayList records = new ArrayList();
        // 1) This allows us to parse date Strings.
        java.text.SimpleDateFormat isoDateTimeFormat =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        // 2) Make sure we are at the start of the file.
        raf.seek(0);
        while (true) {
          String line = raf.readLine();
          // 3) Read one line at a time. When finished, we get a null return.
          if (line == null)
            break;
          // 4) Skip the header lines
          if (line.startsWith(token))
            continue;
          // 5) A StringTokenizer will break the line up into tokens, using the "," character.
          // It turns out that raf.readLine() leave the line endings on, so by including them in here,
          // they will be ignored by the StringTokenizer.
          StringTokenizer stoker = new StringTokenizer(line, ",\r\n");
          while (stoker.hasMoreTokens()) {
            // 6) Get the comma-delimited tokens and parse them according to their data type.
            Date d = isoDateTimeFormat.parse(stoker.nextToken());
            double lat = Double.parseDouble(stoker.nextToken());
            double lon = Double.parseDouble(stoker.nextToken());
            double amp = Double.parseDouble(stoker.nextToken());
            int nstrikes = Integer.parseInt(stoker.nextToken());
            // 7) Store them in a Strike object and keep a list of them.
            Strike s = new Strike(d, lat, lon, amp, nstrikes);
            records.add(s);
          }
        }
        // 8) Return the number of records.
        return records.size();
      }

      /* INSERT private */class Strike {
        int d;
        double lat, lon, amp;
        int n;

        Strike(Date d, double lat, double lon, double amp, int n) {
          // 9) We are keeping the data as a number of seconds
          this.d = (int) (d.getTime() / 1000);
          this.lat = lat;
          this.lon = lon;
          this.amp = amp;
          this.n = n;
        }
      }
    };
    return new nested().readAllData(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet for implementing the open method
   * 
   * @param raf
   * @param rootGroup
   * @param cancelTask
   * @throws IOException
   */
  public static void implementBuild(RandomAccessFile raf, Group.Builder rootGroup,
      CancelTask cancelTask) throws IOException {
    // 1) Read through the data, find out how many records there are.
    int n;
    try {
      n = readALlData(raf);
    } catch (ParseException e) {
      // 2) Not really a very robust way to handle this;
      // it would maybe be better to discard individual malformed lines.
      throw new IOException("bad data");
    }

    // 3) Create a Dimension named record, or length n. Add it to the file.
    Dimension dim = Dimension.builder("record", n).build();
    rootGroup.addDimension(dim);

    // 4) Add a Variable named date. It has the single dimension named record.
    // To be udunits compatible, we have decided to encode it as seconds since 1970-01-01 00:00:00,
    // which we set as the units. We make it an integer data type.
    rootGroup.addVariable(Variable.builder().setName("date").setDataType(DataType.INT)
        .addDimension(dim).addAttribute(new Attribute("long_name", "date of strike"))
        .addAttribute(new Attribute("units", "seconds since 1970-01-01 00:00;00")));

    // 5) Similarly, we go through and add the other Variables, adding units and long_name attributes, etc.
    rootGroup.addVariable(Variable.builder().setName("lat").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "latitude"))
        .addAttribute(new Attribute("units", "degrees_north")));

    rootGroup.addVariable(Variable.builder().setName("lon").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "longitude"))
        .addAttribute(new Attribute("units", "degrees_east")));

    rootGroup.addVariable(Variable.builder().setName("strikeAmplitude").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "amplitude of strike"))
        .addAttribute(new Attribute("units", "kAmps"))
        .addAttribute(new Attribute("missing_value", new Double(999))));

    rootGroup.addVariable(Variable.builder().setName("strokeCount").setDataType(DataType.INT)
        .addDimension(dim).addAttribute(new Attribute("long_name", "number of strokes per flash"))
        .addAttribute(new Attribute("units", "")));

    // 7) Add a few global attributes. On a real IOSP, we would try to make this much more complete.
    rootGroup.addAttribute(new Attribute("title", "USPN Lightning Data"));
    rootGroup.addAttribute(new Attribute("history", "Read directly by Netcdf Java IOSP"));
  }

  /**
   * Code snippet to create data structures to hold read data
   */
  public static void createDataArrays() {
    /* INSERT private */ArrayInt.D1 dateArray;
    /* INSERT private */ArrayDouble.D1 latArray;
    /* INSERT private */ArrayDouble.D1 lonArray;
    /* INSERT private */ArrayDouble.D1 ampArray;
    /* INSERT private */ArrayInt.D1 nstrokesArray;
  }

  /**
   * Code snippet to implement reads for sample IOSP
   * 
   * @param raf
   * @return
   * @throws IOException
   * @throws NumberFormatException
   * @throws ParseException
   */
  public static int implementReadMethods(RandomAccessFile raf)
      throws IOException, NumberFormatException, ParseException {
    class nested { /* DOCS-IGNORE */
      private int readAllData(RandomAccessFile raf)
          throws IOException, NumberFormatException, ParseException {
        ArrayList records = new ArrayList();
        // ...
        // 1) Create the Strike records same as above ....
        // ...
        records = getRecords(raf, records); /* DOCS-IGNORE */
        int n = records.size();
        int[] shape = new int[] {n};
        // 2) Once we know how many records there are, we create a 1D Array of that length.
        // For convenience we cast them to the rank and type specific Array subclass.
        dateArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);
        latArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
        lonArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
        ampArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
        nstrokesArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);

        // 3) Loop through all the records and transfer the data into the corresponding Arrays.
        for (int i = 0; i < records.size(); i++) {
          Strike strike = (Strike) records.get(i);
          dateArray.set(i, strike.d);
          latArray.set(i, strike.lat);
          lonArray.set(i, strike.lon);
          ampArray.set(i, strike.amp);
          nstrokesArray.set(i, strike.n);
        }

        return n;
      }
    };
    return new nested().readAllData(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet to cache read Variables
   * 
   * @param rootGroup
   * @param dim
   */
  public static void setCachedData(Group.Builder rootGroup, Dimension dim, ArrayInt.D1 dateArray,
      ArrayDouble.D1 latArray) {
    // ...
    rootGroup.addVariable(Variable.builder().setName("date").setDataType(DataType.INT)
        .addDimension(dim).addAttribute(new Attribute("long_name", "date of strike"))
        .addAttribute(new Attribute("units", "seconds since 1970-01-01 00:00;00"))
        .setCachedData(dateArray, false));

    rootGroup.addVariable(Variable.builder().setName("lat").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "latitude"))
        .addAttribute(new Attribute("units", "degrees_north")).setCachedData(latArray, false));
    // ...
    // do this for all variables
  }

  /**
   * Code snippet to add coordinate systems and typed datasets
   * 
   * @param rootGroup
   * @param dim
   */
  public static void addCoordSystemsAndTypedDatasets(Group.Builder rootGroup, Dimension dim,
      ArrayInt.D1 dateArray, ArrayDouble.D1 latArray, ArrayDouble.D1 lonArray) {
    // ...
    // 1) Add attributes on time, lat, and lon variables that identify them as coordinate axes
    rootGroup.addVariable(Variable.builder().setName("date").setDataType(DataType.INT)
        .addDimension(dim).addAttribute(new Attribute("long_name", "date of strike"))
        .addAttribute(new Attribute("units", "seconds since 1970-01-01 00:00;00"))
        .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()))
        .setCachedData(dateArray, false));

    rootGroup.addVariable(Variable.builder().setName("lat").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "latitude"))
        .addAttribute(new Attribute("units", "degrees_north"))
        .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
        .setCachedData(latArray, false));

    rootGroup.addVariable(Variable.builder().setName("lon").setDataType(DataType.DOUBLE)
        .addDimension(dim).addAttribute(new Attribute("long_name", "longitude"))
        .addAttribute(new Attribute("units", "degrees_east"))
        .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
        .setCachedData(lonArray, false));
    // ...
    // 2) Add some global attributes identifying the Convention, the datatype,
    // and which dimension to use to find the observations
    rootGroup.addAttribute(new Attribute("Conventions", "Unidata Observation Dataset v1.0"));
    rootGroup.addAttribute(new Attribute("cdm_data_type", "Point"));
    rootGroup.addAttribute(new Attribute("observationDimension", "record"));

    // 3) The Point data type also requires that the time range and lat/lon bounding box be specified as shown
    // in global attributes.
    MAMath.MinMax mm = MAMath.getMinMax(dateArray);
    rootGroup.addAttribute(
        new Attribute("time_coverage_start", ((int) mm.min) + "seconds since 1970-01-01 00:00;00"));
    rootGroup.addAttribute(
        new Attribute("time_coverage_end", ((int) mm.max) + "seconds since 1970-01-01 00:00;00"));

    mm = MAMath.getMinMax(latArray);
    rootGroup.addAttribute(new Attribute("geospatial_lat_min", new Double(mm.min)));
    rootGroup.addAttribute(new Attribute("geospatial_lat_max", new Double(mm.max)));

    mm = MAMath.getMinMax(lonArray);
    rootGroup.addAttribute(new Attribute("geospatial_lon_min", new Double(mm.min)));
    rootGroup.addAttribute(new Attribute("geospatial_lon_max", new Double(mm.max)));
  }

  /**
   * Code snippet to register an IOSP
   * 
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static void registerIOSP()
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    NetcdfFiles.registerIOProvider("UspLightning");
  }

  /****************
   * helpers
   */

  // Strike class
  static class Strike {
    int d;
    double lat, lon, amp;
    int n;

    Strike(Date d, double lat, double lon, double amp, int n) {
      this.d = (int) (d.getTime() / 1000);
      this.lat = lat;
      this.lon = lon;
      this.amp = amp;
      this.n = n;
    }
  }

  // Helper for read implementations
  private static ArrayList getRecords(RandomAccessFile raf, ArrayList records)
      throws IOException, ParseException {
    java.text.SimpleDateFormat isoDateTimeFormat =
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    raf.seek(0);
    while (true) {
      String line = raf.readLine();
      if (line == null)
        break;
      if (line.startsWith(token))
        continue;
      StringTokenizer stoker = new StringTokenizer(line, ",\r\n");
      while (stoker.hasMoreTokens()) {
        Date d = isoDateTimeFormat.parse(stoker.nextToken());
        double lat = Double.parseDouble(stoker.nextToken());
        double lon = Double.parseDouble(stoker.nextToken());
        double amp = Double.parseDouble(stoker.nextToken());
        int nstrikes = Integer.parseInt(stoker.nextToken());
        Strike s = new Strike(d, lat, lon, amp, nstrikes);

        records.add(s);
      }
    }
    return records;
  }
}
