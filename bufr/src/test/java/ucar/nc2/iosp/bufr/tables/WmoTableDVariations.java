/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr.tables;

import static java.util.Collections.sort;
import static ucar.nc2.iosp.bufr.tables.BufrTables.openStream;
import static ucar.nc2.iosp.bufr.tables.WmoXmlReader.elementsUsedFromTableD;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class WmoTableDVariations {

  private static void printMainHeading(String title, Formatter out) {
    char borderChar = '=';
    boxedPrint(title, borderChar, out);
  }

  private static void printSubHeading(String title, Formatter out) {
    char borderChar = '-';
    boxedPrint(title, borderChar, out);
  }

  private static void boxedPrint(String title, char boxChar, Formatter out) {
    char[] borderChars = new char[title.length() + 4];
    Arrays.fill(borderChars, boxChar);
    String border = String.valueOf(borderChars) + "\n";
    out.format(border);
    out.format(Joiner.on(" ").join(boxChar, title, boxChar) + "\n");
    out.format(border);
    out.format("\n");
  }

  private static void tableDVariations(String tableLocation, Formatter out, MissingDetailLevel missingDetailLevel)
      throws IOException {
    InputStream ios = openStream(tableLocation);

    printMainHeading("Analyzing WMO BUFR TableD", out);
    out.format("Loading table %s\n", tableLocation);
    out.format("Showing missing entries at level '%s' (%s)\n\n", missingDetailLevel,
        missingDetailLevel.getDescription());
    // we're going to be printing some xml, so let's set the formatting
    XMLOutputter xmlOutputter = new XMLOutputter();
    org.jdom2.output.Format fmt = org.jdom2.output.Format.getPrettyFormat();
    // used to block indent example xml output
    String blockIndent = "    ";
    // have xml indent match the block indent
    fmt.setIndent(blockIndent);
    fmt.setLineSeparator(fmt.getLineSeparator() + blockIndent);
    xmlOutputter.setFormat(fmt);

    // read the table into a Document
    Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      builder.setExpandEntities(false);
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    // Start at the root
    Element root = doc.getRootElement();

    // let's find the various sets of sequence definition terms

    // holds a sorted set of unique terms used to define a sequence (e.g. Category, FXY1, FXY2)
    // sorted by the number of terms used to define the sequence. If two terms list have the same
    // length but different values, compare them using their hashCodes.
    TreeSet<List<String>> termNameVariations = new TreeSet<>((termNameVariation1, termNameVariation2) -> {
      int sort;
      if (termNameVariation1.size() == termNameVariation2.size()) {
        sort = termNameVariation1.hashCode() - termNameVariation2.hashCode();
      } else {
        sort = termNameVariation1.size() - termNameVariation2.size();
      }
      return sort;
    });

    // To try to make sure things are unique, we will index the following maps using
    // a hash of the names array
    // holds an example sequence definition using a given set of terms
    Map<Integer, String> examples = new HashMap<>();
    // holds a count of how many times a given set of terms is used to define a sequence
    Map<Integer, Integer> counts = new HashMap<>();
    // look though all of the direct children of the root
    List<Element> sequences = root.getChildren();
    for (Element sequenceDefTerm : sequences) {
      // make a list of the terms used to define a sequence
      ArrayList<String> termNames = sequenceDefTerm.getChildren().stream().map(Element::getName).sorted()
          .collect(Collectors.toCollection(ArrayList::new));
      // sort the list terms (order matters on computing the hashcode)
      // use the first encounter of a set of terms to define an example of the set of terms
      examples.putIfAbsent(termNames.hashCode(), xmlOutputter.outputString(sequenceDefTerm));
      // increase the count of times the set of terms has been used
      counts.compute(termNames.hashCode(), (hash, count) -> (count == null) ? 1 : count + 1);
      termNameVariations.add(termNames);
    }

    // show variety details
    printSubHeading("Variation analysis of sequence definitions terms", out);

    // show examples
    int variationCount = 0;
    for (List<String> termNames : termNameVariations) {
      variationCount += 1;
      out.format("Variation %d: %d sequence attributes - %s\n", variationCount, termNames.size(), termNames.toString());
      out.format("Found in %d of %d sequence entries.\n", counts.get(termNames.hashCode()), sequences.size());
      out.format("Example Sequence:\n");
      out.format(blockIndent + examples.get(termNames.hashCode()));
      out.format("\n\n");
    }

    // see how often a term appears in a sequence definition
    printSubHeading("Sequence definition term frequency analysis", out);
    Map<String, List<String>> frequency = new HashMap<>();
    List<String> allTermNames = termNameVariations.last();
    for (String termName : allTermNames) {
      // check if a termName appears in every variation of the set of terms used to define
      // a sequence
      boolean foundInAll = termNameVariations.stream().allMatch(name -> name.contains(termName));
      String key = "all";
      // if the term isn't found in all variations, see how many it does appear in
      if (!foundInAll) {
        long found = termNameVariations.stream().filter(name -> name.contains(termName)).count();
        key = String.format("%d of %d", found, termNameVariations.size());
      }
      // store frequency information regarding the number of variations a term appears in
      frequency.computeIfAbsent(key, f -> new ArrayList<>()).add(termName);
    }

    // what do we currently used from Table D?
    String[] elems = elementsUsedFromTableD(root);
    out.format(
        "Currently, only sequences containing at least one of the following terms are being used by netCDF-Java:\n");
    for (int t = 1; t < elems.length; t++) {
      out.format("    *%s\n", elems[t]);
    }
    out.format("\n");

    // print a summary of the frequency analysis of term usage in sequence definitions
    // but treat the terms that appear in every sequence definition special
    out.format("These terms appear in every sequence definition: %s\n\n", Joiner.on(", ").join(frequency.get("all")));

    List<String> keys = new ArrayList<>(frequency.keySet());
    sort(keys);
    keys.stream().filter(key -> !key.equals("all"))
        .map(key -> String.format("Found in %s variations: %s\n", key, Joiner.on(", ").join(frequency.get(key))))
        .forEach(out::format);
    out.format("\n");

    // given what netCDF-Java is actually looking for, let's see how many of the potential sequences will
    // be used.
    // counts.get(termNames.hashCode()) termName: list of terms, count: number of sequence entries associated with it
    List<String> elemsArray = Arrays.asList(elems);
    Long uses = termNameVariations.stream()
        // only consider termNameVariations that include a term netCDF-Java recognizes
        .filter(nameVar -> nameVar.stream().anyMatch(elemsArray::contains))
        .mapToLong(nameVar -> counts.get(nameVar.hashCode())).sum();

    out.format("Overall, %s out of %s potential sequences are seen by netCDF-Java.\n\n", uses, sequences.size());

    if (missingDetailLevel != MissingDetailLevel.none) {
      // which term name variations are currently missed by netCDF-Java
      ArrayList<List<String>> termNameVariationsNotCaught = termNameVariations.stream()
          // only consider termNameVariations that include a term netCDF-Java recognizes
          .filter(nameVar -> nameVar.stream().noneMatch(elemsArray::contains))
          .collect(Collectors.toCollection(ArrayList::new));

      if (missingDetailLevel == MissingDetailLevel.example) {
        // show an example of missed potential sequences
        ArrayList<String> missingExamples = examples.keySet().stream()
            // filter sequences that have a termNameVariation that is not
            // detected by netCDF-Java
            .filter(
                termNameHash -> termNameVariationsNotCaught.stream().map(List::hashCode).anyMatch(termNameHash::equals))
            .map(examples::get).collect(Collectors.toCollection(ArrayList::new));
        // show missing examples
        if (!missingExamples.isEmpty()) {
          printSubHeading("Examples of missed TableD entries", out);
          missingExamples.forEach(example -> out.format(blockIndent + example + "\n"));
          out.format("\n");
        }
      } else if (missingDetailLevel == MissingDetailLevel.all) {
        // show every missed potential sequence
        ArrayList<String> allMissing = sequences.stream()
            // filter sequences that have a termNameVariation that is not
            // detected by netCDF-Java
            .filter(seq -> {
              ArrayList<String> termNames = seq.getChildren().stream().map(Element::getName).sorted()
                  .collect(Collectors.toCollection(ArrayList::new));
              return termNameVariationsNotCaught.contains(termNames);
            })
            // format missed sequence elements and print
            .map(xmlOutputter::outputString).collect(Collectors.toCollection(ArrayList::new));
        // show all missing
        if (!allMissing.isEmpty()) {
          printSubHeading("Missed TableD entries", out);
          allMissing.forEach(example -> out.format(blockIndent + example + "\n"));
          out.format("\n");
        }
      }
    }
    printSubHeading("Analysis complete!", out);
  }

  public static void main(String[] args) throws IOException {
    Formatter out = new Formatter(System.out);
    MissingDetailLevel missingDetailLevel = MissingDetailLevel.none;
    String tableLocation = "resource:/resources/bufrTables/wmo/BUFR_16_0_0_TableD_E.xml";
    out.format("\n");
    tableDVariations(tableLocation, out, missingDetailLevel);
  }

  enum MissingDetailLevel {
    none("do not show any potential missed sequences"), example("show an example of missed potential sequences"), all(
        "show all missed potential sequences");

    private String description;

    MissingDetailLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
