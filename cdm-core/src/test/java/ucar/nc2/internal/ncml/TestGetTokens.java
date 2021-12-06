/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.internal.ncml.NcmlReader.getTokens;

public class TestGetTokens {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String sep1 = "*|*"; // obnoxious multicharacter separator
  private String sep2 = "."; // obnoxious because in regex the . is a special character
  private String sep3 = "\\"; // obnoxious because of the possibly unintended escape sequence
  private String sep4 = "foo bar"; // obnoxious because it has spaces embedded in a string
  private String sep5 = ""; // should default to basic space separator
  private String sep6 = null;
  private String sep7 = "Broken? * **";

  private String values1 = "Broken? * **";
  private String values2 = "String*|*with*|**|**|*as*|*a*|*separator"; // double separator is particularly nasty?
  private String values3 = "Another string with foobar and foo bar embedded";
  private String values4 = "This string has a \\ in it";
  private String values5 = "This.string.should.have.ten.tokens.when.separated.on.dot";
  private String values6 = "This string.has.several *|*different foo bar separators!";

  private List<String> values1_sep1 = new ArrayList<String>();
  private List<String> values2_sep1 = new ArrayList<String>();
  private List<String> values3_sep1 = new ArrayList<String>();
  private List<String> values4_sep1 = new ArrayList<String>();
  private List<String> values5_sep1 = new ArrayList<String>();
  private List<String> values6_sep1 = new ArrayList<String>();

  private List<String> values1_sep2 = new ArrayList<String>();
  private List<String> values2_sep2 = new ArrayList<String>();
  private List<String> values3_sep2 = new ArrayList<String>();
  private List<String> values4_sep2 = new ArrayList<String>();
  private List<String> values5_sep2 = new ArrayList<String>();
  private List<String> values6_sep2 = new ArrayList<String>();

  private List<String> values1_sep3 = new ArrayList<String>();
  private List<String> values2_sep3 = new ArrayList<String>();
  private List<String> values3_sep3 = new ArrayList<String>();
  private List<String> values4_sep3 = new ArrayList<String>();
  private List<String> values5_sep3 = new ArrayList<String>();
  private List<String> values6_sep3 = new ArrayList<String>();

  private List<String> values1_sep4 = new ArrayList<String>();
  private List<String> values2_sep4 = new ArrayList<String>();
  private List<String> values3_sep4 = new ArrayList<String>();
  private List<String> values4_sep4 = new ArrayList<String>();
  private List<String> values5_sep4 = new ArrayList<String>();
  private List<String> values6_sep4 = new ArrayList<String>();

  private List<String> values1_sep5 = new ArrayList<String>();
  private List<String> values2_sep5 = new ArrayList<String>();
  private List<String> values3_sep5 = new ArrayList<String>();
  private List<String> values4_sep5 = new ArrayList<String>();
  private List<String> values5_sep5 = new ArrayList<String>();
  private List<String> values6_sep5 = new ArrayList<String>();

  private List<String> values1_sep6 = new ArrayList<String>();
  private List<String> values2_sep6 = new ArrayList<String>();
  private List<String> values3_sep6 = new ArrayList<String>();
  private List<String> values4_sep6 = new ArrayList<String>();
  private List<String> values5_sep6 = new ArrayList<String>();
  private List<String> values6_sep6 = new ArrayList<String>();
  private List<String> values1_sep7 = new ArrayList<String>();

  @Before
  public void setUp() throws Exception {

    values1_sep1.add("Broken? * **");
    values2_sep1.add("String");
    values2_sep1.add("with");
    values2_sep1.add("as");
    values2_sep1.add("a");
    values2_sep1.add("separator");
    values3_sep1.add("Another string with foobar and foo bar embedded");
    values4_sep1.add("This string has a \\ in it");
    values5_sep1.add("This.string.should.have.ten.tokens.when.separated.on.dot");
    values6_sep1.add("This string.has.several ");
    values6_sep1.add("different foo bar separators!");

    values1_sep2.add("Broken? * **");
    values2_sep2.add("String*|*with*|**|**|*as*|*a*|*separator");
    values3_sep2.add("Another string with foobar and foo bar embedded");
    values4_sep2.add("This string has a \\ in it");
    values5_sep2.add("This");
    values5_sep2.add("string");
    values5_sep2.add("should");
    values5_sep2.add("have");
    values5_sep2.add("ten");
    values5_sep2.add("tokens");
    values5_sep2.add("when");
    values5_sep2.add("separated");
    values5_sep2.add("on");
    values5_sep2.add("dot");
    values6_sep2.add("This string");
    values6_sep2.add("has");
    values6_sep2.add("several *|*different foo bar separators!");

    values1_sep3.add("Broken? * **");
    values2_sep3.add("String*|*with*|**|**|*as*|*a*|*separator");
    values3_sep3.add("Another string with foobar and foo bar embedded");
    values4_sep3.add("This string has a ");
    values4_sep3.add(" in it");
    values5_sep3.add("This.string.should.have.ten.tokens.when.separated.on.dot");
    values6_sep3.add("This string.has.several *|*different foo bar separators!");

    values1_sep4.add("Broken? * **");
    values2_sep4.add("String*|*with*|**|**|*as*|*a*|*separator");
    values3_sep4.add("Another string with foobar and ");
    values3_sep4.add(" embedded");
    values4_sep4.add("This string has a \\ in it");
    values5_sep4.add("This.string.should.have.ten.tokens.when.separated.on.dot");
    values6_sep4.add("This string.has.several *|*different ");
    values6_sep4.add(" separators!");

    values1_sep5.add("Broken?");
    values1_sep5.add("*");
    values1_sep5.add("**");
    values2_sep5.add("String*|*with*|**|**|*as*|*a*|*separator");
    values3_sep5.add("Another");
    values3_sep5.add("string");
    values3_sep5.add("with");
    values3_sep5.add("foobar");

    values3_sep5.add("and");
    values3_sep5.add("foo");
    values3_sep5.add("bar");
    values3_sep5.add("embedded");
    values4_sep5.add("This");
    values4_sep5.add("string");
    values4_sep5.add("has");
    values4_sep5.add("a");
    values4_sep5.add("\\");
    values4_sep5.add("in");
    values4_sep5.add("it");
    values5_sep5.add("This.string.should.have.ten.tokens.when.separated.on.dot");
    values6_sep5.add("This");
    values6_sep5.add("string.has.several");
    values6_sep5.add("*|*different");
    values6_sep5.add("foo");
    values6_sep5.add("bar");
    values6_sep5.add("separators!");

    values1_sep6.add("Broken?");
    values1_sep6.add("*");
    values1_sep6.add("**");
    values2_sep6.add("String*|*with*|**|**|*as*|*a*|*separator");
    values3_sep6.add("Another");
    values3_sep6.add("string");
    values3_sep6.add("with");
    values3_sep6.add("foobar");

    values3_sep6.add("and");
    values3_sep6.add("foo");
    values3_sep6.add("bar");
    values3_sep6.add("embedded");
    values4_sep6.add("This");
    values4_sep6.add("string");
    values4_sep6.add("has");
    values4_sep6.add("a");
    values4_sep6.add("\\");
    values4_sep6.add("in");
    values4_sep6.add("it");
    values5_sep6.add("This.string.should.have.ten.tokens.when.separated.on.dot");
    values6_sep6.add("This");
    values6_sep6.add("string.has.several");
    values6_sep6.add("*|*different");
    values6_sep6.add("foo");
    values6_sep6.add("bar");
    values6_sep6.add("separators!");

    values1_sep7.add("");
  }

  @Test
  public void testGetTokens() throws Exception {

    assertThat(values1_sep1).isEqualTo(getTokens(values1, sep1));
    assertThat(values2_sep1).isEqualTo(getTokens(values2, sep1));
    assertThat(values3_sep1).isEqualTo(getTokens(values3, sep1));
    assertThat(values4_sep1).isEqualTo(getTokens(values4, sep1));
    assertThat(values5_sep1).isEqualTo(getTokens(values5, sep1));
    assertThat(values6_sep1).isEqualTo(getTokens(values6, sep1));

    assertThat(values1_sep2).isEqualTo(getTokens(values1, sep2));
    assertThat(values2_sep2).isEqualTo(getTokens(values2, sep2));
    assertThat(values3_sep2).isEqualTo(getTokens(values3, sep2));
    assertThat(values4_sep2).isEqualTo(getTokens(values4, sep2));
    assertThat(values5_sep2).isEqualTo(getTokens(values5, sep2));
    assertThat(values6_sep2).isEqualTo(getTokens(values6, sep2));

    assertThat(values1_sep3).isEqualTo(getTokens(values1, sep3));
    assertThat(values2_sep3).isEqualTo(getTokens(values2, sep3));
    assertThat(values3_sep3).isEqualTo(getTokens(values3, sep3));
    assertThat(values4_sep3).isEqualTo(getTokens(values4, sep3));
    assertThat(values5_sep3).isEqualTo(getTokens(values5, sep3));
    assertThat(values6_sep3).isEqualTo(getTokens(values6, sep3));

    assertThat(values1_sep4).isEqualTo(getTokens(values1, sep4));
    assertThat(values2_sep4).isEqualTo(getTokens(values2, sep4));
    assertThat(values3_sep4).isEqualTo(getTokens(values3, sep4));
    assertThat(values4_sep4).isEqualTo(getTokens(values4, sep4));
    assertThat(values5_sep4).isEqualTo(getTokens(values5, sep4));
    assertThat(values6_sep4).isEqualTo(getTokens(values6, sep4));

    assertThat(values1_sep6).isEqualTo(getTokens(values1, sep6));
    assertThat(values2_sep6).isEqualTo(getTokens(values2, sep6));
    assertThat(values3_sep6).isEqualTo(getTokens(values3, sep6));
    assertThat(values4_sep6).isEqualTo(getTokens(values4, sep6));
    assertThat(values5_sep6).isEqualTo(getTokens(values5, sep6));
    assertThat(values6_sep6).isEqualTo(getTokens(values6, sep6));

    assertThat(values1_sep5).isEqualTo(getTokens(values1, sep5));
    assertThat(values2_sep5).isEqualTo(getTokens(values2, sep5));
    assertThat(values3_sep5).isEqualTo(getTokens(values3, sep5));
    assertThat(values4_sep5).isEqualTo(getTokens(values4, sep5));
    assertThat(values5_sep5).isEqualTo(getTokens(values5, sep5));
    assertThat(values6_sep5).isEqualTo(getTokens(values6, sep5));

    assertThat(values1_sep7).isEqualTo(getTokens(values1, sep7));

  }
}
