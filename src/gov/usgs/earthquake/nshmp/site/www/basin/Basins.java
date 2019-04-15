package gov.usgs.earthquake.nshmp.site.www.basin;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import gov.usgs.earthquake.nshmp.geo.BorderType;
import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.geo.Region;
import gov.usgs.earthquake.nshmp.geo.Regions;
import gov.usgs.earthquake.nshmp.geo.json.Feature;
import gov.usgs.earthquake.nshmp.geo.json.FeatureCollection;
import gov.usgs.earthquake.nshmp.geo.json.GeoJson;
import gov.usgs.earthquake.nshmp.geo.json.Properties;

/**
 * Basin container to hold basin regions created from the basins.geojson file.
 * 
 * <p> Use {@link Basins#getBasins()}
 * 
 * @author Brandon Clayton
 */
public class Basins implements Iterable<Basins.BasinRegion> {
  private static final String BASIN_FILE = "basins.geojson";

  private List<BasinRegion> basinRegions;

  private final String json;

  private Basins(List<BasinRegion> basinRegions, String json) {
    this.basinRegions = basinRegions;
    this.json = json;
  }

  /** Return the basin regions (backed by {@link ImmutableList}). */
  public List<BasinRegion> basinRegions() {
    return basinRegions;
  }

  /**
   * Find a basin region that contains a specific longitude and latitude.
   * 
   * <p> Note: null is returned if no basin region is found
   * 
   * @param latitude in degrees.
   * @param longitude in degrees.
   */
  public BasinRegion findRegion(double latitude, double longitude) {
    Location loc = Location.create(latitude, longitude);

    return basinRegions.stream()
        .filter((basin) -> basin.region.contains(loc))
        .findFirst()
        .orElse(null);
  }

  /**
   * Read in the basins.geojson file and return a new instance of
   * {@link Basins}.
   * 
   * @throws RuntimeException If file cannot be read.
   */
  public static Basins getBasins() {
    try {
      URL url = Basins.class.getResource(BASIN_FILE);
      String json = Resources.toString(url, StandardCharsets.UTF_8);

      FeatureCollection fc = GeoJson.from(url).toFeatureCollection();

      ImmutableList.Builder<BasinRegion> basinBuilder = ImmutableList.builder();

      for (Feature feature : fc.features()) {
        basinBuilder.add(new BasinRegion(feature));
      }

      return new Basins(basinBuilder.build(), json);
    } catch (IOException ioe) {
      // TODO propagate IOE from method
      throw new RuntimeException(ioe);
    }
  }

  /** Return the raw JSON string. */
  public String json() {
    return json;
  }

  @Override
  public Iterator<BasinRegion> iterator() {
    return basinRegions.iterator();
  }

  /** Container for a basin region. */
  public static class BasinRegion {
    public final String title;
    public final String id;
    public final BasinModel defaultModel;
    public final Region region;

    private BasinRegion(Feature feature) {
      Properties properties = feature.properties();
      title = properties.getString("title");
      id = properties.getString("id");
      String modelId = properties.getString("defaultModel");
      defaultModel = BasinModel.fromId(modelId);
      region = Regions.create(
          title,
          feature.asPolygonBorder(),
          BorderType.MERCATOR_LINEAR);
    }

  }

}
