package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class StringLockerTest {
  @Test
  public void testNonconflicting() throws InterruptedException {
    Thread t = new Thread(() -> {
      StringLocker locker = new StringLocker();
      locker.control("first");
      locker.control("second");
      locker.release("first");
      locker.release("second");
    });
    t.start();
    t.join(500);
    Assert.assertFalse(t.isAlive());
  }


  @Test
  public void testNonconflictingOtherOrder() throws InterruptedException {
    Thread t = new Thread(() -> {
      StringLocker locker = new StringLocker();
      locker.control("first");
      locker.control("second");
      locker.release("second");
      locker.release("first");
    });
    t.start();
    t.join(500);
    Assert.assertFalse(t.isAlive());
  }
}
