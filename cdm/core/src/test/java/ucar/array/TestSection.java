/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import org.junit.Before;
import org.junit.Test;
import ucar.nc2.write.NcdumpArray;

import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link Section} */
public class TestSection {

  int m = 4, n = 5, p = 6;
  Array<Double> A;
  Section sec;
  Array<Double> secA;

  int m1 = 2;
  int m2 = 3;
  int mlen = (m2 - m1 + 1);

  @Before
  public void setUp() throws InvalidRangeException {
    int[] shape = new int[] {m, n, p};
    double[] data = new double[m * n * p];
    int count = 0;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < p; k++) {
          data[count++] = (i * 100 + j * 10 + k);
        }
      }
    }

    A = Arrays.factory(ArrayType.DOUBLE, shape, data);
    sec = new Section(m1 + ":" + m2 + ",:,:");
    secA = Arrays.section(A, sec);
  }

  @Test
  public void testSection() {
    Section sec = A.getSection();
    assertThat(sec.getRank()).isEqualTo(3);
    assertThat(sec.getSize()).isEqualTo(m * n * p);
    int[] shape = sec.getShape();
    assertThat(shape[0]).isEqualTo(m);
    assertThat(shape[1]).isEqualTo(n);
    assertThat(shape[2]).isEqualTo(p);
  }

  @Test
  public void testSectionSpec() {
    assertThat(secA.getRank()).isEqualTo(3);
    assertThat(secA.length()).isEqualTo((mlen * n * p));
    int[] shape = secA.getShape();
    assertThat(shape[0]).isEqualTo(mlen);
    assertThat(shape[1]).isEqualTo(n);
    assertThat(shape[2]).isEqualTo(p);
  }

  @Test
  public void testSectionRead() {
    Index ima = secA.getIndex();
    for (int i = 0; i < mlen; i++) {
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < p; k++) {
          double val = secA.get(ima.set(i, j, k));
          assertThat(val).isEqualTo((i + m1) * 100 + j * 10 + k);
        }
      }
    }
  }

  @Test
  public void testIter() {
    Iterator<Double> iter = secA.iterator();
    for (int i = 0; i < mlen; i++) {
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < p; k++) {
          double val = iter.next();
          double expected = (double) (i + m1) * 100 + j * 10 + k;
          assertThat(val).isEqualTo(expected);
        }
      }
    }
  }

  @Test
  public void testSectionReduce() throws InvalidRangeException {
    Section sec2 = new Section("2,:,:");
    Array<Double> secA2 = Arrays.reduce(Arrays.section(A, sec2));
    Index ima = secA2.getIndex();

    assertThat(secA2.getRank()).isEqualTo(2);
    assertThat(secA2.length()).isEqualTo((n * p));

    int[] shape = secA2.getShape();
    assertThat(shape[0]).isEqualTo(n);
    assertThat(shape[1]).isEqualTo(p);

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        ima.set(i, j);
        double val = secA2.get(ima);
        double expected = (double) 200 + i * 10 + j;
        assertThat(val).isEqualTo(expected);
      }
    }

    Iterator<Double> iter = secA2.iterator();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        double val = iter.next();
        double expected = (double) 200 + i * 10 + j;
        assertThat(val).isEqualTo(expected);
      }
    }
  }

  @Test
  public void testSectionNoReduce() throws ucar.array.InvalidRangeException {
    Section sec2 = new Section("2,:,:");
    Array<Double> secA2 = Arrays.section(A, sec2);
    int[] shape = secA2.getShape();

    assertThat(secA2.getRank()).isEqualTo(3);
    assertThat(secA2.length()).isEqualTo((n * p));

    Index ima = secA2.getIndex();
    assertThat(shape[0]).isEqualTo(1);
    assertThat(shape[1]).isEqualTo(n);
    assertThat(shape[2]).isEqualTo(p);

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        ima.set(0, i, j);
        double val = secA2.get(ima);
        assertThat(val).isEqualTo(200 + i * 10 + j);
      }
    }

    Iterator<Double> iter = secA2.iterator();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        double val = iter.next();
        assertThat(val).isEqualTo(200 + i * 10 + j);
      }
    }
  }

  @Test
  public void testDoubleRange() throws ucar.array.InvalidRangeException {
    Section sec2 = new Section("2,:,:");
    Array<Double> secA2 = Arrays.reduce(Arrays.section(A, sec2));
    Section sec3 = new Section(":,1");
    Array<Double> secA3 = Arrays.reduce(Arrays.section(secA2, sec3));
    assertThat(secA3.getRank()).isEqualTo(1);
    assertThat(secA3.length()).isEqualTo(n);

    int[] shape = secA3.getShape();
    assertThat(shape[0]).isEqualTo(n);

    Index ima = secA3.getIndex();
    for (int j = 0; j < n; j++) {
      ima.set(j);
      double val = secA3.get(ima);
      // System.out.println("testDoubleRange read1 = "+ val);
      assertThat(val).isEqualTo(200 + j * 10 + 1);
    }

    Iterator<Double> iter = secA3.iterator();
    for (int i = 0; i < n; i++) {
      double val = iter.next();
      assertThat(val).isEqualTo(200 + i * 10 + 1);
    }
  }

  @Test
  public void testSectionAgainstOriginal() throws ucar.array.InvalidRangeException {
    Section sec2 = new Section(1 + ":" + (m - 2) + ",0:" + (n - 2) + ",1:" + (p - 1));
    Array<Double> secA2 = Arrays.reduce(Arrays.section(A, sec2));

    Index ima = secA2.getIndex();
    assertThat(secA2.getRank()).isEqualTo(3);
    assertThat(secA2.length()).isEqualTo((m - 2) * (n - 1) * (p - 1));

    int[] shape = secA2.getShape();
    assertThat(shape[0]).isEqualTo(m - 2);
    assertThat(shape[1]).isEqualTo(n - 1);
    assertThat(shape[2]).isEqualTo(p - 1);

    Index imaOrg = A.getIndex();
    for (int i = 0; i < m - 2; i++) {
      for (int j = 0; j < n - 1; j++) {
        for (int k = 0; k < p - 1; k++) {
          double val = secA2.get(ima.set(i, j, k));
          double valOrg = A.get(imaOrg.set(i + 1, j, k + 1));
          assertThat(val).isEqualTo(valOrg);
        }
      }
    }
  }

  @Test
  public void testSlice() throws InvalidRangeException {
    Array<Double> secA2 = Arrays.slice(A, 0, 2);

    assertThat(secA2.getRank()).isEqualTo(2);
    assertThat(secA2.length()).isEqualTo((n * p));
    int[] shape = secA2.getShape();
    assertThat(shape[0]).isEqualTo(n);
    assertThat(shape[1]).isEqualTo(p);

    Index ima = secA2.getIndex();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        ima.set(i, j);
        double val = secA2.get(ima);
        assertThat(val).isEqualTo(200 + i * 10 + j);
      }
    }

    Iterator<Double> iter = secA2.iterator();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p; j++) {
        double val = iter.next();
        assertThat(val).isEqualTo(200 + i * 10 + j);
      }
    }
  }

  @Test
  public void testSectionCopy() throws ucar.array.InvalidRangeException {
    Section sec2 = new Section(1 + ":" + (m - 2) + ",0:" + (n - 2) + ",1:" + (p - 1));
    Array<Double> secA2 = Arrays.section(A, sec2);

    // LOOK copy the data so that physical order is the same as * logical order
    // Array<Double> secA3 = Arrays.copy(secA2, sec2);
    // ucar.ma2.ArrayDouble secA2 = (ucar.ma2.ArrayDouble) A.section(new Section(1 + ":" + (m - 2) + ",0:" + (n - 2) +
    // ",1:" + (p - 1)).getRanges()).copy();

    Index imaOrg = A.getIndex();
    Index ima = secA2.getIndex();

    assertThat(secA2.getRank()).isEqualTo(3);
    assertThat(secA2.length()).isEqualTo((m - 2) * (n - 1) * (p - 1));

    int[] shape = secA2.getShape();
    assertThat(shape[0]).isEqualTo(m - 2);
    assertThat(shape[1]).isEqualTo(n - 1);
    assertThat(shape[2]).isEqualTo(p - 1);

    for (int i = 0; i < m - 2; i++) {
      for (int j = 0; j < n - 1; j++) {
        for (int k = 0; k < p - 1; k++) {
          double val = secA2.get(ima.set(i, j, k));
          // LOOK secA2.setDouble(ima, 0.0);
          double valOrg = A.get(imaOrg.set(i + 1, j, k + 1));
          assertThat(val).isEqualTo(valOrg);
        }
      }
    }
  }

  @Test
  public void testIntersect() throws ucar.array.InvalidRangeException {
    Section have = new Section("101:101,0:1919");
    Section want = new Section("100:229:3,100:191:7");

    boolean b = have.intersects(want);
    assertThat(b).isTrue();

    b = have.intersects(new Section("100:100,100:191:7"));
    assertThat(b).isFalse();
  }

  @Test
  public void testIntersectWithStrides() throws ucar.array.InvalidRangeException {
    Section have = new Section("121:121:1,0:1919:1");
    Section want = new Section("100:229:3,100:191:7");
    Section intersectCorrect = new Section("121:121:3,100:191:7");
    Section intersect = want.intersect(have);
    assertThat(intersect).isEqualTo(intersectCorrect);

    int offset = want.offset(intersect);
    assertThat(offset).isEqualTo(98);
  }

  @Test
  public void testSlice3D() throws InvalidRangeException {
    Array<Double> a3 = Arrays.makeArray(new int[] {10, 10, 10}, 1000, 0.0, 1.0);
    Array<Double> a2 = Arrays.slice(a3, 0, 1);
    Array<Double> a1 = Arrays.slice(a2, 0, 1);

    System.out.printf("a1= %f%n", a1.get(0));
    System.out.printf("a2= %f%n", a2.get(1, 0));
    System.out.printf("a2= %f%n", a3.get(1, 1, 0));
    assertThat(a1.get(0)).isEqualTo(a2.get(1, 0));
    assertThat(a1.get(0)).isEqualTo(a3.get(1, 1, 0));
  }

  @Test
  public void testFill() throws InvalidRangeException {
    int[] shape = new int[] {m, n, p};
    assertThat(Section.fill(null, shape)).isEqualTo(new Section(shape));
    assertThat(Section.fill(Section.SCALAR, new int[0])).isEqualTo(Section.SCALAR);
    assertThat(new Section().getRank()).isEqualTo(0);
    try {
      assertThat(Section.fill(new Section(), shape).getRank()).isEqualTo(0);
      fail();
    } catch (Exception e) {
      // expected
    }

    Section s = Section.builder().appendRange(m).appendRange(null).appendRange(m).build();
    Section sf = Section.fill(s, shape);
    assertThat(sf.getRank()).isEqualTo(3);
    assertThat(sf.getRange(0)).isEqualTo(new Range(m));
    assertThat(sf.getRange(1)).isEqualTo(new Range(n));
    assertThat(sf.getRange(2)).isEqualTo(new Range(m));
  }

  @Test
  public void testConstruct1() throws InvalidRangeException {
    int[] shape = new int[] {3, 0, -1};
    Section s = new Section(shape);
    assertThat(s.getRank()).isEqualTo(3);
    assertThat(s.getRange(0)).isEqualTo(new Range(3));
    assertThat(s.getRange(1)).isEqualTo(Range.EMPTY);
    assertThat(s.getRange(2)).isEqualTo(Range.VLEN);
  }

  @Test
  public void testConstruct2() throws InvalidRangeException {
    int[] origin = new int[] {1, 1, 1};
    int[] shape = new int[] {3, 0, -1};
    Section s = new Section(origin, shape);
    assertThat(s.getRank()).isEqualTo(3);
    assertThat(s.getRange(0)).isEqualTo(new Range(1, 3));
    assertThat(s.getRange(1)).isEqualTo(Range.EMPTY);
    assertThat(s.getRange(2)).isEqualTo(Range.VLEN);
  }

  @Test
  public void testConstruct3() throws InvalidRangeException {
    Section s = Section.builder().appendRange(null).appendRange(null).appendRange(m).appendRange(null).build();
    int[] shape = new int[] {3, 0, -1, -1};
    Section sf = new Section(s.getRanges(), shape);
    assertThat(sf.getRank()).isEqualTo(4);
    assertThat(sf.getRange(0)).isEqualTo(new Range(3));
    assertThat(sf.getRange(1)).isEqualTo(Range.EMPTY);
    assertThat(sf.getRange(2)).isEqualTo(new Range(m));
    assertThat(sf.getRange(3)).isEqualTo(Range.VLEN);
  }

  @Test
  public void testCompact() throws InvalidRangeException {
    Section s = Section.builder().appendRange(null).appendRange(1, 10, 2).appendRange(1).build();
    Section sc = s.compact();
    assertThat(sc.getRank()).isEqualTo(3);
    assertThat(sc.getRange(0)).isNull();
    assertThat(sc.getRange(1)).isEqualTo(new Range(0, 4));
    assertThat(sc.getRange(2)).isEqualTo(Range.SCALAR);
  }

  @Test
  public void testSectionSpecRoundtrip() throws InvalidRangeException {
    String spec = 1 + ":" + (m - 2) + ",0:" + (n - 2) + ",1:" + (p - 1) + ":2";
    Section s = new Section(spec);
    assertThat(s.toString()).isEqualTo(spec);
    assertThat(s.show()).isEqualTo("1:2\n0:3\n1:5:2");
  }

  @Test
  public void testShow() throws InvalidRangeException {
    String spec = 1 + ":" + (m - 2) + ",0:" + (n - 2) + ",1:" + (p - 1);
    Section s = new Section(spec);
    assertThat(s.show()).isEqualTo("1:2\n0:3\n1:5");

    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.show()).isEqualTo("0:98\nanda=99:100");
  }

  @Test
  public void testIsVariableLen() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.isVariableLength()).isFalse();

    Section s = Section.builder().appendRange(-1).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s.isVariableLength()).isTrue();
  }

  @Test
  public void testGetShape() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.getShape()).isEqualTo(new int[] {99, 2});

    Section s = Section.builder().appendRange(-1).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s.getShape()).isEqualTo(new int[] {-1, 2});
  }

  @Test
  public void testGetOrigin() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.getOrigin()).isEqualTo(new int[] {0, 99});

    Section s = Section.builder().appendRange(-1).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s.getOrigin()).isEqualTo(new int[] {0, 99});
  }

  @Test
  public void testGetStride() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.getStride()).isEqualTo(new int[] {1, 1});

    Section s = Section.builder().appendRange(-1).appendRange(new Range(99, 100, 17)).build();
    assertThat(s.getStride()).isEqualTo(new int[] {1, 17});
  }

  @Test
  public void testFindRange() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(new Range("anda", 99, 100)).build();
    assertThat(s2.find("anaa")).isNull();
    assertThat(s2.find("anda")).isNotNull();
  }

  @Test
  public void testEquivilent() throws InvalidRangeException {
    Section s2 = Section.builder().appendRange(99).appendRange(null).build();
    assertThat(s2.equivalent(new int[] {99, 100})).isTrue();

    Section s = Section.builder().appendRange(1, 99).appendRange(null).build();
    assertThat(s.equivalent(new int[] {99, 100})).isFalse();
  }

  @Test
  public void testRoundtripBuilder() throws InvalidRangeException {
    Section s = Section.builder().appendRange(1, 99).appendRange(null).build();
    Section roundtrip = s.toBuilder().build();
    assertThat(roundtrip).isEqualTo(s);
    assertThat(roundtrip.hashCode()).isEqualTo(s.hashCode());
  }

  @Test
  public void testBuilder() throws InvalidRangeException {
    Section.Builder sb = Section.builder();
    sb.appendRange("test", 1, 2, 3);
    sb.appendRange(null);
    sb.appendRanges(new int[] {1, -1});

    Section s = sb.build();
    assertThat(s.getRank()).isEqualTo(4);
    assertThat(s.getRange(1)).isNull();
    assertThat(s.getRange(2)).isEqualTo(Range.SCALAR);
    assertThat(s.getRange(3)).isEqualTo(Range.VLEN);

    sb.replaceRange(1, new Range(44));
    sb.removeVlen();
    sb.insertRange(1, Range.SCALAR);

    s = sb.build();
    assertThat(s.getRank()).isEqualTo(4);
    assertThat(s.getRange(1)).isEqualTo(Range.SCALAR);
    assertThat(s.getRange(2).length()).isEqualTo(44);
    assertThat(s.getRange(3)).isEqualTo(Range.SCALAR);
  }

  @Test
  public void testComposeSliceWithSection() throws ucar.array.InvalidRangeException {
    // 1:1,0:3,1:5
    Section sec1 = new Section("1,0:3,1:5");
    Array<Double> secA1 = Arrays.section(A, sec1);
    assertThat(secA1.getRank()).isEqualTo(3);
    assertThat(secA1.length()).isEqualTo(20);

    int[] shape = secA1.getShape();
    assertThat(shape[0]).isEqualTo(1);
    assertThat(shape[1]).isEqualTo(4);
    assertThat(shape[2]).isEqualTo(5);

    Array<Double> secA2 = Arrays.slice(secA1, 1, 2);
    assertThat(secA2.getRank()).isEqualTo(2);
    assertThat(secA2.length()).isEqualTo(5);

    shape = secA2.getShape();
    assertThat(shape[0]).isEqualTo(1);
    assertThat(shape[1]).isEqualTo(5);

    Index imaOrg = A.getIndex();
    Index ima = secA2.getIndex();
    for (int k = 0; k < p - 1; k++) {
      double val = secA2.get(ima.set(0, k));
      double valOrg = A.get(imaOrg.set(1, 2, k + 1));
      assertThat(val).isEqualTo(valOrg);
    }

    System.out.printf("array((i * 100 + j * 10 + k))%s%n", NcdumpArray.printArray(A));
    System.out.printf("section(%s) %s%n", sec1, NcdumpArray.printArray(secA1));
    System.out.printf("slice(1,2) %s%n", NcdumpArray.printArray(secA2));

    double[] expected = new double[] {121.0, 122.0, 123.0, 124.0, 125.0};
    int count = 0;
    for (double val : secA2) {
      assertThat(val).isEqualTo(expected[count++]);
    }
  }

  @Test
  public void testCompose() throws InvalidRangeException {
    Section sa = Section.builder().appendRange(2).appendRange(3).build();
    assertThat(sa.compose(null)).isEqualTo(sa);

    Section sb = Section.builder().appendRange(null).appendRange(2).build();
    Section expected = Section.builder().appendRange(2).appendRange(2).build();
    assertThat(sa.compose(sb)).isEqualTo(expected);
  }

}
