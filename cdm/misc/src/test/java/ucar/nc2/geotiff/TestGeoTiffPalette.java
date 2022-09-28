/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.dt.grid.GeoGrid;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.awt.Color;

/**
 * Testing the color palette features of GeotiffWriter
 *
 * @author WeatherGod
 * @since 8/12/20224
 */
public class TestGeoTiffPalette {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCreateColorMap() {
    HashMap<Integer, Color> result = GeotiffWriter.createColorMap(new int[] {10, 3, 145, 4},
        new String[] {"#00AAff", "#151412", "#DE01aB", "#100ABB"});
    HashMap<Integer, Color> expected = new HashMap<Integer, Color>();
    expected.put(10, new Color(0, 170, 255));
    expected.put(3, new Color(21, 20, 18));
    expected.put(145, new Color(222, 1, 171));
    expected.put(4, new Color(16, 10, 187));
    Assert.assertEquals(expected, result);

    Exception badargs = Assert.assertThrows(IllegalArgumentException.class, () -> {
      GeotiffWriter.createColorMap(new int[] {10, 3}, new String[] {"#00AAff"});
    });
    Assert.assertEquals("flag_values and flag_colors must be of equal length", badargs.getMessage());

    Exception badcolor = Assert.assertThrows(NumberFormatException.class, () -> {
      GeotiffWriter.createColorMap(new int[] {10, 3}, new String[] {"#00AAff", "Yellow"});
    });
    Assert.assertEquals("For input string: \"Yellow\"", badcolor.getMessage());
  }

  @Test
  public void testSetColorTable() {
    HashMap<Integer, Color> colorMap =
        GeotiffWriter.createColorMap(new int[] {1, 2, 3, 4}, new String[] {"#00AAff", "#151412", "#DE01aB", "#100ABB"});
    GeotiffWriter writer = new GeotiffWriter("dummy.tif");
    writer.setColorTable(colorMap, Color.black);
    int[] resultTable = writer.getColorTable();

    // Explicitly spelling out the RGB channel values [0, 255]
    // because there were some off-by-one errors in initial development.
    int[] expectedRGBs = new int[] {0, 170, 255,  // #00AAff
                                    21, 20, 18,   // #151412
                                    222, 1, 171,  // #DE01aB
                                    16, 10, 187}; // #100ABB

    int[] expectedTable = new int[3 * 256];
    // The colortable uses black for the default color as per above,
    // which translate to RGB values of (255, 255, 255) in the TIFF color notation (see note below).
    // Java arrays are initialized to zero, so we need to fill it with 255 before putting in our table.
    for (int i = 0; i < expectedTable.length; i++) {
      expectedTable[i] = 255;
    }

    // Now put in our color table.
    for (int i = 1; i <= 4; i++) {
      // TIFF ColorTable Channel values are between 0 and (256*256 - 1), inclusively, as per tiff conventions.
      // Java Color Channel values are between 0 and 255, inclusively.
      // Java Channel Value 0 should map to TIFF Channel Value 255
      // 1 should map to 511
      // 2 should map to 767
      // 255 should map to 65535
      Assert.assertEquals(expectedRGBs[0 + 3 * (i - 1)], colorMap.get(i).getRed());
      expectedTable[0 * 256 + i] = (colorMap.get(i).getRed() + 1) * 256 - 1;

      Assert.assertEquals(expectedRGBs[1 + 3 * (i - 1)], colorMap.get(i).getGreen());
      expectedTable[1 * 256 + i] = (colorMap.get(i).getGreen() + 1) * 256 - 1;

      Assert.assertEquals(expectedRGBs[2 + 3 * (i - 1)], colorMap.get(i).getBlue());
      expectedTable[2 * 256 + i] = (colorMap.get(i).getBlue() + 1) * 256 - 1;
    }
    Assert.assertArrayEquals(expectedTable, resultTable);

    // Also make sure unsetting works, too.
    writer.setColorTable(null);
    resultTable = writer.getColorTable();
    Assert.assertNull(resultTable);
  }


  @Test
  public void testWritePalette() throws IOException {
    String gridOut = tempFolder.newFile().getAbsolutePath();
    String baseline = "src/test/data/ucar/nc2/geotiff/baseline_palette.tif";
    logger.info("****geotiff palette write {}", gridOut);

    HashMap<Integer, Color> colorMap =
        GeotiffWriter.createColorMap(new int[] {1, 2, 3, 4}, new String[] {"#00AAff", "#151412", "#DE01aB", "#100ABB"});
    int[] colorTable;

    Array dtArray;
    try (GridDataset dataset = GridDataset.open("src/test/data/ucar/nc2/geotiff/categorical.nc")) {
      final GeoGrid grid = dataset.findGridByName("drought");
      assert grid != null;
      final GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;
      int rtindex = -1;
      int tindex = -1;
      CoordinateAxis1D timeAxis = gcs.getTimeAxis1D();
      assert timeAxis != null;
      tindex = (int) timeAxis.getSize() - 1; // last one
      dtArray = grid.readDataSlice(rtindex, -1, tindex, 0, -1, -1);

      try (GeotiffWriter writer = new GeotiffWriter(gridOut)) {
        writer.setColorTable(colorMap, Color.black);
        writer.writeGrid(dataset, grid, dtArray, false, DataType.UBYTE);
        colorTable = writer.getColorTable();
      }

      // read it back in to check the tags
      try (GeoTiff geotiff = new GeoTiff(gridOut)) {
        geotiff.read();
        logger.debug("{}", geotiff.showInfo());

        IFDEntry photoTag = geotiff.findTag(Tag.PhotometricInterpretation);
        Assert.assertNotNull(photoTag);
        Assert.assertEquals(1, photoTag.count);
        Assert.assertEquals(3, photoTag.value[0]);

        IFDEntry colorTableTag = geotiff.findTag(Tag.ColorMap);
        Assert.assertNotNull(colorTableTag);
        Assert.assertEquals(3 * 256, colorTableTag.count);
        Assert.assertArrayEquals(colorTable, colorTableTag.value);
      }

      // compare file s are equal
      File file1 = new File(gridOut);
      File file2 = new File(baseline);

      Assert.assertTrue(FileUtils.contentEquals(file1, file2));
    }
  }
}
