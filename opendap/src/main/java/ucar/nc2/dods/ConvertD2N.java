/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.dap.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import ucar.ma2.*;
import ucar.nc2.Variable;

/** Convert Dods object tree to cdm objects. */
class ConvertD2N {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsNetcdfFile.class);

  /*
   * Difficult cases.
   * - V is a top variable vs member of a structure (or group ??)
   * - Dgrid.member request may or may not get returned inside a Structure.
   * - Structures may have arbitrary nesting.
   */

  /*
   * DodsV: a container for dods basetypes, so we can track stuff as we process it.
   * We make the DodsV tree follow the name heirarchy, by putting the darray object to the side.
   * The root is just a container, but the other nodes each represent a BaseType/Variable.
   * 
   * DDS has a vector of BaseTypes, which may be:
   * scalar primitive (byte, int, float, String, etc)
   * array of primitive: DArray with (empty) PrimitiveVector with "template" as the BaseType.
   * DConstructor is a container for other BaseTypes, can call getVariables() to get them
   * - DStructure
   * - DGrid first Variable is the Array, rest are the maps
   * - DSequence
   * array of DStructure: DArray with BaseTypePrimitiveVector whose template is a DStructure
   * 
   * DataDDS also contains the data:
   * o scalar primitive (byte, int, float, String, etc): getValue()
   * o array of primitive: DArray with PrimitiveVector (BytePrimitiveVector, FloatPrimitiveVector, etc): getValue(i)
   * Get internal java array with getInternalStorage().
   * o array of String: DArray with BaseTypePrimitiveVector containing String objects
   * o scalar DStructure: same as DDS
   * o scalar DGrid: same as DDS
   * o array of DStructure: DArray with BaseTypePrimitiveVector, whose values are DStructure
   * X array of DGrid, DSequence: (not allowd)
   * o DSequence: values = Vector (rows) containing Vector (fields)
   */

  /**
   * Convert a DataDDS into an Array for a Structure member variable.
   *
   * @param v must be a member of a structure
   * @param section the requested variable section, as a List of type Range
   * @param dataV the dataDDS has been parsed into this dodsV, this is the top variable containing v
   * @param flatten if true, remove the StructureData "wrapper".
   * @return the data as as Array
   * @throws IOException on io error
   * @throws DAP2Exception on bad things happening
   */
  public Array convertNestedVariable(ucar.nc2.Variable v, List<Range> section, DodsV dataV, boolean flatten)
      throws IOException, DAP2Exception {
    Array data = convertTopVariable(v, section, dataV);
    if (flatten) {
      ArrayStructure as = (ArrayStructure) data;

      // make list of names
      List<String> names = new ArrayList<>();
      Variable nested = v;
      while (nested.isMemberOfStructure()) {
        names.add(0, nested.getShortName());
        nested = nested.getParentStructure();
      }

      StructureMembers.Member m = findNested(as, names, v.getShortName());
      Array mdata = m.getDataArray();
      if (mdata instanceof ArraySequenceNested) {
        // gotta unroll
        ArraySequenceNested arraySeq = (ArraySequenceNested) mdata;
        return arraySeq.flatten();
      }
      return mdata;
    }
    return data;
  }

  private StructureMembers.Member findNested(ArrayStructure as, List<String> names, String want) {
    String name = names.get(0);
    StructureMembers sm = as.getStructureMembers();
    StructureMembers.Member m = sm.findMember(name);
    if (name.equals(want))
      return m;

    // keep going
    ArrayStructure nested = (ArrayStructure) m.getDataArray();
    names.remove(0);
    return findNested(nested, names, want);
  }

  /**
   * Convert a DataDDS into an Array for a top level variable, ie not a Structure member variable.
   *
   * @param v must be a top variable
   * @param section the requested variable section
   * @param dataV the dataDDS has been parsed into this dodsV
   * @return the data as as Array
   * @throws IOException on io error
   * @throws DAP2Exception on bad
   */
  public Array convertTopVariable(ucar.nc2.Variable v, List<Range> section, DodsV dataV)
      throws IOException, DAP2Exception {
    Array data = convert(dataV);

    // arrays
    if ((dataV.darray != null) && (dataV.bt instanceof DString)) {

      if (v.getDataType() == DataType.STRING)
        return convertStringArray(data, v);
      else if (v.getDataType() == DataType.CHAR)
        return convertStringArrayToChar(dataV.darray, v, section);
      else {
        String mess = "DODSVariable convertArray String invalid dataType= " + v.getDataType();
        logger.error(mess);
        throw new IllegalArgumentException(mess);
      }

    }

    if ((dataV.bt instanceof DString) && (v.getDataType() == DataType.CHAR)) {
      // special case: convert String back to CHAR
      return convertStringToChar(data, v);
    }

    return data;
  }

  /**
   * Convert a DataDDS into an Array
   *
   * @param dataV the dataDDS has been parsed into this dodsV
   * @return the data as as Array
   * @throws IOException on io error
   * @throws DAP2Exception on bad
   */
  public Array convert(DodsV dataV) throws DAP2Exception {

    // scalars
    if (dataV.darray == null) {
      if (dataV.bt instanceof DStructure) {
        ArrayStructure structArray = makeArrayStructure(dataV);
        iconvertDataStructure((DStructure) dataV.bt, structArray.getStructureMembers());
        return structArray;

      } else if (dataV.bt instanceof DGrid) {
        throw new IllegalStateException("DGrid without a darray");

      } else if (dataV.bt instanceof DSequence) {
        ArrayStructure structArray = makeArrayStructure(dataV);
        iconvertDataSequenceArray((DSequence) dataV.bt, structArray.getStructureMembers());
        return structArray;

      } else { // scalar
        DataType dtype = dataV.getDataType();
        Array scalarData = Array.factory(dtype, new int[0]);
        IndexIterator scalarIndex = scalarData.getIndexIterator();
        iconvertDataPrimitiveScalar(dataV.bt, scalarIndex);
        return scalarData;
      }
    }

    // arrays
    if (dataV.bt instanceof DStructure) {
      ArrayStructure structArray = makeArrayStructure(dataV);
      iconvertDataStructureArray(dataV.darray, structArray.getStructureMembers());
      return structArray;

    } else if (dataV.bt instanceof DString) {
      return convertStringArray(dataV.darray);

    } else {
      // the DGrid case comes here also
      // create the array, using DODS internal array so there's no copying
      PrimitiveVector pv = dataV.darray.getPrimitiveVector();
      Object storage = pv.getInternalStorage();
      DataType dtype = dataV.getDataType();
      return Array.factory(dtype, makeShape(dataV.darray), storage);
    }
  }

  private ArrayStructure makeArrayStructure(DodsV dataV) {
    StructureMembers members = new StructureMembers(dataV.getNetcdfShortName());
    for (DodsV dodsV : dataV.children) {
      StructureMembers.Member m =
          members.addMember(dodsV.getNetcdfShortName(), null, null, dodsV.getDataType(), dodsV.getShape());
      Array data;
      if ((dodsV.bt instanceof DStructure) || (dodsV.bt instanceof DGrid)) {
        data = makeArrayStructure(dodsV);
      } else if (dodsV.bt instanceof DSequence) {
        data = makeArrayNestedSequence(dodsV);
        m.setShape(data.getShape()); // fix the shape based on the actual data LOOK
      } else {
        data = Array.factory(dodsV.getDataType(), dodsV.getShapeAll());
      }
      m.setDataArray(data);
      m.setDataObject(data.getIndexIterator()); // for setting values
    }

    return new ArrayStructureMA(members, dataV.getShapeAll());
  }

  private ArrayStructure makeArrayNestedSequence(DodsV dataV) {

    // make the members
    StructureMembers members = new StructureMembers(dataV.getClearName());
    for (DodsV dodsV : dataV.children) {
      members.addMember(dodsV.getClearName(), null, null, dodsV.getDataType(), dodsV.getShape());
    }

    // make the ArraySequence
    // LOOK single nested
    DSequence outerSeq = (DSequence) dataV.parent.bt;
    int outerLength = outerSeq.getRowCount();
    ArraySequenceNested aseq = new ArraySequenceNested(members, outerLength);

    // tell it how long each one is
    String name = dataV.getClearName();
    for (int row = 0; row < outerLength; row++) {
      for (BaseType bt : outerSeq.getRow(row)) {
        if (bt.getClearName().equals(name)) {
          DSequence innerSeq = (DSequence) bt;
          int innerLength = innerSeq.getRowCount();
          aseq.setSequenceLength(row, innerLength);
        }
      }
    }
    aseq.finish();

    // ArraySequence makes the inner data arrays; now make iterators for them
    List<StructureMembers.Member> memberList = members.getMembers();
    for (StructureMembers.Member m : memberList) {
      Array data = m.getDataArray();
      m.setDataObject(data.getIndexIterator()); // for setting values
    }

    return aseq;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // Port to StructureMember.Builder. Not ready yet because of partial construction.

  private ArrayStructure makeArrayStructureNew(DodsV dataV) {
    StructureMembers.Builder members = StructureMembers.builder().setName(dataV.getNetcdfShortName());
    for (DodsV dodsV : dataV.children) {
      StructureMembers.MemberBuilder mb =
          members.addMember(dodsV.getNetcdfShortName(), null, null, dodsV.getDataType(), dodsV.getShape());
      Array data;
      if ((dodsV.bt instanceof DStructure) || (dodsV.bt instanceof DGrid)) {
        data = makeArrayStructure(dodsV);
      } else if (dodsV.bt instanceof DSequence) {
        data = makeArrayNestedSequence(dodsV);
        mb.setShape(data.getShape()); // fix the shape based on the actual data LOOK
      } else {
        data = Array.factory(dodsV.getDataType(), dodsV.getShapeAll());
      }
      mb.setDataArray(data);
      mb.setDataObject(data.getIndexIterator()); // for setting values
    }

    return new ArrayStructureMA(members.build(), dataV.getShapeAll());
  }

  private ArrayStructure makeArrayNestedSequenceNew(DodsV dataV) {

    // make the members
    StructureMembers.Builder smb = StructureMembers.builder().setName(dataV.getClearName());
    for (DodsV dodsV : dataV.children) {
      smb.addMember(dodsV.getClearName(), null, null, dodsV.getDataType(), dodsV.getShape());
    }
    StructureMembers members = smb.build();

    // make the ArraySequence
    // LOOK single nested
    DSequence outerSeq = (DSequence) dataV.parent.bt;
    int outerLength = outerSeq.getRowCount();
    ArraySequenceNested aseq = new ArraySequenceNested(members, outerLength);

    // tell it how long each one is
    String name = dataV.getClearName();
    for (int row = 0; row < outerLength; row++) {
      for (BaseType bt : outerSeq.getRow(row)) {
        if (bt.getClearName().equals(name)) {
          DSequence innerSeq = (DSequence) bt;
          int innerLength = innerSeq.getRowCount();
          aseq.setSequenceLength(row, innerLength);
        }
      }
    }
    aseq.finish();

    // ArraySequence makes the inner data arrays; now make iterators for them
    List<StructureMembers.Member> memberList = members.getMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Array data = m.getDataArray();
      m.setDataObject(data.getIndexIterator()); // for setting values
    }

    return aseq;
  }

  // dataV is an array of DStructure: DArray with BaseTypePrimitiveVector, whose values are DStructure
  private void iconvertDataStructureArray(DVector darray, StructureMembers members) throws DAP2Exception {

    List<StructureMembers.Member> mlist = members.getMembers();
    for (StructureMembers.Member member : mlist) {

      // get the Array for this member
      String name = member.getName();
      IndexIterator ii = (IndexIterator) member.getDataObject();

      // loop over each row, fill up the data
      BaseTypePrimitiveVector pv = (BaseTypePrimitiveVector) darray.getPrimitiveVector();
      for (int row = 0; row < pv.getLength(); row++) {
        DStructure ds_data = (DStructure) pv.getValue(row);
        BaseType member_data = ds_data.getVariable(name);
        iconvertData(member_data, ii);
      }
    }
  }

  private void iconvertDataSequenceArray(DSequence dseq, StructureMembers members) throws DAP2Exception {
    for (int row = 0; row < dseq.getRowCount(); row++) {
      for (BaseType member_data : dseq.getRow(row)) {
        StructureMembers.Member member = members.findMember(member_data.getEncodedName());
        IndexIterator ii = (IndexIterator) member.getDataObject();
        iconvertData(member_data, ii);
      }
    }
  }

  private void iconvertDataStructure(DConstructor ds, StructureMembers members) throws DAP2Exception {
    List<StructureMembers.Member> mlist = members.getMembers();
    for (StructureMembers.Member member : mlist) {

      // get the Array for this member
      IndexIterator ii = (IndexIterator) member.getDataObject();

      // track down the corresponding DODS member
      String name = member.getName();
      String dodsName = name; // LOOK should be: findDodsName (name);
      if (dodsName == null) {
        throw new DAP2Exception("Cant find dodsName for member variable " + name);
      }

      BaseType bt = ds.getVariable(dodsName);
      iconvertData(bt, ii); // recursively fill up this data array
    }
  }

  private void iconvertData(opendap.dap.BaseType dodsVar, IndexIterator ii) throws DAP2Exception {

    if (dodsVar instanceof DSequence) {
      DSequence dseq = (DSequence) dodsVar;
      StructureData sd = (StructureData) ii.getObjectNext();
      iconvertDataSequenceArray(dseq, sd.getStructureMembers());

    } else if ((dodsVar instanceof DStructure) || (dodsVar instanceof DGrid)) { // nested scalar structure
      DConstructor ds = (DConstructor) dodsVar;
      StructureData sd = (StructureData) ii.getObjectNext();
      iconvertDataStructure(ds, sd.getStructureMembers());

      // arrays
    } else if (dodsVar instanceof DVector) {
      DVector da = (DVector) dodsVar;
      BaseType bt = da.getPrimitiveVector().getTemplate();

      if (bt instanceof DStructure) {
        StructureData sd = (StructureData) ii.getObjectNext();
        iconvertDataStructureArray(da, sd.getStructureMembers());

      } else if (bt instanceof DGrid) {
        throw new UnsupportedOperationException();

      } else if (bt instanceof DSequence) {
        throw new UnsupportedOperationException();

      } else {
        iconvertDataPrimitiveArray(da.getPrimitiveVector(), ii);
      }

      // primitive scalars
    } else {
      iconvertDataPrimitiveScalar(dodsVar, ii);
    }

  }

  // convert a DODS scalar value
  private void iconvertDataPrimitiveScalar(BaseType dodsScalar, IndexIterator ii) {
    if (dodsScalar instanceof DString) {
      String sval = ((DString) dodsScalar).getValue();
      ii.setObjectNext(sval);

    } else if (dodsScalar instanceof DUInt32) {
      int ival = ((DUInt32) dodsScalar).getValue();
      long lval = DataType.unsignedIntToLong(ival); // LOOK unsigned
      ii.setLongNext(lval);

    } else if (dodsScalar instanceof DUInt16) {
      short sval = ((DUInt16) dodsScalar).getValue(); // LOOK unsigned
      int ival = DataType.unsignedShortToInt(sval);
      ii.setIntNext(ival);

    } else if (dodsScalar instanceof DFloat32)
      ii.setFloatNext(((DFloat32) dodsScalar).getValue());
    else if (dodsScalar instanceof DFloat64)
      ii.setDoubleNext(((DFloat64) dodsScalar).getValue());
    else if (dodsScalar instanceof DInt32)
      ii.setIntNext(((DInt32) dodsScalar).getValue());
    else if (dodsScalar instanceof DInt16)
      ii.setShortNext(((DInt16) dodsScalar).getValue());
    else if (dodsScalar instanceof DByte)
      ii.setByteNext(((DByte) dodsScalar).getValue());
    else
      throw new IllegalArgumentException(
          "DODSVariable extractScalar invalid dataType= " + dodsScalar.getClass().getName());
  }

  // convert a DODS scalar value
  private void iconvertDataPrimitiveArray(PrimitiveVector pv, IndexIterator ii) {
    BaseType bt = pv.getTemplate();

    // set the data value, using scalarIndex from Variable
    if (bt instanceof DString) {
      BaseTypePrimitiveVector bpv = (BaseTypePrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++) {
        DString ds = (DString) bpv.getValue(row);
        ii.setObjectNext(ds.getValue()); // LOOK CHAR ?
      }

    } else if (bt instanceof DUInt32) {
      UInt32PrimitiveVector bpv = (UInt32PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++) {
        int ival = bpv.getValue(row);
        long lval = DataType.unsignedIntToLong(ival); // LOOK unsigned
        ii.setLongNext(lval);
      }
    } else if (bt instanceof DUInt16) {
      UInt16PrimitiveVector bpv = (UInt16PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++) {
        short sval = bpv.getValue(row); // LOOK unsigned
        int ival = DataType.unsignedShortToInt(sval);
        ii.setIntNext(ival);
      }
    } else if (bt instanceof DFloat32) {
      Float32PrimitiveVector bpv = (Float32PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++)
        ii.setFloatNext(bpv.getValue(row));

    } else if (bt instanceof DFloat64) {
      Float64PrimitiveVector bpv = (Float64PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++)
        ii.setDoubleNext(bpv.getValue(row));

    } else if (bt instanceof DInt32) {
      Int32PrimitiveVector bpv = (Int32PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++)
        ii.setIntNext(bpv.getValue(row));

    } else if (bt instanceof DInt16) {
      Int16PrimitiveVector bpv = (Int16PrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++)
        ii.setShortNext(bpv.getValue(row));

    } else if (bt instanceof DByte) {
      BytePrimitiveVector bpv = (BytePrimitiveVector) pv;
      for (int row = 0; row < bpv.getLength(); row++)
        ii.setByteNext(bpv.getValue(row));

    } else
      throw new IllegalArgumentException("DODSVariable extractScalar invalid dataType= " + bt.getClass().getName());
  }

  private Array convertStringArray(DArray dv) {
    opendap.dap.PrimitiveVector pv = dv.getPrimitiveVector();
    BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector) pv;
    int nStrings = btpv.getLength();
    String[] storage = new String[nStrings];
    for (int i = 0; i < nStrings; i++) {
      DString bb = (DString) btpv.getValue(i);
      storage[i] = bb.getValue();
    }
    return Array.factory(DataType.STRING, makeShape(dv), storage);
  }

  // deal with length=1 barfalloney
  // older opendap servers send netcdf char data as Strings of length 1 (!!)
  private Array convertStringArray(Array data, Variable ncVar) {
    String[] storage = (String[]) data.getStorage();
    int max_len = 0;
    for (String s : storage) {
      max_len = Math.max(max_len, s.length());
    }
    if (max_len > 1)
      return data;

    // below is the length=1 barfalloney
    int count = 0;
    int n = (int) data.getSize();
    char[] charStorage = new char[n];
    for (String s : storage) {
      if (s.length() > 0)
        charStorage[count++] = s.charAt(0);
    }

    // return data thats been changed to chars
    return Array.factory(DataType.CHAR, data.getShape(), charStorage);
  }

  private Array convertStringArrayToChar(DArray dv, Variable ncVar, List<Range> section) {
    opendap.dap.PrimitiveVector pv = dv.getPrimitiveVector();
    BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector) pv;
    int nStrings = btpv.getLength();

    if (section == null)
      section = ncVar.getRanges();
    int[] shape = new Section(section).getShape();
    int total = (int) Index.computeSize(shape);

    //// deal with strlen = 1 silliness
    int max_len = 0;
    for (int i = 0; i < nStrings; i++) {
      DString bb = (DString) btpv.getValue(i);
      String val = bb.getValue();
      max_len = Math.max(max_len, val.length());
    }
    if (max_len == 1) {
      char[] charStorage = new char[total];
      for (int i = 0; i < nStrings; i++) {
        DString bb = (DString) btpv.getValue(i);
        String val = bb.getValue();
        charStorage[i] = (val.length() > 0) ? val.charAt(0) : 0;
      }
      // return data thats been changed to chars
      return Array.factory(DataType.CHAR, shape, charStorage);
    }

    //// this is the "normal case", where String[rank] converted to char[rank+1]

    int[] varShape = ncVar.getShape();
    int strLen = varShape[ncVar.getRank() - 1];

    char[] storage = new char[total];
    int pos = 0;
    for (int i = 0; i < nStrings; i++) {
      DString bb = (DString) btpv.getValue(i);
      String val = bb.getValue();
      int len = Math.min(val.length(), strLen);
      for (int k = 0; k < len; k++)
        storage[pos + k] = val.charAt(k);
      pos += strLen;
    }

    return Array.factory(DataType.CHAR, shape, storage);
  }

  private Array convertStringToChar(Array data, Variable ncVar) {
    String s = (String) data.getObject(Index.scalarIndexImmutable);

    int total = (int) ncVar.getSize();
    char[] storage = new char[total];

    int len = Math.min(s.length(), total);
    for (int k = 0; k < len; k++)
      storage[k] = s.charAt(k);

    return Array.factory(DataType.CHAR, ncVar.getShape(), storage);
  }

  private int[] makeShape(opendap.dap.DArray dodsArray) {
    int count = dodsArray.getDimensions().size();

    int[] shape = new int[count];
    count = 0;
    for (opendap.dap.DArrayDimension dad : dodsArray.getDimensions()) {
      shape[count++] = dad.getSize();
    }

    return shape;
  }

}
