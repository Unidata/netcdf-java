/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * DO NOT USE
 * 
 * @author caron
 * @since Nov 15, 2008
 */
public class ArrayRagged extends Array {

  protected ArrayRagged(int[] shape) {
    super(DataType.OBJECT, shape);
  }


  @Override
  public Class getElementType() {
    return null; // To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * DO NOT USE, throws UnsupportedOperationException
   */
  @Override
  protected Array createView(Index index) {
    if (index.getSize() == getSize())
      return this;
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getStorage() {
    return null; // To change body of implemented methods use File | Settings | File Templates.
  }// used to create Array from java array

  @Override
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * DO NOT USE, throws UnsupportedOperationException
   */
  @Override
  public Array copy() {
    throw new UnsupportedOperationException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public double getDouble(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setDouble(Index i, double value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public float getFloat(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setFloat(Index i, float value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public long getLong(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setLong(Index i, long value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public int getInt(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setInt(Index i, int value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public short getShort(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setShort(Index i, short value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public byte getByte(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setByte(Index i, byte value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setBoolean(Index i, boolean value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public Object getObject(Index ima) {
    return null; // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setObject(Index ima, Object value) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public char getChar(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  @Override
  public void setChar(Index i, char value) {
    throw new ForbiddenConversionException();
  }

  // trusted, assumes that individual dimension lengths have been checked
  // package private : mostly for iterators
  @Override
  public double getDouble(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setDouble(int index, double value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public float getFloat(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setFloat(int index, float value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public long getLong(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setLong(int index, long value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public int getInt(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setInt(int index, int value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public short getShort(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setShort(int index, short value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public byte getByte(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setByte(int index, byte value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public char getChar(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setChar(int index, char value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public boolean getBoolean(int index) {
    throw new ForbiddenConversionException();
  }

  @Override
  public void setBoolean(int index, boolean value) {
    throw new ForbiddenConversionException();
  }

  @Override
  public Object getObject(int elem) {
    return null; // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setObject(int elem, Object value) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

}
