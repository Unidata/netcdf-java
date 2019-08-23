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



package opendap.servers;

/**
 * Thrown when a <code>RelOp</code> operation is called
 * on two types for which it makes no sense to compre, such as
 * attempting to ascertain is a String is less than a Float.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class InvalidOperatorException extends DAP2ServerSideException {
  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public InvalidOperatorException(String s) {
    super(opendap.dap.DAP2Exception.MALFORMED_EXPR, "Invalid Operator Exception: " + s);
  }


  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * message and OPeNDAP error code (see <code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public InvalidOperatorException(int err, String s) {
    super(err, s);
  }
}


