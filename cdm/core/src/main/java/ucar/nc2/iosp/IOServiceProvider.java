/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/**
 * This is the service provider interface for the low-level I/O access classes (read only).
 * This is only used by service implementors.
 *
 * An implementation must have a no-argument constructor.
 *
 * The NetcdfFile class manages all registered IOServiceProvider classes.
 * When NetcdfFile.open() is called:
 * <ol>
 * <li>the file is opened as a ucar.unidata.io.RandomAccessFile;</li>
 * <li>the file is handed to the isValidFile() method of each registered
 * IOServiceProvider class (until one returns true, which means it can read the file).</li>
 * <li>the open() method on the resulting IOServiceProvider class is handed the file.</li>
 *
 * @author caron
 */
public interface IOServiceProvider {

  /**
   * Check if this is a valid file for this IOServiceProvider.
   * You must make this method thread safe, ie dont keep any state.
   * 
   * @param raf RandomAccessFile
   * @return true if valid.
   * @throws java.io.IOException if read error
   */
  boolean isValidFile(RandomAccessFile raf) throws IOException;

  /**
   * Open existing file, and populate ncfile with it. This method is only called by the
   * NetcdfFile constructor on itself. The provided NetcdfFile object will be empty
   * except for the location String and the IOServiceProvider associated with this
   * NetcdfFile object.
   *
   * @param raf the file to work on, it has already passed the isValidFile() test.
   * @param ncfile add objects to this empty NetcdfFile
   * @param cancelTask used to monitor user cancellation; may be null.
   * @throws IOException if read error
   * @deprecated Use build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask)
   */
  @Deprecated
  void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException;

  /**
   * If this iosp implements build().
   */
  boolean isBuilder();

  /**
   * Open existing file, and populate it. Note that you cannot reference the NetcdfFile within this routine.
   * This is the bridge to immutable objects that will be used exclusively in version 6.
   *
   * @param raf the file to work on, it has already passed the isValidFile() test.
   * @param rootGroup add objects to the root group.
   * @param cancelTask used to monitor user cancellation; may be null.
   * @throws IOException if read error
   */
  void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException;

  /** Sometimes the builder needs access to the finished objects. This is called after ncfile.build() */
  void buildFinish(NetcdfFile ncfile);

  /**
   * Read data from a top level Variable and return a memory resident Array. This Array has the same element type as the
   * Variable, and the
   * requested shape.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read. There must be a Range for each Dimension in the variable, in order.
   *        Note: no nulls allowed.
   *        IOSP may not modify.
   * @return the requested data in a memory-resident Array
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   * @see ucar.ma2.Range
   */
  ucar.ma2.Array readData(Variable v2, Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /**
   * Read data from a top level Variable and send data to a WritableByteChannel.
   * Must be in big-endian order.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read.
   *        There must be a Range for each Dimension in the variable, in order.
   *        Note: no nulls allowed. IOSP may not modify.
   * @param channel write data to this WritableByteChannel
   * @return the number of bytes written to the channel
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   * @deprecated do not use
   */
  @Deprecated
  long readToByteChannel(Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /** @deprecated do not use */
  @Deprecated
  long streamToByteChannel(Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /**
   * Read data from a top level Variable and send data to a OutputStream.
   * Must be in big-endian order, following ncstream conventions.
   * Default implementation just reads to memory and writes to stream.
   * Allow override for possible performance improvements.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read.
   *        There must be a Range for each Dimension in the variable, in order.
   *        Note: no nulls allowed. IOSP may not modify.
   * @param out write data to this OutputStream
   * @return the number of bytes written to the channel
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  long readToOutputStream(Variable v2, Section section, OutputStream out)
      throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /**
   * Allows reading sections of nested variables
   * 
   * @param cer section specification : what data is wanted
   * @return requested data array
   * @throws IOException on read error
   * @throws InvalidRangeException if section spec is invalid
   */
  ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException;

  /**
   * Get the structure iterator. iosps with top level sequences must override.
   * Not threadsafe; do not use multiple StructureDataIterator for the same iosp.
   *
   * @param s the Structure
   * @param bufferSize the buffersize
   * @return the data iterator
   * @throws java.io.IOException if problem reading data
   */
  StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException;

  /**
   * Close the file.
   * It is the IOServiceProvider's job to close the file (even though it didnt open it),
   * and to free any other resources it has used.
   * 
   * @throws IOException if read error
   */
  void close() throws IOException;

  /**
   * Extend the NetcdfFile if the underlying dataset has changed
   * in a way that is compatible with the current metadata.
   * For example, if the unlimited dimension has grown.
   *
   * @return true if the NetcdfFile was extended.
   * @throws IOException if a read error occured when accessing the underlying dataset.
   * @deprecated Do not use.
   */
  @Deprecated
  boolean syncExtend() throws IOException;

  /**
   * Release any system resources like file handles.
   * Optional, implement only if you are able to reacquire.
   * Used when object is made inactive in cache.
   */
  void release() throws IOException;

  /**
   * Reacquire any resources like file handles
   * Used when reactivating in cache.
   */
  void reacquire() throws IOException;

  // public long getLastModified(); LOOK: dont add this for backwards compatibility. Probably add back in in version 5

  /**
   * A way to communicate arbitrary information to an iosp.
   * 
   * @param message opaque message sent to the IOSP object when its opened (not when isValidFile() is called)
   * @return opaque return, may be null.
   */
  Object sendIospMessage(Object message);

  /**
   * Debug info for this object.
   * 
   * @param o which object
   * @return debug info for this object
   */
  String toStringDebug(Object o);

  /**
   * Show debug / underlying implementation details
   * 
   * @return debug info
   */
  String getDetailInfo();

  /**
   * Get a unique id for this file type.
   * 
   * @return registered id of the file type
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeId();

  /**
   * Get the version of this file type.
   * 
   * @return version of the file type
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeVersion();

  /**
   * Get a human-readable description for this file type.
   * 
   * @return description of the file type
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeDescription();

}
