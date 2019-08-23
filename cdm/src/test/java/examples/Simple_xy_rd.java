/*
 * This is part of the netCDF package.
 * Copyright 2006 University Corporation for Atmospheric Research/Unidata.
 * See COPYRIGHT file for conditions of use.
 * 
 * This is a simple example which reads a small dummy array, which was
 * written by simple_xy_wr.java. This is intended to illustrate the use
 * of the netCDF Java API.
 * 
 * This example demonstrates the netCDF Java API.
 * 
 * Full documentation of the netCDF Java API can be found at:
 * http://www.unidata.ucar.edu/software/netcdf-java/
 */
package examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class Simple_xy_rd {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String args[]) throws IOException {

    final int NX = 6;
    final int NY = 12;
    // This is the array we will read.
    int[][] dataIn = new int[NX][NY];

    // Open the file. The ReadOnly parameter tells netCDF we want
    // read-only access to the file.
    NetcdfFile dataFile = null;
    String filename = "simple_xy.nc";
    // Open the file.
    try {

      dataFile = NetcdfFile.open(filename, null);

      // Retrieve the variable named "data"
      Variable dataVar = dataFile.findVariable("data");

      if (dataVar == null) {
        System.out.println("Cant find Variable data");
        return;
      }

      // Read all the values from the "data" variable into memory.
      int[] shape = dataVar.getShape();
      int[] origin = new int[2];

      ArrayInt.D2 dataArray;

      dataArray = (ArrayInt.D2) dataVar.read(origin, shape);

      // Check the values.
      assert shape[0] == NX;
      assert shape[1] == NY;

      for (int j = 0; j < shape[0]; j++) {
        for (int i = 0; i < shape[1]; i++) {
          dataIn[j][i] = dataArray.get(j, i);
        }
      }

      // The file is closed no matter what by putting inside a try/catch block.
    } catch (java.io.IOException e) {
      e.printStackTrace();

    } catch (InvalidRangeException e) {
      e.printStackTrace();

    } finally {
      if (dataFile != null)
        try {
          dataFile.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
    }

    System.out.println("*** SUCCESS reading example file simple_xy.nc!");

  }

}
