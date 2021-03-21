/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import ucar.array.ArrayType;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.internal.util.EscapeStrings;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A String expression for denoting a section of a Variable to be read.
 * Parse a section specification String. These have the form:
 *
 * <pre>
 *  section specification := selector | selector '.' selector
 *  selector := varName ['(' dims ')']
 *  varName := ESCAPED_STRING
 * <p/>
 *   dims := dim | dim, dims
 *   dim := ':' | slice | start ':' end | start ':' end ':' stride
 *   slice := INTEGER
 *   start := INTEGER
 *   stride := INTEGER
 *   end := INTEGER
 *   ESCAPED_STRING : must escape characters = ".("
 * </pre>
 * <p/>
 * Nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
 * Optional components are enclosed between square braces '[' and ']'.
 */
public class ParsedArraySectionSpec {
  private static final boolean debugSelector = false;

  /**
   * Parse a section specification String.
   *
   * @param ncfile look for variable in here
   * @param variableSection the string to parse, eg "record(12).wind(1:20,:,3)"
   * @return return ParsedSectionSpec, parsed representation of the variableSection String
   * @throws IllegalArgumentException when token is misformed, or variable name doesnt exist in ncfile
   * @throws ucar.array.InvalidRangeException if section does not match variable shape
   */
  public static ParsedArraySectionSpec parseVariableSection(NetcdfFile ncfile, String variableSection)
      throws ucar.array.InvalidRangeException {
    List<String> tokes = EscapeStrings.tokenizeEscapedName(variableSection);
    if (tokes.isEmpty())
      throw new IllegalArgumentException("empty sectionSpec = " + variableSection);

    String selector = tokes.get(0);
    ParsedArraySectionSpec outerV = parseSelector(ncfile, selector);

    // parse each selector, find the inner variable
    ParsedArraySectionSpec current = outerV;
    for (int i = 1; i < tokes.size(); i++) {
      selector = tokes.get(i);
      current.child = parseSelector(current.getVariable(), selector);
      current = current.child;
    }

    return outerV;
  }

  // selector := varFullNameEsc(indexSelect) or memberNameEsc(indexSelect)
  // parse variable name and index selector out of the selector String. variable name must be escaped
  private static ParsedArraySectionSpec parseSelector(Object parent, String selector)
      throws ucar.array.InvalidRangeException {
    String varNameEsc, indexSelect = null;

    int pos1 = EscapeStrings.indexOf(selector, '(');
    if (pos1 < 0) { // no index
      varNameEsc = selector;
    } else {
      varNameEsc = selector.substring(0, pos1);
      int pos2 = selector.indexOf(')', pos1 + 1);
      indexSelect = selector.substring(pos1, pos2);
    }
    if (debugSelector)
      System.out.println(" parseVariableSection <" + selector + "> = <" + varNameEsc + ">, <" + indexSelect + ">");

    Variable v = null;
    if (parent instanceof NetcdfFile) { // then varNameEsc = varFullNameEsc (i.e. includes groups)
      NetcdfFile ncfile = (NetcdfFile) parent;
      v = ncfile.findVariable(varNameEsc);

    } else if (parent instanceof Structure) { // then varNameEsc = memberNameEsc (i.e. includes groups)
      Structure s = (Structure) parent;
      v = s.findVariable(NetcdfFiles.makeNameUnescaped(varNameEsc)); // s.findVariable wants unescaped version
    }
    if (v == null)
      throw new IllegalArgumentException(" cant find variable: " + varNameEsc + " in selector=" + selector);

    if (v.getArrayType() == ArrayType.SEQUENCE)
      indexSelect = null; // ignore whatever was sent

    // get the selected Ranges, or all, and add to the list
    ucar.array.Section section;
    if (indexSelect != null) {
      section = new ucar.array.Section(indexSelect);
      section = ucar.array.Section.fill(section, v.getShape()); // Check section has no nulls, set from shape array.
    } else {
      section = v.getShapeAsArraySection(); // all
    }

    return new ParsedArraySectionSpec(v, section);
  }

  public static ParsedArraySectionSpec makeFromVariable(Variable v, String selector)
      throws ucar.array.InvalidRangeException {
    String varNameEsc;
    String indexSelect = null;

    int pos1 = EscapeStrings.indexOf(selector, '(');
    if (pos1 < 0) { // no index
      varNameEsc = selector;
    } else {
      varNameEsc = selector.substring(0, pos1);
      int pos2 = selector.indexOf(')', pos1 + 1);
      indexSelect = selector.substring(pos1, pos2);
    }
    if (debugSelector)
      System.out.println(" parseVariableSection <" + selector + "> = <" + varNameEsc + ">, <" + indexSelect + ">");

    if (v.getArrayType() == ArrayType.SEQUENCE) {
      indexSelect = null; // ignore whatever was sent
    }

    // get the selected Ranges, or all, and add to the list
    ucar.array.Section section;
    if (indexSelect != null) {
      section = new ucar.array.Section(indexSelect);
      section = ucar.array.Section.fill(section, v.getShape()); // Check section has no nulls, set from shape array.
    } else {
      section = v.getShapeAsArraySection(); // all
    }

    return new ParsedArraySectionSpec(v, section);
  }

  /**
   * Make section specification String from a ucar.array.Section for a Variable.
   *
   * @param v for this Variable.
   * @param section list of Range. Must includes all parent structures. May be null, meaning use all.
   *        Individual ranges may be null, meaning all for that dimension.
   * @return section specification String.
   */
  public static String makeSectionSpecString(Variable v, @Nullable ucar.array.Section section) {
    StringBuilder sb = new StringBuilder();
    makeSpec(sb, v, section);
    return sb.toString();
  }

  private static ucar.array.Section makeSpec(StringBuilder sb, Variable v, ucar.array.Section orgSection) {
    if (v.isMemberOfStructure()) {
      assert v.getParentStructure() != null;
      orgSection = makeSpec(sb, v.getParentStructure(), orgSection);
      sb.append('.');
    }

    ucar.array.Section vsection = (orgSection == null) ? v.getShapeAsArraySection() : orgSection;

    sb.append(v.isMemberOfStructure() ? NetcdfFiles.makeValidSectionSpecName(v.getShortName())
        : NetcdfFiles.makeFullNameSectionSpec(v));

    if (!v.isVariableLength() && !v.isScalar()) { // sequences cant be sectioned
      sb.append('(');
      for (int count = 0; count < v.getRank(); count++) {
        ucar.array.Range r = vsection.getRange(count);
        if (r == null)
          r = new ucar.array.Range(v.getDimension(count).getLength());
        if (count > 0)
          sb.append(", ");
        sb.append(r);
      }
      sb.append(')');
    }

    if (orgSection == null) {
      return null;
    }

    // return (orgRanges == null) ? null : ranges.subList(v.getRank(), ranges.size());
    List<Range> ranges = vsection.getRanges();
    return new Section(ranges.subList(v.getRank(), vsection.getRank()));
  }

  ///////////////////////////////////////////////////////////////////////////
  // Modify to allow setting after creation
  private final Variable variable; // the variable
  private final ucar.array.Section section; // section for this variable, filled in from variable if needed
  private ParsedArraySectionSpec child; // if not null, variable is a Structure, and this is one of its members

  public ParsedArraySectionSpec(Variable variable, ucar.array.Section section) {
    this.variable = variable;
    this.section = section;
  }

  public Variable getVariable() {
    return variable;
  }

  public ucar.array.Section getArraySection() {
    return section;
  }

  public ParsedArraySectionSpec getChild() {
    return child;
  }

  @Override
  public String toString() {
    return "ParsedSectionSpec{" + "v=" + variable.getFullName() + ", section=" + section + ", child=" + child + '}';
  }

  public String makeSectionSpecString() {
    return ParsedArraySectionSpec.makeSectionSpecString(this.variable, this.section);
  }

}
