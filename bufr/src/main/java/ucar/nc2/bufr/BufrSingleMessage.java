/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.bufr;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import static ucar.nc2.bufr.BufrIosp.obsRecordName;

/* Make NetcdfFile from single message, for BufrMessageViewer and bit counting. */
public class BufrSingleMessage {
  public BufrIosp iosp;

  public Sequence fromSingleMessage(RandomAccessFile raf, Message single) throws IOException {
    iosp = new BufrIosp();

    // instead of iosp.build()
    iosp.setRaf(raf);
    iosp.protoMessage = single;
    if (!iosp.protoMessage.isTablesComplete()) {
      throw new IllegalStateException("BUFR file has incomplete tables");
    }
    iosp.config = BufrConfig.openFromMessage(raf, iosp.protoMessage, null);
    Group.Builder rootGroupb = Group.builder();
    new BufrIospBuilder(iosp.protoMessage, iosp.config, rootGroupb, raf.getLocation());
    iosp.isSingle = true;

    NetcdfFile.Builder<?> ncfileb = NetcdfFile.builder().setIosp(iosp).setLocation(raf.getLocation());
    ncfileb.setRootGroup(rootGroupb);
    NetcdfFile ncfile = ncfileb.build();
    iosp.buildFinish(ncfile);

    Group rootGroup = ncfile.getRootGroup();
    Sequence obsStructure = (Sequence) rootGroup.findVariableLocal(obsRecordName);

    return obsStructure;
  }
}
