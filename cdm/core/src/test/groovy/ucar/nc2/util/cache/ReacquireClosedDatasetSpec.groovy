package ucar.nc2.util.cache

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import ucar.nc2.dataset.DatasetUrl
import ucar.nc2.dataset.NetcdfDataset
import ucar.unidata.util.test.TestDir

/**
 * Tests caching behavior when datasets are closed and then reacquired.
 *
 * @author cwardgar
 * @since 2016-01-02
 */
class ReacquireClosedDatasetSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(ReacquireClosedDatasetSpec)
    
    def setupSpec() {
        // All datasets, once opened, will be added to this cache. Config values copied from CdmInit.
        NetcdfDataset.initNetcdfFileCache(100, 150, 12 * 60);
    }

    def cleanupSpec() {
        // Undo global changes we made in setupSpec() so that they do not affect subsequent test classes.
        NetcdfDataset.shutdown();
    }

    def "reacquire"() {
        setup: 'location'
        String location = TestDir.cdmLocalTestDataDir + "jan.nc"

        when: 'Acquire and close dataset 4 times'
        (1..4).each {
            NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(location), true, null).close()
        }

        and: 'Query cache stats'
        Formatter formatter = new Formatter()
        NetcdfDataset.netcdfFileCache.showStats(formatter)

        then: 'The cache will have recorded 1 miss (1st trial) and 3 hits (subsequent trials)'
        // This is kludgy, but FileCache doesn't provide getHits() or getMisses() methods.
        formatter.toString().trim() ==~ /hits= 3 miss= 1 nfiles= \d+ elems= \d+/

        // Prior to 2016-03-09 bug fix in AbstractIOServiceProvider.getLastModified(),
        // this would record 0 hits and 4 misses.
    }
}
