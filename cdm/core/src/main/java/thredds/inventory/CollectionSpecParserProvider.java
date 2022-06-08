/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import java.util.Formatter;

/**
 * A Service Provider of {@link CollectionSpecParserAbstract}.
 *
 * @since 5.4
 */
public interface CollectionSpecParserProvider {
  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this provider can parse this collection specification */
  default boolean canParse(String spec) {
    return spec.startsWith(getProtocol() + ":");
  }

  /**
   * Creates a {@link thredds.inventory.CollectionSpecParserAbstract} from a collection specification
   *
   * @param spec collection specification
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.CollectionSpecParserAbstract}
   */
  CollectionSpecParserAbstract create(String spec, Formatter errlog);

  /**
   * Create an {@link thredds.inventory.CollectionSpecParserAbstract} from a given spec
   *
   * @param rootDir the root directory of the collection specification
   * @param regEx the regular expression of the collection specification
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.CollectionSpecParserAbstract}
   */
  CollectionSpecParserAbstract create(String rootDir, String regEx, Formatter errlog);
}
