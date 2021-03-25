package me.ag2s.epublib.domain;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.util.StringUtil;
import java.io.Serializable;

public class TitledResourceReference extends ResourceReference
    implements Serializable {

  private static final long serialVersionUID = 3918155020095190080L;
  private String fragmentId;
  private String title;

  public TitledResourceReference(Resource resource) {
    this(resource, null);
  }

  public TitledResourceReference(Resource resource, String title) {
    this(resource, title, null);
  }

  public TitledResourceReference(Resource resource, String title,
      String fragmentId) {
    super(resource);
    this.title = title;
    this.fragmentId = fragmentId;
  }

  public String getFragmentId() {
    return fragmentId;
  }

  public void setFragmentId(String fragmentId) {
    this.fragmentId = fragmentId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }


  /**
   * If the fragmentId is blank it returns the resource href, otherwise
   * it returns the resource href + '#' + the fragmentId.
   *
   * @return If the fragmentId is blank it returns the resource href,
   *    otherwise it returns the resource href + '#' + the fragmentId.
   */
  public String getCompleteHref() {
    if (StringUtil.isBlank(fragmentId)) {
      return resource.getHref();
    } else {
      return resource.getHref() + Constants.FRAGMENT_SEPARATOR_CHAR
          + fragmentId;
    }
  }
  @Override
  public Resource getResource() {
    resource.setTitle(title);
    return resource;
  }

  public void setResource(Resource resource, String fragmentId) {
    super.setResource(resource);
    this.fragmentId = fragmentId;
  }

  /**
   * Sets the resource to the given resource and sets the fragmentId to null.
   */
  public void setResource(Resource resource) {
    setResource(resource, null);
  }
}
