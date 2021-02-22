package thredds.inventory;

/**
 * A factory that produces a CloseableIterator.
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
 *  void method(CloseableIterable&lt;T&gt; ballotsIterable) {
 *     try (Stream&lt;T&gt; ballotsStream = ballotsIterable.iterator().stream()) {
 *       ballotsStream.filter(b -&gt; b.state == State.CAST)
 *                    .forEach(ballot -&gt; {
 *                       // do stuff
 *                    });
 *     }
 *  }
 * </pre>
 */
public interface CloseableIterable<T> extends Iterable<T> {
  @Override
  CloseableIterator<T> iterator();
}
