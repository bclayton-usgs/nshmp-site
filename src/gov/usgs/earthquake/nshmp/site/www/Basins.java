package gov.usgs.earthquake.nshmp.site.www;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.common.collect.ImmutableList;

import gov.usgs.earthquake.nshmp.geo.BorderType;
import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.geo.Region;
import gov.usgs.earthquake.nshmp.geo.Regions;
import gov.usgs.earthquake.nshmp.json.Feature;
import gov.usgs.earthquake.nshmp.json.FeatureCollection;
import gov.usgs.earthquake.nshmp.json.GeoJsonType;
import gov.usgs.earthquake.nshmp.json.Geometry;
import gov.usgs.earthquake.nshmp.json.Polygon;
import gov.usgs.earthquake.nshmp.json.Properties;

/**
 * Class to read in the GeoJson feature collection file of basins, basins.geojson.
 * <br><br>
 * 
 * The predefined basins of interest with default {@link BasinModel}s are:
 *    <ul>
 *      <li> Los Angeles: cvms426m01 </li>
 *      <li> Bay Area: bayarea </li>
 *      <li> Wasatch Front: Wasatch </li>
 *      <li> Puget Lowland: Seattle </li>
 *    </ul>
 * <br>
 *  
 * Use the static factory method {@link #getBasins()} to read in the 
 *    basins.geojson file into a {@link FeatureCollection} and 
 *     a {@code List} of {@link BasinRegion}s.
 * <br><br>
 * 
 * To obtain a {@link BasinRegion} that contains a certain 
 *    {@link Location}, use {@link Basins#findRegion(Location)} or 
 *    {@link Basins#findRegion(double, double)}.
 * <br><br>
 * 
 * @author Brandon Clayton
 */
public class Basins {
  /** A {@code List} of all the basins read in from basins.geojson */
  public ImmutableList<BasinRegion> basinRegions;
  /** The {@link FeatureCollection} read in */
  public FeatureCollection featureCollection;

  /** File to read in */
  private static final String BASIN_FILE = "basins.geojson";
 
  /**
   * Return a new instance of {@code Basins} with the static
   *    factory method {@link #getBasins()}.
   *    
   * @param basins The {@code List} of {@link BasinRegion}s.
   * @param fc The {@link FeatureCollection}.
   */
  private Basins(ImmutableList<BasinRegion> basins, FeatureCollection fc) {
    this.basinRegions = basins;
    this.featureCollection = fc;
  }
 
  /**
   * Static factory method to read in a {@link FeatureCollection} from
   *    the basins.geojson GeoJson feature collection file and
   *    return a new instance of {@link Basins}.
   *    
   * @return A new instance of {@code Basins}.
   * @throws RuntimeException If file cannot be read.
   */
  public static Basins getBasins() {
    try {
      URL url = Basins.class.getResource(BASIN_FILE);
      if (url == null) {
        throw new RuntimeException("Could not find: " + BASIN_FILE);
      }
      
      FeatureCollection fc = FeatureCollection.read(url);
    
      ImmutableList.Builder<BasinRegion> basinBuilder = ImmutableList.builder();
    
      for (Feature feature : fc.getFeatures()) {
        basinBuilder.add(new BasinRegion(feature));
      }
    
      return new Basins(basinBuilder.build(), fc);
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException("Could not read in " + BASIN_FILE);
    }
  }

  /**
   * Find a {@link BasinRegion} that contains a specific longitude
   *    and latitude inside the {@code BasinRegion}'s {@code Polygon}.
   * <br>
   * 
   * If no {@code BasinRegion} is found, return {@code null}.
   * <br>
   * 
   * @param lat The latitude in degrees.
   * @param lon The longitude in degrees.
   * @return The {@code BasinRegion} that contains the specific 
   *    location or {@code null}.
   */
  public BasinRegion findRegion(double latitude, double longitude) {
    Location loc = Location.create(latitude, longitude);
    return this.findRegion(loc);
  }
  
  /**
   * Find a {@link BasinRegion} that contains a specific {@link Location}
   *    inside the {@code BasinRegion}'s {@code Polygon}.
   * <br>
   * 
   * If no {@code BasinRegion} is found, return {@code null}.
   * <br>
   * 
   * @param loc The {@code Location}
   * @return The {@code BasinRegion} that contains the specific 
   *    location or {@code null}.
   */
  public BasinRegion findRegion(Location loc) {
    for (BasinRegion basin : basinRegions) {
      if (basin.region.contains(loc)) {
        return basin;
      }
    }
    return null;
  }
 
  /**
   * Create a {@code BasinRegion} using a GeoJson {@link Feature} with a 
   *    {@link Polygon} GeoJson {@link Geometry}.
   * <br><br>
   * 
   * A {@code BasinRegion} represents a predefined region representing 
   *    a basin of interest. 
   * <br><br>
   * 
   * The following {@link Properties} of the {@code Feature} must be present:
   *    <ul>
   *      <li> "title" </li>
   *      <li> "id" </li>
   *      <li> "defaultModel" </li>
   *    </ul>
   * The "defaultModel" must be one of the {@link BasinModel} corresponding
   * 
   * 
   * @author Brandon Clayton
   */
  public static class BasinRegion {
    /** 
     * The title for a {@code BasinRegion}, provided by 
     *    {@link Properties#getStringProperty(String)} where 
     *    the argument equals "title".
     */
    public final String title;
    /** 
     * The id for a {@code BasinRegion}, provided by
     *    {@link Properties#geStringProperty(String)} where 
     *    the argument equals "id".
     */
    public final String id;
    /** 
     * The default basin model for a {@code BasinRegion}, provided
     *    by {@link Properties#getStringProperty(String)} where 
     *    the argument equals "defaultModel"
     */
    public final BasinModel defaultModel;
    /** 
     * The {@link Region} for the {@code BasinRegion} created 
     *    with {@link Polygon#toRegion(String)}.
     */
    public final Region region;
    
    /**
     * Create a new {@code BasinRegion}.
     * 
     * @param feature The {@link Feature}
     */
    private BasinRegion(Feature feature) {
      Properties properties = feature.getProperties();
      this.title = properties.getStringProperty("title");
      this.id = properties.getStringProperty("id");
      String modelId = properties.getStringProperty("defaultModel");
      this.defaultModel = BasinModel.fromId(modelId);
      this.region = feature.getGeometry().asPolygon().toRegion(title);
    }
    
    /**
     * Turn a {@code BasinRegion} into a JSON {@code String}.
     * 
     * @return The JSON {@code String}.
     */
    public String toJsonString() {
      return Util.GSON.toJson(this);
    }
    
  }

}
