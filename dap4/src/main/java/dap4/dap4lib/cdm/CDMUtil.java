/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib.cdm;

import dap4.core.dmr.DapDimension;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;
import dap4.core.util.DapException;
import dap4.core.util.Slice;
import ucar.ma2.ForbiddenConversionException;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.CDMNode;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import java.util.ArrayList;
import java.util.List;

/**
 * CDM related Constants and utilities
 * common to client and server code
 */

public abstract class CDMUtil {

  static final String hexchars = "0123456789abcdef";

  static final public Index SCALAR = new Index(new int[0], new int[0]);

  /**
   * Convert a list of ucar.ma2.Range to a list of Slice
   * More or less the inverst of create CDMRanges
   *
   * @param rangelist the set of ucar.ma2.Range
   * @return the equivalent list of Slice
   */
  public static List<Slice> createSlices(List<Range> rangelist) throws dap4.core.util.DapException {
    List<Slice> slices = new ArrayList<Slice>(rangelist.size());
    for (int i = 0; i < rangelist.size(); i++) {
      Range r = rangelist.get(i);
      // r does not store last
      int stride = r.stride();
      int first = r.first();
      int n = r.length();
      int stop = first + (n * stride);
      Slice cer = new Slice(first, stop - 1, stride);
      slices.add(cer);
    }
    return slices;
  }

  /**
   * Test a List<Range> against a List<DapDimension>
   * to see if the range list represents the whole
   * set of dimensions within the specified indices.
   *
   * @param rangelist the set of ucar.ma2.Range
   * @param dimset the set of DapDimensions
   * @param start start looking here
   * @param stop stop looking here
   * @return true if rangelist is whole; false otherwise.
   */

  public static boolean isWhole(List<Range> rangelist, List<DapDimension> dimset, int start, int stop)
      throws dap4.core.util.DapException {
    int rsize = (rangelist == null ? 0 : rangelist.size());
    if (rsize != dimset.size())
      throw new dap4.core.util.DapException("range/dimset rank mismatch");
    if (rsize == 0)
      return true;
    if (start < 0 || stop < start || stop > rsize)
      throw new dap4.core.util.DapException("Invalid start/stop indices");

    for (int i = start; i < stop; i++) {
      Range r = rangelist.get(i);
      DapDimension d = dimset.get(i);
      if (r.stride() != 1 || r.first() != 0 || r.length() != d.getSize())
        return false;
    }
    return true;
  }

  /**
   * Test a List<Range> against a List<Slice>
   * to see if the range list is whole
   * wrt the slices
   *
   * @param rangelist the set of ucar.ma2.Range
   * @param slices the set of slices
   * @return true if rangelist is whole wrt slices; false otherwise.
   */
  public static boolean isWhole(List<Range> rangelist, List<Slice> slices) throws dap4.core.util.DapException {
    if (rangelist.size() != slices.size())
      return false;
    for (int i = 0; i < rangelist.size(); i++) {
      Range r = rangelist.get(i);
      Slice slice = slices.get(i);
      if (r.stride() != 1 || r.first() != 0 || r.length() != slice.getCount())
        return false;
    }
    return true;
  }

  /**
   * Test a List<Range> against the CDM variable's dimensions
   * to see if the range list is whole
   * wrt the dimensions
   *
   * @param rangelist the set of ucar.ma2.Range
   * @param var the cdm var
   * @return true if rangelist is whole wrt slices; false otherwise.
   */
  public static boolean isWhole(List<Range> rangelist, Variable var) throws dap4.core.util.DapException {
    List<Dimension> dimset = var.getDimensions();
    if (rangelist.size() != dimset.size())
      return false;
    for (int i = 0; i < rangelist.size(); i++) {
      Range r = rangelist.get(i);
      Dimension dim = dimset.get(i);
      if (r.stride() != 1 || r.first() != 0 || r.length() != dim.getLength())
        return false;
    }
    return true;
  }

  public static List<ucar.ma2.Range> createCDMRanges(List<Slice> slices) throws DapException {
    List<ucar.ma2.Range> cdmranges = new ArrayList<Range>();
    for (int i = 0; i < slices.size(); i++) {
      Slice r = slices.get(i);
      try {
        ucar.ma2.Range cmdr;
        cmdr = new ucar.ma2.Range((int) r.getFirst(), (int) r.getLast(), (int) r.getStride());
        cdmranges.add(cmdr);
      } catch (InvalidRangeException ire) {
        throw new DapException(ire);
      }
    }
    return cdmranges;
  }

  /**
   * NetcdfDataset can end up wrapping a variable
   * in multiple wrapping classes (e.g. VariableDS).
   * Goal of this procedure is to get down to the
   * lowest level Variable instance
   *
   * @param var possibly wrapped variable
   * @return the lowest level Variable instance
   */
  public static Variable unwrap(Variable var) {
    return (Variable) CDMNode.unwrap(var);
  }

  /**
   * NetcdfDataset can wrap a NetcdfFile.
   * Goal of this procedure is to get down to the
   * lowest level NetcdfFile instance.
   *
   * @param file NetcdfFile or NetcdfDataset
   * @return the lowest level NetcdfFile instance
   */
  public static NetcdfFile unwrapfile(NetcdfFile file) {
    for (;;) {
      if (file instanceof NetcdfDataset) {
        NetcdfDataset ds = (NetcdfDataset) file;
        file = ds.getReferencedFile();
        if (file == null)
          break;
      } else
        break;
    }
    return file;
  }

  public static boolean hasVLEN(List<Range> ranges) {
    if (ranges == null || ranges.size() == 0)
      return false;
    return ranges.get(ranges.size() - 1) == Range.VLEN;
  }

  /**
   * Test if any dimension is variable length
   */
  public static boolean hasVLEN(Variable v) {
    return containsVLEN(v.getDimensions());
  }

  /**
   * Test if any dimension is variable length
   */
  public static boolean containsVLEN(List<Dimension> dimset) {
    if (dimset == null)
      return false;
    for (Dimension dim : dimset) {
      if (dim.isVariableLength())
        return true;
    }
    return false;
  }

  /**
   * Compute the shape inferred from a set of slices.
   * 'Effective' means that any trailing vlen will be
   * ignored.
   *
   * @param dimset from which to generate shape
   * @return effective shape
   */
  public static int[] computeEffectiveShape(List<DapDimension> dimset) {
    if (dimset == null || dimset.size() == 0)
      return new int[0];
    int effectiverank = dimset.size();
    int[] shape = new int[effectiverank];
    for (int i = 0; i < effectiverank; i++) {
      shape[i] = (int) dimset.get(i).getSize();
    }
    return shape;
  }

  /**
   * Convert an array of one type of values to another type
   *
   * @param dsttype target type
   * @param srctype source type
   * @param src array of values to convert
   * @return resulting array of converted values as an object
   */

  public static Object convertVector(DapType dsttype, DapType srctype, Object src) {
    int i;

    TypeSort srcatomtype = srctype.getAtomicType();
    TypeSort dstatomtype = dsttype.getAtomicType();

    if (srcatomtype == dstatomtype) {
      return src;
    }
    if (srcatomtype.isIntegerType() && TypeSort.getSignedVersion(srcatomtype) == TypeSort.getSignedVersion(dstatomtype))
      return src;

    Object result = CDMTypeFcns.convert(dstatomtype, srcatomtype, src);
    if (result == null)
      throw new ForbiddenConversionException();
    return result;
  }

  public static String getChecksumString(byte[] checksum) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < checksum.length; i++) {
      byte b = checksum[i];
      buf.append(hexchars.charAt(b >> 4));
      buf.append(hexchars.charAt(b & 0xF));
    }
    return buf.toString();
  }

  public static List<Range> dimsetToRanges(List<DapDimension> dimset) throws dap4.core.util.DapException {
    if (dimset == null)
      return null;
    List<Range> ranges = new ArrayList<>();
    for (int i = 0; i < dimset.size(); i++) {
      DapDimension dim = dimset.get(i);
      try {
        Range r = new Range(dim.getShortName(), 0, (int) dim.getSize() - 1, 1);
        ranges.add(r);
      } catch (InvalidRangeException ire) {
        throw new dap4.core.util.DapException(ire);
      }
    }
    return ranges;
  }

  public static List<Slice> shapeToSlices(int[] shape) throws dap4.core.util.DapException {
    if (shape == null)
      return null;
    List<Slice> slices = new ArrayList<>(shape.length);
    for (int i = 0; i < shape.length; i++) {
      Slice sl = new Slice(0, shape[i] - 1, 1);
      slices.add(sl);
    }
    return slices;
  }

  public static Index cdmIndexToIndex(ucar.ma2.Index cdmidx) {
    Index dapidx = new Index(cdmidx.getCurrentCounter(), cdmidx.getShape());
    return dapidx;
  }

  public static ucar.ma2.Index indexToCcMIndex(Index d4) {
    return (Index) d4;
  }

  /**
   * Convert DataIndex to list of slices
   * 
   * @param indices to convert
   * @return list of corresponding slices
   */

  public static List<Slice> indexToSlices(Index indices) throws DapException {
    // short circuit the scalar case
    int rank = indices.getRank();
    if (rank == 0)
      return Slice.SCALARSLICES;
    // offset = d3*(d2*(d1*(x1))+x2)+x3
    List<Slice> slices = new ArrayList<>(rank);
    for (int i = 0; i < rank; i++) {
      int isize = indices.getCurrentCounter()[i];
      slices.add(new Slice(isize, isize + 1, 1, indices.getShape(i)));
    }
    return slices;
  }

  /**
   * If a set of slices refers to a single position,
   * then return the corresponding Index. Otherwise,
   * throw Exception.
   *
   * @param slices
   * @return Index corresponding to slices
   * @throws DapException
   */
  public static Index slicesToIndex(List<Slice> slices) throws DapException {
    int[] positions = new int[slices.size()];
    int[] dimsizes = new int[slices.size()];
    for (int i = 0; i < positions.length; i++) {
      Slice s = slices.get(i);
      if (s.getCount() != 1)
        throw new DapException("Attempt to convert non-singleton sliceset to index");
      positions[i] = s.getFirst();
      dimsizes[i] = s.getMax();
    }
    return new Index(positions, dimsizes);
  }

}
