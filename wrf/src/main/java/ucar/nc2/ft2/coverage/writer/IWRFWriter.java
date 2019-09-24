package ucar.nc2.ft2.coverage.writer;
/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

import java.io.File;
import java.util.ArrayList;
import ucar.nc2.ft2.coverage.Coverage;

public interface IWRFWriter {

  java.io.OutputStream writeStream(ArrayList<Coverage> coverages, String mapSource, boolean isWindEarthRel);

  void writeFile( ArrayList<Coverage> coverages, String mapSource, boolean isWindEarthRel, File outputFileName);

}
