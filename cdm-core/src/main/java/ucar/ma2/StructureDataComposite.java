/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.util.Map;
import java.util.HashMap;

/**
 * A composite of other StructureData.
 * If multiple members of same name exist, the first one added is used
 * TODO make Immutable
 */
public class StructureDataComposite extends StructureData {
  protected Map<String, StructureData> proxy = new HashMap<>(32);

  public static StructureDataComposite create(Iterable<StructureData> sdatas) {
    Map<String, StructureData> proxy = new HashMap<>(32);
    StructureMembers.Builder builder = StructureMembers.builder();
    for (StructureData sdata : sdatas) {
      if (sdata != null) {
        for (StructureMembers.Member m : sdata.getMembers()) {
          if (!builder.hasMember(m.getName())) {
            builder.addMember(m.toBuilder(true));
            proxy.put(m.getName(), sdata);
          }
        }
      }
    }

    return new StructureDataComposite(builder.build(), proxy);
  }

  private StructureDataComposite(StructureMembers smembers, Map<String, StructureData> proxy) {
    super(smembers);
    this.proxy = proxy;
  }

  /** @deprecated use StructureDataComposite.make(StructureData... sdatas) */
  @Deprecated
  public StructureDataComposite() {
    super(new StructureMembers(""));
  }

  /** @deprecated use StructureDataComposite.make(StructureData... sdatas) */
  @Deprecated
  public void add(StructureData sdata) {
    for (StructureMembers.Member m : sdata.getMembers()) {
      if (this.members.findMember(m.getName()) == null) {
        this.members.addMember(m);
        proxy.put(m.getName(), sdata);
      }
    }
  }

  /** @deprecated use StructureDataComposite.make(StructureData... sdatas) */
  @Deprecated
  public void add(int pos, StructureData sdata) {
    for (StructureMembers.Member m : sdata.getMembers()) {
      if (this.members.findMember(m.getName()) == null) {
        this.members.addMember(pos++, m);
        proxy.put(m.getName(), sdata);
      }
    }
  }

  public Array getArray(StructureMembers.Member m) {
    StructureData sdata = proxy.get(m.getName());
    return sdata.getArray(m.getName());
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return proxy.get(m.getName()).convertScalarFloat(m.getName());
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return proxy.get(m.getName()).convertScalarDouble(m.getName());
  }

  public int convertScalarInt(StructureMembers.Member m) {
    return proxy.get(m.getName()).convertScalarInt(m.getName());
  }

  public long convertScalarLong(StructureMembers.Member m) {
    return proxy.get(m.getName()).convertScalarLong(m.getName());
  }

  public double getScalarDouble(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarDouble(m.getName());
  }

  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayDouble(m.getName());
  }

  public float getScalarFloat(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarFloat(m.getName());
  }

  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayFloat(m.getName());
  }

  public byte getScalarByte(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarByte(m.getName());
  }

  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayByte(m.getName());
  }

  public int getScalarInt(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarInt(m.getName());
  }

  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayInt(m.getName());
  }

  public short getScalarShort(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarShort(m.getName());
  }

  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayShort(m.getName());
  }

  public long getScalarLong(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarLong(m.getName());
  }

  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayLong(m.getName());
  }

  public char getScalarChar(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarChar(m.getName());
  }

  public char[] getJavaArrayChar(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayChar(m.getName());
  }

  public String getScalarString(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarString(m.getName());
  }

  public String[] getJavaArrayString(StructureMembers.Member m) {
    return proxy.get(m.getName()).getJavaArrayString(m.getName());
  }

  public StructureData getScalarStructure(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarStructure(m.getName());
  }

  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return proxy.get(m.getName()).getArrayStructure(m.getName());
  }

  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return proxy.get(m.getName()).getArraySequence(m.getName());
  }

  public Object getScalarObject(StructureMembers.Member m) {
    return proxy.get(m.getName()).getScalarObject(m.getName());
  }

}
