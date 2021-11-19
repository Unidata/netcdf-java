/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr.writer;

import ucar.nc2.bufr.*;
import ucar.unidata.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Read BUFR files and split them.
 * Currently only files.
 *
 * @author caron
 * @since 8/26/13
 */
public class BufrSplitter2 {
  File dirOut;
  MessageDispatchDDS dispatcher;
  Formatter out;
  int total_msgs;

  public BufrSplitter2(String dirName, Formatter out) throws IOException {
    this.out = out;
    dirOut = new File(dirName);
    if (dirOut.exists() && !dirOut.isDirectory()) {
      throw new IllegalArgumentException(dirOut + " must be a directory");
    } else if (!dirOut.exists()) {
      if (!dirOut.mkdirs())
        throw new IllegalArgumentException(dirOut + " filed to create");
    }
    dispatcher = new MessageDispatchDDS(null, dirOut);
  }

  // TODO - needs to be a directory, or maybe an MFILE collection
  public void execute(String filename) throws IOException {
    try (RandomAccessFile mraf = new RandomAccessFile(filename, "r")) {
      MessageScanner scanner = new MessageScanner(mraf);

      while (scanner.hasNext()) {
        Message m = scanner.next();
        if (m == null)
          continue;
        total_msgs++;
        if (m.getNumberDatasets() == 0)
          continue;

        m.setRawBytes(scanner.getMessageBytes(m));

        // decide what to do with the message
        dispatcher.dispatch(m);
      }

      dispatcher.resetBufrTableMessages();
    }
  }

  public void exit() {
    dispatcher.exit(out);
  }

}
