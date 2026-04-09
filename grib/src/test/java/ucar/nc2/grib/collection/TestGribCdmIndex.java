/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import thredds.inventory.MFile;
import thredds.inventory.MFiles;

public class TestGribCdmIndex {

  @Test
  public void testMakeIndexFile() {
    MFile directory = MFiles.create("/path/to/");
    MFile idxMFile = GribCdmIndex.makeIndexFile("collection name", directory);
    assertThat(idxMFile.getPath()).startsWith(directory.getPath());
    assertThat(idxMFile.getName()).endsWith(GribCdmIndex.NCX_SUFFIX);


    directory = MFiles.create("cdms3:bucket?path/to/#delimiter=/");
    idxMFile = GribCdmIndex.makeIndexFile("collection name", directory);
    assertThat(idxMFile.getPath()).startsWith("cdms3:bucket?path/to/");
    assertThat(idxMFile.getPath()).endsWith("#delimiter=/");
  }

  @Test
  public void testMakeNameFromIndexFilename() {
    String indexFilename = "/path/to/collection_name" + GribCdmIndex.NCX_SUFFIX;
    String expectedName = "collection_name";
    String actualName = GribCdmIndex.makeNameFromIndexFilename(indexFilename);
    assertThat(actualName).isEqualTo(expectedName);

    indexFilename = "cdms3:bucket?path/to/collection_name" + GribCdmIndex.NCX_SUFFIX + "#delimiter=/";
    actualName = GribCdmIndex.makeNameFromIndexFilename(indexFilename);
    assertThat(actualName).isEqualTo(expectedName);
  }

}
