/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.array.InvalidRangeException;
import ucar.array.Section;

/** Test {@link ucar.nc2.iosp.IndexChunker} */
public class TestIndexChunker {

  @Test
  public void testFull() throws InvalidRangeException {
    int[] shape = new int[] {123, 22, 92, 12};
    Section section = new Section(shape);
    IndexChunker index = new IndexChunker(shape, section);
    assertThat(index.getTotalNelems()).isEqualTo(section.computeSize());
    IndexChunker.Chunk chunk = index.next();
    assertThat(chunk.getNelems()).isEqualTo(section.computeSize());
    assertThat(index.hasNext()).isFalse();
  }

  @Test
  public void testPart() throws InvalidRangeException {
    int[] full = new int[] {2, 10, 20};
    int[] part = new int[] {2, 5, 20};
    Section section = new Section(part);
    IndexChunker index = new IndexChunker(full, section);
    assertThat(index.getTotalNelems()).isEqualTo(section.computeSize());
    IndexChunker.Chunk chunk = index.next();
    assertThat(chunk.getNelems()).isEqualTo(section.computeSize() / 2);
  }

  @Test
  public void testIndexChunkerToString() throws InvalidRangeException {
    int[] full = new int[] {2, 10, 20};
    int[] part = new int[] {2, 5, 20};
    Section section = new Section(part);
    IndexChunker index = new IndexChunker(full, section);
    assertThat(index.toString()).isEqualTo("wantSize=1,2 maxSize=200,2 wantStride=1,1 stride=20,200");
  }

}
