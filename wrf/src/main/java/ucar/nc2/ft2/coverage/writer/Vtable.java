/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Vtable {

  private ArrayList<VtableEntry> table;
  private boolean isStrict = false;

  public Vtable(String file, boolean strict){

    table = new ArrayList<>();
    if (strict){
      isStrict = true;
      loadVtableStrict(file);
    }
    else{
      loadVtableExact(file);
    }
  }

  public ArrayList<VtableEntry> getTable(){
    return table;
  }

  public boolean tableIsStrict(){
    return isStrict;
  }

  public void displayVtable(ArrayList<VtableEntry> entries){

    System.out.println("\nThe vtable is:");
    table.forEach( e -> System.out.println( e ) );
  }

  private void loadVtableStrict(String fname) {

    String line;
    String begin;
    String varId;
    ArrayList<String> varIdList = new ArrayList<>();

    final String regex = "[^a-zA-Z]";

    try (Scanner scanner = new Scanner(new File(fname))) {

      while (scanner.hasNext()) {
        line = scanner.nextLine();
        begin = line.substring(0, 1);
        if (begin.matches(regex)) {
          if ((!line.startsWith("-") && (!line.startsWith("#")))) {
            VtableEntry vt = new VtableEntry(line);
            String name = vt.getMetgridName().substring(0,2);
            switch (name){
              case ("SM"):
                vt.setMetgridName("SM");
                break;
              case ("ST"):
                vt.setMetgridName("ST");
                break;
              default:
                break;
            }
            varId = vt.getGRIB2Var();
            if ( !varIdList.contains(varId)) {
              varIdList.add(vt.getGRIB2Var());
              table.add(vt);
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadVtableExact(String fname) {

    String line;
    String begin;
    final String regex = "[^a-zA-Z]";

    try (Scanner scanner = new Scanner(new File(fname))) {

      while (scanner.hasNext()) {
        line = scanner.nextLine();
        begin = line.substring(0, 1);
        if (begin.matches(regex)) {
          if ((!line.startsWith("-") && (!line.startsWith("#")))) {
            VtableEntry vt = new VtableEntry(line);
            table.add(vt);
          }
        }
      }
    } catch(IOException e){
        e.printStackTrace();
    }
  }
}
