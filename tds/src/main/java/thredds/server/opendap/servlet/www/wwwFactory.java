/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet.www;

import opendap.dap.BaseTypeFactory;
import opendap.dap.DArray;
import opendap.dap.DByte;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import opendap.dap.DURL;

/**
 * The default client-side Factory for BaseType objects.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see BaseTypeFactory
 */

public class wwwFactory implements BaseTypeFactory {


  // ..................................

  /**
   * Construct a new DByte.
   *
   * @return the new DByte
   */
  public DByte newDByte() {
    return new wwwByte();
  }

  /**
   * Construct a new DByte with name n.
   *
   * @param n the variable name
   * @return the new DByte
   */
  public DByte newDByte(String n) {
    return new wwwByte(n);
  }

  // ..................................

  /**
   * Construct a new DInt16.
   *
   * @return the new DInt16
   */
  public DInt16 newDInt16() {
    return new wwwI16();
  }

  /**
   * Construct a new DInt16 with name n.
   *
   * @param n the variable name
   * @return the new DInt16
   */
  public DInt16 newDInt16(String n) {
    return new wwwI16(n);
  }

  // ..................................

  /**
   * Construct a new DUInt16.
   *
   * @return the new DUInt16
   */
  public DUInt16 newDUInt16() {
    return new wwwUI16();
  }

  /**
   * Construct a new DUInt16 with name n.
   *
   * @param n the variable name
   * @return the new DUInt16
   */
  public DUInt16 newDUInt16(String n) {
    return new wwwUI16(n);
  }

  // ..................................

  /**
   * Construct a new DInt32.
   *
   * @return the new DInt32
   */
  public DInt32 newDInt32() {
    return new wwwI32();
  }

  /**
   * Construct a new DInt32 with name n.
   *
   * @param n the variable name
   * @return the new DInt32
   */
  public DInt32 newDInt32(String n) {
    return new wwwI32(n);
  }

  // ..................................

  /**
   * Construct a new DUInt32.
   *
   * @return the new DUInt32
   */
  public DUInt32 newDUInt32() {
    return new wwwUI32();
  }

  /**
   * Construct a new DUInt32 with name n.
   *
   * @param n the variable name
   * @return the new DUInt32
   */
  public DUInt32 newDUInt32(String n) {
    return new wwwUI32(n);
  }

  // ..................................

  /**
   * Construct a new DFloat32.
   *
   * @return the new DFloat32
   */
  public DFloat32 newDFloat32() {
    return new wwwF32();
  }

  /**
   * Construct a new DFloat32 with name n.
   *
   * @param n the variable name
   * @return the new DFloat32
   */
  public DFloat32 newDFloat32(String n) {
    return new wwwF32(n);
  }

  // ..................................

  /**
   * Construct a new DFloat64.
   *
   * @return the new DFloat64
   */
  public DFloat64 newDFloat64() {
    return new wwwF64();
  }

  /**
   * Construct a new DFloat64 with name n.
   *
   * @param n the variable name
   * @return the new DFloat64
   */
  public DFloat64 newDFloat64(String n) {
    return new wwwF64(n);
  }

  // ..................................

  /**
   * Construct a new DString.
   *
   * @return the new DString
   */
  public DString newDString() {
    return new wwwString();
  }

  /**
   * Construct a new DString with name n.
   *
   * @param n the variable name
   * @return the new DString
   */
  public DString newDString(String n) {
    return new wwwString(n);
  }

  // ..................................

  /**
   * Construct a new DURL.
   *
   * @return the new DURL
   */
  public DURL newDURL() {
    return new wwwURL();
  }

  /**
   * Construct a new DURL with name n.
   *
   * @param n the variable name
   * @return the new DURL
   */
  public DURL newDURL(String n) {
    return new wwwURL(n);
  }

  // ..................................

  /**
   * Construct a new DArray.
   *
   * @return the new DArray
   */
  public DArray newDArray() {
    return new wwwArray();
  }

  /**
   * Construct a new DArray with name n.
   *
   * @param n the variable name
   * @return the new DArray
   */
  public DArray newDArray(String n) {
    return new wwwArray(n);
  }

  // ..................................

  /**
   * Construct a new DGrid.
   *
   * @return the new DGrid
   */
  public DGrid newDGrid() {
    return new wwwGrid();
  }

  /**
   * Construct a new DGrid with name n.
   *
   * @param n the variable name
   * @return the new DGrid
   */
  public DGrid newDGrid(String n) {
    return new wwwGrid(n);
  }

  // ..................................

  /**
   * Construct a new DStructure.
   *
   * @return the new DStructure
   */
  public DStructure newDStructure() {
    return new wwwStructure();
  }

  /**
   * Construct a new DStructure with name n.
   *
   * @param n the variable name
   * @return the new DStructure
   */
  public DStructure newDStructure(String n) {
    return new wwwStructure(n);
  }

  // ..................................

  /**
   * Construct a new DSequence.
   *
   * @return the new DSequence
   */
  public DSequence newDSequence() {
    return new wwwSequence();
  }

  /**
   * Construct a new DSequence with name n.
   * 
   * @param n the variable name
   * @return the new DSequence
   */
  public DSequence newDSequence(String n) {
    return new wwwSequence(n);
  }

}


