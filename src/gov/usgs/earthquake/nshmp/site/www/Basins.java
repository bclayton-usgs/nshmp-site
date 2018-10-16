package gov.usgs.earthquake.nshmp.site.www;

import java.awt.Polygon;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
  
  public final String json;

  /** File to read in */
  private static final String BASIN_FILE = "basins.geojson";
 
  /**
   * Return a new instance of {@code Basins} with the static
   *    factory method {@link #getBasins()}.
   *    
   * @param basins The {@code List} of {@link BasinRegion}s.
   * @param fc The {@link FeatureCollection}.
   */
  private Basins(ImmutableList<BasinRegion> basins, FeatureCollection fc, String json) {
    this.basinRegions = basins;
    this.featureCollection = fc;
    this.json = json;
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
      String json = Resources.toString(url, StandardCharsets.UTF_8);
//      if (url == null) {
//        throw new RuntimeException("Could not find: " + BASIN_FILE);
//      }
      
      FeatureCollection fc = GeoJson.fromJson(url);
    
      ImmutableList.Builder<BasinRegion> basinBuilder = ImmutableList.builder();
    
      for (Feature feature : fc.features()) {
        basinBuilder.add(new BasinRegion(feature));
      }
    
      
      return new Basins(basinBuilder.build(), fc, json);
//    } catch (IOException | NullPointerException e) {
      
    } catch (IOException ioe) {
      // TODO propagate IOE from method
      throw new RuntimeException(ioe);
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
      Properties properties = feature.properties();
      this.title = properties.getString("title");
      this.id = properties.getString("id");
      String modelId = properties.getString("defaultModel");
      this.defaultModel = BasinModel.fromId(modelId);
      this.region = Regions.create(
          title, 
          feature.asPolygonBorder(), 
          BorderType.MERCATOR_LINEAR);
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
