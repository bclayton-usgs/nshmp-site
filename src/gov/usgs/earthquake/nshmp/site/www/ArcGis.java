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
  static ArcGisResult callService(double latitude, double longitude) 
      throws IOException {
    String urlStr = SERVICE_URL + 
        "geometry=" + longitude + "," + latitude + 
        "&tolerance=1&mapExtent=1&imageDisplay=1&f=json";
    
    URL url = new URL(urlStr);
    InputStreamReader reader = new InputStreamReader(url.openStream());
    final ArcGisReturn svcReturn = GSON.fromJson(reader, ArcGisReturn.class);
    ArcGisResult svcResult = null;
    try {
      svcResult = svcReturn.results.get(0);
    } catch (Exception e) {
      throw new IllegalStateException("No result from: " + urlStr);
    }
    return svcResult;
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
    Map<String, Double> basinModels;
    
    void setVs30(double vs30) {
      this.vs30 = vs30;
    }
    
    void setBasinModels(Map<String, Double> basinModels) {
      this.basinModels = basinModels;
    }
  }
  
}
