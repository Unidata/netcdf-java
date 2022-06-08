/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.s3;

import com.google.re2j.Pattern;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.CollectionSpecParserAbstract;
import thredds.inventory.CollectionSpecParserProvider;
import ucar.unidata.io.s3.CdmS3Uri;

/**
 * Parses the collection specification string for S3 files.
 **/
public class CollectionSpecParserS3 extends CollectionSpecParserAbstract {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String protocol = CdmS3Uri.SCHEME_CDM_S3;

  private CollectionSpecParserS3(String spec, String rootDir, String filterAndDateMark, String delimiter,
      String fragment, Formatter errlog) {
    super(spec, rootDir, filterAndDateMark, delimiter, fragment, errlog);
  }

  /**
   * Example spec: "cdms3:top-bucket/directory/#dateFormatMark#regExp#delimiter=/"
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
   * and you can't use any part of the dateFormat to filter on.
   *
   * @param collectionSpec the collection spec
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.s3.CollectionSpecParserS3}
   */
  public static CollectionSpecParserS3 create(String collectionSpec, Formatter errlog) {
    final String delimiter = getDelimiter(collectionSpec);
    final String fragment = getFragment(collectionSpec);
    final String rootDir = getRootDirForS3(collectionSpec, delimiter);
    final String filterAndDateMark = getFilterAndDateMarkStringForS3(collectionSpec, delimiter);
    return new CollectionSpecParserS3(collectionSpec, rootDir, filterAndDateMark, delimiter, fragment, errlog);
  }

  /**
   * @param rootDir the root directory
   * @param regExp the regular expression to use as a filter
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.s3.CollectionSpecParserS3}
   */
  public static CollectionSpecParserS3 create(String rootDir, String regExp, Formatter errlog) {
    final String delimiter = getDelimiter(rootDir);
    final String fragment = getFragment(rootDir);
    return new CollectionSpecParserS3(rootDir + delimiter + regExp, rootDir, regExp, delimiter, fragment, errlog);
  }

  @Override
  public String getFilePath(String filename) {
    if (!rootDir.contains("?")) {
      return rootDir + "?" + filename;
    }
    return rootDir.endsWith(delimiter) ? rootDir + filename : rootDir + delimiter + filename;
  }

  private static String getRootDirForS3(String spec, String delimiter) {
    // Remove fragments (may contain delimiter)
    final String[] hashSegments = spec.split("#");
    final String specWithoutFragments =
        Arrays.stream(hashSegments).filter(s -> !s.contains("=")).collect(Collectors.joining(""));

    // The rootDir should include the scheme, bucket, key prefix (if there is a delimiter)
    final String bucket = specWithoutFragments.split(Pattern.quote("?"))[0];
    final String key = specWithoutFragments.contains("?") ? specWithoutFragments.split(Pattern.quote("?"))[1] : "";
    final String prefix = getRootDir(key, "", delimiter);
    return prefix.isEmpty() ? bucket : bucket + "?" + prefix + delimiter; // trailing delimiter necessary
  }

  private static String getDelimiter(String spec) {
    if (!spec.toLowerCase(Locale.ROOT).contains("delimiter=")) {
      return "";
    }

    return spec.toLowerCase(Locale.ROOT).split("delimiter=")[1].substring(0, 1);
  }

  private static String getFragment(String spec) {
    final String[] hashSegments = spec.split("#");
    // The date mark can also contain '#', so check for an '=' to verify it is a fragment
    final String fragment = Arrays.stream(hashSegments).filter(s -> s.contains("=")).collect(Collectors.joining(""));
    return fragment.isEmpty() ? "" : "#" + fragment;
  }

  // Return MFileS3 "name" (entire key unless delimiter is set, otherwise key after prefix)
  private static String getFilterAndDateMarkStringForS3(String spec, String delimiter) {
    if (!spec.contains("?")) {
      return null;
    }

    if (delimiter.isEmpty() && spec.contains("**")) {
      throw new IllegalArgumentException("Must set a delimiter in order to use pattern '**' in spec: " + spec);
    }

    final String keyAndFragments = spec.split(Pattern.quote("?"))[1];

    // remove fragments
    final String[] hashSegments = keyAndFragments.split("#");
    final List<String> nonFragments =
        Arrays.stream(hashSegments).filter(segment -> !segment.contains("=")).collect(Collectors.toList());
    final String key = String.join("#", nonFragments);

    return getFilterAndDateMark(key, delimiter);
  }

  /**
   * An S3 Service Provider of {@link CollectionSpecParserAbstract}.
   */
  public static class Provider implements CollectionSpecParserProvider {
    @Override
    public String getProtocol() {
      return protocol;
    }

    @Override
    public CollectionSpecParserAbstract create(String spec, Formatter errlog) {
      return CollectionSpecParserS3.create(spec, errlog);
    }

    @Override
    public CollectionSpecParserAbstract create(String rootDir, String regEx, Formatter errlog) {
      return CollectionSpecParserS3.create(rootDir, regEx, errlog);
    }
  }
}
