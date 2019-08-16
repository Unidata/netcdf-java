package thredds.ui.image;

import org.junit.Test;

public class TestImageIO {

  @Test
  public void testStuff() {
    String[] readers = javax.imageio.ImageIO.getReaderFormatNames();
    for (String reader : readers) {
      System.out.println(" reader = " + reader);
    }
    System.out.println("-------------------------------------------");
    String[] readerMimes = javax.imageio.ImageIO.getReaderMIMETypes();
    for (String readerMime : readerMimes) {
      System.out.println(" readerMimes = " + readerMime);
    }
    System.out.println("-------------------------------------------");
    String[] writers = javax.imageio.ImageIO.getWriterFormatNames();
    for (String writer : writers) {
      System.out.println(" writers = " + writer);
    }
    System.out.println("-------------------------------------------");
    String[] writerMimes = javax.imageio.ImageIO.getWriterMIMETypes();
    for (String writerMime : writerMimes) {
      System.out.println(" writerMimes = " + writerMime);
    }
    System.out.println("-------------------------------------------");
  }

}
