package ucar.nc2.iosp.bufr.writer;

import java.io.FileOutputStream;
import java.io.OutputStream;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.unidata.io.RandomAccessFile;

public class TestBufr2Xml {

  @Test
  public void testStuff() throws Exception {

    //String filename = "C:/temp/cache/uniqueMessages.bufr";
    String filename = "C:/data/formats/bufr/uniqueExamples.bufr";
    int size = 0;
    int count = 0;

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r");
        OutputStream out = new FileOutputStream("C:/data/formats/bufr/uniqueE/" + count + ".xml")) {
      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message message = scan.next();
        if (message == null || !message.isTablesComplete() ||
            !message.isBitCountOk()) continue;
        byte[] mbytes = scan.getMessageBytesFromLast(message);
        NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
        NetcdfDataset ncd = new NetcdfDataset(ncfile);
        new Bufr2Xml(message, ncd, out, true);
        out.close();
        count++;
        size += message.getMessageSize();
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }

    System.out.printf("total size= %f Kb %n", .001 * size);
  }

}
