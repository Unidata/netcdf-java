/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureMA;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/**
 * Enhance StructureData, for both StructureDS and SequenceDS.
 *
 * @deprecated use ucar.internal.dataset.StructureDataArrayEnhancer
 */
@Deprecated
class StructureDataEnhancer {
  private static final Logger logger = LoggerFactory.getLogger(StructureDataEnhancer.class);
  private final StructureEnhanced topStructure;

  StructureDataEnhancer(StructureEnhanced topStructure) {
    this.topStructure = topStructure;
  }

  // possible things needed:
  // 1) enum/scale/offset/missing/unsigned conversion
  // 2) name, info change
  // 3) variable with cached data added to StructureDS through NcML

  ArrayStructure enhance(ArrayStructure orgAS, Section section) throws IOException {
    // Can have length 0, if unlimited dimension
    if ((!(orgAS instanceof ArraySequence) && orgAS.getSize() == 0)
        || !convertNeeded(topStructure, orgAS.getStructureMembers())) {
      // name, info change only LOOK what is this?
      convertMemberInfo(topStructure, orgAS.getStructureMembers());
      return orgAS;
    }

    // LOOK! converting to ArrayStructureMA
    // do any enum/scale/offset/missing/unsigned conversions
    ArrayStructure newAS = ArrayStructureMA.factoryMA(orgAS);
    for (StructureMembers.Member m : newAS.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) topStructure.findVariable(m.getName());
      if ((v2 == null) && (topStructure.getOriginalVariable() != null)) // these are from orgVar - may have been renamed
        v2 = findVariableFromOrgName(topStructure, m.getName());
      if (v2 == null)
        continue;

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        if (vds.needConvert()) {
          Array mdata = newAS.extractMemberArray(m);
          // mdata has not yet been enhanced, but vds would *think* that it has been if we used the 1-arg version of
          // VariableDS.convert(). So, we use the 2-arg version to explicitly request enhancement.
          mdata = vds.convert(mdata, vds.getEnhanceMode());
          newAS.setMemberArray(m, mdata);
        }

      } else if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        if (convertNeeded(innerStruct, null)) {

          if (innerStruct.getDataType() == DataType.SEQUENCE) {
            ArrayObject.D1 seqArray = (ArrayObject.D1) newAS.extractMemberArray(m);
            ArrayObject.D1 newSeq =
                (ArrayObject.D1) Array.factory(DataType.SEQUENCE, new int[] {(int) seqArray.getSize()});
            m.setDataArray(newSeq); // put back into member array

            // wrap each Sequence
            for (int i = 0; i < seqArray.getSize(); i++) {
              ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
              newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
            }

            // non-Sequence Structures
          } else {
            Array mdata = newAS.extractMemberArray(m);
            StructureDataEnhancer innerEnhancer = new StructureDataEnhancer(innerStruct);
            mdata = innerEnhancer.enhance((ArrayStructure) mdata, null);
            newAS.setMemberArray(m, mdata);
          }

        }

        // always convert the inner StructureMembers
        convertMemberInfo(innerStruct, m.getStructureMembers());
      }
    }

    StructureMembers sm = newAS.getStructureMembers();
    convertMemberInfo(topStructure, sm);

    // check for variables that have been added by NcML
    for (Variable v : topStructure.getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = topStructure.getParentGroup().findVariableLocal(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(section);
          StructureMembers.Member m =
              sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          newAS.setMemberArray(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return newAS;
  }

  /* Convert original structureData to one that conforms to the enhanced Structure in topStructure. */
  StructureData enhance(StructureData orgData, int recno) throws IOException {
    if (!convertNeeded(topStructure, orgData.getStructureMembers())) {
      // name, info change only
      convertMemberInfo(topStructure, orgData.getStructureMembers());
      return orgData;
    }

    // otherwise we create a new StructureData and convert to it. expensive
    StructureMembers smResult = orgData.getStructureMembers().toBuilder(false).build();
    StructureDataW result = new StructureDataW(smResult);

    for (StructureMembers.Member m : orgData.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) topStructure.findVariable(m.getName());
      if ((v2 == null) && (topStructure.getOriginalVariable() != null)) // why ?
        v2 = findVariableFromOrgName(topStructure, m.getName());
      if (v2 == null) {
        findVariableFromOrgName(topStructure, m.getName()); // debug
        logger.warn("StructureDataDS.convert Cant find member " + m.getName());
        continue;
      }
      StructureMembers.Member mResult = smResult.findMember(m.getName());

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        Array mdata = orgData.getArray(m);

        if (vds.needConvert())
          // mdata has not yet been enhanced, but vds would *think* that it has been if we used the 1-arg version of
          // VariableDS.convert(). So, we use the 2-arg version to explicitly request enhancement.
          mdata = vds.convert(mdata, vds.getEnhanceMode());

        result.setMemberData(mResult, mdata);
      }

      // recurse into sub-structures
      if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;

        if (innerStruct.getDataType() == DataType.SEQUENCE) {
          Array a = orgData.getArray(m);

          if (a instanceof ArrayObject.D1) { // LOOK when does this happen vs ArraySequence?
            ArrayObject.D1 seqArray = (ArrayObject.D1) a;
            ArrayObject.D1 newSeq =
                (ArrayObject.D1) Array.factory(DataType.SEQUENCE, new int[] {(int) seqArray.getSize()});
            mResult.setDataArray(newSeq); // put into result member array

            for (int i = 0; i < seqArray.getSize(); i++) {
              ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
              newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
            }

          } else {
            ArraySequence seqArray = (ArraySequence) a;
            result.setMemberData(mResult, new SequenceConverter(innerStruct, seqArray)); // wrap in converter
          }

          // non-Sequence Structures
        } else {
          Array mdata = orgData.getArray(m);
          StructureDataEnhancer innerEnhancer = new StructureDataEnhancer(innerStruct);
          mdata = innerEnhancer.enhance((ArrayStructure) mdata, null);
          result.setMemberData(mResult, mdata);
        }

        // always convert the inner StructureMembers
        convertMemberInfo(innerStruct, mResult.getStructureMembers());
      }
    }

    StructureMembers sm = result.getStructureMembers();
    convertMemberInfo(topStructure, sm);

    // check for variables that have been added by NcML
    for (Variable v : topStructure.getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = topStructure.getParentGroup().findVariableLocal(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(Section.builder().appendRange(recno, recno).build());
          StructureMembers.Member m =
              sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          result.setMemberData(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return result;
  }

  // the wrapper StructureMembers must be converted to correspond to the wrapper Structure
  private static void convertMemberInfo(StructureEnhanced structureEn, StructureMembers wrapperSm) {
    for (StructureMembers.Member m : wrapperSm.getMembers()) {
      Variable v = structureEn.findVariable(m.getName());
      if ((v == null) && (structureEn.getOriginalVariable() != null)) // may have been renamed
        v = (Variable) findVariableFromOrgName(structureEn, m.getName());

      // a section will have missing variables LOOK wrapperSm probably wrong in that case
      if (v != null) {
        m.setVariableInfo(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType());
      }

      // nested structures
      if (v instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v;
        convertMemberInfo(innerStruct, m.getStructureMembers());
      }

    }
  }

  // look for the top variable that has an orgVar with the wanted orgName
  private static VariableEnhanced findVariableFromOrgName(StructureEnhanced structureEn, String orgName) {
    for (Variable vTop : structureEn.getVariables()) {
      Variable v = vTop;
      while (v instanceof VariableEnhanced) {
        VariableEnhanced ve = (VariableEnhanced) v;
        if ((ve.getOriginalName() != null) && (ve.getOriginalName().equals(orgName)))
          return (VariableEnhanced) vTop;
        v = ve.getOriginalVariable();
      }
    }
    return null;
  }

  // verify that the variable has data in the data array
  private static boolean varHasData(Variable v, StructureMembers sm) {
    if (sm.findMember(v.getShortName()) != null)
      return true;
    while (v instanceof VariableEnhanced) {
      VariableEnhanced ve = (VariableEnhanced) v;
      if (sm.findMember(ve.getOriginalName()) != null)
        return true;
      v = ve.getOriginalVariable();
    }
    return false;
  }

  // is conversion needed?

  private static boolean convertNeeded(StructureEnhanced structureEn, StructureMembers smData) {

    for (Variable v : structureEn.getVariables()) {
      if (v instanceof VariableDS) {
        VariableDS vds = (VariableDS) v;
        if (vds.needConvert())
          return true;
      } else if (v instanceof StructureDS) {
        StructureDS nested = (StructureDS) v;
        if (convertNeeded(nested, null))
          return true;
      }

      // a variable with no data in the underlying smData
      if ((smData != null) && !varHasData(v, smData))
        return true;
    }

    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class SequenceConverter extends ArraySequence {
    StructureDS orgStruct;
    ArraySequence orgSeq;

    SequenceConverter(StructureDS orgStruct, ArraySequence orgSeq) {
      super(orgSeq.getStructureMembers(), orgSeq.getShape());
      this.orgStruct = orgStruct;
      this.orgSeq = orgSeq;
      this.nelems = orgSeq.getStructureDataCount();

      // copy and convert the members
      members = orgSeq.getStructureMembers().toBuilder(false).build();
      convertMemberInfo(orgStruct, members);
    }

    @Override
    public StructureDataIterator getStructureDataIterator() { // throws java.io.IOException {
      return new StructureDataIteratorEnhanced(orgStruct, orgSeq.getStructureDataIterator());
    }
  }

  static class StructureDataIteratorEnhanced implements StructureDataIterator {
    private StructureDataIterator orgIter;
    private final StructureDataEnhancer enhancer;
    private int count;

    StructureDataIteratorEnhanced(StructureEnhanced newStruct, StructureDataIterator orgIter) {
      this.enhancer = new StructureDataEnhancer(newStruct);
      this.orgIter = orgIter;
    }

    @Override
    public boolean hasNext() throws IOException {
      return orgIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      StructureData sdata = orgIter.next();
      return enhancer.enhance(sdata, count++);
    }

    @Override
    public void setBufferSize(int bytes) {
      orgIter.setBufferSize(bytes);
    }

    @Override
    public StructureDataIterator reset() {
      orgIter = orgIter.reset();
      return (orgIter == null) ? null : this;
    }

    @Override
    public int getCurrentRecno() {
      return orgIter.getCurrentRecno();
    }

    @Override
    public void close() {
      orgIter.close();
    }
  }

}
