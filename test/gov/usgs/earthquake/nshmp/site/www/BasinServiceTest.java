package gov.usgs.earthquake.nshmp.site.www;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.internal.NshmpSite;
import gov.usgs.earthquake.nshmp.site.www.BasinTermService.Response;

/**
 * Check basin term service.
 * 
 * <p> To run tests: Must have a config.properties file in root of project with
 * a "service_host" field that defines the host were the basin service is
 * deployed. Example: service_host = http://localhost:8080
 * 
 * @author Brandon Clayton
 */
public class BasinServiceTest {

  private static final Path DATA_PATH = Paths.get("test/gov/usgs/earthquake/nshmp/site/data");

  private static final String CONFIG_FILE = "config.properties";

  private static final String SERVICE_PATH = "/nshmp-site-ws/basin";

  private static final String RESULT_SUFFIX = "-result.json";

  private static String SERVICE_URL;

  private static final List<NshmpSite> LOCATIONS = ImmutableList.of(
      /* LA Basin */
      NshmpSite.LOS_ANGELES_CA,
      NshmpSite.NORTHRIDGE_CA,

      /* Bay Area */
      NshmpSite.SAN_FRANCISCO_CA,
      NshmpSite.SAN_JOSE_CA,
      NshmpSite.OAKLAND_CA,

      /* Wasatch Front */
      NshmpSite.SALT_LAKE_CITY_UT,
      NshmpSite.PROVO_UT,

      /* Puget Lowland */
      NshmpSite.SEATTLE_WA,
      NshmpSite.TACOMA_WA,

      /* Outside basin */
      NshmpSite.ELKO_NV);

  static {
    Properties prop = new Properties();
    InputStream input = null;

    try {
      input = new FileInputStream(CONFIG_FILE);
      prop.load(input);

      SERVICE_URL = prop.getProperty("service_host") + SERVICE_PATH;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testService() throws Exception {
    for (NshmpSite site : LOCATIONS) {
      compareResults(site);
    }
  }

  private static void compareResults(NshmpSite site) throws Exception {
    Response expected = readExpected(site);
    Response actual = generateActual(site);

    assertEquals(
        BasinUtil.GSON.toJson(expected.request),
        BasinUtil.GSON.toJson(actual.request));

    assertEquals(
        BasinUtil.GSON.toJson(expected.response),
        BasinUtil.GSON.toJson(actual.response));
  }

  private static Response generateActual(NshmpSite site) throws Exception {
    Location loc = site.location();

    String serviceQuery = SERVICE_URL + "?latitude=" + loc.lat() + "&longitude=" + loc.lon();
    URL url = new URL(serviceQuery);
    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
    Response svcResponse = BasinUtil.GSON.fromJson(reader, Response.class);
    reader.close();

    return svcResponse;
  }

  private static Response readExpected(NshmpSite site) throws Exception {
    Path resultPath = DATA_PATH.resolve(site.id() + RESULT_SUFFIX);
    String result = new String(Files.readAllBytes(resultPath));
    return BasinUtil.GSON.fromJson(result, Response.class);
  }

  private static void writeExpected(NshmpSite site) throws Exception {
    Response svcResponse = generateActual(site);
    String result = BasinUtil.GSON.toJson(svcResponse, Response.class);
    Path resultPath = DATA_PATH.resolve(site.id() + RESULT_SUFFIX);
    Files.write(resultPath, result.getBytes());
  }

  public static void main(String[] args) throws Exception {
    for (NshmpSite site : LOCATIONS) {
      writeExpected(site);
    }
  }

}
