package ucar.nc2.ft2.coverage.writer;
/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

import java.util.Formatter;

/**
 * Simple pojo holding a line of data from a WRF vtable file.
 *
 * An Example:
 *
 * GRIB1| Level| From |  To  | metgrid  | metgrid | metgrid                                 |GRIB2|GRIB2|GRIB2|GRIB2|
 * Param| Type |Level1|Level2| Name     | Units   | Description                             |Discp|Catgy|Param|Level|
 * -----+------+------+------+----------+---------+-----------------------------------------+-----------------------+
 *   11 | 100  |   *  |      | TT       | K       | Temperature                             |  0  |  0  |  0  | 100 |
 *   33 | 100  |   *  |      | UU       | m s-1   | U                                       |  0  |  2  |  2  | 100 |
 *  223 |   1  |   0  |      | CANWAT   | kg m-2  | Plant Canopy Surface Water              |  2  |  0  | 196 |   1 |
 *            .........
 * -----+------+------+------+----------+---------+-----------------------------------------+-----------------------+
 * #
 * #  Vtable for NAM pressure-level data from the ncep server.
 *           .........
 * #
 *
 * @author hvandam
 * @since 8/5/2019
 */

class VtableEntry {

  private String param;
  private String levelType;
  private String fromLevel;
  private String toLevel;
  private String metgridName;
  private String metgridUnits;
  private String metgridDesc;
  private String discipline;
  private String category;
  private String parameter;
  private String level;

  public VtableEntry( String line ){

    final String regex = "\\|";   // need to escape the escape then the pipe symbol, which is regex control
    String[] items = line.split(regex);

    this.param = items[0].trim();
    this.levelType = items[1].trim();
    this.fromLevel = items[2].trim();
    this.toLevel = items[3].trim();
    this.metgridName = items[4].trim();
    this.metgridUnits = items[5].trim();
    this.metgridDesc = items[6].trim();
    this.discipline = items[7].trim();
    this.category = items[8].trim();
    this.parameter = items[9].trim();
    this.level = items[10].trim();
  }

  public String getParam() { return param;}
  public String getLevelType() { return levelType;}
  public String getFromLevel() { return fromLevel;}
  public String getToLevel() {  return toLevel;}
  public String getMetgridName() {return metgridName;}
  public String getMetgridUnits() { return metgridUnits;}
  public String getMetgridDesc() { return metgridDesc;}
  public String getDiscipline() { return discipline;}
  public String getCategory() { return category;}
  public String getParameter() { return parameter;}
  public String getLevel() { return level;}

  public String getGRIB2Var(){
    return "VAR_" + discipline + '-' + category + '-' + parameter + "_L" + level;
  }

  public String getGRIB1Var(){
    return "GRIB1" + param + "-" + levelType;
  }

  public String toString(){
    Formatter fmt = new Formatter();
    fmt.format("G1 Param: %3s, LevelType: %3s, fromLevel: %3s, toLevel: %3s, metName: %10s, metUnits: %10s, metDesc: %40s, Discp: %3s, Catgy: %3s, Param: %3s, Level: %3s ",
        param, levelType, fromLevel, toLevel, metgridName, metgridUnits, metgridDesc, discipline, category, parameter, level);
    return fmt.toString();
  }
}