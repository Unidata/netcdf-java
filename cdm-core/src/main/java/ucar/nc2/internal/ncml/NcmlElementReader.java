/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import org.jdom2.Element;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.util.CancelTask;

import java.io.IOException;

class NcmlElementReader extends NcmlReader implements ucar.nc2.internal.cache.FileFactory {
  private final Element netcdfElem;
  private final String ncmlLocation;
  private final String location;

  NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
    this.ncmlLocation = ncmlLocation;
    this.location = location;
    this.netcdfElem = netcdfElem;
  }

  @Override
  public NetcdfFile open(DatasetUrl cacheName, int buffer_size, CancelTask cancelTask, Object spiObject)
      throws IOException {
    NetcdfFile.Builder<?> result = readNcml(ncmlLocation, location, netcdfElem, cancelTask);
    result.setLocation(cacheName.getTrueurl());
    return result.build();
  }
}
