/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
//
// Author: James Gallagher <jgallagher@opendap.org>
//
// All rights reserved.
//
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// - Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// - Neither the name of the OPeNDAP nor the names of its contributors may
// be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import opendap.dap.parsers.DDSXMLParser;
import ucar.nc2.dods.EscapeStringsDap;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * An <code>Attribute</code> holds information about a single attribute in an
 * <code>AttributeTable</code>. It has a type, and contains either a
 * <code>Vector</code> of <code>String</code>s containing the attribute's
 * values, or a reference to an <code>AttributeTable</code>, if the
 * <code>Attribute</code> is a container. An <code>Attribute</code> may also
 * be created as an alias pointing to another <code>Attribute</code> of any
 * type, including container.
 *
 * @author jehamby
 * @see AttributeTable
 */
public class Attribute extends DAPNode {

  private static final boolean _Debug = false;
  private static final boolean DebugValueChecking = false;

  /**
   * Unknown attribute type. This is currently unused.
   */
  public static final int UNKNOWN = 0;

  /**
   * Alias attribute type. This is an attribute that works like a
   * UNIX style soft link to another attribute.
   */
  public static final int ALIAS = 1;

  /**
   * Container attribute type. This Attribute holds an AttributeTable.
   */
  public static final int CONTAINER = 2;

  /**
   * Byte attribute type. Holds an unsigned Byte.
   */
  public static final int BYTE = 3;

  /**
   * Int16 attribute type. Holds a signed Short.
   */
  public static final int INT16 = 4;

  /**
   * UInt16 attribute type. Holds an unsigned Short.
   */
  public static final int UINT16 = 5;

  /**
   * Int32 attribute type. Holds a signed Integer.
   */
  public static final int INT32 = 6;

  /**
   * UInt32 attribute type. Holds an unsigned Integer.
   */
  public static final int UINT32 = 7;

  /**
   * Float32 attribute type. Holds a Float.
   */
  public static final int FLOAT32 = 8;

  /**
   * Float64 attribute type. Holds a Double.
   */
  public static final int FLOAT64 = 9;

  /**
   * String attribute type. Holds a String.
   */
  public static final int STRING = 10;

  /**
   * URL attribute type. Holds a String representing a URL.
   */
  public static final int URL = 11;

  /**
   * The type of the attribute.
   */
  private int type;

  /**
   * Either an AttributeTable or a Vector of String.
   */
  private AttributeTable attrTable;
  private List<String> attr;

  /**
   * Construct a container attribute.
   *
   * @param container the <code>AttributeTable</code> container.
   * @deprecated Use the ctor with the name.
   */
  public Attribute(AttributeTable container) {
    type = CONTAINER;
    attrTable = container;
  }

  /**
   * Construct an <code>Attribute</code> with the given type and initial
   * value.
   *
   * @param type the type of attribute to create. Use one of the type
   *        constants defined by this class.
   * @param clearname the name of the attribute.
   * @param value the initial value of this attribute. Use the
   *        <code>appendValue</code> method to create a vector of values.
   * @param check if true, check the value and throw
   *        AttributeBadValueException if it's not valid; if false do not check its
   *        validity.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *         member of type
   */
  public Attribute(int type, String clearname, String value, boolean check) throws AttributeBadValueException {
    super(clearname);
    if (check)
      value = forceValue(type, value);

    this.type = type;
    attr = new ArrayList<>();
    attr.add(value);
  }

  /**
   * Construct a container attribute.
   *
   * @param container the <code>AttributeTable</code> container.
   */
  public Attribute(String clearname, AttributeTable container) {
    super(clearname);
    type = CONTAINER;
    attrTable = container;
  }

  /**
   * Construct an empty attribute with the given type.
   *
   * @param type the type of attribute to create. Use one of the type
   *        constants defined by this class, other than <code>CONTAINER</code>.
   * @throws IllegalArgumentException thrown if
   *         <code>type</code> is <code>CONTAINER</code>. To construct an empty
   *         container attribute, first construct and empty AttributeTable and then
   *         use that to construct the Attribute.
   */
  public Attribute(String clearname, int type) throws IllegalArgumentException {
    super(clearname);
    this.type = type;
    if (type == CONTAINER)
      throw new IllegalArgumentException("Can't construct an Attribute(CONTAINER)");
    else
      attr = new ArrayList<>();
  }

  /**
   * Returns the attribute type as a <code>String</code>.
   *
   * @return the attribute type <code>String</code>.
   */
  public final String getTypeString() {
    switch (type) {
      case CONTAINER:
        return "Container";
      case ALIAS:
        return "Alias";
      case BYTE:
        return "Byte";
      case INT16:
        return "Int16";
      case UINT16:
        return "UInt16";
      case INT32:
        return "Int32";
      case UINT32:
        return "UInt32";
      case FLOAT32:
        return "Float32";
      case FLOAT64:
        return "Float64";
      case STRING:
        return "String";
      case URL:
        return "Url";
      // case BOOLEAN: return "Boolean";
      default:
        return "";
    }
  }

  /**
   * Returns the attribute type as a <code>String</code>.
   *
   * @return the attribute type <code>String</code>.
   */
  public static final int getTypeVal(String s) {

    if (s.equalsIgnoreCase("Container"))
      return CONTAINER;
    else if (s.equalsIgnoreCase("Byte"))
      return BYTE;
    else if (s.equalsIgnoreCase("Int16"))
      return INT16;
    else if (s.equalsIgnoreCase("UInt16"))
      return UINT16;
    else if (s.equalsIgnoreCase("Int32"))
      return INT32;
    else if (s.equalsIgnoreCase("UInt32"))
      return UINT32;
    else if (s.equalsIgnoreCase("Float32"))
      return FLOAT32;
    else if (s.equalsIgnoreCase("Float64"))
      return FLOAT64;
    else if (s.equalsIgnoreCase("String"))
      return STRING;
    else if (s.equalsIgnoreCase("URL"))
      return URL;
    else
      return UNKNOWN;
  }

  /**
   * Returns the attribute type constant.
   *
   * @return the attribute type constant.
   */
  public int getType() {
    return type;
  }

  /**
   * Returns true if the attribute is a container.
   *
   * @return true if the attribute is a container.
   */
  public boolean isContainer() {
    return (type == CONTAINER);
  }

  /**
   * Returns true if the attribute is an alias.
   *
   * @return true if the attribute is an alias.
   */
  public boolean isAlias() {
    return (false);
  }


  /**
   * Returns the <code>AttributeTable</code> container.
   *
   * @return the <code>AttributeTable</code> container.
   * @throws NoSuchAttributeException If
   *         the instance of Attribute on which it is called is not a container.
   */
  public AttributeTable getContainer() throws NoSuchAttributeException {
    checkContainerUsage();
    return (AttributeTable) attr;
  }

  /**
   * Returns the <code>AttributeTable</code> container.
   *
   * @return the <code>AttributeTable</code> container, or null if not a container.
   */
  @Nullable
  public AttributeTable getContainerN() {
    return attrTable;
  }

  /**
   * Returns the values of this attribute as an <code>Enumeration</code>
   * of <code>String</code>.
   *
   * @return an <code>Enumeration</code> of <code>String</code>.
   */
  public List<String> getValues() throws NoSuchAttributeException {
    checkVectorUsage();
    return attr;
  }

  /** Returns the values of this attribute as an <code>Iterator</code> of <code>String</code>. */
  public Iterator<String> getValuesIterator() {
    return (attr != null) ? attr.iterator() : null;
  }

  /** Returns the nummber of values held in this attribute. */
  public int getNumVal() throws NoSuchAttributeException {
    checkVectorUsage();
    return attr.size();
  }

  /** Returns the attribute value at <code>index</code>. */
  public String getValueAt(int index) throws NoSuchAttributeException {
    checkVectorUsage();
    return attr.get(index);
  }

  /** Returns the attribute value at <code>index</code>, or null if a container */
  @Nullable
  public String getValueAtN(int index) {
    return (attr == null) ? null : attr.get(index);
  }

  /**
   * Append a value to this attribute. Always checks the validity of the attribute's value.
   *
   * @param value the attribute <code>String</code> to add.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *         member of type
   */
  public void appendValue(String value) throws NoSuchAttributeException, AttributeBadValueException {
    checkVectorUsage();
    appendValue(value, true);
  }

  /**
   * Append a value to this attribute.
   *
   * @param value the attribute <code>String</code> to add.
   * @param check if true, check the validity of he attribute's value, if
   *        false don't.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *         member of type
   */
  public void appendValue(String value, boolean check) throws NoSuchAttributeException, AttributeBadValueException {

    checkVectorUsage();
    if (check)
      value = forceValue(type, value);

    attr.add(value);
  }

  /**
   * Remove the <code>i</code>'th <code>String</code> from this attribute.
   *
   * @param index the index of the value to remove.
   */
  public void deleteValueAt(int index) throws AttributeBadValueException, NoSuchAttributeException {
    checkVectorUsage();
    attr.remove(index);
  }

  /**
   * Check if the value is legal for a given type.
   *
   * @param type the type of the value.
   * @param value the value <code>String</code>.
   * @throws AttributeBadValueException if the value is not a legal
   *         member of type
   */
  private static void dispatchCheckValue(int type, String value) throws AttributeBadValueException {

    switch (type) {

      case BYTE:
        if (!checkByte(value))
          throw new AttributeBadValueException("`" + value + "' is not a Byte value.");
        break;

      case INT16:
        if (!checkShort(value))
          throw new AttributeBadValueException("`" + value + "' is not an Int16 value.");
        break;

      case UINT16:
        if (!checkUShort(value))
          throw new AttributeBadValueException("`" + value + "' is not an UInt16 value.");
        break;

      case INT32:
        if (!checkInt(value))
          throw new AttributeBadValueException("`" + value + "' is not an Int32 value.");
        break;

      case UINT32:
        if (!checkUInt(value))
          throw new AttributeBadValueException("`" + value + "' is not an UInt32 value.");
        break;

      case FLOAT32:
        if (!checkFloat(value))
          throw new AttributeBadValueException("`" + value + "' is not a Float32 value.");
        break;

      case FLOAT64:
        if (!checkDouble(value))
          throw new AttributeBadValueException("`" + value + "' is not a Float64 value.");
        break;

      // case BOOLEAN:
      // if(!checkBoolean(value))
      // throw new AttributeBadValueException("`" + value + "' is not a Boolean value.");
      // break;

      default:
        // Assume UNKNOWN, CONTAINER, STRING, and URL are okay.
    }
  }

  /**
   * Check if the value is legal for a given type
   * and try to convert to specified type.
   *
   * @param type the type of the value.
   * @param value the value <code>String</code>.
   * @return original or converted value
   * @throws AttributeBadValueException if the value is not a legal
   *         member of type
   */
  private static String forceValue(int type, String value) throws AttributeBadValueException {
    try {
      dispatchCheckValue(type, value);
    } catch (AttributeBadValueException abe) {
      if (type == BYTE) {// Try again: allow e.g. negative byte values
        short val = Short.parseShort(value);
        if (val > 255 && val < -128)
          throw new AttributeBadValueException("Cannot convert to byte: " + value);
        value = Integer.toString((val & 0xFF));
      }
    }
    return value;
  }

  /**
   * Check if string is a valid Byte.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkByte(String s) throws AttributeBadValueException {
    try {
      // Byte.parseByte() can't be used because values > 127 are allowed
      short val = Short.parseShort(s);
      if (DebugValueChecking) {
        log.debug("Attribute.checkByte() - string: '" + s + "'   value: " + val);
      }
      if (val > 0xFF || val < 0)
        return false;
      else
        return true;
    } catch (NumberFormatException e) {
      throw new AttributeBadValueException("`" + s + "' is not a Byte value.");
    }
  }

  /**
   * Check if string is a valid Int16.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkShort(String s) {
    try {
      short val = Short.parseShort(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkShort() - string: '" + s + "'   value: " + val);
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid UInt16.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUShort(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
      long val = Long.parseLong(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkUShort() - string: '" + s + "'   value: " + val);
      }
      if (val > 0xFFFFL)
        return false;
      else
        return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid Int32.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkInt(String s) {
    try {
      // Coverity[FB.DLS_DEAD_LOCAL_STORE]
      int val = Integer.parseInt(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkInt() - string: '" + s + "'   value: " + val);
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid UInt32.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUInt(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
      long val = Long.parseLong(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkUInt() - string: '" + s + "'   value: " + val);
      }
      if (val > 0xFFFFFFFFL)
        return false;
      else
        return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid Float32.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkFloat(String s) {
    try {
      // Coverity[FB.DLS_DEAD_LOCAL_STORE]=
      float val = Float.parseFloat(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkFloat() - string: '" + s + "'   value: " + val);
      }
      return true;
    } catch (NumberFormatException e) {
      if (s.equalsIgnoreCase("nan") || s.equalsIgnoreCase("inf"))
        return true;

      return false;
    }
  }

  /**
   * Check if string is a valid Float64.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkDouble(String s) {
    try {
      // Coverity[FB.DLS_DEAD_LOCAL_STORE]
      double val = Double.parseDouble(s);
      if (DebugValueChecking) {
        DAPNode.log.debug("Attribute.checkDouble() - string: '" + s + "'   value: " + val);
      }
      return true;
    } catch (NumberFormatException e) {
      if (s.equalsIgnoreCase("nan") || s.equalsIgnoreCase("inf"))
        return true;

      return false;
    }
  }

  private void checkVectorUsage() throws NoSuchAttributeException {
    if (attr == null) {
      throw new NoSuchAttributeException(
          "The Attribute '" + getEncodedName() + "' is a container. " + "It's contents are Attribues, not values.");
    }
  }

  private void checkContainerUsage() throws NoSuchAttributeException {
    if (attrTable == null) {
      throw new NoSuchAttributeException("The Attribute '" + getEncodedName() + "' is not a container (AttributeTable)."
          + "It's content is made up of values, not other Attributes.");
    }
  }


  public void print(PrintWriter os, String pad) {
    if (this.attrTable != null) {
      this.attrTable.print(os, pad);

    } else {
      os.print(pad + getTypeString() + " " + getEncodedName() + " ");

      int count = 0;
      for (String val : this.attr) {
        if (count > 0)
          os.print(", ");
        /*
         * Base quoting on type
         * boolean useQuotes = false;
         * if (val.indexOf(' ') >= 0 ||
         * val.indexOf('\t') >= 0 ||
         * val.indexOf('\n') >= 0 ||
         * val.indexOf('\r') >= 0
         * ) {
         * 
         * if (val.indexOf('\"') != 0)
         * useQuotes = true;
         * }
         * 
         * if (useQuotes)
         * os.print("\"" + val + "\"");
         * else
         * os.print(val);
         */
        if (this.type == Attribute.STRING) {
          String quoted = "\"" + EscapeStringsDap.backslashEscapeDapString(val) + "\"";
          for (int i = 0; i < quoted.length(); i++) {
            os.print((char) ((int) quoted.charAt(i)));
          }
          // os.print(quoted);
        } else {
          os.print(val);
        }
        count++;
      }
      os.println(";");
    }
    if (_Debug)
      os.println("Leaving Attribute.print()");
    os.flush();
  }

  /**
   * Print the attribute on the given <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> to use for output.
   * @param pad the number of spaces to indent each line.
   */
  public final void print(OutputStream os, String pad) {
    print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))), pad);
  }

  /**
   * Print the attribute on the given <code>PrintWriter</code> with
   * four spaces of indentation.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   */
  public final void print(PrintWriter os) {
    print(os, "");
  }

  /**
   * Print the attribute on the given <code>OutputStream</code> with
   * four spaces of indentation.
   *
   * @param os the <code>OutputStream</code> to use for output.
   */
  public final void print(OutputStream os) {
    print(os, "");
  }

  public void printXML(OutputStream os) {
    printXML(os, "");
  }

  public void printXML(OutputStream os, String pad) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
    printXML(pw, pad);
    pw.flush();
  }

  public void printXML(PrintWriter pw) {
    printXML(pw, "");
  }

  public void printXML(PrintWriter pw, String pad) {
    printXML(pw, pad, false);
  }

  public void printXML(PrintWriter pw, String pad, boolean constrained) {
    if (this.attrTable != null) {
      this.attrTable.printXML(pw, pad, constrained);
    } else {
      pw.println(pad + "<Attribute name=\"" + DDSXMLParser.normalizeToXML(getEncodedName()) + "\" type=\""
          + getTypeString() + "\">");
      for (String val : this.attr) {
        pw.println(pad + "\t" + "<value>" + DDSXMLParser.normalizeToXML(val) + "</value>");
      }
      pw.println(pad + "</Attribute>");
    }
    pw.flush();
  }

  /**
   * Returns a clone of this <code>Attribute</code>.
   * See DAPNode.cloneDag()
   *
   * @param map track previously cloned nodes
   * @return a clone of this <code>Attribute</code>.
   */
  public Attribute cloneDAG(CloneMap map) throws CloneNotSupportedException {
    Attribute a = (Attribute) super.cloneDAG(map);
    // assume type, is_alias, and aliased_to have been cloned already
    if (type == CONTAINER)
      a.attrTable = (AttributeTable) cloneDAG(map, attrTable);
    else
      a.attr = new ArrayList<>(attr); // ok, attr is a vector of strings
    return a;
  }

}
