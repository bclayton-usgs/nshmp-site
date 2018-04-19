package gov.usgs.earthquake.nshmp.site.www;

/**
 * Basin models
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
    this.z1p0 = "Z1p0" + id;
    this.z2p5 = "Z2p5" + id;
  }
  
  /**
   * Given a basin model id, find and return the corresponding
   *    {@code BasinModels}
   *    
   * @param modelId - The basin model id.
   * @return The {@code BasinModels} associated with model id.
   */
  static BasinModel fromId(String modelId) {
    for (BasinModel basin : values()) {
      if (basin.id.equals(modelId)) return basin; 
    }
    throw new IllegalStateException("Basin model does not exist: " + modelId);
  }
  
}
