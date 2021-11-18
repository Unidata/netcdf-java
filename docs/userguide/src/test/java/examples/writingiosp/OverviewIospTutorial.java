package examples.writingiosp;

import ucar.array.*;
import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Iterator;

public class OverviewIospTutorial {

  /**
   * Code snippet for implementing an IOSP
   */
  public static AbstractIOServiceProvider getIOSP() {
    /* INSERT public */class MyIosp extends AbstractIOServiceProvider {

      /**
       * Methods that must be implemented
       */
      public boolean isValidFile(RandomAccessFile raf) throws IOException {
        // You must examine the file that is passed to you, and quickly and accurately determine if it can be opened
        // by this IOSP. You may not keep any state (i.e. store any information) in this call,
        // and it must be thread-safe.
        return false; /* DOCS-IGNORE */
      }

      public Array readArrayData(Variable v2, Section section)
          throws IOException, ucar.array.InvalidRangeException {
        // Data will be read from Variable through this call. The Section defines the requested data subset.
        return null; /* DOCS-IGNORE */
      }

      public String getFileTypeId() {
        // See below for details on File Types.
        return null; /* DOCS-IGNORE */
      }

      public String getFileTypeDescription() {
        // See below for details on File Types.
        return null; /* DOCS-IGNORE */
      }

      public boolean isBuilder() {
        // This method should return true.
        // See notes below regarding the Builder pattern and API changes.
        return true; /* DOCS-IGNORE */
      }

      public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask)
          throws IOException {
        // If isValidFile returns true, the build method will be called.
        // This method should populate a CDM object from the RandomAccessFile, using the Group.Builder object.
        // If you need to do a lot of I/O, you should periodically check cancelTask.isCancel(), and if its true,
        // return immediately. This allows users to cancel the opening of a dataset if its taking too long.
      }

      /**
       * Methods with a default implementation, that can optionally be overriden
       */
      public void buildFinish(NetcdfFile ncfile) {
        super.buildFinish(ncfile);
        // Implement any clean-up or finish actions for your file type.
      }

      public String getFileTypeVersion() {
        // See below for details on File Types.
        return null; /* DOCS-IGNORE */
      }

      public Iterator<StructureData> getSequenceIterator(Sequence s, int bufferSize) {
        // If any of your top-level variables (not inside of a Structure) are Sequences,
        // this is how the data in them will be accessed, and you must implement it.
        return null; /* DOCS-IGNORE */
      }

      public Object sendIospMessage(Object message) {
        // This allows applications to pass an arbitrary object to the IOSP,
        // through the NetcdfFiles.open( location, buffer_size, cancelTask, spiObject) method.
        // As a rule, you should not count on having such special information available,
        // unless you are controlling all data access in an application.
        return null; /* DOCS-IGNORE */
      }

      public String getDetailInfo() {
        // Here you can pass any information that is useful to debugging.
        // It can be viewed through the ToolsUI application.
        return null; /* DOCS-IGNORE */
      }
    }
    return new MyIosp(); /* DOCS-IGNORE */
  }
}
