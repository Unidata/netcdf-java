/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An iterator that must be closed.
 * <p>
 * </p>
 * 
 * <pre>
 * void method(CloseableIterable&lt;T&gt; ballotsIterable) {
 *   try (CloseableIterator&lt;T&gt; ballotsIter = ballotsIterable.iterator()) {
 *     while (ballotsIter.hasNext()) {
 *       T ballot = ballotsIter.next();
 *       // do stuff
 *     }
 *   }
 * }
 * </pre>
 * 
 * or
 * <p>
 * </p>
 * 
 * <pre>
 *  void method(CloseableIterator&lt;T&gt; ballotsIter) {
 *     try (Stream&lt;T&gt; ballotsStream = ballotsIter.stream()) {
 *       ballotsStream.filter(b -&gt; b.state == State.CAST)
 *                    .forEach(ballot -&gt; {
 *                       // do stuff
 *                    });
 *     }
 *  }
 * </pre>
 * <p>
 * The convention is that if CloseableIterator is passed to a method, it is not already in a try-finally block.
 * <p>
 * </p>
 *
 * @see <a href=
 *      "https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/util/CloseableIterator.java">Spring
 *      CloseableIterator</a>
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

  /**
   * IOException is converted to RuntimeException.
   */
  @Override
  void close();

  /**
   * Could be parallel.
   */
  default Spliterator<T> spliterator() {
    return Spliterators.spliterator(this, 0, 0);
  }

  /**
   * Convert to a stream, can be declared as a resource in a try-with-resources statement.
   */
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false).onClose(this::close);
  }
}
