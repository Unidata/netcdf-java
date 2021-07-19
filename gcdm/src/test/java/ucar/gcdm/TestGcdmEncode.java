/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
@Ignore("not ready")
public class TestGcdmEncode {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testExample() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "ft/grid/namExtract/20060926_0000.nc";
    Path path = Paths.get(localFilename);
    encode(path);
  }

  void encode(Path path) throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(path.toString(), errlog)) {
      assertThat(gridDataset).isNotNull();
      GcdmGridProto.GridDataset proto = GcdmGridConverter.encodeGridDataset(gridDataset);
      GcdmGridDataset.Builder builder = GcdmGridDataset.builder();
      GcdmGridConverter.decodeGridDataset(proto, builder, errlog);
      GridDataset roundtrip = builder.build(false);
      assertThat(roundtrip).isEqualTo(gridDataset);
    }
  }

}
