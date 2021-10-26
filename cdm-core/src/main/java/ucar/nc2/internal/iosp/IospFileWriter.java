/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.array.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;

import java.io.Closeable;
import java.io.IOException;

/** This is an interface to Netcdf-3 and Netcdf-4 file writing. */
public interface IospFileWriter extends Closeable {
  /**
   * Create new file, populate it from the objects in rootGroup.
   *
   * @param filename name of file to create.
   * @param rootGroup has the metadata of the file to be created. It is then modified, do not reuse.
   * @param extra if > 0, pad header with extra bytes (netcdf3 only).
   * @param preallocateSize if > 0, set length of file to this upon creation - this (usually) pre-allocates contiguous
   *        storage.
   * @param largeFile if want large file format (netcdf3 only).
   * @return the NetcdfFile being written to
   */
  NetcdfFile create(String filename, Group.Builder rootGroup, int extra, long preallocateSize, boolean largeFile)
      throws IOException;

  /**
   * Open existing file, populate rootGroup.
   * Netcdf-3 writing is restricted to writing data into existing variables, including extending the record dimension.
   * Netcdf-4 writing is general, can change and delete metadata etc.
   *
   * @param location the existing file
   * @param rootGroup has the metadata of the file to be created. It is then modified, do not reuse.
   * @throws IOException if I/O error
   */
  void openForWriting(String location, Group.Builder rootGroup, CancelTask cancelTask) throws IOException;

  NetcdfFile getOutputFile();

  /**
   * Set the fill flag.
   * For new files, set in the create() method. This method is to set fill for existing files that you want to write.
   * If true, the data is first written with fill values.
   * Set to false if you expect to write all data values, set to true if you want to be
   * sure that unwritten data values have the fill value in it.
   *
   * @param fill set fill mode true or false
   */
  void setFill(boolean fill);

  /**
   * Write data into a variable.
   * 
   * @param v2 variable to write; must already exist.
   * @param section the section of data to write.
   *        There must be a Range for each Dimension in the variable, in order.
   *        The shape must match the shape of values.
   *        The origin and stride indicate where the data is placed into the stored Variable array.
   * @param values data to write. The shape must match section.getShape().
   * @throws IOException if I/O error
   * @throws InvalidRangeException if invalid section
   */
  void writeData(Variable v2, Section section, Array<?> values) throws IOException, InvalidRangeException;

  /**
   * Append a structureData along the unlimited dimension
   *
   * @param s belongs to this structure
   * @param sdata the stuctureData to append
   * @return the recnum where it was written
   */
  int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException;

  /**
   * TODO review this
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * For netcdf 4: Any change is ok.
   * For netcdf 3: You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter. Strings are padded to 4 byte boundaries, ok to use padding
   * if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   */
  void updateAttribute(Variable v2, Attribute att) throws IOException;

  void updateAttribute(Group g, Attribute att) throws IOException;

}
