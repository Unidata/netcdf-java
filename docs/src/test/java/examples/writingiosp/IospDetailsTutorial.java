package examples.writingiosp;

import ucar.array.ArrayType;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.*;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public class IospDetailsTutorial {

  private static final String pattern1 = "pattern1";
  private static final String pattern2 = "pattern2";

  /**
   * Code snippet with first isValidFile example
   * 
   * @param raf
   * @throws IOException
   */
  public static void isValidExample1(RandomAccessFile raf) throws IOException {
    class nestedClass { /* DOCS-IGNORE */
      public boolean isValidFile(RandomAccessFile raf) throws IOException {
        // 1) Start reading at the first byte of the file
        raf.seek(0);
        // 2) Read 8 bytes and convert to String
        byte[] b = new byte[8];
        raf.read(b);
        String test = new String(b);
        // 3) Compare to known patterns
        return test.equals(pattern1) || test.equals(pattern2);
      }
    }
    new nestedClass().isValidFile(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet with second isValidFile example
   * 
   * @param raf
   * @throws IOException
   */
  public static void isValidExample2(RandomAccessFile raf) {
    class nestedClass { /* DOCS-IGNORE */
      public boolean isValidFile(RandomAccessFile raf) {
        try {
          // 1) The IOSP will read in numbers that it expects to be in big-endian format.
          // It must not assume what mode the RandomAccessFile is in.
          raf.order(RandomAccessFile.BIG_ENDIAN);
          raf.seek(0);
          // 2) It creates a BufrInput object and delegates the work to it.
          // Since this is a local instance, this is thread-safe.
          BufrInput bi = new BufrInput(raf);
          return bi.isValidFile();
          // 2) Catch the IOExceptions
        } catch (IOException ex) {
          return false;
        }
      }
    }
    new nestedClass().isValidFile(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet for a bad example of isValidFile
   * 
   * @param raf
   */
  public static void isValidExample3(RandomAccessFile raf) throws IOException {
    class nestedClass { /* DOCS-IGNORE */
      private Grib1Input scanner;
      private int edition;

      public boolean isValidFile(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.order(RandomAccessFile.BIG_ENDIAN);
        scanner = new Grib1Input(raf);
        edition = scanner.getEdition();
        return (edition == 1);
      }
    }
    new nestedClass().isValidFile(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet for improving bad example of isValidFile
   * 
   * @param raf
   */
  public static void isValidExample4(RandomAccessFile raf) throws IOException {
    class nestedClass { /* DOCS-IGNORE */
      private Grib1Input scanner;
      private int edition;

      public boolean isValidFile(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.order(RandomAccessFile.BIG_ENDIAN);
        Grib1Input scanner = new Grib1Input(raf);
        int edition = scanner.getEdition();
        return (edition == 1);
      }

      public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask)
          throws IOException {
        raf.seek(0);
        raf.order(RandomAccessFile.BIG_ENDIAN);
        scanner = new Grib1Input(raf);
        edition = scanner.getEdition();
        // ...
      }
    }
    new nestedClass().isValidFile(raf); /* DOCS-IGNORE */
  }

  /**
   * Code snippet to add global attributes to a file
   * 
   * @param rootGroup
   */
  public static void addGlobalAttribute(Group.Builder rootGroup) {
    rootGroup.addAttribute(new Attribute("Conventions", "CF-1.0"));
    rootGroup.addAttribute(new Attribute("version", 42));
  }

  /**
   * Code snippet to add attributes to a variable
   */
  public static void addVarAttribute() {
    Variable.Builder var = Variable.builder().setName("variable");
    var.addAttribute(Attribute.builder("missing_value").setDataType(DataType.DOUBLE)
        .setValues(Array.factory(DataType.DOUBLE, new int[] {1, 2}, new double[] {999.0, -999.0}))
        .build());
  }

  /**
   * Code snippet to add dimensions to a file
   * 
   * @param rootGroup
   */
  public static void addDimension(Group.Builder rootGroup) {
    rootGroup.addDimension(Dimension.builder("lat", 190).build());
    rootGroup.addDimension(Dimension.builder("lon", 360).build());
  }

  /**
   * Code snippet to add unsigned attribute to a cariables
   */
  public static void unsignedAttribute(Variable.Builder var) {
    var.addAttribute(new Attribute("_Unsigned", "true"));
  }

  /**
   * Code snippet to create a variable for elevation data
   * 
   * @param rootGroup
   */
  public static void createVariable(Group.Builder rootGroup) {
    rootGroup.addVariable(Variable.builder().setParentGroupBuilder(rootGroup).setName("elevation")
        .setArrayType(ArrayType.SHORT).setDimensionsByName("lat lon")
        .addAttribute(new Attribute("units", "m"))
        .addAttribute(
            new Attribute("long_name", "digital elevation in meters above mean sea level"))
        .addAttribute(new Attribute("missing_value", (short) -9999)));
  }

  /**
   * \
   * Code snippet to create a coordinate variable
   * 
   * @param rootGroup
   */
  public static void createCoordinateVariable(Group.Builder rootGroup) {
    Variable.Builder lat = Variable.builder().setParentGroupBuilder(rootGroup).setName("lat")
        .setArrayType(ArrayType.FLOAT).setDimensionsByName("lat")
        .addAttribute(new Attribute("units", "degrees_north"));
    rootGroup.addVariable(lat);
  }

  /**
   * Code snippet to add data to a variable
   * 
   * @param lat
   */
  public static void setVariableData(Variable.Builder lat) {
    ucar.array.Array data = new ucar.array.Array(ArrayType.FLOAT, )
            Array.makeArray(DataType.FLOAT, 180, 90.0, -1.0);
    lat.setSourceData(data);
  }

  /**
   * Code snippet to read an entire Array
   * 
   * @param raf
   * @param v2
   * @param wantSection
   * @return
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static ucar.array.Array readExample1(RandomAccessFile raf, Variable v2, Section wantSection)
      throws IOException, InvalidRangeException {
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    int size = (int) v2.getSize();
    short[] arr = new short[size];

    int count = 0;
    while (count < size)
      arr[count++] = raf.readShort(); // copy into primitive array

    Array data = Array.factory(DataType.SHORT, v2.getShape(), arr);
    return data.section(wantSection.getRanges());
  }

  /**
   * Code snippet to read data using LayoutRegular helper
   * 
   * @param raf
   * @param v2
   * @param wantSection
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static Array readExample2(RandomAccessFile raf, Variable v2, Section wantSection)
      throws IOException, InvalidRangeException {
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    int size = (int) v2.getSize();
    int[] arr = new int[size];

    LayoutRegular layout = new LayoutRegular(0, v2.getElementSize(), v2.getShape(), wantSection);
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      raf.seek(chunk.getSrcPos());
      raf.readInt(arr, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
    }
    return Array.factory(DataType.INT, v2.getShape(), arr);
  }

  public static void readExample3(RandomAccessFile raf, Group.Builder rootGroup)
      throws IOException, InvalidRangeException {
    class nestedClass { /* DOCS-IGNORE */
      class VarInfo {
        long filePos;
        int otherStuff;
      }

      private RandomAccessFile raf;

      public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask)
          throws IOException {
        // save RandomAccessFile as instance variable
        this.raf = raf;
        // ...
        Variable.Builder elev = Variable.builder().setName("elevation").setDataType(DataType.SHORT);
        // .. add Variable attributes as above

        VarInfo vinfo = new VarInfo();
        // figure out where the elevation Variable's data starts
        vinfo.filePos = calcPosition();
        vinfo.otherStuff = 42;
        elev.setSPobject(vinfo);
        // add Variable
        rootGroup.addVariable(elev);
        // ...
      }

      public Array readData(Variable v2, Section wantSection)
          throws IOException, InvalidRangeException {
        VarInfo vinfo = (VarInfo) v2.getSPobject();

        raf.seek(vinfo.filePos);
        raf.order(RandomAccessFile.BIG_ENDIAN);
        int size = (int) v2.getSize();
        int[] arr = new int[size];
        // ...

        return Array.factory(DataType.INT, v2.getShape(), arr);
      }
    }
    nestedClass obj = new nestedClass(); /* DOC-IGNORE */
    obj.build(raf, rootGroup, null); /* DOC-IGNORE */
    obj.readData(rootGroup.build().findVariableLocal("elevation"), null); /* DOC-IGNORE */
  }

  /****************
   * helpers
   */

  // dummy BufrInput class
  private static class BufrInput {
    BufrInput(RandomAccessFile raf) {}

    boolean isValidFile() {
      return true;
    }
  }

  // dummy Grib1Input class
  private static class Grib1Input {
    Grib1Input(RandomAccessFile raf) {}

    int getEdition() {
      return 1;
    }
  }

  private static int calcPosition() {
    return 0;
  }
}
