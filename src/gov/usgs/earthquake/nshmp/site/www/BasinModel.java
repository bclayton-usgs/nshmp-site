package gov.usgs.earthquake.nshmp.site.www;

/**
 * Basin models to obtain basin terms.
 * <br><br>
 * 
 * Models include:
 *    <ul>
 *      <li> bayarea </li>
 *      <li> cca06 </li>
 *      <li> cvh1510 </li>
 *      <li> cvms4 </li>
 *      <li> cvms426 </li>
 *      <li> cvms426m01 </li>
 *      <li> linthurber </li>
 *      <li> SchmandtLin </li>
 *      <li> Seattle </li>
 *      <li> SchenRitzwoller </li>
 *      <li> Wasatch </li>
 *    </ul>
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
 
  /**
   *  The {@code BasinModel} id taken from the {@link ArcGis} web service 
   *    return without the "z1p0-" or "z2p5-" prepended to it.
   */
  public String id;
  /** The {@code BasinModel} Z1p0 id */
  public String z1p0;
  /** The {@code BasinModel} Z2p5 id */
  public String z2p5;
 
  /**
   * Create a new {@code BasinModel} {@code enum}.
   * @param id The {@code BasinModel} id
   */
  private BasinModel(String id) {
    this.id = id;
    this.z1p0 = "z1p0" + id;
    this.z2p5 = "z2p5" + id;
  }
  
  /**
   * Given a basin model id, find and return the corresponding
   *    {@code BasinModel}
   * 
   * @param modelId - The basin model id.
   * @return The {@code BasinModel} associated with model id.
   * @throws RuntimeException If {@code BasinModel} is not found.
   */
  public static BasinModel fromId(String modelId) {
    
    for (BasinModel basin : values()) {
      if (basin.id.equals(modelId)) return basin; 
    }
    
    throw new RuntimeException("Basin model does not exist: " + modelId);
  }
  
}
