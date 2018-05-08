package gov.usgs.earthquake.nshmp.site.www;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import gov.usgs.earthquake.nshmp.geo.Location;

import static gov.usgs.earthquake.nshmp.site.www.Util.GSON;

/**
 * Class to handle all calls to the ArcGis Online service:
 *  https://dev-earthquake.cr.usgs.gov/arcgis/rest/services/haz/basin/MapServer/identify
 * <br><br>
 * 
 * To call the service with a point: {@link #callPointService(Location)}
 * <br><br>
 * 
 * To call the service with a envelope of points: 
 *    {@link #callEnvelopeService(double, double, double, double)}.
 * <br>
 * 
 * @author Brandon Clayton
 */
public class ArcGis {
  private static final String SERVICE_URL = "https://dev01-gis01.cr.usgs.gov/" +
      "arcgis/rest/services/haz/basin/MapServer/identify?"; 
  
  /**
   * Call the identify ArcGis Online web service for a single point location
   *    and return a {@code ArcGisResult}.
   * <br><br>
   * 
   * Deserialization of the ArcGis JSON response is conducted threw,
   *    {@link Util.ArcGisDeserializer}.
   * <br><br>
   * 
   * NOTE: Currently the ArcGis service cannot handle values
   *    to the tenth, to combat this a envelope of points is used using 
   *    a +- 0.001 degrees.
   *    
   * @param latitude - The latitude in degrees of a location.
   * @param longitude - The longitude in degrees of a location.
   * @return The {@code ArcGisResult} with basin terms.
   */
  static ArcGisResult callPointService(Location loc) {
    final double latitude = loc.lat();
    final double longitude = loc.lon();
    
    String urlStr = SERVICE_URL + 
        "geometryType=esriGeometryPoint" +
        "&geometry=" + longitude + "," + latitude +
        "&tolerance=1&mapExtent=1&imageDisplay=1&f=json";
    
    try {
      URL url = new URL(urlStr);
      InputStreamReader reader = new InputStreamReader(url.openStream());
      final ArcGisReturn svcReturn = GSON.fromJson(reader, ArcGisReturn.class);
      reader.close();
      
      return svcReturn.results.get(0);
    } catch (IOException ioe) {
      throw new RuntimeException("Could not reach: " + urlStr);
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("Empty results array returned from: " + urlStr);
    }
  }
 
  /**
   * Call the identify ArcGis Online web service for envelope of points
   *    and return a {@code ArcGisReturn}.
   * <br>
   * Deserialization of the ArcGis JSON response is conducted throw,
   *    {@link Util.ArcGisDeserializer}.
   *    
   * @param minlatitude - The minimum latitude in degrees of a location.
   * @param maxlatitude - The maximum latitude in degrees of a location.
   * @param minlongitude - The minimum longitude in degrees of a location.
   * @param maxlongitude - The maximum longitude in degrees of a location.
   * @return The {@code ArcGisReturn} with basin terms.
   * @throws IOException
   */
  static ArcGisReturn callEnvelopeService(
      double minlatitude,
      double maxlatitude,
      double minlongitude,
      double maxlongitude) throws IOException {
   /* TODO
    * - Handle exceptions better using try catch
    * - Use LocationList?
    * - Add service to handle this call
    */
    String urlStr = SERVICE_URL + 
        "geometryType=esriGeometryEnvelope" +
        "&geometry=" + minlongitude + "," + minlatitude +  "," +
        maxlongitude + "," + maxlatitude +
        "&tolerance=1&mapExtent=1&imageDisplay=1&f=json";
    
    URL url = new URL(urlStr);
    InputStreamReader reader = new InputStreamReader(url.openStream());
    final ArcGisReturn svcReturn = GSON.fromJson(reader, ArcGisReturn.class);
    reader.close();
    
    return svcReturn;
  }
  
  /**
   * Container class to hold the JSON results from the ArcGis web service.
   * <br>
   * The container class is used in the {@link Util.ArcGisDeserializer}. 
   */
  static class ArcGisReturn {
    List<ArcGisResult> results;
    
    String toJsonString() {
      return Util.GSON.toJson(this, ArcGisResult.class);
    }
  }
 
  /**
   * Container class for a single result from the ArcGis web service. 
   * <br>
   * The container class is used in the {@link Util.ArcGisDeserializer}. 
   */
  static class ArcGisResult {
    final double latitude;
    final double longitude;
    final double vs30;
    final Map<String, Double> basinModels;
  
    ArcGisResult(
        Map<String, Double> basinModels, 
        double vs30,
        double latitude, 
        double longitude) {
      this.basinModels = basinModels;
      this.vs30 = vs30;
      this.latitude = latitude;
      this.longitude = longitude;
    }
    
    String toJsonString() {
      return Util.GSON.toJson(this, ArcGisResult.class);
    }
  }
  
}
