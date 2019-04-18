package gov.usgs.earthquake.nshmp.site.www.basin;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import gov.usgs.earthquake.nshmp.geo.Location;

/**
 * Test for BasinData.
 * 
 * @author Brandon Clayton
 */
public class BasinDataTest {

  private static final Basins BASINS = Basins.getBasins();
  private static final BasinData BASIN_DATA = BasinData.readBasinData(BASINS);

  @Test
  public void equals() {
    for (Basin basin : BASIN_DATA.getBasinData().keySet()) {
      Map<Location, BasinValues> basinRecords = BASIN_DATA.getBasinData(basin);

      for (Location loc : basinRecords.keySet()) {
        BasinValues expected = basinRecords.get(loc);
        BasinValues actual = BASIN_DATA.getBasinValues(basin, loc);

        assertEquals(expected.z1p0.model, actual.z1p0.model);
        assertEquals(expected.z2p5.model, actual.z2p5.model);

        assertEquals(expected.z1p0.value, actual.z1p0.value, 0);
        assertEquals(expected.z2p5.value, actual.z2p5.value, 0);
      }
    }
  }

}
