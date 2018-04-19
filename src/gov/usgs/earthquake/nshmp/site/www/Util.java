package gov.usgs.earthquake.nshmp.site.www;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import gov.usgs.earthquake.nshmp.site.www.ArcGis.ArcGisResult;

/**
 * Utilities class 
 * 
 * @author Brandon Clayton
 */
public class Util {
  /**
   * A {@code Gson} instance with serializers and deserializers:
   *    <ul> 
   *      <li> {@link BasinRegionSerializer} </li>
   *      <li> {@link BasinModelSerializer} </li>
   *      <li> {@link ArcGisDeserializer} </li>
   *    </ul>
   */ 
  static final Gson GSON;
  
  static {
    GSON = new GsonBuilder()
        .registerTypeAdapter(BasinRegion.class, new BasinRegionSerializer())
        .registerTypeAdapter(BasinModel.class, new BasinModelSerializer())
        .registerTypeAdapter(ArcGisResult.class, new ArcGisDeserializer())
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create();
  }

  /**
   * URL query key identifiers for {@link ArcGis} and {@link BasinTermService}.
   */
  enum Key {
    /* ArcGIS Service Keys */
    ATTRIBUTES,
    VS30,
    Z1P0,
    Z2P5,
    
    /* Basin Term Service Keys */
    LATITUDE,
    LONGITUDE,
    MODEL;
    
    /**
     * Convert the {@code Enum} to a upper camel case {@code String}.
     */
    String toUpperCamel() {
      return UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
    }
   
    /**
     * Convert the {@code Enum} to a lower case {@code String}.
     */
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  /**
   * Service request identifiers
   */
  enum Status {
    ERROR,
    SUCCESS,
    USAGE;
    
    /**
     * Convert the {@code Enum} to a lower case {@code String}.
     */
    @Override 
    public String toString() {
      return name().toLowerCase();
    }
  }
 
  /**
   * Return the {@code String} value of the matching {@value Key}
   *    in a {@code HttpServletRequest.getParameterMap()}.
   *    
   * @param paramMap - The parameter map
   * @param key - The {@code Enum}
   * @return The value associated with the key.
   */
  static String readValue(Map<String, String[]> paramMap, Key key) {
    String keyStr = key.toString();
    String[] values = paramMap.get(keyStr);
    checkNotNull(values, "Missing query key: %s", keyStr);
    checkState(!isNullOrEmpty(values[0]), "Empty value array for key: %s", key);
    
    return values[0];
  }
 
  /**
   * A {@code JsonSerializer} for {@code BasinRegions} 
   */
  private static final class BasinRegionSerializer 
      implements JsonSerializer<BasinRegion> {

    @Override
    public JsonElement serialize(
        BasinRegion basinRegion, 
        Type typeOfSrc, 
        JsonSerializationContext context) {
  
      JsonObject json = new JsonObject();
  
      json.addProperty("label", basinRegion.label);
      json.addProperty("id", basinRegion.id);
      json.addProperty("minlatitude", basinRegion.minlatitude);
      json.addProperty("maxlatitude", basinRegion.maxlatitude);
      json.addProperty("minlongitude", basinRegion.minlongitude);
      json.addProperty("maxlongitude", basinRegion.maxlongitude);
      json.addProperty("defaultModel", basinRegion.defaultModel.id);
  
      return json;
    }
  }

  /**
   * A {@code JsonSerializer} for {@code BasinModels} 
   */
  private static final class BasinModelSerializer 
      implements JsonSerializer<BasinModel> {
    
    @Override
    public JsonElement serialize(
        BasinModel basinModel,
        Type typeOfSrc,
        JsonSerializationContext context) {
  
      JsonObject json = new JsonObject();
  
      json.addProperty("id", basinModel.id);
      json.addProperty("z1p0", basinModel.z1p0);
      json.addProperty("z2p5", basinModel.z2p5);
  
      return json;
    }
  }
  
  /**
   * A {@code JsonDeserializer} for {@code ArcGisResult} 
   */
  static final class ArcGisDeserializer
      implements JsonDeserializer<ArcGisResult> {

    @Override
    public ArcGisResult deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
  
      JsonObject jsonObject = json.getAsJsonObject();
      JsonObject attributesJson = jsonObject.get(Key.ATTRIBUTES.toString())
          .getAsJsonObject();
      Map<String, Double> basinModels = new HashMap<>();
  
      for (String key : attributesJson.keySet()) {
        if (key.contains(Key.Z1P0.toUpperCamel()) || 
            key.contains(Key.Z2P5.toUpperCamel())) {
          Double value = attributesJson.get(key).getAsDouble();
          value = Double.isNaN(value) ? null : value;
          basinModels.put(key, value);
        }
      }
 
      double vs30 = attributesJson.get(Key.VS30.toUpperCamel()).getAsDouble();
      ArcGisResult result = new ArcGisResult();
      result.setBasinModels(basinModels);
      result.setVs30(vs30);
  
      return result;
    }
  }
 
  /**
   * Method for obtaining error messages in JSON format. 
   * @param url - The URL that threw an error
   * @param e - The error
   * @return JSON string
   */
  static String errorMessage(String url, Throwable e) {
    Error error = new Error(url, e);
    return GSON.toJson(error);
  }
  
  /**
   * Container for creating an error message. 
   */
  private static class Error {
    final String status;
    final String request;
    final String message;
    
    private Error(String request, Throwable e) {
      this.status = Status.ERROR.toString();
      this.request = request;
      this.message = e.getMessage();
    }
  }
  
  /**
   * Container class for a {@code Enum}. 
   */
  static class EnumParameter<E extends Enum<E>>{
    final String label;
    final Set<E> values;
    
    EnumParameter(String label, Set<E> values) {
      this.label = label;
      this.values = values;
    }
  }
  
}
