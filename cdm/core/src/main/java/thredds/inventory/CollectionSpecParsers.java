/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import java.util.Formatter;
import java.util.ServiceLoader;
import javax.annotation.Nullable;

/**
 * Static helper methods for CollectionSpecParserAbstract objects.
 *
 * @since 5.4
 */
public class CollectionSpecParsers {

  /**
   * Create an {@link thredds.inventory.CollectionSpecParserAbstract} from a given spec
   *
   * @param spec the collection specification to parse
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.CollectionSpecParserAbstract}
   */
  public static CollectionSpecParserAbstract create(String spec, Formatter errlog) {
    final CollectionSpecParserProvider collectionSpecParserProvider = getProvider(spec);

    return collectionSpecParserProvider != null ? collectionSpecParserProvider.create(spec, errlog)
        : new CollectionSpecParser(spec, errlog);
  }

  /**
   * Create an {@link thredds.inventory.CollectionSpecParserAbstract} from a given spec
   *
   * @param rootDir the root directory of the collection specification
   * @param regEx the regular expression of the collection specification
   * @param errlog put error messages here, may be null
   * @return {@link thredds.inventory.CollectionSpecParserAbstract}
   */
  public static CollectionSpecParserAbstract create(String rootDir, String regEx, Formatter errlog) {
    final CollectionSpecParserProvider collectionSpecParserProvider = getProvider(rootDir);

    return collectionSpecParserProvider != null ? collectionSpecParserProvider.create(rootDir, regEx, errlog)
        : new CollectionSpecParser(rootDir, regEx, errlog);
  }

  @Nullable
  private static CollectionSpecParserProvider getProvider(String spec) {
    CollectionSpecParserProvider collectionSpecParserProvider = null;

    // look for dynamically loaded CollectionSpecParserProvider
    for (CollectionSpecParserProvider provider : ServiceLoader.load(CollectionSpecParserProvider.class)) {
      if (provider.canParse(spec)) {
        collectionSpecParserProvider = provider;
        break;
      }
    }

    return collectionSpecParserProvider;
  }
}
