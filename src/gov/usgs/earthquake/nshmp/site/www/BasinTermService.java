package gov.usgs.earthquake.nshmp.site.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gov.usgs.earthquake.nshmp.internal.GeoJson;
import gov.usgs.earthquake.nshmp.internal.GeoJson.Feature;
import gov.usgs.earthquake.nshmp.internal.GeoJson.FeatureCollection;
import gov.usgs.earthquake.nshmp.site.www.ArcGis.ArcGisResult;
import gov.usgs.earthquake.nshmp.site.www.Util.EnumParameter;
import gov.usgs.earthquake.nshmp.site.www.Util.Key;
import gov.usgs.earthquake.nshmp.site.www.Util.Status;

import static gov.usgs.earthquake.nshmp.site.www.Util.GSON;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Web service for getting basin terms for {@code BasinModels} 
 *    and {@code BasinRegions}.
 * <br> 
 * The basin terms are produced from the ArcGis Online web service, 
 *    {@link ArcGis}.
 * <br>
 * Syntax: /nshmp-site/basin?latitude={latitude}&longitude={longitude}
 *    &model={basinModel}
 * <br>
 * Example: /nshmp-site/basin?latitude=47&longitude=-122.5&model=Seattle 
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
	
	private static final String SERVICE_NAME = "Basin Term Service";
	private static final String SERVICE_DESCRIPTION = "Get basin terms";
	private static final String SERVICE_SYNTAX = "%s://%s/nshmp-site" +
	      "?latitude={latitude}" +
	      "&longitude={longitude}" + 
	      "&model={basinModel}";
	
	/**
	 * Handle the GET request and return JSON response. 
	 * <br>
	 * If the query string is empty then the JSON response is the {@link Metadata}.
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
	  String requestUrl = request.getRequestURL()
	      .append("?")
	      .append(query)
	      .toString();
	  
	  /*
     * Checking custom header for a forwarded protocol so generated links can
     * use the same protocol and not cause mixed content errors.
     */
    String protocol = request.getHeader("X-FORWARDED-PROTO");
    if (protocol == null) {
      /* Not a forwarded request. Honor reported protocol and port. */
      protocol = request.getScheme();
      host += ":" + request.getServerPort();
    }
    
    requestUrl = requestUrl.replace("http://", protocol + "://");
    
    try {
      if (isNullOrEmpty(query)) {
        writer.printf(GSON.toJson(new Metadata()), protocol, host);
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
	 * Obtain the basin terms from {@link ArcGis#callService(double, double)} 
	 *     and return {@code Response} with only the {@code BasinModels} of interest.
	 * <br>
	 * If the latitude and longitude supplied in the query is not contained 
	 *     in a {@code BasinRegion}, the {@link ArcGis#callPointService}
	 *     is not called and the resulting z1p0 and z2p5 values are set to 
	 *     null using {@link BasinTermService#processNullResult}.
	 *     
	 * @param requestData - The {@code RequestData} 
	 * @param requestUrl - The full request url
	 * @return The {@code Response} to turn into JSON.
	 * @throws IOException
	 */
	private Response processBasinTerm(Map<String, String[]> paramMap, String requestUrl) 
	    throws IOException {
	  
	  RequestData requestData = buildRequest(paramMap);
	  
	  if (requestData.basinRegion == null) {
	    return processNullResult(requestData, requestUrl);
	  }
	  
    ArcGisResult arcGisResult = ArcGis.callPointService(
        requestData.latitude, 
        requestData.longitude);
   
    double lat = arcGisResult.latitude;
    double lon = arcGisResult.longitude;
    
    Double z1p0Val = arcGisResult.basinModels.get(requestData.basinModel.z1p0);
    Double z2p5Val = arcGisResult.basinModels.get(requestData.basinModel.z2p5);
	  
    BasinResponse z1p0 = new BasinResponse(requestData.basinModel.z1p0, z1p0Val);
	  BasinResponse z2p5 = new BasinResponse(requestData.basinModel.z2p5, z2p5Val);
	  
	  ResponseData responseData = new ResponseData(lat, lon, z1p0, z2p5);
	  
	  return new Response(requestData, responseData, requestUrl);
	}
	
	/**
	 * Convience method to return null values for z1p0 and z2p5.
	 * <br>
	 * Called when the supplied latitude and longitude is not contained in 
	 *     any {@code BasinRegion}.
	 *     
	 * @param requestData - The {@link RequestData}
	 * @param requestUrl - The query string
	 * @return A new {@link Response} with null values.
	 */
	private static Response processNullResult(
	    RequestData requestData, 
	    String requestUrl) {
	  double lat = requestData.latitude;
	  double lon = requestData.longitude;
	  
	  BasinResponse z1p0 = new BasinResponse("", null);
	  BasinResponse z2p5 = new BasinResponse("", null);
	  
	  ResponseData responseData = new ResponseData(lat, lon, z1p0, z2p5);
	  
	  return new Response(requestData, responseData, requestUrl);
	}
	
	/**
	 * Return {@code RequestData} by getting the parameters from the 
	 *     {@code HttpServletRequest.getParameterMap()}.
	 *     
	 * @param paramMap The request parameter map.
	 * @return A new instance of {@code RequestData}
	 */
	private static RequestData buildRequest(Map<String, String[]> paramMap) {
	  double lat = Double.valueOf(Util.readValue(paramMap, Key.LATITUDE));
	  double lon = Double.valueOf(Util.readValue(paramMap, Key.LONGITUDE));
	  
	  BasinRegion basinRegion = BasinRegion.findRegion(lat, lon);
	  BasinModel basinModel = basinRegion == null ? null : 
	      getBasinModel(basinRegion, paramMap); 
	  
	  return new RequestData(basinRegion, basinModel, lat, lon);
	}

	/**
	 * Return 
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
	 *       <li> basinModel - {@code BasinModels} </li>
	 *       <li> BasinRegion - {@code BasinRegions} </li>
	 *     </ul>
	 */
	private static class RequestData {
	  final double latitude;
	  final double longitude;
	  final BasinModel basinModel;
	  final BasinRegion basinRegion;
	  
	  RequestData(
	      BasinRegion basinRegion, 
	      BasinModel basinModel, 
	      double latitude, 
	      double longitude) {
	    this.basinRegion = basinRegion;
	    this.basinModel = basinModel;
	    this.latitude = latitude;
	    this.longitude = longitude;
	  }
	}
	
	/**
	 * Container class to hold information for each basin term. 
	 */
	private static class BasinResponse {
	  String model;
	  Double value;
	 
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
	  double latitude;
	  double longitude;
	  BasinResponse z1p0;
	  BasinResponse z2p5;
	  
	  ResponseData(double lat, double lon, BasinResponse z1p0, BasinResponse z2p5) {
	    this.latitude = lat;
	    this.longitude = lon;
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
	  
	  Response(RequestData requestData, ResponseData responseData, String url) {
	    this.status = Util.toLowerCase(Status.SUCCESS);
	    this.name = SERVICE_NAME;
	    this.date = new Date().toString();
	    this.url = url;
	    this.request = requestData;
	    this.response = responseData;
	  }
	}
	
	/**
	 * Container to produce the {@code BasinTermService} usage that shows:
	 *     <ul>
	 *       <li> All basin models - {@code EnumSet.of(BasinModels)} </li>
	 *       <li> All basin regions - {@code EnumSet.of(BasinRegions} </li>
	 *       <li> GeoJson feature collection - {@link BasinRegion#toFeatureCollection()} </li>
	 *     </ul> 
	 */
	private static class Metadata {
	  final String status;
	  final String name;
	  final String description;
	  final String syntax;
	  final EnumParameter<BasinModel> basinModels; 
	  final EnumParameter<BasinRegion> basinRegions; 
	  
	  Metadata() {
	    this.status = Util.toLowerCase(Status.USAGE);
	    this.name = SERVICE_NAME;
	    this.description = SERVICE_DESCRIPTION;
	    this.syntax = SERVICE_SYNTAX;
	    
	    this.basinModels = new EnumParameter<>(
	        "Basin models",
	        EnumSet.allOf(BasinModel.class));
	    
	    this.basinRegions = new EnumParameter<>(
	        "Basin regions",
	        EnumSet.allOf(BasinRegion.class));
	  }
	}

}
