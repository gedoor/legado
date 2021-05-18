package me.ag2s.epublib.domain;

import java.io.Serializable;

public class ResourceReference implements Serializable {

  private static final long serialVersionUID = 2596967243557743048L;

  protected Resource resource;

  public ResourceReference(Resource resource) {
    this.resource = resource;
  }


  public Resource getResource() {
    return resource;
  }

  /**
   * Besides setting the resource it also sets the fragmentId to null.
   *
   * @param resource resource
   */
  public void setResource(Resource resource) {
    this.resource = resource;
  }


  /**
   * The id of the reference referred to.
   *
   * null of the reference is null or has a null id itself.
   *
   * @return The id of the reference referred to.
   */
  public String getResourceId() {
    if (resource != null) {
      return resource.getId();
    }
    return null;
  }
}
