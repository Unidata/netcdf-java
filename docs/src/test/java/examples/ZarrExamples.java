package examples;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.FilterProvider;
import ucar.nc2.filter.Filters;
import ucar.nc2.filter.Shuffle;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Map;

public class ZarrExamples {

  private static String pathToDirectoryStore = "";
  private static String pathToZipStore = "";
  private static String pathToObjectStore = "";

  public static void readZarrStores() throws IOException {
    // a local file path
    NetcdfFile directoryStoreZarr = NetcdfFiles.open(pathToDirectoryStore);
    // a local file path + '.zip'
    NetcdfFile zipdirectoryStoreZarr = NetcdfFiles.open(pathToZipStore);
    // an object store path, sarting with 'cdms3:' and ending with 'delimiter=' + the store delimiter
    NetcdfFile objectStoreZarr = NetcdfFiles.open(pathToObjectStore);
  }

  public static void implementFilter() {
    byte[] dataOut = null; /* DOCS-IGNORE */
    /* INSERT public */class MyFilter extends Filter {

      @Override
      public byte[] encode(byte[] dataIn) throws IOException {
        // your encoding implementation here
        return dataOut;
      }

      @Override
      public byte[] decode(byte[] dataIn) throws IOException {
        // your decoding implementation here
        return dataOut;
      }
    }
  }

  public static void implementFilterProvider() {
    /* INSERT public */class MyFilterProvider implements FilterProvider {

      @Override
      public String getName() {
        // returns a string identifier for your filter
        return "myFilter"; // see notes on filter names and ids
      }

      @Override
      public int getId() {
        // returns a numeric identifier for your filter
        return 32768; // see notes on filter names and ids
      }

      @Override
      public Filter create(Map<String, Object> properties) {
        return new MyFilter(properties); // return an instance of your filter
      }
    }
  }

  public static void implementFilterProvider2() {
    /* INSERT public */class DefaultFilterProvider implements FilterProvider {

      @Override
      public String getName() {
        return "defaultFilter";
      }

      @Override
      public int getId() {
        return -1;
      }

      @Override
      public boolean canProvide(String name) {
        return true;
      }

      @Override
      public boolean canProvide(int id) {
        return true;
      }

      @Override
      public Filter create(Map<String, Object> properties) {
        return new DefaultFilter(properties); // return an instance of your filter
      }
    }
  }

  /**
   * Shell classes for code snippets
   */
  static class MyFilter extends Filter{

    public MyFilter(Map<String, Object> properties) {
      super();
    }

    @Override
    public byte[] encode(byte[] dataIn) throws IOException {
      return new byte[0];
    }

    @Override
    public byte[] decode(byte[] dataIn) throws IOException {
      return new byte[0];
    }
  }

  static class DefaultFilter extends Filter {

    public DefaultFilter(Map<String, Object> properties) {
      super();
    }

    @Override
    public byte[] encode(byte[] dataIn) throws IOException {
      return new byte[0];
    }

    @Override
    public byte[] decode(byte[] dataIn) throws IOException {
      return new byte[0];
    }
  }
}
