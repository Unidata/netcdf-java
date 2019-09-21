//
// McIDASException.java
//

/*
 * This source file is part of the edu.wisc.ssec.mcidas package and is
 * Copyright (C) 1998 - 2011 by Tom Whittaker, Tommy Jasmin, Tom Rink,
 * Don Murray, James Kelly, Bill Hibbard, Dave Glowacki, Curtis Rueden
 * and others.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA
 */

package ucar.mcidas;

/**
 * McIDASException class is to handle exceptions when dealing
 * with McIDAS files or data.
 *
 * @author Don Murray - Unidata
 */

public class McIDASException extends Exception {

  /**
   * Constructs a McIDASException with no specified detail message.
   */
  public McIDASException() {
    super();
  }

  /**
   * Constructs a McIDASException with the specified detail message.
   *
   * @param s the detail message.
   */
  public McIDASException(String s) {
    super(s);
  }

}
