/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.CharBuffer;
import org.junit.Test;

public class TestArrayByteOrder {

  @Test
  public void testArrayByte() {
    byte[] data = {1, 2, 3, 4};
    ArrayByte a = (ArrayByte) Array.factory(DataType.BYTE, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    for (int i = 0; i < 4; i++) {
      assertThat(bb.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 4; i++) {
      assertThat(bb.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayShort() {
    short[] data = {1, 2, 3, 4};
    ArrayShort a = (ArrayShort) Array.factory(DataType.SHORT, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    ShortBuffer sb = bb.asShortBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(sb.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    sb = bb.asShortBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(sb.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayInt() {
    int[] data = {1, 2, 3, 4};
    ArrayInt a = (ArrayInt) Array.factory(DataType.INT, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    IntBuffer ib = bb.asIntBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(ib.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    ib = bb.asIntBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(ib.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayLong() {
    long[] data = {1L, 2L, 3L, 4L};
    ArrayLong a = (ArrayLong) Array.factory(DataType.LONG, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    LongBuffer lb = bb.asLongBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(lb.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    lb = bb.asLongBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(lb.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayDouble() {
    double[] data = {1.0, 2.0, 3.0, 4.0};
    ArrayDouble a = (ArrayDouble) Array.factory(DataType.DOUBLE, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    DoubleBuffer db = bb.asDoubleBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(db.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    db = bb.asDoubleBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(db.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayFloat() {
    float[] data = {1.0f, 2.0f, 3.0f, 4.0f};
    ArrayFloat a = (ArrayFloat) Array.factory(DataType.FLOAT, new int[] {4}, data);

    // Big Endian
    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    FloatBuffer fb = bb.asFloatBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(fb.get(i)).isEqualTo(data[i]);
    }

    // Little Endian
    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    fb = bb.asFloatBuffer();
    for (int i = 0; i < 4; i++) {
      assertThat(fb.get(i)).isEqualTo(data[i]);
    }
  }

  @Test
  public void testArrayChar() {
    char[] data = {'a', 'b', 'c', 'd'};
    ArrayChar a = (ArrayChar) Array.factory(DataType.CHAR, new int[] {4}, data);

    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    // ByteOrder is irrelevant for single bytes, but we check if it's set
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    for (int i = 0; i < 4; i++) {
      assertThat(bb.get(i)).isEqualTo((byte) data[i]);
    }

    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 4; i++) {
      assertThat(bb.get(i)).isEqualTo((byte) data[i]);
    }
  }

  @Test
  public void testArrayString() {
    String[] data = {"one", "two", "three"};
    ArrayString a = ArrayString.factory(Index.factory(new int[] {3}), data);

    ByteBuffer bb = a.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    CharBuffer cb = bb.asCharBuffer();
    StringBuilder sb = new StringBuilder();
    for (String s : data) {
      sb.append(s).append('\0');
    }
    assertThat(cb.toString()).isEqualTo(sb.toString());

    bb = a.getDataAsByteBuffer(ByteOrder.LITTLE_ENDIAN);
    assertThat(bb.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    cb = bb.asCharBuffer();
    assertThat(cb.toString()).isEqualTo(sb.toString());
  }

  @Test
  public void testByteSectionByteBuffer() throws InvalidRangeException {
    byte[] data = {1, 2, 3, 4};
    ArrayByte a = (ArrayByte) Array.factory(DataType.BYTE, new int[] {4}, data);
    ArrayByte section = (ArrayByte) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 1);
    assertThat(bb.get(0)).isEqualTo((byte) 2);
    assertThat(bb.get(1)).isEqualTo((byte) 3);
  }

  @Test
  public void testShortSectionByteBuffer() throws InvalidRangeException {
    short[] data = {1, 2, 3, 4};
    ArrayShort a = (ArrayShort) Array.factory(DataType.SHORT, new int[] {4}, data);
    ArrayShort section = (ArrayShort) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 2);
    ShortBuffer sb = bb.asShortBuffer();
    assertThat(sb.get(0)).isEqualTo((short) 2);
    assertThat(sb.get(1)).isEqualTo((short) 3);
  }

  @Test
  public void testIntSectionByteBuffer() throws InvalidRangeException {
    int[] data = {1, 2, 3, 4};
    ArrayInt a = (ArrayInt) Array.factory(DataType.INT, new int[] {4}, data);
    ArrayInt section = (ArrayInt) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 4);
    IntBuffer ib = bb.asIntBuffer();
    assertThat(ib.get(0)).isEqualTo(2);
    assertThat(ib.get(1)).isEqualTo(3);
  }

  @Test
  public void testLongSectionByteBuffer() throws InvalidRangeException {
    long[] data = {1L, 2L, 3L, 4L};
    ArrayLong a = (ArrayLong) Array.factory(DataType.LONG, new int[] {4}, data);
    ArrayLong section = (ArrayLong) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 8);
    LongBuffer lb = bb.asLongBuffer();
    assertThat(lb.get(0)).isEqualTo(2L);
    assertThat(lb.get(1)).isEqualTo(3L);
  }

  @Test
  public void testFloatSectionByteBuffer() throws InvalidRangeException {
    float[] data = {1.0f, 2.0f, 3.0f, 4.0f};
    ArrayFloat a = (ArrayFloat) Array.factory(DataType.FLOAT, new int[] {4}, data);
    ArrayFloat section = (ArrayFloat) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 4);
    FloatBuffer fb = bb.asFloatBuffer();
    assertThat(fb.get(0)).isEqualTo(2.0f);
    assertThat(fb.get(1)).isEqualTo(3.0f);
  }

  @Test
  public void testDoubleSectionByteBuffer() throws InvalidRangeException {
    double[] data = {1.0, 2.0, 3.0, 4.0};
    ArrayDouble a = (ArrayDouble) Array.factory(DataType.DOUBLE, new int[] {4}, data);
    ArrayDouble section = (ArrayDouble) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 8);
    DoubleBuffer db = bb.asDoubleBuffer();
    assertThat(db.get(0)).isEqualTo(2.0);
    assertThat(db.get(1)).isEqualTo(3.0);
  }

  @Test
  public void testCharSectionByteBuffer() throws InvalidRangeException {
    char[] data = {'a', 'b', 'c', 'd'};
    ArrayChar a = (ArrayChar) Array.factory(DataType.CHAR, new int[] {4}, data);
    ArrayChar section = (ArrayChar) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    assertThat(bb.limit()).isEqualTo(2 * 1);
    assertThat(bb.get(0)).isEqualTo((byte) 'b');
    assertThat(bb.get(1)).isEqualTo((byte) 'c');
  }

  @Test
  public void testStringSectionByteBuffer() throws InvalidRangeException {
    String[] data = {"one", "two", "three", "four"};
    ArrayString a = ArrayString.factory(Index.factory(new int[] {4}), data);
    ArrayString section = (ArrayString) a.section(new int[] {1}, new int[] {2});

    ByteBuffer bb = section.getDataAsByteBuffer(ByteOrder.BIG_ENDIAN);
    CharBuffer cb = bb.asCharBuffer();
    assertThat(cb.toString()).isEqualTo("two\0three\0");
  }
}
