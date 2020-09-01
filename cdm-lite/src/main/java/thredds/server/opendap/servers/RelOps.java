/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servers;

import opendap.dap.BaseType;

/**
 * The RelOps interface defines how each type responds to relational
 * operators. Most (all?) types will not have sensible responses to all of
 * the relational operators (e.g. DByte won't know how to match a regular
 * expression but DString will). For those operators that are nonsensical a
 * class should throw InvalidOperator.
 *
 * @author jhrg
 * @version $Revision: 15901 $
 */

public interface RelOps {
  public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

  public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
}


