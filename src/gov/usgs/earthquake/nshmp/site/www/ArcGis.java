package gov.usgs.earthquake.nshmp.site.www;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import static gov.usgs.earthquake.nshmp.site.www.Util.GSON;

/**
 * Class to handle all calls to the ArcGis Online service:
 *  https://dev-earthquake.cr.usgs.gov/arcgis/rest/services/haz/basin/MapServer/identify
 *   
 * @author Brandon Clayton
 */
public class ArcGis {
  private static final String SERVICE_URL = "https://dev-earthquake.cr.usgs.gov/" +
      "arcgis/rest/services/haz/basin/MapServer/identify?"; 
  
  /**
   * Call the identify ArcGis Online web service for a single point location
   *    and return a {@code ArcGisResult}.
   * <br>
   * Deserialization of the ArcGis JSON response is conducted throw,
   *    {@link Util.ArcGisDeserializer}.
   *    
   * @param latitude - The latitude in degrees of a location.
   * @param longitude - The longitude in degrees of a location.
   * @return The {@code ArcGisResult} with basin terms.
   * @throws IOException
   */
  static ArcGisResult callPointService(double latitude, double longitude) 
      throws IOException {
    final double threshold = 0.001;
    
    double latMinus = latitude - threshold;
    double latPlus = latitude + threshold;
    double lonMinus = longitude - threshold;
    double lonPlus = longitude + threshold;
    
    String urlStr = SERVICE_URL + 
        "geometryType=esriGeometryEnvelope" +
        "&geometry=" + lonMinus + "," + latMinus +  "," +
        lonPlus + "," + latPlus +
        "&tolerance=1&mapExtent=1&imageDisplay=1&f=json";
    
    URL url = new URL(urlStr);
    InputStreamReader reader = new InputStreamReader(url.openStream());
    final ArcGisReturn svcReturn = GSON.fromJson(reader, ArcGisReturn.class);
    reader.close();
    ArcGisResult svcResult = null;
    
    try {
      svcResult = svcReturn.results.get(0);
    } catch (Exception e) {
      throw new IllegalStateException("No result from: " + urlStr);
    }
    
    return svcResult;
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
    ArrayList<ArcGisResult> results;
  }
 
  /**
   * Container class for a single result from the ArcGis web service. 
   * <br>
   * The container class is used in the {@link Util.ArcGisDeserializer}. 
   */
  static class ArcGisResult {
    double vs30;
    double latitude;
    double longitude;
    Map<String, Double> basinModels;
   
    void setVs30(double vs30) {
      this.vs30 = vs30;
    }
    
    void setBasinModels(Map<String, Double> basinModels) {
      this.basinModels = basinModels;
    }
    
    void setCoordinates(double lat, double lon) {
      this.latitude = lat;
      this.longitude = lon;
    }
  }
  
}
