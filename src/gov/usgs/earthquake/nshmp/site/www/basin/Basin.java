package gov.usgs.earthquake.nshmp.site.www.basin;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

import java.util.Arrays;

/**
 * Basin ids corresponding to file name in data directory.
 * 
 * @author Brandon Clayton
 */
public enum Basin {
  
  BAY_AREA,
  LOS_ANGELES,
  PUGET_LOWLAND,
  WASATCH_FRONT;
  
  public final String id;
  
  private Basin() {
    id = toString();
  }
  
  @Override
  public String toString() {
    return UPPER_UNDERSCORE.to(LOWER_HYPHEN, name());
  }
 
  /**
   * Returns a basin with specified id.
   * 
   * @param id The id of the basin
   */
  public static Basin fromId(String id) {
    return Arrays.asList(values()).stream()
        .filter(basin -> basin.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Basin [" + id + "] not found"));
  }
  
}
