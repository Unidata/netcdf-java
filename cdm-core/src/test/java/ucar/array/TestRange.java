/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.collect.Iterables;
import org.junit.Test;

import java.util.Random;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link ucar.array.Range} */
public class TestRange {
  Random random = new Random(System.currentTimeMillis());

  @Test
  public void testNonNegetive() {
    assertThrows(InvalidRangeException.class, () -> new Range(-10, 10, 9)).getMessage()
        .contains("first (-10) must be >= 0");

    assertThrows(InvalidRangeException.class, () -> new Range(0, -10, 9)).getMessage()
        .contains("last (-10) must be >= first (0)");
  }

  @Test
  public void testBasics() throws InvalidRangeException {
    Range r1 = new Range("test", 42, 47, 3);
    Range r2 = r1.copyWithName("test");
    assertThat(r1).isEqualTo(r2);
    assertThat(r1.hashCode()).isEqualTo(r2.hashCode());

    Range r1p = new Range("test", 42, 47, 3);
    assertThat(r1).isEqualTo(r1p);
    assertThat(r1.hashCode()).isEqualTo(r1p.hashCode());

    Range s1 = Range.make(42, 47);
    Range s2 = Range.make(42, 47);
    assertThat(s1).isEqualTo(s2);
    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());

    Range t1 = Range.make("tt", 47);
    Range t2 = Range.make("tt", 47);
    assertThat(t1).isEqualTo(t2);
    assertThat(t1.hashCode()).isEqualTo(t2.hashCode());

    Range r = new Range(42);
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(42);
    assertThat(r.last()).isEqualTo(41);
    assertThat(r.name()).isNull();
    assertThat(Iterables.size(r)).isEqualTo(42);
  }

  @Test
  public void testMakeWithLen() {
    Range r = Range.make("test", 42);
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(42);
    assertThat(r.last()).isEqualTo(41);
    assertThat(r.name()).isEqualTo("test");
    assertThat(Iterables.size(r)).isEqualTo(42);

    int count = 0;
    for (int idx : r) {
      count++;
    }
    assertThat(count).isEqualTo(42);

    assertThat(r.copyWithName("FAKE").length()).isEqualTo(42);
    assertThat(r.copyWithName("FAKE").name()).isEqualTo("FAKE");
  }

  @Test
  public void testMakeWithFirstLast() {
    Range r = Range.make(4, 10);
    assertThat(r.first()).isEqualTo(4);
    assertThat(r.last()).isEqualTo(10);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(7);
    assertThat(r.name()).isNull();
    assertThat(Iterables.size(r)).isEqualTo(r.length());

    int count = 0;
    for (int idx : r) {
      count++;
    }
    assertThat(count).isEqualTo(r.length());

    assertThat(r.copyWithName("FAKE").length()).isEqualTo(r.length());
    assertThat(r.copyWithName("FAKE").name()).isEqualTo("FAKE");
  }

  @Test
  public void testCopyWithStride() throws InvalidRangeException {
    Range r = Range.make("test", 42);

    Range rs = r.copyWithStride(6);
    assertThat(rs.stride()).isEqualTo(6);
    assertThat(rs.first()).isEqualTo(r.first());
    assertThat(rs.last()).isEqualTo(36);
    assertThat(rs.length()).isEqualTo(r.length() / 6);

    rs = r.copyWithStride(66);
    assertThat(rs.stride()).isEqualTo(66);
    assertThat(rs.first()).isEqualTo(r.first());
    assertThat(rs.last()).isEqualTo(r.first());
    assertThat(rs.length()).isEqualTo(1);

    assertThrows(InvalidRangeException.class, () -> r.copyWithStride(-1));
  }

  @Test
  public void testCopyWithStride2() throws InvalidRangeException {
    Range r0 = Range.make(0, 10, 3);
    assertThat(r0.stride()).isEqualTo(3);
    assertThat(r0.first()).isEqualTo(0);
    assertThat(r0.last()).isEqualTo(9);
    assertThat(r0.length()).isEqualTo(4);

    Range r = Range.make("test", 10);
    Range rs = r.copyWithStride(3);
    assertThat(r0).isEqualTo(rs);
  }

  @Test
  public void testEmpty() {
    Range r = Range.EMPTY;
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(0);
    assertThat(r.last()).isEqualTo(-1);
    assertThat(r.name()).isEqualTo("EMPTY");
    assertThat(Iterables.size(r)).isEqualTo(0);

    int count = 0;
    for (int idx : r) {
      count++;
    }
    assertThat(count).isEqualTo(0);

    // cant copy empty
    assertThrows(RuntimeException.class, () -> r.copyWithName("FAKE"));
  }

  @Test
  public void testScalar() {
    Range r = Range.SCALAR;
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(1);
    assertThat(r.last()).isEqualTo(0);
    assertThat(r.name()).isEqualTo("SCALAR");
    assertThat(Iterables.size(r)).isEqualTo(1);

    int count = 0;
    for (int idx : r) {
      count++;
    }
    assertThat(count).isEqualTo(1);
    assertThat(r.copyWithName("FAKE").length()).isEqualTo(1);
    assertThat(r.copyWithName("FAKE").name()).isEqualTo("FAKE");
  }

  @Test
  public void testVlen() {
    Range r = Range.VLEN;
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(-1);
    assertThat(r.last()).isEqualTo(-2);
    assertThat(r.name()).isEqualTo("VLEN");
    assertThat(Iterables.size(r)).isEqualTo(0);

    int count = 0;
    for (int idx : r) {
      count++;
    }
    assertThat(count).isEqualTo(0);

    // cant copy vlen
    assertThrows(RuntimeException.class, () -> r.copyWithName("FAKE"));
  }

  @Test
  public void testStride() throws InvalidRangeException {
    int nx = 1237;
    Range r = new Range(0, nx - 1, 9);
    System.out.printf("%s%n", r);
    System.out.printf("%d %d %d %d%n", r.first(), r.last(), r.stride(), r.length());
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.last()).isEqualTo(1233);
    assertThat(r.stride()).isEqualTo(9);
    assertThat(r.length()).isEqualTo(138);

    Section s = new Section(r, r);
    Section.Iterator iter = s.getIterator(new int[] {nx, nx});
    int[] iterResult = new int[2];
    int count = 0;
    for (int y = r.first(); y <= r.last(); y += r.stride()) {
      for (int x = r.first(); x <= r.last(); x += r.stride()) {
        assertThat(iter.hasNext()).isTrue();
        int iterN = iter.next(iterResult);
        assertThat(iterResult[0]).isEqualTo(y);
        assertThat(iterResult[1]).isEqualTo(x);
        assertThat(iterN).isEqualTo(y * nx + x);
        count++;
      }
    }
    assertThat(138 * 138).isEqualTo(count);
  }

  @Test
  public void testContains() throws InvalidRangeException {
    int nx = 1237;
    Range r = new Range(1, nx, 1);
    assertThat(r.length()).isEqualTo(nx);
    assertThat(r.contains(-1)).isFalse();
    assertThat(r.contains(0)).isFalse();
    assertThat(r.contains(1)).isTrue();
    assertThat(r.contains(nx / 11)).isTrue();
    assertThat(r.contains(nx / 2)).isTrue();
    assertThat(r.contains(nx - 1)).isTrue();
    assertThat(r.contains(nx)).isTrue();
    assertThat(r.contains(nx + 1)).isFalse();
  }

  @Test
  public void testContainsWithStride() throws InvalidRangeException {
    int nx = 1237;
    Range r = new Range(1, nx, 2);
    assertThat(r.length()).isEqualTo(nx / 2 + 1);
    assertThat(r.contains(-1)).isFalse();
    assertThat(r.contains(0)).isFalse();
    assertThat(r.contains(1)).isTrue();
    assertThat(r.contains(nx / 11)).isFalse();
    assertThat(r.contains(1 + nx / 11)).isTrue();
    assertThat(r.contains(1 + nx / 2)).isTrue();
    assertThat(r.contains(nx - 1)).isFalse();
    assertThat(r.contains(nx)).isTrue();
    assertThat(r.contains(nx + 1)).isFalse();
  }

  @Test
  public void testCompose() throws InvalidRangeException {
    int nx = 1237;
    Range base1 = new Range(0, nx);
    assertThat(base1.first()).isEqualTo(0);
    assertThat(base1.last()).isEqualTo(nx);
    assertThat(base1.stride()).isEqualTo(1);
    assertThat(base1.length()).isEqualTo(nx + 1);

    Range r = base1.compose(new Range(0, nx));
    assertThat(r).isEqualTo(base1);

    r = base1.compose(new Range(0, nx, 2));
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.last()).isEqualTo(nx - 1);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo((nx + 1) / 2);

    Range base2 = new Range(0, nx, 2);
    assertThat(base2.first()).isEqualTo(0);
    assertThat(base2.last()).isEqualTo(nx - 1);
    assertThat(base2.stride()).isEqualTo(2);
    assertThat(base2.length()).isEqualTo((nx + 1) / 2);

    r = base2.compose(new Range(0, nx / 2, 2));
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.last()).isEqualTo(nx - 1);
    assertThat(r.stride()).isEqualTo(4);
    assertThat(r.length()).isEqualTo(1 + nx / 4);

    r = base2.compose(new Range(0, 20));
    assertThat(r.first()).isEqualTo(0);
    assertThat(r.last()).isEqualTo(2 * 20);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(21);

    r = base2.compose(new Range(10, 30));
    assertThat(r.first()).isEqualTo(2 * 10);
    assertThat(r.last()).isEqualTo(2 * 30);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(21);

    Range base3 = new Range(11, nx, 2);
    assertThat(base3.first()).isEqualTo(11);
    assertThat(base3.last()).isEqualTo(nx);
    assertThat(base3.stride()).isEqualTo(2);
    assertThat(base3.length()).isEqualTo(1 + (nx - 11) / 2);

    r = base3.compose(new Range(10, 30));
    assertThat(r.first()).isEqualTo(11 + 2 * 10);
    assertThat(r.last()).isEqualTo(11 + 2 * 30);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(21);

    r = base3.compose(new Range(10, 30, 3));
    assertThat(r.first()).isEqualTo(11 + 2 * 10);
    assertThat(r.length()).isEqualTo(21 / 3);
    assertThat(r.stride()).isEqualTo(6);
    assertThat(r.last()).isEqualTo(67);
  }

  @Test
  public void testComposeSlice() throws InvalidRangeException {
    int nx = 1237;
    Range base1 = new Range(0, nx);
    assertThat(base1.first()).isEqualTo(0);
    assertThat(base1.last()).isEqualTo(nx);
    assertThat(base1.stride()).isEqualTo(1);
    assertThat(base1.length()).isEqualTo(nx + 1);

    Range r = base1.compose(new Range(19, 19));
    assertThat(r.first()).isEqualTo(19);
    assertThat(r.last()).isEqualTo(19);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(1);
  }

  @Test
  public void testCompact() throws InvalidRangeException {
    int nx = 1237;
    Range r = new Range(1, nx, 2);
    assertThat(r.first()).isEqualTo(1);
    assertThat(r.last()).isEqualTo(nx);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + nx / 2);

    Range compact = r.compact();
    assertThat(compact.first()).isEqualTo(0);
    assertThat(compact.last()).isEqualTo((nx - 1) / 2);
    assertThat(compact.stride()).isEqualTo(1);
    assertThat(compact.length()).isEqualTo((nx + 1) / 2);
  }

  @Test
  public void testElementIndex() throws InvalidRangeException {
    int start = 11;
    int len = 999;
    Range r = new Range(start, start + len - 1);
    assertThat(r.first()).isEqualTo(start);
    assertThat(r.last()).isEqualTo(start + len - 1);
    assertThat(r.stride()).isEqualTo(1);
    assertThat(r.length()).isEqualTo(len);

    for (int i = 0; i < 100; i++) {
      int index = random.nextInt(r.length());
      assertThat(r.index(r.element(index))).isEqualTo(index);
    }

    for (int i = 0; i < 100; i++) {
      int elem = start + random.nextInt(r.length());
      assertThat(r.contains(elem));
      assertThat(r.element(r.index(elem))).isEqualTo(elem);
    }
  }

  @Test
  public void testElementIndexStrided() throws InvalidRangeException {
    int start = 11;
    int len = 999;
    Range r = new Range(start, start + len - 1, 5);
    assertThat(r.first()).isEqualTo(start);
    assertThat(r.last()).isEqualTo(1006);
    assertThat(r.stride()).isEqualTo(5);
    assertThat(r.length()).isEqualTo((len + 1) / 5);

    for (int i = 0; i < 100; i++) {
      int index = random.nextInt(r.length());
      assertThat(r.index(r.element(index))).isEqualTo(index);
    }

    for (int i = 0; i < 100; i++) {
      int elem = start + 5 * random.nextInt(r.length());
      assertThat(r.contains(elem));
      assertThat(r.element(r.index(elem))).isEqualTo(elem);
    }
  }

  @Test
  public void testIntersect() throws InvalidRangeException {
    int nx = 1237;
    Range base = new Range(11, nx);
    assertThat(base.first()).isEqualTo(11);
    assertThat(base.last()).isEqualTo(nx);
    assertThat(base.stride()).isEqualTo(1);
    assertThat(base.length()).isEqualTo(nx - 10);

    Range r = base.intersect(new Range(0, nx));
    assertThat(r).isEqualTo(base);

    r = base.intersect(new Range(0, nx - 11));
    assertThat(r.first()).isEqualTo(11);
    assertThat(r.last()).isEqualTo(nx - 11);
    assertThat(r.stride()).isEqualTo(base.stride());
    assertThat(r.length()).isEqualTo(nx - 21);

    r = base.intersect(new Range(7, nx - 11));
    assertThat(r.first()).isEqualTo(11);
    assertThat(r.last()).isEqualTo(nx - 11);
    assertThat(r.stride()).isEqualTo(base.stride());
    assertThat(r.length()).isEqualTo(nx - 21);

    r = base.intersect(new Range(20, nx - 11));
    assertThat(r.first()).isEqualTo(20);
    assertThat(r.last()).isEqualTo(nx - 11);
    assertThat(r.stride()).isEqualTo(base.stride());
    assertThat(r.length()).isEqualTo(nx - 30);

    r = base.intersect(new Range(20, nx + 11));
    assertThat(r.first()).isEqualTo(20);
    assertThat(r.last()).isEqualTo(nx);
    assertThat(r.stride()).isEqualTo(base.stride());
    assertThat(r.length()).isEqualTo(nx - 19);
  }

  @Test
  public void testIntersectStrided() throws InvalidRangeException {
    int nx = 1237;
    Range base = new Range(11, nx, 2);
    assertThat(base.first()).isEqualTo(11);
    assertThat(base.last()).isEqualTo(nx);
    assertThat(base.stride()).isEqualTo(2);
    assertThat(base.length()).isEqualTo(1 + (nx - 11) / 2);

    Range r = base.intersect(new Range(0, 20));
    assertThat(r.first()).isEqualTo(11);
    assertThat(r.last()).isEqualTo(19);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + (20 - 11) / 2);

    r = base.intersect(new Range(11, 30));
    assertThat(r.first()).isEqualTo(11);
    assertThat(r.last()).isEqualTo(29);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + (30 - 11) / 2);

    r = base.intersect(new Range(11, nx + 30));
    assertThat(r).isEqualTo(base);

    r = base.intersect(new Range(99, nx + 30));
    assertThat(r.first()).isEqualTo(99);
    assertThat(r.last()).isEqualTo(nx);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + (nx - 99) / 2);

    r = base.intersect(new Range(99, nx + 30));
    assertThat(r.first()).isEqualTo(99);
    assertThat(r.last()).isEqualTo(nx);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + (nx - 99) / 2);

    r = base.intersect(new Range(98, nx + 30));
    assertThat(r.first()).isEqualTo(99);
    assertThat(r.last()).isEqualTo(nx);
    assertThat(r.stride()).isEqualTo(2);
    assertThat(r.length()).isEqualTo(1 + (nx - 99) / 2);
  }

  @Test
  public void testIntersects() throws InvalidRangeException {
    int nx = 1234;
    Range base = new Range(11, nx);

    assertThat(base.intersects(new Range(0, nx))).isEqualTo(true);
    assertThat(base.intersects(new Range(0, 10))).isEqualTo(false);
    assertThat(base.intersects(new Range(0, 11))).isEqualTo(true);
    assertThat(base.intersects(new Range(11, 11))).isEqualTo(true);
    assertThat(base.intersects(new Range(nx, nx))).isEqualTo(true);
    assertThat(base.intersects(new Range(nx + 1, nx + 1))).isEqualTo(false);
    assertThat(base.intersects(new Range(nx - 1, nx + 1))).isEqualTo(true);
  }

  @Test
  public void testIntersectsStrided() throws InvalidRangeException {
    int nx = 1234;
    Range base = new Range(11, nx, 2);

    assertThat(base.intersects(new Range(0, nx))).isEqualTo(true);
    assertThat(base.intersects(new Range(0, 10))).isEqualTo(false);
    assertThat(base.intersects(new Range(0, 11))).isEqualTo(true);
    assertThat(base.intersects(new Range(11, 11))).isEqualTo(true);
    assertThat(base.intersects(new Range(nx, nx))).isEqualTo(false);
    assertThat(base.intersects(new Range(nx + 1, nx + 1))).isEqualTo(false);
    assertThat(base.intersects(new Range(nx - 1, nx + 1))).isEqualTo(true);
  }

  @Test
  public void testGetFirstInInterval() throws InvalidRangeException {
    int nx = 1237;
    Range base = new Range(11, nx, 2);

    assertThat(base.getFirstInInterval(7)).isEqualTo(11);
    assertThat(base.getFirstInInterval(11)).isEqualTo(11);
    assertThat(base.getFirstInInterval(12)).isEqualTo(13);
    assertThat(base.getFirstInInterval(13)).isEqualTo(13);
    assertThat(base.getFirstInInterval(nx - 2)).isEqualTo(nx - 2);
    assertThat(base.getFirstInInterval(nx - 1)).isEqualTo(nx);
    assertThat(base.getFirstInInterval(nx)).isEqualTo(nx);
    assertThat(base.getFirstInInterval(nx + 1)).isEqualTo(-1);
  }
}
