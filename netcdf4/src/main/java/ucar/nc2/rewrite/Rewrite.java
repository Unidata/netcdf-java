/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.rewrite;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;
import java.util.List;

/** Rewrite a NetcdfFile to netcdf-4. Experimental. */
public class Rewrite {
  static final boolean NETCDF4 = true;

  NetcdfFile ncIn;
  NetcdfFileWriter ncOut;
  NetcdfFileWriter.Version version;
  boolean isRadial = false;

  public Rewrite(NetcdfFile ncIn, NetcdfFileWriter ncOut) {
    this.ncIn = ncIn;
    this.ncOut = ncOut;
    this.version = ncOut.getVersion();
  }

  public void rewrite() throws IOException, InvalidRangeException {
    Attribute attr = ncIn.getRootGroup().findAttribute("featureType");
    if(attr.getStringValue().contains("RADIAL"))
        isRadial = true;
    createGroup(null, ncIn.getRootGroup());

    ncOut.create();

    transferData(ncIn.getRootGroup());

    ncOut.close();
  }

  private int anon = 0;
  void createGroup(Group newParent, Group oldGroup) throws IOException, InvalidRangeException {
    Group newGroup = ncOut.addGroup(newParent, oldGroup.getShortName());

    for (Attribute att : oldGroup.getAttributes())
      newGroup.addAttribute(att);

    for (Dimension dim : oldGroup.getDimensions()) {
      ncOut.addDimension(newGroup, dim.getShortName(), dim.getLength(), dim.isUnlimited(), dim.isVariableLength());
    }

    for (Variable v : oldGroup.getVariables()) {
      List<Dimension> dims = v.getDimensions();

      // all dimensions must be shared (!)
      for (Dimension dim : dims) {
        if (!dim.isShared()) {
          dim.setName("anon"+anon);
          dim.setShared(true);
          anon++;
          ncOut.addDimension(newGroup, dim.getShortName(), dim.getLength(), dim.isUnlimited(), dim.isVariableLength());
        }
      }

      // LOOK need to set chunksize
      Variable nv;
      if (!isRadial && v.getRank() >= 3) {  // make first dimension last
        StringBuilder sb = new StringBuilder();
        for (int i=1; i<dims.size(); i++)
          sb.append(dims.get(i).getShortName()).append(" ");
        sb.append(dims.get(0).getShortName());
        nv = ncOut.addVariable(null, v.getShortName(), v.getDataType(), sb.toString());

      } else {
        nv = ncOut.addVariable(null, v.getShortName(), v.getDataType(), v.getDimensionsString());
      }

      for (Attribute att : v.getAttributes())
        ncOut.addVariableAttribute(nv, att);
    }

    // recurse
    for (Group g : oldGroup.getGroups())
      createGroup(newGroup, g);
  }

  void transferData(Group oldGroup) throws IOException, InvalidRangeException {

    for (Variable v : oldGroup.getVariables()) {
      if (!isRadial && v.getRank() >= 3) {
        invertOneVar(v);

      } else {
        System.out.printf("write %s%n",v.getNameAndDimensions());
        Array data = v.read();
        Variable nv = ncOut.findVariable(v.getFullName());
        ncOut.write(nv,  data);
      }
    }

    // recurse
    for (Group g : oldGroup.getGroups())
      transferData( g);
  }

  // turn var(nt, any..) into newvar(any.., nt)
  void invertOneVar(Variable oldVar) throws IOException, InvalidRangeException {
    System.out.printf("invertOneVar %s  ",oldVar.getNameAndDimensions());
    int rank = oldVar.getRank();
    int[] origin = new int[rank];

    int[] shape = oldVar.getShape(); // old Shape

    Variable nv = ncOut.findVariable(oldVar.getFullName());
    Cache cache = new Cache(shape, nv.getShape(), oldVar.getDataType());

    System.out.printf(" read slice");
    int nt = shape[0];
    for (int k=0; k<nt; k++)  { // loop over outermost dimension
      shape[0] = 1;
      origin[0] = k;

      Array data = oldVar.read(origin, shape); // read inner
      System.out.printf(" %d", k);
      cache.transfer(data.reduce(), k);
    }
    System.out.printf(" %n");

    cache.write(nv);
  }

  private class Cache {
    int[] shape, newshape;
    int nt, chunksize;

    Array result, work;
    int counter = 0;

    Cache(int[] shape, int[] newshape, DataType dataType)  {
      System.out.printf("shape = %d, ", new Section(shape).computeSize()/1000);
      System.out.printf("newshape = %d, ", new Section(newshape).computeSize()/1000);

      this.nt = shape[0];
      Section s = new Section(shape);
      long totalSize = s.computeSize();
      this.chunksize = (int)(totalSize / nt);
      System.out.printf("chunksize = %d (Kb)%n", this.chunksize/1000);

      this.shape = shape;
      this.newshape = newshape;
      this.result = Array.factory(dataType, this.newshape);

      // get view of result as a 2d array (any..., nt);
      int[] reshape = new int[] {this.chunksize, this.nt};
      this.work = this.result.reshapeNoCopy(reshape);
    }

    // transfer the kth slice, where k is index on outer dimension of old var
    void transfer(Array slice, int k) {
      Index ima = work.getIndex();
      ima.set1(k); // this one stays fixed

      int count = 0;
      IndexIterator ii = slice.getIndexIterator();
      while (ii.hasNext()) {
        work.setDouble(ima.set0(count), ii.getDoubleNext());
        count++;
      }
    }

    // look put the write in q background task tto overlap the read...
    void write(Variable newVar) throws IOException, InvalidRangeException {
      System.out.printf("  write slice (");
      int[] resultShape = result.getShape();
      for (int k : resultShape) System.out.printf("%d,", k);
      System.out.printf(")%n");

      ncOut.write(newVar, result);
    }
  }

}
