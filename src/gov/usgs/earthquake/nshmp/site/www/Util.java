package gov.usgs.earthquake.nshmp.site.www;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
   *      <li> {@link BasinModelSerializer} </li>
   *      <li> {@link ArcGisDeserializer} </li>
   *    </ul>
   */ 
  static final Gson GSON;
  
  static {
    GSON = new GsonBuilder()
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
    LAT,
    LON,
    NULL,
    VS30,
    Z1P0,
    Z2P5,
    
    /* Basin Term Service Keys */
    LATITUDE,
    LONGITUDE,
    MODEL;
  }
  
  /**
   * Service request identifiers
   */
  enum Status {
    ERROR,
    SUCCESS,
    USAGE;
  }
  
  /**
   * Return a lower case {@code String} from an {@code Enum}. 
   * @param e The {@code Enum}
   * @return The lower case string
   */
  public static <E extends Enum<E>> String toLowerCase(E e) {
    return e.name().toLowerCase();
  }
 
  /**
   * Return a lower camel case {@code String} from an {@code Enum}.
   * @param e The {@code Enum}
   * @return The lower camel case string.
   */
  public static <E extends Enum<E>> String toLowerCamelCase(E e) {
    return UPPER_UNDERSCORE.to(LOWER_CAMEL, e.name());
  }
 
  /**
   * Return a upper camel case {@code String} from an {@code Enum}.
   * @param e The {@code Enum}
   * @return The upper camel case string.
   */
  public static <E extends Enum<E>> String toUpperCamelCase(E e) {
    return UPPER_UNDERSCORE.to(UPPER_CAMEL, e.name());
  }
  
  /**
   * Container class for an {@code Enum}. 
   */
  static class EnumParameter<E extends Enum<E>>{
    final String label;
    final Set<E> values;
    
    EnumParameter(String label, Set<E> values) {
      this.label = label;
      this.values = values;
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
    String keyStr = toLowerCase(key);
    String[] values = paramMap.get(keyStr);
    checkNotNull(values, "Missing query key: %s", keyStr);
    checkState(!isNullOrEmpty(values[0]), "Empty value array for key: %s", key);
    
    return values[0];
  }
 
  /**
   * Convenience method to read in values from the ArcGis service return
   * @param json
   * @param key
   * @return
   */
  static Double readArcValue(JsonObject json, String key) {
    JsonElement jsonEl = json.get(key);
    checkNotNull(jsonEl, "Could not get key from the ArcGis Online Service return: %s", key);
    Double val = jsonEl.getAsString().equals(toUpperCamelCase(Key.NULL)) ? 
        null : jsonEl.getAsDouble(); 
    
    return val;
  }
  
  /**
   * Attributes for serialization and deserializtion. 
   */
  private enum Attr {
    DEFAULT_MODEL,
    ID,
    LABEL;
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
  
      json.addProperty(toLowerCamelCase(Attr.ID), basinModel.id);
      json.addProperty(toLowerCamelCase(Key.Z1P0), basinModel.z1p0);
      json.addProperty(toLowerCamelCase(Key.Z2P5), basinModel.z2p5);
  
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
      JsonObject attributesJson = jsonObject.get(toLowerCase(Key.ATTRIBUTES))
          .getAsJsonObject();
      Map<String, Double> basinModels = new TreeMap<>();
  
      for (String key : attributesJson.keySet()) {
        if (key.contains(toLowerCamelCase(Key.Z1P0)) || 
            key.contains(toLowerCamelCase(Key.Z2P5))) {
          Double value = readArcValue(attributesJson, key); 
          basinModels.put(key, value);
        }
      }
      
      double latitude = readArcValue(attributesJson, toUpperCamelCase(Key.LAT));
      double longitude = readArcValue(attributesJson, toUpperCamelCase(Key.LON));
      
      ArcGisResult result = new ArcGisResult(basinModels, latitude, longitude);
  
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
      this.status = toLowerCase(Status.ERROR);
      this.request = request;
      this.message = e.getMessage();
    }
  }
  
}
