package ucar.nc2;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A list of strings that only allows one thread to use any given value at the same time.
 *
 * @author cmrose
 * @deprecated will move to ucar.nc2.util in ver 6.
 */
@Deprecated
public class StringLocker {

  private final Set<String> stringSet = new ConcurrentSkipListSet<>();

  public synchronized void control(String item) {
    // If the string is in use by another thread then wait() for the other thread
    while (stringSet.contains(item)) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    // Finished waiting so the thread can have the string
    stringSet.add(item);
  }

  public synchronized void release(String item) {
    // Tell StringLocker the thread is done with the string
    stringSet.remove(item);
    notifyAll();
  }

  public String toString() {
    return stringSet.toString();
  }

}
