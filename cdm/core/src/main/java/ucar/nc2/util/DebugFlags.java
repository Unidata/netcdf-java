/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/** A set of boolena flags. */
public interface DebugFlags {

  /**
   * Create a set of debug flags from a list of flag names.
   *
   * @param flagsOn space-separated list of flags to turn on.
   */
  static DebugFlags create(String flagsOn) {
    return DebugFlags.create(flagsOn);
  }

  /** Test if named debug flag is set. */
  boolean isSet(String flagName);

  /** Set named debug flag. */
  void set(String flagName, boolean value);

  ////////////////////////////////////////////////////////

  class DebugFlagsImpl implements DebugFlags {
    private final Map<String, Boolean> map = new HashMap<>();

    private DebugFlagsImpl(String flagsOn) {
      StringTokenizer stoke = new StringTokenizer(flagsOn);
      while (stoke.hasMoreTokens()) {
        set(stoke.nextToken(), true);
      }
    }

    @Override
    public boolean isSet(String flagName) {
      Boolean b = map.get(flagName);
      return (b != null) && b;
    }

    @Override
    public void set(String flagName, boolean value) {
      map.put(flagName, value);
    }

  }

}
