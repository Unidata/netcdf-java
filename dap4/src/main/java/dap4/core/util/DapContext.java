/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.util;

import java.util.List;
import java.util.Map;

/**
 * Provide a general map of Object->Object to serve
 * to pass context/env info into various classes.
 * Note that we cannot use e.g. java.util.Properties
 * because it is a String->String map.
 */

public class DapContext extends java.util.HashMap<Object, Object> {
  public DapContext() {
    super();
  }

  public DapContext insert(Map<String, String> map, boolean override) {
    for (Map.Entry entry : map.entrySet()) {
      if (!super.containsKey(entry.getKey()) || override)
        super.put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public String toString() {
    StringBuilder buf = new StringBuilder("DapContext{");
    boolean first = true;
    for (Map.Entry<Object, Object> entry : super.entrySet()) {
      if (!first)
        buf.append(",");
      buf.append("|");
      buf.append(DapUtil.stringable(entry.getKey()).toString());
      buf.append("|");
      buf.append("=");
      buf.append("|");
      buf.append(DapUtil.stringable(entry.getValue()).toString());
      buf.append("|");
      first = false;
    }
    buf.append("}");
    return buf.toString();
  }

}
