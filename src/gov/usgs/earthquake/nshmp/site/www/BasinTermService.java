package gov.usgs.earthquake.nshmp.site.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.site.www.ArcGis.ArcGisResult;
import gov.usgs.earthquake.nshmp.site.www.Basins.BasinRegion;
import gov.usgs.earthquake.nshmp.site.www.Util.EnumParameter;
import gov.usgs.earthquake.nshmp.site.www.Util.Key;
import gov.usgs.earthquake.nshmp.site.www.Util.Status;

import static gov.usgs.earthquake.nshmp.site.www.Util.GSON;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Web service for getting basin terms for {@link BasinModel}s 
 *    and {@link BasinRegion}s.
 * <br> <br>
 * 
 * The basin terms are produced from the {@link ArcGis} Online web service.
 * <br><br>
 * 
 * Usage: /nshmp-site-ws/basin
 * <br><br>
 * 
 * GeoJson: /nshmp-site-ws/basin/geojson
 * <br><br>
 * 
 * Syntax: /nshmp-site-ws/basin?latitude={latitude}&amp;
 *    longitude={longitude}&amp;model={basinModel}
 * <br>
 * Where: 
 *    <ul> 
 *      <li> latitude is in degrees </li>
 *      <li> longitude is in degrees </li>
 *      <li> model is one of the {@link BasinModel}s </li>
 *    </ul>
 * <br>
 * 
 * NOTE: Latitude and longitude must be supplied. If model is not supplied the 
 *    default model is used as defined in the basins.geojson file. 
 * <br><br>
 * 
 * Example: /nshmp-site-ws/basin?latitude=47&amp;longitude=-122.5&amp;model=Seattle 
 * <br><br>
 *   
 * @author Brandon Clayton
 */
@WebServlet(
    name = "Basin Term Service",
    description = "Utility for getting basin terms",
    urlPatterns = {
        "/basin",
        "/basin/*"})
public class BasinTermService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Web service name */
	private static final String SERVICE_NAME = "Basin Term Service";
	/** Web service description */
	private static final String SERVICE_DESCRIPTION = "Get basin terms";
	/** Web service url syntax */
	private static final String SERVICE_SYNTAX = "%s://%s/nshmp-site-ws/basin" +
	      "?latitude={latitude}" +
	      "&longitude={longitude}" + 
	      "&model={basinModel}";
	/** Web service usage url */
	private static final String SERVICE_USAGE = "%s://%s/nshmp-site-ws/basin";
	/** URL to return basins.geojson */
	private static final String SERVICE_GEOJSON = "%s://%s/nshmp-site-ws/basin/geojson";
	
	/**
	 * Handle the GET request and return JSON response. 
	 * <br><br>
	 * 
	 * Different responses:
	 *   <ul>
	 *     <li> path info equals geojson: the basins.geojson file </li>
	 *     <li> query empty: {@link Metadata} </li>
	 *     <li> query not empty: {@link Response} </li>
	 *   </ul>
	 */
	@Override
	protected void doGet(
	    HttpServletRequest request, 
	    HttpServletResponse response) 
	        throws ServletException, IOException {
		
	  PrintWriter writer = response.getWriter();
	  
	  String query = request.getQueryString();
	  String pathInfo = request.getPathInfo();
	  String host = request.getServerName();
	  
	  /*
     * Checking custom header for a forwarded protocol so generated links can
     * use the same protocol and not cause mixed content errors.
     */
	  String protocol = request.getHeader("X-FORWARDED-PROTO");
    if (protocol == null) {
      protocol = request.getScheme();
      host += ":" + request.getServerPort();
    }
    
    StringBuffer urlBuf = request.getRequestURL();
    if (query != null) urlBuf.append("?").append(query);
    String requestUrl = urlBuf.toString();
    requestUrl = requestUrl.replace("http://", protocol + "://");
    
    try {
      if (!isNullOrEmpty(pathInfo) && pathInfo.equals("/geojson")) {
        Basins basins = Basins.getBasins();
        writer.println(basins.featureCollection.toJsonString());
        return;
      }
      
      if (isNullOrEmpty(query)) {
        writer.printf(GSON.toJson(new Metadata()), 
            protocol, host, protocol, host, protocol, host);
        return;
      }
      
      Map<String, String[]> paramMap = request.getParameterMap();
      Response svcResponse = processBasinTerm(paramMap, requestUrl);
      String json = GSON.toJson(svcResponse);
      writer.println(json);
    } catch(Exception e) {
      writer.println(Util.errorMessage(requestUrl, e));
    }
	}
	
	/**
	 * Process query and obtain the basin terms 
	 *     from {@link ArcGis#callPointService(double, double)} 
	 *     and return {@link Response} with only the {@link BasinModel} of interest.
	 * <br><br>
	 * 
	 * If the latitude and longitude supplied in the query is not contained 
	 *     in a {@code BasinRegion}, the {@link ArcGis#callPointService}
	 *     is not called and the resulting z1p0 and z2p5 values are set to 
	 *     {@code null} using {@link BasinTermService#processNullResult}.
	 *     
	 * @param paramMap - The {@code HttpServletRequest} parameter map,
	 *     {@code Map<String, String[]>}.
	 * @param requestUrl - The full request url.
	 * @return The {@code Response} to turn into JSON.
	 */
	private Response processBasinTerm(
	    Map<String, String[]> paramMap, 
	    String requestUrl) {
	  
	  RequestData requestData = buildRequest(paramMap);
	  
	  if (requestData.basinRegion == null) {
	    return processNullResult(requestData, requestUrl);
	  }
	 
	  Location loc = Location.create(requestData.latitude, requestData.longitude);
    ArcGisResult arcGisResult = ArcGis.callPointService(loc);
   
    Double z1p0 = arcGisResult.basinModels.get(requestData.basinModel.z1p0);
    Double z2p5 = arcGisResult.basinModels.get(requestData.basinModel.z2p5);
    
    /*
     * TODO This gets the job done for now. Seattle is a special case where
     * z1p0 is returned as a converted z2p5 value, instead of the model
     * value itself. 
     */
    if (requestData.basinRegion.id.equals("pugetLowland") && z2p5 != null) {
      z1p0 = 0.1039 * z2p5 + 0.2029;
    }
    
    BasinResponse z1p0resp = new BasinResponse(requestData.basinModel.z1p0, z1p0);
	  BasinResponse z2p5resp = new BasinResponse(requestData.basinModel.z2p5, z2p5);
	  
	  ResponseData responseData = new ResponseData(z1p0resp, z2p5resp);
	  
	  return new Response(requestData, responseData, arcGisResult, requestUrl);
	}
	
	/**
	 * Convience method to return null values for z1p0 and z2p5.
	 * <br><br>
	 * 
	 * Called when the supplied latitude and longitude is not contained in 
	 *     any {@link BasinRegion}.
	 *     
	 * @param requestData - The {@link RequestData}
	 * @param requestUrl - The query string
	 * @return A new {@link Response} with null values.
	 */
	private static Response processNullResult(
	    RequestData requestData, 
	    String requestUrl) {
	  BasinResponse z1p0 = new BasinResponse("", null);
	  BasinResponse z2p5 = new BasinResponse("", null);
	  
	  ResponseData responseData = new ResponseData(z1p0, z2p5);
	  
	  return new Response(requestData, responseData, null, requestUrl);
	}
	
	/**
	 * Return {@link RequestData} by getting the parameters from the 
	 *     {@code HttpServletRequest.getParameterMap()}.
	 *     
	 * @param paramMap The request parameter map.
	 * @return A new instance of {@code RequestData}
	 */
	private static RequestData buildRequest(Map<String, String[]> paramMap) {
	  double latitude = Double.valueOf(Util.readValue(paramMap, Key.LATITUDE));
	  double longitude = Double.valueOf(Util.readValue(paramMap, Key.LONGITUDE));
	 
	  Basins basins = Basins.getBasins();
	  BasinRegion basinRegion = basins.findRegion(latitude, longitude);
	  BasinModel basinModel = basinRegion == null ? null : 
	      getBasinModel(basinRegion, paramMap); 
	  
	  return new RequestData(basinRegion, basinModel, latitude, longitude);
	}

	/**
	 * Return a {@link BasinModel} based on if the {@link BasinRegion} was
	 *     found or the "model" key appears in the query string. 
	 *     
	 * @param basinRegion
	 * @param paramMap
	 * @return
	 */
	private static BasinModel getBasinModel(
	    BasinRegion basinRegion, 
	    Map<String, String[]> paramMap) {
	  Boolean hasBasinModel = paramMap.containsKey(Util.toLowerCase(Key.MODEL));
	  
	  return hasBasinModel ? 
	      BasinModel.fromId(Util.readValue(paramMap, Key.MODEL)) :
	      basinRegion.defaultModel;
	}
	
	/**
	 * Container class to hold the request parameters from the URL query, including:
	 *     <ul>
	 *       <li> latitude - {@code double} </li>
	 *       <li> longitude - {@code double} </li>
	 *       <li> {@link BasinModel} </li>
	 *       <li> {@link BasinRegion} </li>
	 *     </ul>
	 */
	private static class RequestData {
	  final double latitude;
	  final double longitude;
	  final BasinModel basinModel;
	  final BasinRegionRequest basinRegion;
	  
	  RequestData(
	      BasinRegion basinRegion, 
	      BasinModel basinModel, 
	      double latitude, 
	      double longitude) {
	    this.basinRegion = BasinRegionRequest.getBasinRegionRequest(basinRegion); 
	    this.basinModel = basinModel;
	    this.latitude = latitude;
	    this.longitude = longitude;
	  }
	}

	/**
	 * Container class to hold information about the {@link BasinRegion}.
	 */
	private static class BasinRegionRequest {
	  final String title;
	  final String id;
	  
	  BasinRegionRequest(BasinRegion basinRegion) {
	    this.title = basinRegion.title;
	    this.id = basinRegion.id;
	  }
	  
	  private static BasinRegionRequest getBasinRegionRequest(BasinRegion basinRegion) {
	    return basinRegion == null ? null : new BasinRegionRequest(basinRegion);
	  }
	}
	
	/**
	 * Container class to hold information for each basin term, z1p0 and z2p5. 
	 */
	private static class BasinResponse {
	  final String model;
	  final Double value;
	 
	  BasinResponse(String model, Double value) {
	    this.model = model;
	    this.value = value;
	  }
	}
	
	/**
	 * Container class to hold a {@code BasinResponse} for:
	 *     <ul>
	 *       <li> z1p0 </li>
	 *       <li> z2p5 </li>
	 *     </ul> 
	 */
	private static class ResponseData {
	  final BasinResponse z1p0;
	  final BasinResponse z2p5;
	  
	  ResponseData(BasinResponse z1p0, BasinResponse z2p5) {
	    this.z1p0 = z1p0;
	    this.z2p5 = z2p5;
	  }
	}
	
	/**
	 * Container structure for the JSON response. Example:
	 * <pre>
	 * {
	 *   status: "success",
	 *   name: "Basin Term Service",
	 *   date: ,
	 *   url: "/nshmp-site/basin?",
	 *   request: {
	 *     latitude: ,
	 *     longitude: ,
	 *     basinModel: {},
	 *     basinRegion: {},
	 *   },
	 *   response: {
	 *     z1p0: {
	 *       model: ,
	 *       value: ,
	 *     }, 
	 *     z2p5: {
	 *       model: ,
	 *       value: ,
	 *     },
	 *   }
	 * }
	 * </pre>
	 */
	private static class Response {
	  final String status;
	  final String name;
	  final String date;
	  final String url;
	  final RequestData request;
	  final ResponseData response;
	  transient final ArcGisResult arcGisResponse;
	  
	  Response(
	      RequestData requestData, 
	      ResponseData responseData, 
	      ArcGisResult arcGisResponse,
	      String url) {
	    this.status = Util.toLowerCase(Status.SUCCESS);
	    this.name = SERVICE_NAME;
	    this.date = new Date().toString();
	    this.url = url;
	    this.request = requestData;
	    this.response = responseData;
	    this.arcGisResponse = arcGisResponse;
	  }
	}
	
	/**
	 * Container to produce the {@code BasinTermService} usage that shows:
	 *     <ul>
	 *       <li> All basin models - {@code EnumSet.of(BasinModels)} </li>
	 *       <li> All basin regions - {@code List<BasinRegion>} </li>
	 *     </ul> 
	 */
	private static class Metadata {
	  final String status;
	  final String name;
	  final String description;
	  final String usage;
	  final String geojson;
	  final String syntax;
	  final EnumParameter<BasinModel> basinModels; 
	  final List<BasinRegion> basinRegions; 
	  
	  Metadata() {
	    Basins basins = Basins.getBasins();
	    
	    this.status = Util.toLowerCase(Status.USAGE);
	    this.name = SERVICE_NAME;
	    this.description = SERVICE_DESCRIPTION;
	    this.usage = SERVICE_USAGE;
	    this.geojson = SERVICE_GEOJSON;
	    this.syntax = SERVICE_SYNTAX;
	    
	    this.basinModels = new EnumParameter<>(
	        "Basin models",
	        EnumSet.allOf(BasinModel.class));

      this.basinRegions = basins.basinRegions;
	  }
	}

}
