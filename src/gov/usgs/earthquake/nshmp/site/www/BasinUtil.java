package gov.usgs.earthquake.nshmp.site.www;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
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
import gov.usgs.earthquake.nshmp.site.www.basin.BasinModel;

class BasinUtil {

  static final Gson GSON;
  static String ARCGIS_HOST;
  static String SERVICE_URL;

  static {
    GSON = new GsonBuilder()
        .registerTypeAdapter(BasinModel.class, new BasinModelSerializer())
        .registerTypeAdapter(ArcGisResult.class, new ArcGisDeserializer())
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    try {
      Properties props = new Properties();
      InputStream config = BasinUtil.class.getResourceAsStream("/config.properties");
      props.load(config);
      SERVICE_URL = props.getProperty("service_host") + "/nshmp-site-ws/basin";
      ARCGIS_HOST = props.getProperty("arcgis_host");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
   * URL query key identifiers for {@link ArcGis} and {@link BasinTermService}
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
    MODEL,
    ID;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    String toUpperCase() {
      return name().toUpperCase();
    }

    String toUpperCamel() {
      return toUpperCamelCase(this);
    }
  }

  private static Double readArcValue(JsonObject json, String key) {
    JsonElement jsonEl = json.get(key);
    checkNotNull(jsonEl, "Could not get [%s] from the ArcGis Online Service", key);
    Double val = jsonEl.getAsString().equals(Key.NULL.toUpperCamel()) ? null : jsonEl.getAsDouble();

    return val;
  }

  /* Enum to upper camel case */
  private static <E extends Enum<E>> String toUpperCamelCase(E e) {
    return UPPER_UNDERSCORE.to(UPPER_CAMEL, e.name());
  }

  /* A {@code JsonSerializer} for {@code BasinModels} */
  private static final class BasinModelSerializer
      implements
      JsonSerializer<BasinModel>,
      JsonDeserializer<BasinModel> {

    @Override
    public JsonElement serialize(
        BasinModel basinModel,
        Type typeOfSrc,
        JsonSerializationContext context) {

      JsonObject json = new JsonObject();

      json.addProperty(Key.ID.toString(), basinModel.id);
      json.addProperty(Key.Z1P0.toString(), basinModel.z1p0);
      json.addProperty(Key.Z2P5.toString(), basinModel.z2p5);

      return json;
    }

    @Override
    public BasinModel deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {

      JsonObject jsonObject = json.getAsJsonObject();
      return BasinModel.fromId(jsonObject.get(Key.ID.toString()).getAsString());
    }
  }

  /* A {@code JsonDeserializer} for {@code ArcGisResult} */
  private static final class ArcGisDeserializer
      implements
      JsonDeserializer<ArcGisResult> {

    @Override
    public ArcGisResult deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {

      JsonObject jsonObject = json.getAsJsonObject();
      JsonObject attributesJson = jsonObject.get(Key.ATTRIBUTES.toString())
          .getAsJsonObject();

      Map<String, Double> basinModels = new TreeMap<>();

      for (String key : attributesJson.keySet()) {
        if (key.contains(Key.Z1P0.toString()) ||
            key.contains(Key.Z2P5.toString())) {
          Double value = readArcValue(attributesJson, key);
          basinModels.put(key, value);
        }
      }

      double vs30 = readArcValue(attributesJson, Key.VS30.toUpperCamel());
      double latitude = readArcValue(attributesJson, Key.LAT.toUpperCamel());
      double longitude = readArcValue(attributesJson, Key.LON.toUpperCamel());

      ArcGisResult result = new ArcGisResult(basinModels, vs30, latitude, longitude);

      return result;
    }
  }

}
