package gov.usgs.earthquake.nshmp.site.www.basin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.internal.Csv;
import gov.usgs.earthquake.nshmp.internal.Csv.Record;
import gov.usgs.earthquake.nshmp.site.www.basin.BasinValues.BasinValue;
import gov.usgs.earthquake.nshmp.site.www.basin.Basins.BasinRegion;
import gov.usgs.earthquake.nshmp.util.Maths;

/**
 * Read in all basin data from data directory.
 * 
 * <p> Use {@link BasinDataTest#getBasinData(Basins)} to read in all basin data.
 * 
 * <p> Use {@link BasinDataTest#getBasinValues(String, Location)} to get
 * {@code BasinValues} associated with a basin and {@code Location}.
 * 
 * @author Brandon Clayton
 */
public class BasinData {

  public static final double BASIN_DATA_SPACING = 0.05;

  private ImmutableMap<Basin, ImmutableMap<Location, BasinValues>> basinData;

  private BasinData(ImmutableMap<Basin, ImmutableMap<Location, BasinValues>> basinData) {
    this.basinData = basinData;
  }

  /**
   * Returns the {@code BasinData} associated with all files in the data
   * directory.
   * 
   * @param basins The basin regions from basins.geojson
   */
  public static BasinData readBasinData(Basins basins) {
    ImmutableMap.Builder<Basin, ImmutableMap<Location, BasinValues>> basinData = ImmutableMap.builder();

    for (BasinRegion region : basins) {
      URL url = BasinDataTest.class.getResource("../data/" + region.basin.id + ".csv");
      Path dataPath = Paths.get(url.getPath());

      Csv csv = Csv.create(dataPath);
      List<String> keys = csv.columnKeys();

      try (Stream<Record> records = csv.records()) {
        ImmutableMap<Location, BasinValues> basinRecords = records
            .map(record -> BasinRecord.fromCSVRecord(record, keys, region))
            .collect(ImmutableMap.toImmutableMap(
                BasinRecord::getLocation,
                BasinRecord::getBasinValues));

        basinData.put(region.basin, basinRecords);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return new BasinData(basinData.build());
  }

  /**
   * Returns a map of the basin data.
   */
  public ImmutableMap<Basin, ImmutableMap<Location, BasinValues>> getBasinData() {
    return basinData;
  }

  /**
   * Returns a map of the basin data of a specific basin.
   * 
   * @param basin The basin of interest
   */
  public ImmutableMap<Location, BasinValues> getBasinData(Basin basin) {
    return getBasinData().get(basin);
  }

  /**
   * Returns the {@code BasinValues} associated with a particular basin and
   * {@code Location}.
   * 
   * @param basinId Basin ID
   * @param loc Location inside basin
   */
  public BasinValues getBasinValues(Basin basin, Location loc) {
    Map<Location, BasinValues> basinRecords = basinData.get(basin);
    checkState(basinRecords != null, "Basin [%s] not supported", basin.id);

    double lat = Maths.round(loc.lat(), BASIN_DATA_SPACING);
    double lon = Maths.round(loc.lon(), BASIN_DATA_SPACING);
    loc = Location.create(lat, lon);

    BasinValues basinValues = basinRecords.get(loc);
    checkState(basinValues != null, "Location [%s] not found in basin [%s]", loc, basin.id);

    return basinRecords.get(loc);
  }

  /**
   * Container class to hold a {@code Location} {@code BasinValues}.
   */
  private static class BasinRecord {
    Location loc;
    BasinValues basinValues;

    BasinRecord(Location loc, BasinValue z1p0, BasinValue z2p5) {
      this.loc = checkNotNull(loc);
      this.basinValues = new BasinValues(checkNotNull(z1p0), checkNotNull(z2p5));
    }

    /**
     * Returns the {@code Location}.
     */
    Location getLocation() {
      return loc;
    }

    /**
     * Returns the {@code BasinValues}.
     */
    BasinValues getBasinValues() {
      return basinValues;
    }

    /**
     * Map a CSV {@code Record} to a {BasinRecord}.
     * 
     * @param record The CSV record
     * @param keys The CSV column keys
     * @param region The basin region
     */
    static BasinRecord fromCSVRecord(Record record, List<String> keys, BasinRegion region) {
      double lat = Double.NaN;
      double lon = Double.NaN;
      BasinValue z1p0 = null;
      BasinValue z2p5 = null;

      for (String key : keys) {
        switch (key) {
          case "lat":
            lat = record.getDouble(key);
            break;
          case "lon":
            lon = record.getDouble(key);
            break;
          case "z1p0":
            z1p0 = new BasinValue(region.defaultModel.z1p0, record.getDouble(key));
            break;
          case "z2p5":
            z2p5 = new BasinValue(region.defaultModel.z2p5, record.getDouble(key));
            break;
          default:
            throw new RuntimeException("Key [" + key + "] not supported");
        }
      }

      return new BasinRecord(Location.create(lat, lon), z1p0, z2p5);
    }
  }

}
