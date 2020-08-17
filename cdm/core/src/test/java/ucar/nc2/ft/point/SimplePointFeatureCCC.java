package ucar.nc2.ft.point;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.ft.IOIterator;

/**
 * @author cwardgar
 * @since 2015/10/06
 */
public class SimplePointFeatureCCC extends PointFeatureCCCImpl {
  private final List<PointFeatureCC> pointFeatCCs;

  public SimplePointFeatureCCC(String name, CalendarDateUnit timeUnit, String altUnits,
      FeatureType collectionFeatureType) {
    super(name, timeUnit, altUnits, collectionFeatureType);
    this.pointFeatCCs = new LinkedList<>();
  }

  public PointFeatureCC add(PointFeatureCC pointFeatCC) {
    this.pointFeatCCs.add(pointFeatCC);
    return pointFeatCC;
  }

  @Override
  public IOIterator<PointFeatureCC> getCollectionIterator() {
    return new IOIterator<PointFeatureCC>() {
      private final Iterator<PointFeatureCC> pfccIter = pointFeatCCs.iterator();

      @Override
      public boolean hasNext() {
        return pfccIter.hasNext();
      }

      @Override
      public PointFeatureCC next() {
        return pfccIter.next();
      }

      @Override
      public void close() {
        // no-op
      }
    };
  }
}
