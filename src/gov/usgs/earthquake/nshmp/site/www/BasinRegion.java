package gov.usgs.earthquake.nshmp.site.www;

import java.util.ArrayList;
import java.util.List;

import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.geo.LocationList;
import gov.usgs.earthquake.nshmp.geo.Region;
import gov.usgs.earthquake.nshmp.geo.Regions;
import gov.usgs.earthquake.nshmp.internal.GeoJson;
import gov.usgs.earthquake.nshmp.internal.GeoJson.Feature;
import gov.usgs.earthquake.nshmp.internal.GeoJson.FeatureCollection;

import com.google.common.base.Optional;

/**
 * Regions of interest to obtain basin terms.
 * 
 * Region Bounds per Allison:
 *  <ul>
 *    <li> Bay Area: lat 36.42 to 39.02 N, long -123.62 to -120.6 W </li>
 *    <li> Los Angeles: lat 32.94 to 35.16 N, long -119.8 to -116.7 W </li> 
 *    <li> Seattle: lat 46.5 to 48.5 N, long -123.5 to -121.5 W </li> 
 *    <li> Wasatch: lat 39.0 to 42.52 N, long -113.26 to -110.74 W  </li>
 *  </ul>
 *  
 *  Default basin model per Allison:
 *  <ul>
 *    <li> Los Angeles: cvms S4.26m01 (Lee et al., 2014) </li>
 *    <li> Bay Area: BayArea10 (Aagaard et al., 2010) </li>
 *    <li> Wasatch: Wasatch08 (Magistrale et al., 2008) </li>
 *    <li> Seattle: Seattle07 (Stephenson, 2007) </li>
 *  </ul>
 *  
 * @author Brandon Clayton
 */
public enum BasinRegion {

  BAY_AREA(
      "Bay Area",
      new double[] {36.42, 39.02},
      new double[] {-123.62, -120.6},
      BasinModel.BAY_AREA),
  
  LOS_ANGELES(
      "Los Angeles",
      new double[] {32.94, 35.16},
      new double[] {-119.8, -116.7},
      BasinModel.CVMS426M01),
  
  SEATTLE(
      "Seattle",
      new double[] {46.5, 48.5},
      new double[] {-123.5, -121.5},
      BasinModel.SEATTLE),
 
  WASATCH(
      "Wasatch",
      new double[] {39.0, 42.52},
      new double[] {-113.26, -110.74},
      BasinModel.WASATCH);
 
  public String label;
  public String id;
  public final double minlatitude;
  public final double maxlatitude;
  public final double minlongitude;
  public final double maxlongitude;
  public final BasinModel defaultModel;
  private Region region;
  
  private BasinRegion(
      String label, 
      double[] latRange, 
      double[] lonRange,
      BasinModel defaultModel) {
    this.label = label;
    this.id = Util.toUpperCamelCase(this);
    
    this.minlatitude = latRange[0];
    this.maxlatitude = latRange[1];
    this.minlongitude = lonRange[0];
    this.maxlongitude = lonRange[1];
    this.defaultModel = defaultModel;
    
    this.region = this.createRegion();
  }
  
  /**
   * Create a {@code Region} using {@link Regions#createRectangular}
   *    
   * @return The {@code Region}
   */
  private Region createRegion() {
    Location minBounds = Location.create(this.minlatitude, this.minlongitude);
    Location maxBounds = Location.create(this.maxlatitude, this.maxlongitude);
    
    return Regions.createRectangular(this.label, minBounds, maxBounds);
  }
 
  /**
   * Return a {@code BasinRegion} that contains a specific latitude and 
   *    longitude.
   *    
   * @param lat - The latitude in degrees.
   * @param lon - The longitude in degrees.
   * @return The {@code BasinRegions}.
   */
  static BasinRegion findRegion(double lat, double lon) {
    for (BasinRegion basinRegion : values()) {
      Location loc = Location.create(lat, lon);
      Boolean isContained = basinRegion.region.contains(loc);
      if (isContained) return basinRegion;
    }
    
    return null;
  }
  
  /**
   * Create a GeoJson polygon {@code Feature} using {@link GeoJson#createPolygon}
   * 
   * @return The polygon {@code Feature}.
   */
  Feature toPolygonFeature() {
    ArrayList<Location> locs = new ArrayList<>();
    locs.add(Location.create(this.minlatitude, this.minlongitude));
    locs.add(Location.create(this.minlatitude, this.maxlongitude));
    locs.add(Location.create(this.maxlatitude, this.maxlongitude));
    locs.add(Location.create(this.maxlatitude, this.minlongitude));
    locs.add(Location.create(this.minlatitude, this.minlongitude));
    
    LocationList locationList = LocationList.create(locs);
    Feature feature = GeoJson.createPolygon(
        this.label, 
        locationList, 
        Optional.of(this.id), 
        Optional.absent());
    
    return feature;
  }
  
  /**
   * Create a {@code FeatureCollection} of all {@code BasinRegion}.
   * 
   * @return The {@code FeatureCollection}.
   */
  static FeatureCollection<Feature> toFeatureCollection() {
    List<Feature> features = new ArrayList<>();
    
    FeatureCollection<Feature> fc = new FeatureCollection<>();
    for (BasinRegion basinRegion : values()) {
      features.add(basinRegion.toPolygonFeature());
    }
    fc.features = features;
    fc.properties = new FeatureCollectionProperties();
    
    return fc;
  }
  
  /**
   * Container class for the {@code FeatureCollection} properties. 
   */
  private static class FeatureCollectionProperties {
    String label;
    
    private FeatureCollectionProperties() {
      this.label = "Basin Regions";
    }
  }
 
}
