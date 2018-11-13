package gov.usgs.earthquake.nshmp.site.www.basin;

/**
 * Basin models for basin terms.
 * 
 * @author Brandon Clayton
 */
public enum BasinModel {
  
  BAY_AREA("bayarea"),
  CCA06("cca06"),
  CVMH1510("cvmh1510"),
  CVMS4("cvms4"),
  CVMS426("cvms426"),
  CVMS426M01("cvms426m01"),
  LINTHURBER("linthurber"),
  SCHMANDT_LIN("SchmandtLin"),
  SEATTLE("Seattle"),
  SCHEN_RITZWOLLER("SchenRitzwoller"),
  WASATCH("Wasatch");
 
  public String id;
  public String z1p0;
  public String z2p5;
 
  private BasinModel(String id) {
    this.id = id;
    z1p0 = "z1p0" + id;
    z2p5 = "z2p5" + id;
  }
  
  /**
   * Return a basin model with specified id.
   *  
   * @param id of the basin model 
   * @throws IllegalArgumentException if a basin model is not found 
   */
  public static BasinModel fromId(String id) {
    for (BasinModel basin : values()) {
      if (basin.id.equals(id)) return basin; 
    }
    
    throw new IllegalArgumentException("Basin model [" + id + "] does not exist");
  }
  
}
