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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * The Util class holds static methods used by this package.
 *
 * @author jehamby
 */
public class Util {

  /**
   * Compares elements in a <code>List</code> of <code>BaseType</code>s and
   * throw a <code>BadSemanticsException</code> if there are any
   * duplicate elements.
   *
   * @param v The <code>Vector</code> to check
   * @param varName the name of the variable which called us
   * @param typeName the type name of the variable which called us
   * @throws BadSemanticsException if there are duplicate elements
   * @throws IndexOutOfBoundsException if size doesn't match the number
   *         of elements in the <code>Enumeration</code>
   */
  static void uniqueNames(List<BaseType> v, String varName, String typeName) throws BadSemanticsException {
    HashSet<String> dedup = new HashSet<>();
    for (BaseType bt : v) {
      if (dedup.add(bt.getEncodedName())) {
        throw new BadSemanticsException(
            "The variable `" + bt.getEncodedName() + "' is used more than once in " + typeName + " `" + varName + "'");
      }
    }
  }

  /**
   * This function escapes non-printable characters and quotes. This is used
   * to make <code>printVal</code> output <code>DString</code> data in the
   * same way as the C++ version. Since Java supports Unicode, this will
   * need to be altered if it's desired to print <code>DString</code> as
   * UTF-8 or some other character encoding.
   *
   * @param s the input <code>String</code>.
   * @return the escaped <code>String</code>.
   */
  static String escattr(String s) {
    StringBuffer buf = new StringBuffer(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == ' ' || (c >= '!' && c <= '~')) {
        // printable ASCII character
        buf.append(c);
      } else {
        // non-printable ASCII character: print as unsigned octal integer
        // padded with leading zeros
        buf.append('\\');
        String numVal = Integer.toString((int) c & 0xFF, 8);
        for (int pad = 0; pad < (3 - numVal.length()); pad++)
          buf.append('0');
        buf.append(numVal);
      }
    }
    return buf.toString();
  }

  /**
   * Filter out runtime exceptions from other exceptions and re-throw
   */
  public static void check(Exception ex) throws RuntimeException {
    if (ex instanceof RuntimeException)
      throw (RuntimeException) ex;
  }



}


