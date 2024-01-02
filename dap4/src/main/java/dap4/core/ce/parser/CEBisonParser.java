/* A Bison parser, made by GNU Bison 3.8.2. */

/*
 * Skeleton implementation for Bison LALR(1) parsers in Java
 * 
 * Copyright (C) 2007-2015, 2018-2021 Free Software Foundation, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * As a special exception, you may create a larger work that contains
 * part or all of the Bison parser skeleton and distribute that work
 * under terms of your choice, so long as that work isn't itself a
 * parser generator using the skeleton or a modified version thereof
 * as a parser skeleton. Alternatively, if you modify or redistribute
 * the parser skeleton itself, you may (at your option) remove this
 * special exception, which will cause the skeleton and the resulting
 * Bison output files to be licensed under the GNU General Public
 * License without this special exception.
 * 
 * This special exception was added by the Free Software Foundation in
 * version 2.2 of Bison.
 */

/*
 * DO NOT RELY ON FEATURES THAT ARE NOT DOCUMENTED in the manual,
 * especially those whose name start with YY_ or yy_. They are
 * private implementation details that can be changed or removed.
 */

package dap4.core.ce.parser;



import java.text.MessageFormat;
import java.util.ArrayList;
/* "%code imports" blocks. */
/* "ce.y":18 */

import dap4.core.util.Slice;
import dap4.core.dmr.parser.ParseException;
import dap4.core.ce.CEAST;

/* "CEBisonParser.java":51 */

/**
 * A Bison parser, automatically generated from <tt>ce.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
abstract class CEBisonParser {
  /** Version number for the Bison executable that generated this parser. */
  public static final String bisonVersion = "3.8.2";

  /** Name of the skeleton that generated this parser. */
  public static final String bisonSkeleton = "lalr1.java";



  /**
   * True if verbose error messages are enabled.
   */
  private boolean yyErrorVerbose = true;

  /**
   * Whether verbose error messages are enabled.
   */
  public final boolean getErrorVerbose() {
    return yyErrorVerbose;
  }

  /**
   * Set the verbosity of error messages.
   * 
   * @param verbose True to request verbose error messages.
   */
  public final void setErrorVerbose(boolean verbose) {
    yyErrorVerbose = verbose;
  }



  public enum SymbolKind {
    S_YYEOF(0), /* "end of file" */
    S_YYerror(1), /* error */
    S_YYUNDEF(2), /* "invalid token" */
    S_NAME(3), /* NAME */
    S_STRING(4), /* STRING */
    S_LONG(5), /* LONG */
    S_DOUBLE(6), /* DOUBLE */
    S_BOOLEAN(7), /* BOOLEAN */
    S_8_(8), /* ',' */
    S_NOT(9), /* NOT */
    S_10_(10), /* ';' */
    S_11_(11), /* '.' */
    S_12_(12), /* '{' */
    S_13_(13), /* '}' */
    S_14_(14), /* '[' */
    S_15_(15), /* ']' */
    S_16_(16), /* ':' */
    S_17_(17), /* '|' */
    S_18_(18), /* '!' */
    S_19_(19), /* '<' */
    S_20_(20), /* '=' */
    S_21_(21), /* '>' */
    S_22_(22), /* '~' */
    S_23_(23), /* '(' */
    S_24_(24), /* ')' */
    S_YYACCEPT(25), /* $accept */
    S_constraint(26), /* constraint */
    S_dimredeflist(27), /* dimredeflist */
    S_clauselist(28), /* clauselist */
    S_clause(29), /* clause */
    S_projection(30), /* projection */
    S_segmenttree(31), /* segmenttree */
    S_segmentforest(32), /* segmentforest */
    S_segment(33), /* segment */
    S_slicelist(34), /* slicelist */
    S_slice(35), /* slice */
    S_subslicelist(36), /* subslicelist */
    S_subslice(37), /* subslice */
    S_extent(38), /* extent */
    S_selection(39), /* selection */
    S_filter(40), /* filter */
    S_predicate(41), /* predicate */
    S_relop(42), /* relop */
    S_eqop(43), /* eqop */
    S_primary(44), /* primary */
    S_dimredef(45), /* dimredef */
    S_fieldname(46), /* fieldname */
    S_constant(47); /* constant */


    private final int yycode_;

    SymbolKind(int n) {
      this.yycode_ = n;
    }

    private static final SymbolKind[] values_ = {SymbolKind.S_YYEOF, SymbolKind.S_YYerror, SymbolKind.S_YYUNDEF,
        SymbolKind.S_NAME, SymbolKind.S_STRING, SymbolKind.S_LONG, SymbolKind.S_DOUBLE, SymbolKind.S_BOOLEAN,
        SymbolKind.S_8_, SymbolKind.S_NOT, SymbolKind.S_10_, SymbolKind.S_11_, SymbolKind.S_12_, SymbolKind.S_13_,
        SymbolKind.S_14_, SymbolKind.S_15_, SymbolKind.S_16_, SymbolKind.S_17_, SymbolKind.S_18_, SymbolKind.S_19_,
        SymbolKind.S_20_, SymbolKind.S_21_, SymbolKind.S_22_, SymbolKind.S_23_, SymbolKind.S_24_, SymbolKind.S_YYACCEPT,
        SymbolKind.S_constraint, SymbolKind.S_dimredeflist, SymbolKind.S_clauselist, SymbolKind.S_clause,
        SymbolKind.S_projection, SymbolKind.S_segmenttree, SymbolKind.S_segmentforest, SymbolKind.S_segment,
        SymbolKind.S_slicelist, SymbolKind.S_slice, SymbolKind.S_subslicelist, SymbolKind.S_subslice,
        SymbolKind.S_extent, SymbolKind.S_selection, SymbolKind.S_filter, SymbolKind.S_predicate, SymbolKind.S_relop,
        SymbolKind.S_eqop, SymbolKind.S_primary, SymbolKind.S_dimredef, SymbolKind.S_fieldname, SymbolKind.S_constant};

    static final SymbolKind get(int code) {
      return values_[code];
    }

    public final int getCode() {
      return this.yycode_;
    }

    /*
     * Return YYSTR after stripping away unnecessary quotes and
     * backslashes, so that it's suitable for yyerror. The heuristic is
     * that double-quoting is unnecessary unless the string contains an
     * apostrophe, a comma, or backslash (other than backslash-backslash).
     * YYSTR is taken from yytname.
     */
    private static String yytnamerr_(String yystr) {
      if (yystr.charAt(0) == '"') {
        StringBuffer yyr = new StringBuffer();
        strip_quotes: for (int i = 1; i < yystr.length(); i++)
          switch (yystr.charAt(i)) {
            case '\'':
            case ',':
              break strip_quotes;

            case '\\':
              if (yystr.charAt(++i) != '\\')
                break strip_quotes;
              /* Fall through. */
            default:
              yyr.append(yystr.charAt(i));
              break;

            case '"':
              return yyr.toString();
          }
      }
      return yystr;
    }

    /*
     * YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     * First, the terminals, then, starting at \a YYNTOKENS_, nonterminals.
     */
    private static final String[] yytname_ = yytname_init();

    private static final String[] yytname_init() {
      return new String[] {"\"end of file\"", "error", "\"invalid token\"", "NAME", "STRING", "LONG", "DOUBLE",
          "BOOLEAN", "','", "NOT", "';'", "'.'", "'{'", "'}'", "'['", "']'", "':'", "'|'", "'!'", "'<'", "'='", "'>'",
          "'~'", "'('", "')'", "$accept", "constraint", "dimredeflist", "clauselist", "clause", "projection",
          "segmenttree", "segmentforest", "segment", "slicelist", "slice", "subslicelist", "subslice", "extent",
          "selection", "filter", "predicate", "relop", "eqop", "primary", "dimredef", "fieldname", "constant", null};
    }

    /* The user-facing name of this symbol. */
    public final String getName() {
      return yytnamerr_(yytname_[yycode_]);
    }

  };


  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>CEBisonParser</tt>.
   */
  public interface Lexer {
    /* Token kinds. */
    /** Token "end of file", to be returned by the scanner. */
    static final int YYEOF = 0;
    /** Token error, to be returned by the scanner. */
    static final int YYerror = 256;
    /** Token "invalid token", to be returned by the scanner. */
    static final int YYUNDEF = 257;
    /** Token NAME, to be returned by the scanner. */
    static final int NAME = 258;
    /** Token STRING, to be returned by the scanner. */
    static final int STRING = 259;
    /** Token LONG, to be returned by the scanner. */
    static final int LONG = 260;
    /** Token DOUBLE, to be returned by the scanner. */
    static final int DOUBLE = 261;
    /** Token BOOLEAN, to be returned by the scanner. */
    static final int BOOLEAN = 262;
    /** Token NOT, to be returned by the scanner. */
    static final int NOT = 263;

    /** Deprecated, use YYEOF instead. */
    public static final int EOF = YYEOF;


    /**
     * Method to retrieve the semantic value of the last scanned token.
     * 
     * @return the semantic value of the last scanned token.
     */
    Object getLVal();

    /**
     * Entry point for the scanner. Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     * 
     * @return the token identifier corresponding to the next token.
     */
    int yylex() throws ParseException;

    /**
     * Emit an errorin a user-defined way.
     *
     *
     * @param msg The string for the error message.
     */
    void yyerror(String msg);


  }


  /**
   * The object doing lexical analysis for us.
   */
  private Lexer yylexer;



  /**
   * Instantiates the Bison-generated parser.
   * 
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public CEBisonParser(Lexer yylexer) {

    this.yylexer = yylexer;

  }


  private java.io.PrintStream yyDebugStream = System.err;

  /**
   * The <tt>PrintStream</tt> on which the debugging output is printed.
   */
  public final java.io.PrintStream getDebugStream() {
    return yyDebugStream;
  }

  /**
   * Set the <tt>PrintStream</tt> on which the debug output is printed.
   * 
   * @param s The stream that is used for debugging output.
   */
  public final void setDebugStream(java.io.PrintStream s) {
    yyDebugStream = s;
  }

  private int yydebug = 0;

  /**
   * Answer the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   */
  public final int getDebugLevel() {
    return yydebug;
  }

  /**
   * Set the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   * 
   * @param level The verbosity level for debugging output.
   */
  public final void setDebugLevel(int level) {
    yydebug = level;
  }


  private int yynerrs = 0;

  /**
   * The number of syntax errors so far.
   */
  public final int getNumberOfErrors() {
    return yynerrs;
  }

  /**
   * Print an error message via the lexer.
   *
   * @param msg The error message.
   */
  public final void yyerror(String msg) {
    yylexer.yyerror(msg);
  }


  protected final void yycdebugNnl(String s) {
    if (0 < yydebug)
      yyDebugStream.print(s);
  }

  protected final void yycdebug(String s) {
    if (0 < yydebug)
      yyDebugStream.println(s);
  }

  private final class YYStack {
    private int[] stateStack = new int[16];
    private Object[] valueStack = new Object[16];

    public int size = 16;
    public int height = -1;

    public final void push(int state, Object value) {
      height++;
      if (size == height) {
        int[] newStateStack = new int[size * 2];
        System.arraycopy(stateStack, 0, newStateStack, 0, height);
        stateStack = newStateStack;

        Object[] newValueStack = new Object[size * 2];
        System.arraycopy(valueStack, 0, newValueStack, 0, height);
        valueStack = newValueStack;

        size *= 2;
      }

      stateStack[height] = state;
      valueStack[height] = value;
    }

    public final void pop() {
      pop(1);
    }

    public final void pop(int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (0 < num) {
        java.util.Arrays.fill(valueStack, height - num + 1, height + 1, null);
      }
      height -= num;
    }

    public final int stateAt(int i) {
      return stateStack[height - i];
    }

    public final Object valueAt(int i) {
      return valueStack[height - i];
    }

    // Print the state stack on the debug stream.
    public void print(java.io.PrintStream out) {
      out.print("Stack now");

      for (int i = 0; i <= height; i++) {
        out.print(' ');
        out.print(stateStack[i]);
      }
      out.println();
    }
  }

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return success (<tt>true</tt>).
   */
  public static final int YYACCEPT = 0;

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return failure (<tt>false</tt>).
   */
  public static final int YYABORT = 1;



  /**
   * Returned by a Bison action in order to start error recovery without
   * printing an error message.
   */
  public static final int YYERROR = 2;

  /**
   * Internal return codes that are not supported for user semantic
   * actions.
   */
  private static final int YYERRLAB = 3;
  private static final int YYNEWSTATE = 4;
  private static final int YYDEFAULT = 5;
  private static final int YYREDUCE = 6;
  private static final int YYERRLAB1 = 7;
  private static final int YYRETURN = 8;


  private int yyerrstatus_ = 0;


  /**
   * Whether error recovery is being done. In this state, the parser
   * reads token until it reaches a known state, and then restarts normal
   * operation.
   */
  public final boolean recovering() {
    return yyerrstatus_ == 0;
  }

  /**
   * Compute post-reduction state.
   * 
   * @param yystate the current state
   * @param yysym the nonterminal to push on the stack
   */
  private int yyLRGotoState(int yystate, int yysym) {
    int yyr = yypgoto_[yysym - YYNTOKENS_] + yystate;
    if (0 <= yyr && yyr <= YYLAST_ && yycheck_[yyr] == yystate)
      return yytable_[yyr];
    else
      return yydefgoto_[yysym - YYNTOKENS_];
  }

  private int yyaction(int yyn, YYStack yystack, int yylen) throws ParseException {
    /*
     * If YYLEN is nonzero, implement the default value of the action:
     * '$$ = $1'. Otherwise, use the top of the stack.
     * 
     * Otherwise, the following line sets YYVAL to garbage.
     * This behavior is undocumented and Bison
     * users should not rely upon it.
     */
    Object yyval = (0 < yylen) ? yystack.valueAt(yylen - 1) : yystack.valueAt(0);

    yyReducePrint(yyn, yystack);

    switch (yyn) {
      case 2: /* constraint: dimredeflist clauselist */
        if (yyn == 2)
        /* "ce.y":103 */
        {
          yyval = constraint(((CEAST.NodeList) (yystack.valueAt(0))));
        } ;
        break;


      case 5: /* clauselist: clause */
        if (yyn == 5)
        /* "ce.y":113 */
        {
          yyval = nodelist(null, ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 6: /* clauselist: clauselist ';' clause */
        if (yyn == 6)
        /* "ce.y":115 */
        {
          yyval = nodelist(((CEAST.NodeList) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 9: /* projection: segmenttree */
        if (yyn == 9)
        /* "ce.y":133 */
        {
          yyval = projection(((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 10: /* segmenttree: segment */
        if (yyn == 10)
        /* "ce.y":138 */
        {
          yyval = segmenttree(null, ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 11: /* segmenttree: segmenttree '.' segment */
        if (yyn == 11)
        /* "ce.y":140 */
        {
          yyval = segmenttree(((CEAST) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 12: /* segmenttree: segmenttree '.' '{' segmentforest '}' */
        if (yyn == 12)
        /* "ce.y":142 */
        {
          yyval = segmenttree(((CEAST) (yystack.valueAt(4))), ((CEAST.NodeList) (yystack.valueAt(1))));
        } ;
        break;


      case 13: /* segmenttree: segmenttree '{' segmentforest '}' */
        if (yyn == 13)
        /* "ce.y":144 */
        {
          yyval = segmenttree(((CEAST) (yystack.valueAt(3))), ((CEAST.NodeList) (yystack.valueAt(1))));
        } ;
        break;


      case 14: /* segmentforest: segmenttree */
        if (yyn == 14)
        /* "ce.y":149 */
        {
          yyval = nodelist(null, ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 15: /* segmentforest: segmentforest ',' segmenttree */
        if (yyn == 15)
        /* "ce.y":151 */
        {
          yyval = nodelist(((CEAST.NodeList) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 16: /* segment: NAME */
        if (yyn == 16)
        /* "ce.y":156 */
        {
          yyval = segment(((String) (yystack.valueAt(0))), null);
        } ;
        break;


      case 17: /* segment: NAME slicelist */
        if (yyn == 17)
        /* "ce.y":158 */
        {
          yyval = segment(((String) (yystack.valueAt(1))), ((CEAST.SliceList) (yystack.valueAt(0))));
        } ;
        break;


      case 18: /* slicelist: slice */
        if (yyn == 18)
        /* "ce.y":163 */
        {
          yyval = slicelist(null, ((Slice) (yystack.valueAt(0))));
        } ;
        break;


      case 19: /* slicelist: slicelist slice */
        if (yyn == 19)
        /* "ce.y":165 */
        {
          yyval = slicelist(((CEAST.SliceList) (yystack.valueAt(1))), ((Slice) (yystack.valueAt(0))));
        } ;
        break;


      case 20: /* slice: '[' ']' */
        if (yyn == 20)
        /* "ce.y":170 */
        {
          yyval = slice(null);
        } ;
        break;


      case 21: /* slice: '[' subslicelist ']' */
        if (yyn == 21)
        /* "ce.y":172 */
        {
          yyval = slice(((CEAST.SliceList) (yystack.valueAt(1))));
        } ;
        break;


      case 22: /* subslicelist: subslice */
        if (yyn == 22)
        /* "ce.y":177 */
        {
          yyval = slicelist(null, ((Slice) (yystack.valueAt(0))));
        } ;
        break;


      case 23: /* subslicelist: subslicelist ',' subslice */
        if (yyn == 23)
        /* "ce.y":179 */
        {
          yyval = slicelist(((CEAST.SliceList) (yystack.valueAt(2))), ((Slice) (yystack.valueAt(0))));
        } ;
        break;


      case 24: /* subslice: extent */
        if (yyn == 24)
        /* "ce.y":184 */
        {
          yyval = subslice(1, ((String) (yystack.valueAt(0))), null, null);
        } ;
        break;


      case 25: /* subslice: extent ':' extent */
        if (yyn == 25)
        /* "ce.y":186 */
        {
          yyval = subslice(2, ((String) (yystack.valueAt(2))), ((String) (yystack.valueAt(0))), null);
        } ;
        break;


      case 26: /* subslice: extent ':' extent ':' extent */
        if (yyn == 26)
        /* "ce.y":188 */
        {
          yyval = subslice(3, ((String) (yystack.valueAt(4))), ((String) (yystack.valueAt(0))),
              ((String) (yystack.valueAt(2))));
        } ;
        break;


      case 27: /* subslice: extent ':' */
        if (yyn == 27)
        /* "ce.y":190 */
        {
          yyval = subslice(4, ((String) (yystack.valueAt(1))), null, null);
        } ;
        break;


      case 28: /* subslice: extent ':' extent ':' */
        if (yyn == 28)
        /* "ce.y":192 */
        {
          yyval = subslice(5, ((String) (yystack.valueAt(3))), null, ((String) (yystack.valueAt(1))));
        } ;
        break;


      case 30: /* selection: segmenttree '|' filter */
        if (yyn == 30)
        /* "ce.y":203 */
        {
          yyval = selection(((CEAST) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 32: /* filter: predicate ',' predicate */
        if (yyn == 32)
        /* "ce.y":209 */
        {
          yyval = logicalAnd(((CEAST) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 33: /* filter: '!' predicate */
        if (yyn == 33)
        /* "ce.y":211 */
        {
          yyval = logicalNot(((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 34: /* predicate: primary relop primary */
        if (yyn == 34)
        /* "ce.y":216 */
        {
          yyval = predicate(((CEAST.Operator) (yystack.valueAt(1))), ((CEAST) (yystack.valueAt(2))),
              ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 35: /* predicate: primary relop primary relop primary */
        if (yyn == 35)
        /* "ce.y":218 */
        {
          yyval = predicaterange(((CEAST.Operator) (yystack.valueAt(3))), ((CEAST.Operator) (yystack.valueAt(1))),
              ((CEAST) (yystack.valueAt(4))), ((CEAST) (yystack.valueAt(2))), ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 36: /* predicate: primary eqop primary */
        if (yyn == 36)
        /* "ce.y":220 */
        {
          yyval = predicate(((CEAST.Operator) (yystack.valueAt(1))), ((CEAST) (yystack.valueAt(2))),
              ((CEAST) (yystack.valueAt(0))));
        } ;
        break;


      case 37: /* relop: '<' '=' */
        if (yyn == 37)
        /* "ce.y":224 */
        {
          yyval = CEAST.Operator.LE;
        } ;
        break;


      case 38: /* relop: '>' '=' */
        if (yyn == 38)
        /* "ce.y":225 */
        {
          yyval = CEAST.Operator.GE;
        } ;
        break;


      case 39: /* relop: '<' */
        if (yyn == 39)
        /* "ce.y":226 */
        {
          yyval = CEAST.Operator.LT;
        } ;
        break;


      case 40: /* relop: '>' */
        if (yyn == 40)
        /* "ce.y":227 */
        {
          yyval = CEAST.Operator.GT;
        } ;
        break;


      case 41: /* eqop: '=' '=' */
        if (yyn == 41)
        /* "ce.y":231 */
        {
          yyval = CEAST.Operator.EQ;
        } ;
        break;


      case 42: /* eqop: '!' '=' */
        if (yyn == 42)
        /* "ce.y":232 */
        {
          yyval = CEAST.Operator.NEQ;
        } ;
        break;


      case 43: /* eqop: '~' '=' */
        if (yyn == 43)
        /* "ce.y":233 */
        {
          yyval = CEAST.Operator.REQ;
        } ;
        break;


      case 46: /* primary: '(' predicate ')' */
        if (yyn == 46)
        /* "ce.y":239 */
        {
          yyval = ((CEAST) (yystack.valueAt(1)));
        } ;
        break;


      case 47: /* dimredef: NAME '=' slice */
        if (yyn == 47)
        /* "ce.y":245 */
        {
          yyval = null;
          dimredef(((String) (yystack.valueAt(2))), ((Slice) (yystack.valueAt(0))));
        } ;
        break;


      case 48: /* fieldname: NAME */
        if (yyn == 48)
        /* "ce.y":249 */
        {
          yyval = fieldname(((String) (yystack.valueAt(0))));
        } ;
        break;


      case 49: /* constant: STRING */
        if (yyn == 49)
        /* "ce.y":253 */
        {
          yyval = constant(CEAST.Constant.STRING, ((String) (yystack.valueAt(0))));
        } ;
        break;


      case 50: /* constant: LONG */
        if (yyn == 50)
        /* "ce.y":254 */
        {
          yyval = constant(CEAST.Constant.LONG, ((String) (yystack.valueAt(0))));
        } ;
        break;


      case 51: /* constant: DOUBLE */
        if (yyn == 51)
        /* "ce.y":255 */
        {
          yyval = constant(CEAST.Constant.DOUBLE, ((String) (yystack.valueAt(0))));
        } ;
        break;


      case 52: /* constant: BOOLEAN */
        if (yyn == 52)
        /* "ce.y":256 */
        {
          yyval = constant(CEAST.Constant.BOOLEAN, ((String) (yystack.valueAt(0))));
        } ;
        break;



      /* "CEBisonParser.java":825 */

      default:
        break;
    }

    yySymbolPrint("-> $$ =", SymbolKind.get(yyr1_[yyn]), yyval);

    yystack.pop(yylen);
    yylen = 0;
    /* Shift the result of the reduction. */
    int yystate = yyLRGotoState(yystack.stateAt(0), yyr1_[yyn]);
    yystack.push(yystate, yyval);
    return YYNEWSTATE;
  }


  /*--------------------------------.
  | Print this symbol on YYOUTPUT.  |
  `--------------------------------*/

  private void yySymbolPrint(String s, SymbolKind yykind, Object yyvalue) {
    if (0 < yydebug) {
      yycdebug(s + (yykind.getCode() < YYNTOKENS_ ? " token " : " nterm ") + yykind.getName() + " ("
          + (yyvalue == null ? "(null)" : yyvalue.toString()) + ")");
    }
  }


  /**
   * Parse input from the scanner that was specified at object construction
   * time. Return whether the end of the input was reached successfully.
   *
   * @return <tt>true</tt> if the parsing succeeds. Note that this does not
   *         imply that there were no syntax errors.
   */
  public boolean parse() throws ParseException, ParseException

  {


    /* Lookahead token kind. */
    int yychar = YYEMPTY_;
    /* Lookahead symbol kind. */
    SymbolKind yytoken = null;

    /* State. */
    int yyn = 0;
    int yylen = 0;
    int yystate = 0;
    YYStack yystack = new YYStack();
    int label = YYNEWSTATE;



    /* Semantic value of the lookahead. */
    Object yylval = null;



    yycdebug("Starting parse");
    yyerrstatus_ = 0;
    yynerrs = 0;

    /* Initialize the stack. */
    yystack.push(yystate, yylval);



    for (;;)
      switch (label) {
        /*
         * New state. Unlike in the C/C++ skeletons, the state is already
         * pushed when we come here.
         */
        case YYNEWSTATE:
          yycdebug("Entering state " + yystate);
          if (0 < yydebug)
            yystack.print(yyDebugStream);

          /* Accept? */
          if (yystate == YYFINAL_)
            return true;

          /* Take a decision. First try without lookahead. */
          yyn = yypact_[yystate];
          if (yyPactValueIsDefault(yyn)) {
            label = YYDEFAULT;
            break;
          }

          /* Read a lookahead token. */
          if (yychar == YYEMPTY_) {

            yycdebug("Reading a token");
            yychar = yylexer.yylex();
            yylval = yylexer.getLVal();

          }

          /* Convert token to internal form. */
          yytoken = yytranslate_(yychar);
          yySymbolPrint("Next token is", yytoken, yylval);

          if (yytoken == SymbolKind.S_YYerror) {
            // The scanner already issued an error message, process directly
            // to error recovery. But do not keep the error token as
            // lookahead, it is too special and may lead us to an endless
            // loop in error recovery. */
            yychar = Lexer.YYUNDEF;
            yytoken = SymbolKind.S_YYUNDEF;
            label = YYERRLAB1;
          } else {
            /*
             * If the proper action on seeing token YYTOKEN is to reduce or to
             * detect an error, take that action.
             */
            yyn += yytoken.getCode();
            if (yyn < 0 || YYLAST_ < yyn || yycheck_[yyn] != yytoken.getCode()) {
              label = YYDEFAULT;
            }

            /* <= 0 means reduce or error. */
            else if ((yyn = yytable_[yyn]) <= 0) {
              if (yyTableValueIsError(yyn)) {
                label = YYERRLAB;
              } else {
                yyn = -yyn;
                label = YYREDUCE;
              }
            }

            else {
              /* Shift the lookahead token. */
              yySymbolPrint("Shifting", yytoken, yylval);

              /* Discard the token being shifted. */
              yychar = YYEMPTY_;

              /*
               * Count tokens shifted since error; after three, turn off error
               * status.
               */
              if (yyerrstatus_ > 0)
                --yyerrstatus_;

              yystate = yyn;
              yystack.push(yystate, yylval);
              label = YYNEWSTATE;
            }
          }
          break;

        /*-----------------------------------------------------------.
        | yydefault -- do the default action for the current state.  |
        `-----------------------------------------------------------*/
        case YYDEFAULT:
          yyn = yydefact_[yystate];
          if (yyn == 0)
            label = YYERRLAB;
          else
            label = YYREDUCE;
          break;

        /*-----------------------------.
        | yyreduce -- Do a reduction.  |
        `-----------------------------*/
        case YYREDUCE:
          yylen = yyr2_[yyn];
          label = yyaction(yyn, yystack, yylen);
          yystate = yystack.stateAt(0);
          break;

        /*------------------------------------.
        | yyerrlab -- here on detecting error |
        `------------------------------------*/
        case YYERRLAB:
          /* If not already recovering from an error, report this error. */
          if (yyerrstatus_ == 0) {
            ++yynerrs;
            if (yychar == YYEMPTY_)
              yytoken = null;
            yyreportSyntaxError(new Context(this, yystack, yytoken));
          }

          if (yyerrstatus_ == 3) {
            /*
             * If just tried and failed to reuse lookahead token after an
             * error, discard it.
             */

            if (yychar <= Lexer.YYEOF) {
              /* Return failure if at end of input. */
              if (yychar == Lexer.YYEOF)
                return false;
            } else
              yychar = YYEMPTY_;
          }

          /*
           * Else will try to reuse lookahead token after shifting the error
           * token.
           */
          label = YYERRLAB1;
          break;

        /*-------------------------------------------------.
        | errorlab -- error raised explicitly by YYERROR.  |
        `-------------------------------------------------*/
        case YYERROR:
          /*
           * Do not reclaim the symbols of the rule which action triggered
           * this YYERROR.
           */
          yystack.pop(yylen);
          yylen = 0;
          yystate = yystack.stateAt(0);
          label = YYERRLAB1;
          break;

        /*-------------------------------------------------------------.
        | yyerrlab1 -- common code for both syntax error and YYERROR.  |
        `-------------------------------------------------------------*/
        case YYERRLAB1:
          yyerrstatus_ = 3; /* Each real token shifted decrements this. */

          // Pop stack until we find a state that shifts the error token.
          for (;;) {
            yyn = yypact_[yystate];
            if (!yyPactValueIsDefault(yyn)) {
              yyn += SymbolKind.S_YYerror.getCode();
              if (0 <= yyn && yyn <= YYLAST_ && yycheck_[yyn] == SymbolKind.S_YYerror.getCode()) {
                yyn = yytable_[yyn];
                if (0 < yyn)
                  break;
              }
            }

            /*
             * Pop the current state because it cannot handle the
             * error token.
             */
            if (yystack.height == 0)
              return false;


            yystack.pop();
            yystate = yystack.stateAt(0);
            if (0 < yydebug)
              yystack.print(yyDebugStream);
          }

          if (label == YYABORT)
            /* Leave the switch. */
            break;



          /* Shift the error token. */
          yySymbolPrint("Shifting", SymbolKind.get(yystos_[yyn]), yylval);

          yystate = yyn;
          yystack.push(yyn, yylval);
          label = YYNEWSTATE;
          break;

        /* Accept. */
        case YYACCEPT:
          return true;

        /* Abort. */
        case YYABORT:
          return false;
      }
  }



  /**
   * Information needed to get the list of expected tokens and to forge
   * a syntax error diagnostic.
   */
  public static final class Context {
    Context(CEBisonParser parser, YYStack stack, SymbolKind token) {
      yyparser = parser;
      yystack = stack;
      yytoken = token;
    }

    private CEBisonParser yyparser;
    private YYStack yystack;


    /**
     * The symbol kind of the lookahead token.
     */
    public final SymbolKind getToken() {
      return yytoken;
    }

    private SymbolKind yytoken;
    static final int NTOKENS = CEBisonParser.YYNTOKENS_;

    /**
     * Put in YYARG at most YYARGN of the expected tokens given the
     * current YYCTX, and return the number of tokens stored in YYARG. If
     * YYARG is null, return the number of expected tokens (guaranteed to
     * be less than YYNTOKENS).
     */
    int getExpectedTokens(SymbolKind yyarg[], int yyargn) {
      return getExpectedTokens(yyarg, 0, yyargn);
    }

    int getExpectedTokens(SymbolKind yyarg[], int yyoffset, int yyargn) {
      int yycount = yyoffset;
      int yyn = yypact_[this.yystack.stateAt(0)];
      if (!yyPactValueIsDefault(yyn)) {
        /*
         * Start YYX at -YYN if negative to avoid negative
         * indexes in YYCHECK. In other words, skip the first
         * -YYN actions for this state because they are default
         * actions.
         */
        int yyxbegin = yyn < 0 ? -yyn : 0;
        /* Stay within bounds of both yycheck and yytname. */
        int yychecklim = YYLAST_ - yyn + 1;
        int yyxend = yychecklim < NTOKENS ? yychecklim : NTOKENS;
        for (int yyx = yyxbegin; yyx < yyxend; ++yyx)
          if (yycheck_[yyx + yyn] == yyx && yyx != SymbolKind.S_YYerror.getCode()
              && !yyTableValueIsError(yytable_[yyx + yyn])) {
            if (yyarg == null)
              yycount += 1;
            else if (yycount == yyargn)
              return 0; // FIXME: this is incorrect.
            else
              yyarg[yycount++] = SymbolKind.get(yyx);
          }
      }
      if (yyarg != null && yycount == yyoffset && yyoffset < yyargn)
        yyarg[yycount] = null;
      return yycount - yyoffset;
    }
  }



  private int yysyntaxErrorArguments(Context yyctx, SymbolKind[] yyarg, int yyargn) {
    /*
     * There are many possibilities here to consider:
     * - If this state is a consistent state with a default action,
     * then the only way this function was invoked is if the
     * default action is an error action. In that case, don't
     * check for expected tokens because there are none.
     * - The only way there can be no lookahead present (in tok) is
     * if this state is a consistent state with a default action.
     * Thus, detecting the absence of a lookahead is sufficient to
     * determine that there is no unexpected or expected token to
     * report. In that case, just report a simple "syntax error".
     * - Don't assume there isn't a lookahead just because this
     * state is a consistent state with a default action. There
     * might have been a previous inconsistent state, consistent
     * state with a non-default action, or user semantic action
     * that manipulated yychar. (However, yychar is currently out
     * of scope during semantic actions.)
     * - Of course, the expected token list depends on states to
     * have correct lookahead information, and it depends on the
     * parser not to perform extra reductions after fetching a
     * lookahead from the scanner and before detecting a syntax
     * error. Thus, state merging (from LALR or IELR) and default
     * reductions corrupt the expected token list. However, the
     * list is correct for canonical LR with one exception: it
     * will still contain any token that will not be accepted due
     * to an error action in a later state.
     */
    int yycount = 0;
    if (yyctx.getToken() != null) {
      if (yyarg != null)
        yyarg[yycount] = yyctx.getToken();
      yycount += 1;
      yycount += yyctx.getExpectedTokens(yyarg, 1, yyargn);
    }
    return yycount;
  }


  /**
   * Build and emit a "syntax error" message in a user-defined way.
   *
   * @param ctx The context of the error.
   */
  private void yyreportSyntaxError(Context yyctx) {
    if (yyErrorVerbose) {
      final int argmax = 5;
      SymbolKind[] yyarg = new SymbolKind[argmax];
      int yycount = yysyntaxErrorArguments(yyctx, yyarg, argmax);
      String[] yystr = new String[yycount];
      for (int yyi = 0; yyi < yycount; ++yyi) {
        yystr[yyi] = yyarg[yyi].getName();
      }
      String yyformat;
      switch (yycount) {
        default:
        case 0:
          yyformat = "syntax error";
          break;
        case 1:
          yyformat = "syntax error, unexpected {0}";
          break;
        case 2:
          yyformat = "syntax error, unexpected {0}, expecting {1}";
          break;
        case 3:
          yyformat = "syntax error, unexpected {0}, expecting {1} or {2}";
          break;
        case 4:
          yyformat = "syntax error, unexpected {0}, expecting {1} or {2} or {3}";
          break;
        case 5:
          yyformat = "syntax error, unexpected {0}, expecting {1} or {2} or {3} or {4}";
          break;
      }
      yyerror(new MessageFormat(yyformat).format(yystr));
    } else {
      yyerror("syntax error");
    }
  }

  /**
   * Whether the given <code>yypact_</code> value indicates a defaulted state.
   * 
   * @param yyvalue the value to check
   */
  private static boolean yyPactValueIsDefault(int yyvalue) {
    return yyvalue == yypact_ninf_;
  }

  /**
   * Whether the given <code>yytable_</code>
   * value indicates a syntax error.
   * 
   * @param yyvalue the value to check
   */
  private static boolean yyTableValueIsError(int yyvalue) {
    return yyvalue == yytable_ninf_;
  }

  private static final byte yypact_ninf_ = -47;
  private static final byte yytable_ninf_ = -1;

  /*
   * YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   * STATE-NUM.
   */
  private static final byte[] yypact_ = yypact_init();

  private static final byte[] yypact_init() {
    return new byte[] {-47, 40, 18, -47, -5, 23, 17, -47, -47, 26, -47, -47, 15, -5, -47, 35, -47, 50, 21, 50, -1, -47,
        -47, 27, -47, 41, -47, -5, -47, 50, -47, 34, 28, -47, -47, -47, -47, -47, 9, 9, -47, 48, 29, -47, -47, 53, -47,
        53, -47, 31, 50, -47, -47, 36, 9, 39, 42, 43, 44, 45, 9, 9, -47, 51, -47, 34, -47, -47, -47, -47, -47, -47, -47,
        33, -47, 53, 9, -47, -47};
  }

  /*
   * YYDEFACT[STATE-NUM] -- Default reduction number in state STATE-NUM.
   * Performed when YYTABLE does not specify something else to do. Zero
   * means the default is an error.
   */
  private static final byte[] yydefact_ = yydefact_init();

  private static final byte[] yydefact_init() {
    return new byte[] {3, 0, 0, 1, 16, 0, 2, 5, 7, 9, 10, 8, 0, 17, 18, 0, 4, 0, 0, 0, 0, 29, 20, 0, 22, 24, 19, 0, 6,
        0, 11, 14, 0, 48, 49, 50, 51, 52, 0, 0, 30, 31, 0, 44, 45, 0, 21, 27, 47, 0, 0, 13, 33, 0, 0, 0, 39, 0, 40, 0,
        0, 0, 23, 25, 12, 15, 46, 32, 42, 37, 41, 38, 43, 34, 36, 28, 0, 26, 35};
  }

  /* YYPGOTO[NTERM-NUM]. */
  private static final byte[] yypgoto_ = yypgoto_init();

  private static final byte[] yypgoto_init() {
    return new byte[] {-47, -47, -47, -47, 49, -47, -19, 32, 52, -47, -2, -47, 24, -46, -47, -47, -31, 0, -47, -42, -47,
        -47, -47};
  }

  /* YYDEFGOTO[NTERM-NUM]. */
  private static final byte[] yydefgoto_ = yydefgoto_init();

  private static final byte[] yydefgoto_init() {
    return new byte[] {0, 1, 2, 6, 7, 8, 9, 32, 10, 13, 14, 23, 24, 25, 11, 40, 41, 60, 61, 42, 16, 43, 44};
  }

  /*
   * YYTABLE[YYPACT[STATE-NUM]] -- What to do in state STATE-NUM. If
   * positive, shift that token. If negative, reduce the rule whose
   * number is the opposite. If YYTABLE_NINF, syntax error.
   */
  private static final byte[] yytable_ = yytable_init();

  private static final byte[] yytable_init() {
    return new byte[] {31, 63, 33, 34, 35, 36, 37, 52, 53, 12, 31, 26, 33, 34, 35, 36, 37, 38, 73, 74, 21, 4, 39, 67, 4,
        48, 15, 17, 5, 77, 22, 65, 39, 29, 78, 45, 50, 18, 19, 50, 3, 51, 46, 20, 64, 18, 19, 55, 56, 57, 58, 59, 56, 4,
        58, 27, 54, 47, 21, 68, 66, 49, 69, 70, 71, 72, 28, 75, 0, 62, 30, 0, 0, 76};
  }

  private static final byte[] yycheck_ = yycheck_init();

  private static final byte[] yycheck_init() {
    return new byte[] {19, 47, 3, 4, 5, 6, 7, 38, 39, 14, 29, 13, 3, 4, 5, 6, 7, 18, 60, 61, 5, 3, 23, 54, 3, 27, 3, 10,
        10, 75, 15, 50, 23, 12, 76, 8, 8, 11, 12, 8, 0, 13, 15, 17, 13, 11, 12, 18, 19, 20, 21, 22, 19, 3, 21, 20, 8,
        16, 5, 20, 24, 29, 20, 20, 20, 20, 17, 16, -1, 45, 18, -1, -1, 73};
  }

  /*
   * YYSTOS[STATE-NUM] -- The symbol kind of the accessing symbol of
   * state STATE-NUM.
   */
  private static final byte[] yystos_ = yystos_init();

  private static final byte[] yystos_init() {
    return new byte[] {0, 26, 27, 0, 3, 10, 28, 29, 30, 31, 33, 39, 14, 34, 35, 3, 45, 10, 11, 12, 17, 5, 15, 36, 37,
        38, 35, 20, 29, 12, 33, 31, 32, 3, 4, 5, 6, 7, 18, 23, 40, 41, 44, 46, 47, 8, 15, 16, 35, 32, 8, 13, 41, 41, 8,
        18, 19, 20, 21, 22, 42, 43, 37, 38, 13, 31, 24, 41, 20, 20, 20, 20, 20, 44, 44, 16, 42, 38, 44};
  }

  /* YYR1[RULE-NUM] -- Symbol kind of the left-hand side of rule RULE-NUM. */
  private static final byte[] yyr1_ = yyr1_init();

  private static final byte[] yyr1_init() {
    return new byte[] {0, 25, 26, 27, 27, 28, 28, 29, 29, 30, 31, 31, 31, 31, 32, 32, 33, 33, 34, 34, 35, 35, 36, 36,
        37, 37, 37, 37, 37, 38, 39, 40, 40, 40, 41, 41, 41, 42, 42, 42, 42, 43, 43, 43, 44, 44, 44, 45, 46, 47, 47, 47,
        47};
  }

  /* YYR2[RULE-NUM] -- Number of symbols on the right-hand side of rule RULE-NUM. */
  private static final byte[] yyr2_ = yyr2_init();

  private static final byte[] yyr2_init() {
    return new byte[] {0, 2, 2, 0, 3, 1, 3, 1, 1, 1, 1, 3, 5, 4, 1, 3, 1, 2, 1, 2, 2, 3, 1, 3, 1, 3, 5, 2, 4, 1, 3, 1,
        3, 2, 3, 5, 3, 2, 2, 1, 1, 2, 2, 2, 1, 1, 3, 3, 1, 1, 1, 1, 1};
  }



  /* YYRLINE[YYN] -- Source line where rule number YYN was defined. */
  private static final short[] yyrline_ = yyrline_init();

  private static final short[] yyrline_init() {
    return new short[] {0, 101, 101, 106, 108, 112, 114, 119, 120, 132, 137, 139, 141, 143, 148, 150, 155, 157, 162,
        164, 169, 171, 176, 178, 183, 185, 187, 189, 191, 195, 202, 207, 208, 210, 215, 217, 219, 224, 225, 226, 227,
        231, 232, 233, 237, 238, 239, 244, 249, 253, 254, 255, 256};
  }


  // Report on the debug stream that the rule yyrule is going to be reduced.
  private void yyReducePrint(int yyrule, YYStack yystack) {
    if (yydebug == 0)
      return;

    int yylno = yyrline_[yyrule];
    int yynrhs = yyr2_[yyrule];
    /* Print the symbols being reduced, and their result. */
    yycdebug("Reducing stack by rule " + (yyrule - 1) + " (line " + yylno + "):");

    /* The symbols being reduced. */
    for (int yyi = 0; yyi < yynrhs; yyi++)
      yySymbolPrint("   $" + (yyi + 1) + " =", SymbolKind.get(yystos_[yystack.stateAt(yynrhs - (yyi + 1))]),
          yystack.valueAt((yynrhs) - (yyi + 1)));
  }

  /*
   * YYTRANSLATE_(TOKEN-NUM) -- Symbol number corresponding to TOKEN-NUM
   * as returned by yylex, with out-of-bounds checking.
   */
  private static final SymbolKind yytranslate_(int t) {
    // Last valid token kind.
    int code_max = 263;
    if (t <= 0)
      return SymbolKind.S_YYEOF;
    else if (t <= code_max)
      return SymbolKind.get(yytranslate_table_[t]);
    else
      return SymbolKind.S_YYUNDEF;
  }

  private static final byte[] yytranslate_table_ = yytranslate_table_init();

  private static final byte[] yytranslate_table_init() {
    return new byte[] {0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 18, 2, 2, 2, 2, 2, 2, 23, 24, 2, 2, 8, 2, 11, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 16, 10, 19, 20, 21, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 14, 2, 15, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 12, 17, 13, 22, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 1, 2, 3, 4, 5, 6, 7, 9};
  }


  private static final int YYLAST_ = 73;
  private static final int YYEMPTY_ = -2;
  private static final int YYFINAL_ = 3;
  private static final int YYNTOKENS_ = 25;

  /* Unqualified %code blocks. */
  /* "ce.y":24 */


  // Provide accessors for the parser lexer
  Lexer getLexer() {
    return this.yylexer;
  }

  void setLexer(Lexer lexer) {
    this.yylexer = lexer;
  }
  /* "ce.y":31 */
  // Abstract Parser actions

  abstract CEAST constraint(CEAST.NodeList clauses) throws ParseException;

  abstract CEAST projection(CEAST segmenttree) throws ParseException;

  abstract CEAST segment(String name, CEAST.SliceList slices) throws ParseException;

  abstract Slice slice(CEAST.SliceList subslices) throws ParseException;

  abstract Slice subslice(int state, String sfirst, String send, String sstride) throws ParseException;

  abstract void dimredef(String name, Slice slice) throws ParseException;

  abstract CEAST selection(CEAST projection, CEAST filter) throws ParseException;

  abstract CEAST logicalAnd(CEAST lhs, CEAST rhs) throws ParseException;

  abstract CEAST logicalNot(CEAST lhs) throws ParseException;

  abstract CEAST predicate(CEAST.Operator op, CEAST lhs, CEAST rhs) throws ParseException;

  abstract CEAST predicaterange(CEAST.Operator op1, CEAST.Operator op2, CEAST lhs, CEAST mid, CEAST rhs)
      throws ParseException;

  abstract CEAST fieldname(String value) throws ParseException;

  abstract CEAST constant(CEAST.Constant sort, String value) throws ParseException;

  abstract CEAST segmenttree(CEAST tree, CEAST segment);

  abstract CEAST segmenttree(CEAST tree, CEAST.NodeList forest);

  abstract CEAST.NodeList nodelist(CEAST.NodeList list, CEAST ast);

  abstract CEAST.SliceList slicelist(CEAST.SliceList list, Slice slice);


  /* "CEBisonParser.java":1534 */

}
