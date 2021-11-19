/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import org.junit.Test;
import ucar.array.InvalidRangeException;
import ucar.array.Section;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link IndexChunkerTiled} */
public class TestIndexChunkerTiled {

  @Test
  public void testChunkerTiledAll() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 20:39,  0:1353 ");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, dataSection);
    int count = 0;
    while (index.hasNext()) {
      IndexChunker.Chunk chunk = index.next();
      System.out.printf(" %s%n", chunk);
      assertThat(chunk.getSrcElem()).isEqualTo(1354 * count);
      assertThat(chunk.getNelems()).isEqualTo(1354);
      assertThat(chunk.getDestElem()).isEqualTo(1354 * count);
      count++;
    }
    assertThat(count).isEqualTo(20);
  }

  @Test
  public void testChunkerTiled() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 20:39,  0:1353 ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    int count = 0;
    while (index.hasNext()) {
      IndexChunker.Chunk chunk = index.next();
      System.out.printf(" %s%n", chunk);
      assertThat(chunk.getSrcElem()).isEqualTo(2708 + 1354 * count);
      assertThat(chunk.getNelems()).isEqualTo(1351);
      assertThat(chunk.getDestElem()).isEqualTo(1351 * count);
      count++;
    }
  }

  @Test
  public void testChunkerTiled2() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 40:59,  0:1353  ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    int count = 0;
    while (index.hasNext()) {
      IndexChunker.Chunk chunk = index.next();
      System.out.printf(" %s%n", chunk);
      assertThat(chunk.getSrcElem()).isEqualTo(1354 * count);
      assertThat(chunk.getNelems()).isEqualTo(1351);
      assertThat(chunk.getDestElem()).isEqualTo(24318 + 1351 * count);
      count++;
    }
  }

  @Test
  public void testIndexChunkerTiledToString() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 40:59,  0:1353  ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    assertThat(index.toString())
        .isEqualTo(String.format("  data = 0:1353 want = 0:1350 intersect = 0:1350 ncontigElements = 1351%n"
            + "  data = 40:59 want = 22:3152 intersect = 40:59 ncontigElements = 20%n"
            + "  data = 0:0 want = 0:2 intersect = 0:0 ncontigElements = 1%n"));
  }

}
