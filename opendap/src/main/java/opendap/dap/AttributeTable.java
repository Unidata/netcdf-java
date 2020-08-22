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
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import opendap.dap.parsers.DDSXMLParser;
import java.io.*;

/**
 * An <code>AttributeTable</code> stores a set of names and, for each name,
 * an <code>Attribute</code> object.
 * It is an Iterable< that returns the attribute names in insert order.
 *
 * @author jehamby
 *
 *      Modified 1/9/2011 Dennis Heimbigner
 *      - Make subclass of BaseType for uniformity
 */

public class AttributeTable extends DAPNode implements Iterable<String> {
  /** A table of Attributes with their names as a key. */
  private LinkedHashMap<String, Attribute> attributes;

  /** Create a new empty <code>AttributeTable</code>. */
  public AttributeTable(String clearname) {
    super(clearname);
    attributes = new LinkedHashMap<>();
  }

  /** @return the # of contained attributes */
  public int size() {
    return (attributes == null) ? 0 : attributes.size();
  }

  /** Returns the <code>Attribute</code> which matches name. */
  @Nullable
  public final Attribute getAttribute(String clearname) { // throws NoSuchAttributeException {
    return attributes.get(clearname);
  }

  /**
   * Adds an attribute to the table. If the given name already
   * refers to an attribute, and the attribute has a vector value,
   * the given value is appended to the attribute vector. Calling
   * this function repeatedly is the way to create an attribute
   * vector.
   * <p/>
   * The function throws an exception if the attribute is a
   * container, or if the type of the input value does not match the
   * existing attribute's type and the <code>check</code> parameter
   * is true. Use the <code>appendContainer</code> method to add container
   * attributes.
   *
   * @param clearname The name of the attribute to add or modify.
   * @param type The type code of the attribute to add or modify.
   * @param value The value to add to the attribute table.
   * @param check Check the validity of the attribute's value?
   * @throws AttributeExistsException thrown if an Attribute with the same
   *         name, but a different type was previously defined.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *         member of type
   * @see AttributeTable#appendContainer(String)
   */
  public final void appendAttribute(String clearname, int type, String value, boolean check) throws DASException {
    Attribute a = attributes.get(clearname);
    if (a != null && (type != a.getType())) {
      // type mismatch error
      throw new AttributeExistsException(
          "The Attribute `" + clearname + "' was previously defined with a different type.");
    } else if (a != null) {
      a.appendValue(value, check);
    } else {
      a = new Attribute(type, clearname, value, check);
      attributes.put(clearname, a);
    }
  }

  /**
   * Adds an attribute to the table. If the given name already
   * refers to an attribute, and the attribute has a vector value,
   * the given value is appended to the attribute vector. Calling
   * this function repeatedly is the way to create an attribute
   * vector.
   * <p/>
   * The function throws an exception if the attribute is a
   * container, or if the type of the input value does not match the
   * existing attribute's type. Use the <code>appendContainer</code>
   * method to add container attributes.
   *
   * @param clearname The name of the attribute to add or modify.
   * @param type The type code of the attribute to add or modify.
   * @param value The value to add to the attribute table.
   * @throws AttributeExistsException thrown if an Attribute with the same
   *         name, but a different type was previously defined.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *         member of type
   * @see AttributeTable#appendContainer(String)
   */
  public final void appendAttribute(String clearname, int type, String value) throws DASException {
    appendAttribute(clearname, type, value, true);
  }

  /**
   * Create and append an attribute container to the table.
   * A container is another <code>AttributeTable</code> object.
   *
   * @param clearname the name of the container to add.
   * @return A pointer to the new <code>AttributeTable</code> object, or null
   *         if a container by that name already exists.
   */
  @Nullable
  public final AttributeTable appendContainer(String clearname) {
    // return null if clearname already exists
    // FIXME! THIS SHOULD RETURN AN EXCEPTION!
    if (attributes.get(clearname) != null)
      return null;

    AttributeTable at = new AttributeTable(clearname);
    Attribute a = new Attribute(clearname, at);
    attributes.put(clearname, a);
    return at;
  }

  /**
   * Create and append an attribute container to the table.
   * A container is another <code>AttributeTable</code> object.
   *
   * @param clearname the name of the container to add.
   *        if a container by that name already exists.
   */
  public final void addContainer(String clearname, AttributeTable at) throws AttributeExistsException {
    if (attributes.get(clearname) != null) {
      throw new AttributeExistsException(
          "The Attribute '" + clearname + "' already exists in the container '" + getEncodedName() + "'");
    }

    Attribute a = new Attribute(clearname, at);
    attributes.put(clearname, a);
  }

  /**
   * Add an alias to the current table.
   * This method is used by the DAS parser to build Aliases
   * for the DAS. And the DDSXMLParser to add them to the DDX
   * <p/>
   * The new (9/26/02) DDS requires the use of <code>
   * addAlias(String, String, String)</code> and is the preffered
   * way of representing the DAS information.
   *
   * @param alias The alias to insert into the attribute table.
   * @param attributeName The normalized name of the attribute to which
   *        the alias will refer.
   * @throws AttributeExistsException thrown if the new alias has the same
   *         name as an existing attribute.
   */
  public final void addAlias(String alias, String attributeName) throws AttributeExistsException {
    // complain if alias name already exists in this AttributeTable.
    if (attributes.get(alias) != null) {
      throw new AttributeExistsException("Could not alias `" + alias + "' to `" + attributeName + "'. "
          + "It is a duplicat name in this AttributeTable");
    }
    Alias newAlias = new Alias(alias, attributeName);
    attributes.put(alias, newAlias);
  }

  /**
   * Delete the attribute named <code>name</code>.
   *
   * @param clearname The name of the attribute to delete. This can be an
   *        attribute of any type, including containers.
   */
  public final void delAttribute(String clearname) {
    attributes.remove(clearname);
  }

  /**
   * Delete the attribute named <code>name</code>. If the attribute has a
   * vector value, delete the <code>i</code>'th element of the vector.
   *
   * @param clearname The name of the attribute to delete. This can be an
   *        attribute of any type, including containers.
   * @param i If the named attribute is a vector, and <code>i</code> is
   *        non-negative, the <code>i</code>'th entry in the vector is deleted.
   *        If <code>i</code> equals -1, the entire attribute is deleted.
   * @see AttributeTable#delAttribute(String)
   */
  public final void delAttribute(String clearname, int i) throws DASException {

    if (i == -1) { // delete the whole attribute
      attributes.remove(clearname);
    } else {
      Attribute a = attributes.get(clearname);
      if (a != null) {
        if (a.isContainer()) {
          attributes.remove(clearname); // delete the entire container
        } else {
          a.deleteValueAt(i);
        }
      }
    }
  }

  /**
   * Print the attribute table on the given <code>PrintWriter</code>.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   * @param pad the number of spaces to indent each line.
   */
  public void print(PrintWriter os, String pad) {
    os.println(pad + getEncodedName() + " {");
    for (String name : attributes.keySet()) {
      Attribute a = getAttribute(name);
      if (a != null)
        a.print(os, pad + "    ");
    }
    os.println(pad + "}");
    os.flush();
  }

  /**
   * Print the attribute table on the given <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> to use for output.
   * @param pad the number of spaces to indent each line.
   */
  public final void print(OutputStream os, String pad) {
    print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))), pad);
  }

  /**
   * Print the attribute table on the given <code>PrintWriter</code> with
   * four spaces of indentation.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   */
  public final void print(PrintStream os) {
    print(os, "");
  }

  /**
   * Print the attribute table on the given <code>PrintWriter</code> with
   * four spaces of indentation.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   */
  public final void print(PrintWriter os) {
    print(os, "");
  }

  /**
   * Print the attribute table on the given <code>OutputStream</code> with
   * four spaces of indentation.
   *
   * @param os the <code>OutputStream</code> to use for output.
   */
  public final void print(OutputStream os) {
    print(os, "");
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
    pw.println(pad + "<Attribute name=\"" + DDSXMLParser.normalizeToXML(getEncodedName()) + "\" type=\"Container\">");

    for (String name : attributes.keySet()) {
      Attribute a = getAttribute(name);
      if (a != null)
        a.printXML(pw, pad + "\t", constrained);
    }
    pw.println(pad + "</Attribute>");
    pw.flush();
  }

  /** Returns a copy of this <code>AttributeTable</code>. */
  public AttributeTable cloneDAG(CloneMap map) throws CloneNotSupportedException {
    AttributeTable at = (AttributeTable) super.cloneDAG(map);
    at.attributes = new LinkedHashMap<>(attributes);
    return at;
  }

  @Override
  public Iterator<String> iterator() {
    return attributes.keySet().iterator();
  }
}