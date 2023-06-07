/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.dap4lib;

import dap4.core.util.ChecksumMode;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4lib.cdm.CDMTypeFcns;
import dap4.dap4lib.cdm.CDMUtil;
import dap4.dap4lib.cdm.nc2.D4StructureDataIterator;
import ucar.ma2.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dap4.core.interfaces.ArrayScheme.*;

public class D4DataCompiler {

  public static boolean DEBUG = false;

  //////////////////////////////////////////////////
  // Constants

  //////////////////////////////////////////////////
  // Instance variables

  protected DapDataset dmr = null;

  protected ChecksumMode checksummode = null;
  protected ByteOrder remoteorder = null;

  protected D4DSP dsp;
  protected DeChunkedInputStream stream = null;

  // Checksum information
  // We have two checksum maps: one for the remotely calculated value
  // and one for the locally calculated value.
  protected Map<DapVariable, Long> localchecksummap = new HashMap<>();
  protected Map<DapVariable, Long> remotechecksummap = new HashMap<>();

  //////////////////////////////////////////////////
  // Constructor(s)

  /**
   * Constructor
   *
   * @param dsp the D4DSP
   * @param checksummode
   * @param remoteorder
   */

  public D4DataCompiler(D4DSP dsp, ChecksumMode checksummode, ByteOrder remoteorder) throws DapException {
    this.dsp = dsp;
    this.dmr = this.dsp.getDMR();
    this.stream = this.dsp.getStream();
    this.checksummode = ChecksumMode.asTrueFalse(checksummode);
    this.remoteorder = remoteorder;
  }

  //////////////////////////////////////////////////
  // Accessors

  public Map<DapVariable, Long> getChecksumMap(DapConstants.ChecksumSource src) {
    switch (src) {
      case LOCAL:
        return this.localchecksummap;
      case REMOTE:
        return this.remotechecksummap;
    }
    return null;
  }

  protected void setChecksum(DapConstants.ChecksumSource src, DapVariable dvar, Long csum) {
    switch (src) {
      case LOCAL:
        this.localchecksummap.put(dvar, csum);
      case REMOTE:
        this.remotechecksummap.put(dvar, csum);
    }
  }

  //////////////////////////////////////////////////
  // DataCompiler API

  /**
   * The goal here is to process the serialized
   * databuffer and pull out top-level variable positions
   * in the serialized databuffer.
   * In some cases -- String, Sequence, Structure --
   * significant transforms are applied to the data
   * to make it usable with ucar.ma2.Array.
   *
   * @throws DapException
   */
  public void compile() throws IOException {
    assert (this.dmr != null && this.stream != null);
    // iterate over the variables represented in the databuffer
    for (DapVariable vv : this.dmr.getTopVariables()) {
      Object storage = compileVar(vv);
      D4Array data = new D4Array(schemeFor(vv), this.dsp, vv).setStorage(storage);
      assert data.getArray() == null;
      data.setArray(createArray(vv, data.getStorage()));
      this.dsp.addVariableData(vv, data);
    }
  }

  /**
   * Return a compiled version of the data for this variable.
   * Possible return values are:
   * 1. String - String[]
   * 2. Opaque - Bytebuffer[]
   * 3. Fixed atomic - <type>[]
   * 4. Structure - Object[][nfields]
   * 5. Sequence - Object[][nfields]
   */
  protected Object compileVar(DapVariable dapvar) throws IOException {
    Object data = null;
    DapType type = dapvar.getBaseType();
    if (dapvar.isTopLevel() && this.checksummode == ChecksumMode.TRUE)
      this.stream.startChecksum();
    if (type.isAtomic())
      data = compileAtomicVar(dapvar);
    else if (type.isStructType()) {
      data = compileStructureArray(dapvar);
    } else if (type.isSeqType()) {
      data = compileSequenceArray(dapvar);
    }
    if (dapvar.isTopLevel() && this.checksummode == ChecksumMode.TRUE) {
      // Save the locally computed checksum
      long localcrc32 = this.stream.endChecksum(); // extract the remotechecksum from databuffer src,
      setChecksum(DapConstants.ChecksumSource.LOCAL, dapvar, localcrc32);
      // Save the checksum sent by the server
      long remotecrc32 = extractChecksum();
      setChecksum(DapConstants.ChecksumSource.REMOTE, dapvar, remotecrc32);
    }
    return data;
  }

  /**
   * Compile fixed-sized atomic types
   * Storage = <type>[]
   * 
   * @param var
   * @return data
   * @throws DapException
   */
  protected Object compileAtomicVar(DapVariable var) throws IOException {
    DapType daptype = var.getBaseType();

    // special cases
    if (daptype.isStringType()) // string or URL
      return compileStringVar(var);
    if (daptype.isOpaqueType())
      return compileOpaqueVar(var);

    // All other fixed-size atomic types
    long dimproduct = var.getCount();
    long total = dimproduct * daptype.getSize();
    byte[] bytes = new byte[(int) total]; // total space required
    int red = this.stream.read(bytes);
    if (red <= 0)
      throw new IOException("D4DataCompiler: read failure");
    if (red < total)
      throw new DapException("D4DataCompiler: short read");
    // Convert to vector of required type
    Object storage = CDMTypeFcns.bytesAsTypeVec(daptype, bytes);
    CDMTypeFcns.decodebytes(this.remoteorder, daptype, bytes, storage);
    return storage;
  }

  /**
   * Read and convert a string typed array
   * 
   * @param var
   * @return cursor
   * @throws DapException
   */
  protected Object compileStringVar(DapVariable var) throws IOException {
    DapType daptype = var.getBaseType();
    assert daptype.isStringType();
    long dimproduct = var.getCount(); // == # strings
    String[] storage = new String[(int) dimproduct];
    int count = 0;
    // read each string
    for (int i = 0; i < dimproduct; i++) {
      int strsize = getCount();
      byte[] sbytes = new byte[strsize];
      int red = this.stream.read(sbytes);
      assert red == strsize;
      storage[count] = new String(sbytes, DapUtil.UTF8);
      count++;
    }
    return storage;
  }

  /**
   * Read and convert an opaque typed array
   * 
   * @param var
   * @return cursor
   * @throws DapException
   */
  protected Object compileOpaqueVar(DapVariable var) throws IOException {
    DapType daptype = var.getBaseType();
    assert daptype.isOpaqueType();
    long dimproduct = var.getCount(); // == # opaque objects
    ByteBuffer[] storage = new ByteBuffer[(int) dimproduct];
    int count = 0;
    // read each string
    for (int i = 0; i < dimproduct; i++) {
      int osize = getCount();
      byte[] obytes = new byte[osize];
      int red = this.stream.read(obytes);
      assert red == osize;
      storage[count] = ByteBuffer.wrap(obytes);
      count++;
    }
    return storage;
  }

  /**
   * Compile a structure array.
   * Since we are using ArrayStructureMA, we need to capture
   * Our storage is Object[dimproduct]; this will be
   * converted properly when the D4Cursor Array is created.
   *
   * @param var the template
   * @return a StructureData[dimproduct] for the data for this struct.
   * @throws DapException
   */
  protected Object compileStructureArray(DapVariable var) throws IOException {
    DapStructure dapstruct = (DapStructure) var.getBaseType();
    long dimproduct = var.getCount();
    Object[] storage = new Object[(int) dimproduct];
    Index idx = Index.factory(CDMUtil.computeEffectiveShape(var.getDimensions()));
    long idxsize = idx.getSize();
    for (int offset = 0; (offset < idxsize); offset++) {
      Object instance = compileStructure(dapstruct);
      storage[offset] = instance;
    }
    return storage;
  }

  /**
   * Compile a structure instance.
   * Storage is Object[dapstruct.getFields().size()];
   *
   * @param dapstruct The template
   * @return A DataStructure for the databuffer for this struct.
   * @throws DapException
   */
  protected Object compileStructure(DapStructure dapstruct) throws IOException {
    List<DapVariable> dfields = dapstruct.getFields();
    Object[] storage = new Object[dfields.size()];
    for (int m = 0; m < dfields.size(); m++) {
      DapVariable dfield = dfields.get(m);
      Object dvfield = compileVar(dfield);
      storage[m] = dvfield;
    }
    return storage;
  }

  /**
   * Compile a sequence array.
   * Our storage is Object[dimproduct]
   * 
   * @param var the template
   * @return Object[recordcount]
   * @throws DapException
   */
  protected Object compileSequenceArray(DapVariable var) throws IOException {
    DapSequence dapseq = (DapSequence) var.getBaseType();
    long dimproduct = var.getCount();
    Object[] storage = new Object[(int) dimproduct];
    Index idx = Index.factory(CDMUtil.computeEffectiveShape(var.getDimensions()));
    long idxsize = idx.getSize();
    for (int offset = 0; (offset < idxsize); offset++) {
      Object seq = compileSequence(dapseq);
      storage[offset] = seq;
    }
    return storage;
  }

  /**
   * Compile a sequence as a set of records.
   * Our storage is Object[] where |storage| == nrecords
   *
   * @param dapseq
   * @return sequence
   * @throws DapException
   */
  public Object compileSequence(DapSequence dapseq) throws IOException {
    List<DapVariable> dfields = dapseq.getFields();
    // Get the count of the number of records
    long nrecs = getCount();
    Object[] records = new Object[(int) nrecs];
    for (int r = 0; r < nrecs; r++) {
      Object record = compileStructure((DapStructure) dapseq);
      records[r] = record;
    }
    return records;
  }

  //////////////////////////////////////////////////
  // Utilities

  protected long extractChecksum() throws IOException {
    assert this.checksummode == ChecksumMode.TRUE;
    byte[] bytes = new byte[4]; // 4 == sizeof(checksum)
    int red = this.stream.read(bytes);
    assert red == 4;
    ByteBuffer bb = ByteBuffer.wrap(bytes).order(this.remoteorder);
    long csum = (int) bb.getInt();
    csum = csum & 0x00000000FFFFFFFF;
    csum = csum & 0x00000000FFFFFFFFL; /* crc is 32 bits unsigned */
    return csum;
  }

  protected void skip(long count) throws IOException {
    for (long i = 0; i < count; i++) {
      int c = this.stream.read();
      if (c < 0)
        break;
    }
  }

  protected int getCount() throws IOException {
    byte[] bytes = new byte[8]; // 8 == sizeof(long)
    int red = this.stream.read(bytes);
    assert red == 8;
    ByteBuffer bb = ByteBuffer.wrap(bytes).order(this.remoteorder);
    long count = bb.getLong();
    count = (count & 0xFFFFFFFF);
    return (int) count;
  }

  /**
   * Compute the size in databuffer of the serialized form
   *
   * @param daptype
   * @return type's serialized form size
   */
  protected int computeTypeSize(DapType daptype) {
    return LibTypeFcns.size(daptype);
  }

  protected long walkByteStrings(long[] positions, ByteBuffer databuffer) throws IOException {
    int count = positions.length;
    long total = 0;
    int savepos = databuffer.position();
    // Walk each bytestring
    for (int i = 0; i < count; i++) {
      int pos = databuffer.position();
      positions[i] = pos;
      int size = getCount();
      total += DapConstants.COUNTSIZE;
      total += size;
      skip(size);
    }
    databuffer.position(savepos);// leave position unchanged
    return total;
  }

  public Array createArray(DapVariable var, Object storage) {
    Array array = null;
    switch (schemeFor(var)) {
      case ATOMIC:
        array = createAtomicArray(var, storage);
        break;
      case STRUCTARRAY:
        array = createStructureArray(var, storage);
        break;
      case SEQARRAY:
        array = createSequenceArray(var, storage);
        break;
      case STRUCTURE:
      case SEQUENCE:
      default:
        assert false;
    }
    return array;
  }

  protected Array createAtomicArray(DapVariable var, Object storage) {
    DapType dtype = var.getBaseType();
    if (dtype.isEnumType()) {
      // Coverity[FB.BC_UNCONFIRMED_CAST]
      dtype = ((DapEnumeration) (var.getBaseType()));
    }
    DataType cdmtype = CDMTypeFcns.daptype2cdmtype(dtype);
    int[] shape = CDMUtil.computeEffectiveShape(var.getDimensions());
    Array array = Array.factory(cdmtype, shape, storage);
    return array;
  }

  protected Array createStructureArray(DapVariable var, Object storage) {
    DapType dtype = var.getBaseType();
    StructureMembers members = computemembers(var);
    int[] shape = CDMUtil.computeEffectiveShape(var.getDimensions());
    Object[] instances = (Object[]) storage;
    StructureData[] structdata = new StructureData[instances.length];
    for (int i = 0; i < instances.length; i++) {
      Object[] fields = (Object[]) instances[i];
      StructureDataW sdw = new StructureDataW(members);
      for (int f = 0; f < members.getMembers().size(); f++) {
        StructureMembers.Member fm = sdw.getMembers().get(f);
        DapVariable d4field = ((DapStructure) dtype).getField(f);
        Object fielddata = fields[f];
        Array fieldarray = createArray(d4field, fielddata);
        sdw.setMemberData(fm, fieldarray);
      }
      structdata[i] = sdw;
    }
    ArrayStructureW array = new ArrayStructureW(members, shape, structdata);
    return array;
  }

  /**
   * Create an Array object for a DAP4 Sequence.
   * Unfortunately, the whole CDM Sequence/VLEN mechanism
   * is completely hosed, with no hope of a simple fix.
   * It appears that the only thing we can do is support scalar sequences.
   * However in useless hope, the code is written as if arrays of sequences
   * can be supported.
   * 
   * @param var
   * @param storage
   * @return
   */
  protected Array createSequenceArray(DapVariable var, Object storage) {
    DapType dtype = var.getBaseType();
    StructureMembers members = computemembers(var);
    int[] shape = CDMUtil.computeEffectiveShape(var.getDimensions());
    Object[] allinstancedata = (Object[]) storage;
    int ninstances = allinstancedata.length;
    if (ninstances != 1) // enforce scalar assumption
      throw new IndexOutOfBoundsException("Non-scalar Dap4 Sequences not supported");
    Array[] allinstances = new Array[ninstances];
    for (int i = 0; i < ninstances; i++) { // iterate over all sequence instances array
      Object[] ithelemdata = (Object[]) allinstancedata[i];
      int nrecords = ithelemdata.length;
      StructureData[] allrecords = new StructureData[nrecords]; // for creating the record iterator
      for (int r = 0; r < nrecords; r++) { // iterate over the records of one sequence
        Object[] onerecorddata = (Object[]) ithelemdata[r];
        StructureDataW onerecord = new StructureDataW(members);
        for (int f = 0; f < members.getMembers().size(); f++) { // iterate over fields in one record
          StructureMembers.Member fm = members.getMember(f);
          DapVariable d4field = ((DapStructure) dtype).getField(f);
          Array fieldarray = createArray(d4field, onerecorddata[f]);
          onerecord.setMemberData(fm, fieldarray);
        }
        allrecords[r] = onerecord;
      }
      D4StructureDataIterator onesequence = new D4StructureDataIterator().setList(allrecords);
      ArraySequence oneseq = new ArraySequence(members, onesequence, nrecords);
      allinstances[i] = oneseq;
    }
    return allinstances[0]; // enforce scalar assumption
  }

  /**
   * Compute the StructureMembers object
   * from a DapStructure. May need to recurse
   * if a field is itself a Structure
   *
   * @param var The DapVariable to use to construct
   *        a StructureMembers object.
   * @return The StructureMembers object for the given DapStructure
   */
  static StructureMembers computemembers(DapVariable var) {
    DapStructure ds = (DapStructure) var.getBaseType();
    StructureMembers sm = new StructureMembers(ds.getShortName());
    List<DapVariable> fields = ds.getFields();
    for (int i = 0; i < fields.size(); i++) {
      DapVariable field = fields.get(i);
      DapType dt = field.getBaseType();
      DataType cdmtype = CDMTypeFcns.daptype2cdmtype(dt);
      StructureMembers.Member m =
          sm.addMember(field.getShortName(), "", null, cdmtype, CDMUtil.computeEffectiveShape(field.getDimensions()));
      m.setDataParam(i); // So we can index into various lists
      // recurse if this field is itself a structure
      if (dt.getTypeSort().isStructType()) {
        StructureMembers subsm = computemembers(field);
        m.setStructureMembers(subsm);
      }
    }
    return sm;
  }

}

