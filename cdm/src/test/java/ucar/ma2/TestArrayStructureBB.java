package ucar.ma2;

import java.nio.ByteBuffer;
import org.junit.Test;

public class TestArrayStructureBB {

  @Test
  public void testStuff() {
    byte[] ba = new byte[20];
    for (int i = 0; i < ba.length; ++i)
      ba[i] = (byte) i;

    ByteBuffer bbw = ByteBuffer.wrap(ba, 5, 15);
    bbw.get(0);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 5");

    bbw.position(5);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 4");
  }



}
