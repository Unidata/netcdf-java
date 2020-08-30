/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import ucar.nc2.write.Ncdump;

/**
 * Test nc2 dods in the JUnit framework.
 *
 * Dataset {
 * Sequence {
 * Int32 age;
 * Sequence {
 * Int16 foo;
 * } stuff;
 * } person1;
 * } NestedSequences;
 * ---------------------------------------------
 * person1.age, person1.foo
 * 1, person1.stuff.foo
 * 0
 * 16
 * 32
 * 48
 * 64
 * 
 * 2, person1.stuff.foo
 * 80
 * 96
 * 112
 * 128
 * 144
 * 
 * 3, person1.stuff.foo
 * 160
 * 176
 * 192
 * 208
 * 224
 * 
 * 5, person1.stuff.foo
 * 240
 * 256
 * 272
 * 288
 * 304
 * 
 * 8, person1.stuff.foo
 * 320
 * 336
 * 352
 * 368
 * 384
 */
public class TestDODSnestedSequence {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNestedSequenceParent() throws IOException {
    DodsNetcdfFile dodsfile = TestDODSRead.open("NestedSeq");

    Variable v = dodsfile.findVariable("person1");
    assert null != v;
    assert v instanceof Structure;
    assert v instanceof DodsStructure;
    assert v.getRank() == 1;
    assert v.isVariableLength();

    Array a = v.read(); // DODSNetcdfFile.readFromServer = <person1>
    assert a.getRank() == 1;
    assert a.getSize() == 5 : a.getSize();
    // NCdump.printArray(a, "person",System.out,null);

    int count = 0;
    int fib = 1, prev = 1;
    IndexIterator iter = a.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      StructureMembers.Member stuff = data.findMember("stuff");
      assert stuff != null;

      assert data.findMember("age") != null;
      int agev = data.getScalarInt("age");
      assert agev == fib : fib + "!=" + agev;

      count++;
      int hold = fib;
      fib += prev;
      prev = hold;
    }

  }

  @Test
  @Ignore("error reading inner sequence: is this legal? I dont think so")
  public void testNestedSequence() throws IOException {
    DodsNetcdfFile dodsfile = TestDODSRead.open("NestedSeq");

    Variable v = dodsfile.findVariable("person1.stuff");
    assert null != v;
    assert v instanceof Structure;
    assert v instanceof DodsStructure;
    assert v.getRank() == 1;
    assert v.isVariableLength();

    /*
     * Array sa = v.readAllStructures(null, true); // DODSNetcdfFile.readFromServer = <person1.stuff>
     * assert sa.getRank() == 1;
     * assert sa.getSize() == 25 : sa.getSize();
     * 
     * int count = 0;
     * IndexIterator iter = sa.getIndexIterator();
     * while (iter.hasNext()) {
     * StructureData data = (StructureData) iter.next();
     * 
     * assert data.findMember("foo") != null;
     * 
     * int foo = data.getScalarInt("foo");
     * assert foo == count*16 : foo;
     * count++;
     * }
     */

    Array a = v.read(); // // DODSNetcdfFile.readFromServer = <person1.stuff>
    assert a.getRank() == 1;
    assert a.getSize() == 25 : a.getSize();

    logger.debug(Ncdump.printArray(a, "stuff", null));

    int count = 0;
    IndexIterator iter = a.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      assert data.findMember("foo") != null;

      int foo = data.getScalarInt("foo");
      assert foo == count * 16 : foo;

      count++;
    }

  }

  @Test
  public void testCE() throws IOException {
    DodsNetcdfFile dodsFile = TestDODSRead.open("NestedSeq2");
    Variable outerSequence = dodsFile.findVariable("person1");

    String CE = "person1.age,person1.stuff&person1.age=3";
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    assert as.getSize() == 1;

    StructureData outerStructure = as.getStructureData(0);
    StructureMembers outerMembers = outerStructure.getStructureMembers();
    assert outerMembers.findMember("age") != null;
    assert outerMembers.findMember("stuff") != null;

    // get at the inner sequence
    ArrayStructure asInner = (ArrayStructure) outerStructure.getArray("stuff");
    StructureMembers innerMembers = asInner.getStructureMembers();
    assert innerMembers.findMember("foo") != null;
    assert innerMembers.findMember("bar") != null;

    assertThat(asInner.getSize()).isEqualTo(1);

    StructureData firstInner = asInner.getStructureData(0);
    StructureMembers firstMembers = firstInner.getStructureMembers();
    assert firstMembers.findMember("foo") != null;
    assert firstMembers.findMember("bar") != null;
  }

}
