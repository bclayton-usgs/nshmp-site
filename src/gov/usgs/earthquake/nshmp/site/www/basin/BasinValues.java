package gov.usgs.earthquake.nshmp.site.www.basin;

/**
 * Container class for basin term values:
 *  - z1p0
 *  - z2p5
 * 
 * @author Brandon Clayton
 */
public class BasinValues {

  final BasinValue z1p0;
  final BasinValue z2p5;

  public BasinValues(BasinValue z1p0, BasinValue z2p5) {
    this.z1p0 = z1p0;
    this.z2p5 = z2p5;
  }

  /**
   * Container class for a single basin term.
   */
  public static class BasinValue {
    final String model;
    final Double value;

    public BasinValue(String model, Double value) {
      this.model = model;
      this.value = value;
    }
  }

}
