/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/** Test promoteGlobalAttribute */
public class TestAggPromote {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testPromote1() throws IOException, InvalidRangeException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
        + "  <aggregation dimName='time' type='joinExisting'>\n" // leavit
        + "    <promoteGlobalAttribute name='times' orgName='time_coverage_end' />\n" // leavit
        + "    <scan dateFormatMark='CG#yyyyDDD_HHmmss' location='src/test/data/ncml/nc/cg/' suffix='.nc' subdirs='false' />\n" // leavit
        + "  </aggregation>\n" // leavit
        + "</netcdf>"; // leavit

    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting1.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(xml), null, null);
    System.out.println(" TestNcmlAggExisting.open " + filename);

    Variable times = ncfile.findVariable("times");
    assert null != times;
    assert times.getRank() == 1;
    assert times.getSize() == 3;

    assert times.getDimension(0).getShortName().equals("time");
  }
}
