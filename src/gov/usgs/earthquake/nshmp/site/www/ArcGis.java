package gov.usgs.earthquake.nshmp.site.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.util.Maths;

import static gov.usgs.earthquake.nshmp.site.www.BasinUtil.GSON;

/**
 * Wrapper around the supporting ArcGIS online basin data service.
 * 
 * <p> ArcGis point geometry wrapper service call:
 * {@link ArcGis#callPointService(Location)}
 * 
 * <p> Note: Latitude and longitude are rounded to the nearest {@code 0.01}
 * 
 * @author Brandon Clayton
 */
class ArcGis {
  
  private static final String QUERY_BASE = "/arcgis/rest/services/haz/basin/MapServer/identify?";
  private static final String SERVICE_URL = BasinUtil.ARCGIS_HOST + QUERY_BASE;

  static final double ROUND_MODEL = 0.01;

  /**
   * Return {@code ArcGisResult} from the ArcGis online web service for a point
   * geometry.
   * 
   * @param latitude in degrees
   * @param longitude in degrees
   */
  static ArcGisResult callPointService(double latitude, double longitude) {
    latitude = Maths.round(latitude, ROUND_MODEL);
    longitude = Maths.round(longitude, ROUND_MODEL);

    String urlStr = SERVICE_URL +
        "geometryType=esriGeometryPoint" +
        "&geometry=" + longitude + "," + latitude +
        "&tolerance=1&mapExtent=1&imageDisplay=1&f=json";

    try {
      URL url = new URL(urlStr);
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      final ArcGisReturn svcReturn = GSON.fromJson(reader, ArcGisReturn.class);
      reader.close();

      ArcGisResult result = svcReturn.results.get(0);
      result.arcUrl = urlStr;

      return result;
    } catch (IOException ioe) {
      throw new RuntimeException("Could not reach: " + urlStr);
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("Empty results array returned from: " + urlStr);
    }
  }

  /**
   * Container class for a single result from the ArcGis web service.
   */
  static class ArcGisResult {
    public String arcUrl;
    public final double latitude;
    public final double longitude;
    public final double vs30;
    public final Map<String, Double> basinModels;

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

    String json() {
      return BasinUtil.GSON.toJson(this, ArcGisResult.class);
    }
  }

  /* Container class to hold the JSON results from the ArcGis web service. */
  static class ArcGisReturn {
    List<ArcGisResult> results;
  }

}
