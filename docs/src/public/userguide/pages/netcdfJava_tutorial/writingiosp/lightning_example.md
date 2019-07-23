---
title: Writing an IOSP - Example
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar
permalink: lightning_example.html
toc: false
---

## Writing an IOSP for Netdf-Java (version 4+)

We will work on an example {% include link_file.html file="/netcdfJava_tutorial/writingiosp/lightningData.txt" text="lightning data test file" %}, which looks like:

~~~
USPLN-LIGHTNING,2006-10-23T18:01:00,2006-10-23T18:01:00
2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1
2006-10-23T17:59:40,5.4274766,-71.2189314,-31.7,1
2006-10-23T17:59:44,9.3568365,-76.8001513,-34.3,1
...
~~~

This is a text file, with variable length lines. We wont worry much about the nuances of the data, we just need to know that there are occasional header lines starting with USPLN-LIGHTNING, and a separate line for each lightning strike, with comma delimited fields. The fields are:

1. date of strike (GMT)
2. latitude
3. longitude
4. intensity
5. number of strokes

#### public boolean isValidFile(RandomAccessFile raf) throws IOException;

First we must identify our files. Its not foolproof, but we will assume that all our files start with the exact String <b>_USPLN-LIGHTNING_</b>.

~~~
  private static final String MAGIC = "USPLN-LIGHTNING";
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
1) raf.seek(0);
   int n = MAGIC.length();
   byte[] b = new byte[n];
2) raf.read(b);
   String got = new String(b);
3) return got.equals(MAGIC);
  }
~~~
  
1. Make sure we are at the start of the file, in general you wont be, since some other IOSP has also been reading from it.
2. Read in the exact number of bytes of the desired String.
3. Turn it into a String and require an exact match.

#### public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException;

In this method, we have to add all the Variable, Attributes and Dimensions to the empty NetcdfFile object. The Dimensions have to have the actual lengths in them, so we need to find out how many strike records there are. Since these are variable length records, we have no choice but to read through the entire file. So we start with creating a private method to do so. We will ignore the occasional header records, and place each strike into a Strike object:

~~~
 private int readAllData(RandomAccessFile raf) throws IOException, NumberFormatException, ParseException {
   ArrayList records = new ArrayList();
1) java.text.SimpleDateFormat isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
   isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
2) raf.seek(0);
   while (true) {
     String line = raf.readLine();
3)   if (line == null) break;
4)   if (line.startsWith(MAGIC)) continue;
5)   StringTokenizer stoker = new StringTokenizer( line, ",\r\n");
     while (stoker.hasMoreTokens()) {
6)     Date d = isoDateTimeFormat.parse(stoker.nextToken());
       double lat = Double.parseDouble(stoker.nextToken());
       double lon = Double.parseDouble(stoker.nextToken());
       double amp = Double.parseDouble(stoker.nextToken());
       int nstrikes = Integer.parseInt(stoker.nextToken());
7)     Strike s = new Strike(d, lat, lon, amp, nstrikes);
       records.add( s);
     }
   }
8) return records.size();
 }
 
 private class Strike {
   int d;
   double lat, lon, amp;
   int n;
   
   Strike( Date d, double lat, double lon, double amp, int n ) {
9)  this.d = (int) (d.getTime() / 1000);
    this.lat = lat;
    this.lon = lon;
    this.amp = amp;
    this.n = n;
   }
 }
~~~

1. This allows us to parse date Strings.
2. Make sure we are at the start of the file
3. Read one line at a time. When finished, we get a null return.
4. Skip the header lines.
5. A StringTokenizer will break the line up into tokens, using the "," character. It turns out that raf.readLine() leaves the line endings on, so by including them in here, they will be ignored by the StringTokenizer.
6. Get the comma-delimited tokens and parse them according to their data type.
7. Store them in a Strike object and keep a list of them.
8. return the number of records.
9. We are keeping the date as a number of seconds (more on this below).

#### Creating the Dimension, Variable, and Attribute objects

Now we can create the necessary objects:

~~~
 public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
1) int n;
   try {
    n = readAllData(raf);
   } catch (ParseException e) {
2)  throw new IOException("bad data");
   }

3) Dimension recordDim = new Dimension("record", n, true);
   ncfile.addDimension( null, recordDim);

4) Variable date = new Variable(ncfile, null, null, "date");
   date.setDimensions("record");
   date.setDataType(DataType.INT);
   String timeUnit = "seconds since 1970-01-01 00:00:00";
   date.addAttribute( new Attribute("long_name", "date of strike"));
   date.addAttribute( new Attribute("units", timeUnit));
   ncfile.addVariable( null, date);

5) Variable lat = new Variable(ncfile, null, null, "lat");
   lat.setDimensions("record");
   lat.setDataType(DataType.DOUBLE);
   lat.addAttribute( new Attribute("long_name", "latitude"));
   lat.addAttribute( new Attribute("units", "degrees_north"));
   ncfile.addVariable( null, lat);

   Variable lon = new Variable(ncfile, null, null, "lon");
   lon.setDimensions("record");
   lon.setDataType(DataType.DOUBLE);
   lon.addAttribute( new Attribute("long_name", "longitude"));
   lon.addAttribute( new Attribute("units", "degrees_east"));
   ncfile.addVariable( null, lon);

   Variable amp = new Variable(ncfile, null, null, "strikeAmplitude");
   amp.setDimensions("record");
   amp.setDataType(DataType.DOUBLE);
   amp.addAttribute( new Attribute("long_name", "amplitude of strike"));
   amp.addAttribute( new Attribute("units", "kAmps"));
   amp.addAttribute( new Attribute("missing_value", new Double(999)));
   ncfile.addVariable( null, amp);

   Variable nstrokes = new Variable(ncfile, null, null, "strokeCount");
   nstrokes.setDimensions("record");
   nstrokes.setDataType(DataType.INT);
   nstrokes.addAttribute( new Attribute("long_name", "number of strokes per flash"));
   nstrokes.addAttribute( new Attribute("units", ""));
   ncfile.addVariable( null, nstrokes);
6) ncfile.addAttribute(null, new Attribute("title", "USPN Lightning Data"));
   ncfile.addAttribute(null, new Attribute("history","Read directly by Netcdf Java IOSP"));

7) ncfile.finish();
  }
~~~

1. Read through the data, find out how many records there are.
2. Not really a very robust way to handle this, it would maybe be better to discard individual malformed lines.
3. Create a <b>Dimension</b> named record, of length n. Add it to the file.
4. Create a Variable named date. It has the single dimension named record. To be udunits compatible, we have decided to encode it as seconds since 1970-01-01 00:00:00, which we set as the units. We make it an integer data type.
5. Similarly we go through and add the other Variables, adding units and long_name attributes, etc.
6. Add a few global attrributes. On a real IOSP, we would try to make this much more complete.
7. Always call <b>ncfile.finish()</b> after adding or modifying the structural metadata.

#### Reading the data

At this point we need to figure out how to implement the read() methods. Since we have no Structures, we can ignore <b>readNestedData()</b>. Of course, you are probably saying "we already read the data, are we just going to throw it away?". So for now, lets suppose that we have decided that these are always small enough files that we can safely read the entire data into memory. This allows us to create the data arrays during the open and cache them in the Variable. The additional code looks like:

~~~
1)private  ArrayInt.D1 dateArray;
  private  ArrayDouble.D1 latArray;
  private  ArrayDouble.D1 lonArray;
  private  ArrayDouble.D1 ampArray;
  private  ArrayInt.D1 nstrokesArray;
  
  private int readAllData(RandomAccessFile raf) throws IOException, NumberFormatException, ParseException {
   ArrayList records = new ArrayList();
   // Creating the Strike records same as above ....
   int n = records.size();
   int[] shape = new int[] {n};
2) dateArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);
   latArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
   lonArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
   ampArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
   nstrokesArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);
   
3) for (int i = 0; i < records.size(); i++) {
    Strike strike = (Strike) records.get(i);
    dateArray.set(i, strike.d);
    latArray.set(i, strike.lat);
    lonArray.set(i, strike.lon);
    ampArray.set(i, strike.amp);
    nstrokesArray.set(i, strike.n);
   }
4) return n;
 }
~~~

1. Create some instance fields to hold the data, one for each netCDF <b>Variable</b> object.
2. Once we know how many records there are, we create a 1D Array of that length. For convenience we cast them to the rank and type specific Array subclass.
3. Loop through all the records and transfer the data into the corresponding Arrays.
4. Once we return from this method, the ArrayList of records, and the Strike objects themselves are no longer used anywhere, so they will get garbaged collected. So we dont have the data taking twice as much space as needed.

#### Then back in the open method, we make the following change on each Variable:
~~~
   Variable date = new Variable(ncfile, null, null, "date");
   // ...
 date.setCachedData(dateArray, false);

   Variable lat = new Variable(ncfile, null, null, "lat");
   // .. 
 lat.setCachedData(latArray, false);

   // do this for all variables
~~~
   
<b>Variable.setCachedData</b> sets the data array for that Variable. It must be the complete data array for the Variable, with the correct type and shape. Having set this, the read() method will never be called for that Variable, it will always be satisfied from the cached data Array. If all Variables have cached data, then the read() method will never be called, and so we dont need to implement it.

#### Adding Coordinate Systems and Typed Dataset information

An an IOServiceProvider implementer, you presumably know everything there is to know about this data file. If you want your data file to be understood by the higher layers of the CDM, you should also add the Coordinate System and Typed Dataset information that is needed. To do so, you need to understand the Conventions used by these layers. In this case, we have Point data, so we are going to use Unidata's _Coordinate Conventions and Unidata's Point Observation Conventions which requires us to add certain attributes. The payoff is that we can then look at our data through the Point tab of the ToolsUI.

The additional code in the <b>open()</b> method looks like this :

~~~
   Variable date = new Variable(ncfile, null, null, "date");
   date.setDimensions("record");
   date.setDataType(DataType.INT);
   String timeUnit = "seconds since 1970-01-01 00:00:00";
   date.addAttribute( new Attribute("long_name", "date of strike"));
   date.addAttribute( new Attribute("units", timeUnit));
1) date.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
   date.setCachedData(dateArray, false);
   ncfile.addVariable( null, date);
~~~

~~~   
   Variable lat = new Variable(ncfile, null, null, "lat");
   lat.setDimensions("record");
   lat.setDataType(DataType.DOUBLE);
   lat.addAttribute( new Attribute("long_name", "latitude"));
   lat.addAttribute( new Attribute("units", "degrees_north"));
1) lat.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
   lat.setCachedData(latArray, false);
   ncfile.addVariable( null, lat);
~~~

~~~   
   Variable lon = new Variable(ncfile, null, null, "lon");
   lon.setDimensions("record");
   lon.setDataType(DataType.DOUBLE);
   lon.addAttribute( new Attribute("long_name", "longitude"));
   lon.addAttribute( new Attribute("units", "degrees_east"));
1) lon.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
   lon.setCachedData(lonArray, false);
   ncfile.addVariable( null, lon);
~~~

~~~   
   Variable amp = new Variable(ncfile, null, null, "strikeAmplitude");
   amp.setDimensions("record");
   amp.setDataType(DataType.DOUBLE);
   amp.addAttribute( new Attribute("long_name", "amplitude of strike"));
   amp.addAttribute( new Attribute("units", "kAmps"));
   amp.addAttribute( new Attribute("missing_value", new Double(999)));
   amp.setCachedData(ampArray, false);
   ncfile.addVariable( null, amp);
~~~

~~~   
   Variable nstrokes = new Variable(ncfile, null, null, "strokeCount");
   nstrokes.setDimensions("record");
   nstrokes.setDataType(DataType.INT);
   nstrokes.addAttribute( new Attribute("long_name", "number of strokes per flash"));
   nstrokes.addAttribute( new Attribute("units", ""));
   nstrokes.setCachedData(nstrokesArray, false);
   ncfile.addVariable( null, nstrokes);
~~~

~~~   
   ncfile.addAttribute(null, new Attribute("title", "USPN Lightning Data"));
   ncfile.addAttribute(null, new Attribute("history","Read directly by Netcdf Java IOSP"));
~~~

~~~   
2) ncfile.addAttribute(null, new Attribute("Conventions","Unidata Observation Dataset v1.0"));
  ncfile.addAttribute(null, new Attribute("cdm_data_type","Point"));
  ncfile.addAttribute(null, new Attribute("observationDimension","record"));
~~~

~~~  
3) MAMath.MinMax mm = MAMath.getMinMax(dateArray);
  ncfile.addAttribute(null, new Attribute("time_coverage_start", ((int)mm.min) +" "+timeUnit));
  ncfile.addAttribute(null, new Attribute("time_coverage_end", ((int)mm.max) +" "+timeUnit));
~~~

~~~  
3) mm = MAMath.getMinMax(latArray);
  ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Double(mm.min)));
  ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Double(mm.max)));
~~~

~~~  
3) mm = MAMath.getMinMax(lonArray);
  ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Double(mm.min)));
  ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Double(mm.max)));
~~~

~~~
   ncfile.finish();
~~~

1. We add three attributes on the time, lat, and lon variables that identify them as coordinate axes of the appropriate type.
2. We add some global attributes identifying the Convention, the datatype, and which dimension to use to find the observations.
3. The Point data type also requires that the time range and lat/lon bounding box be specified as shown in global attributes.

We now have not only a working IOSP, but a PointObsDataset that can be displayed and georeferenced! Working source code for this example is {% include link_file.html file="/netcdfJava_tutorial/writingiosp/UspLightning1.java" text="here" %}. Modify the main program to point to the {% include link_file.html file="/netcdfJava_tutorial/writingiosp/lightningData.txt" text="data file" %}, and try running it. Note that you need to load your [class at runtime ](runtime_loading.html), for example by calling:

~~~
   NetcdfFile.registerIOProvider(UspLightning.class);
~~~