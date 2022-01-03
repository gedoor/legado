package me.ag2s.epublib.browsersupport;

import java.util.EventObject;

import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.util.StringUtil;

/**
 * Used to tell NavigationEventListener just what kind of navigation action
 * the user just did.
 *
 * @author paul
 *
 */
@SuppressWarnings("unused")
public class NavigationEvent extends EventObject {

  private static final long serialVersionUID = -6346750144308952762L;

  private Resource oldResource;
  private int oldSpinePos;
    private Navigator navigator;
    private EpubBook oldBook;
    private int oldSectionPos;
  private String oldFragmentId;

  public NavigationEvent(Object source) {
    super(source);
  }

  public NavigationEvent(Object source, Navigator navigator) {
    super(source);
    this.navigator = navigator;
    this.oldBook = navigator.getBook();
    this.oldFragmentId = navigator.getCurrentFragmentId();
    this.oldSectionPos = navigator.getCurrentSectionPos();
    this.oldResource = navigator.getCurrentResource();
    this.oldSpinePos = navigator.getCurrentSpinePos();
  }

  /**
   * The previous position within the section.
   *
   * @return The previous position within the section.
   */
  public int getOldSectionPos() {
    return oldSectionPos;
  }

  public Navigator getNavigator() {
    return navigator;
  }

  public String getOldFragmentId() {
    return oldFragmentId;
  }

  // package
  void setOldFragmentId(String oldFragmentId) {
    this.oldFragmentId = oldFragmentId;
  }

    public EpubBook getOldBook() {
        return oldBook;
    }

  // package
  void setOldPagePos(int oldPagePos) {
    this.oldSectionPos = oldPagePos;
  }

  public int getCurrentSectionPos() {
    return navigator.getCurrentSectionPos();
  }

  public int getOldSpinePos() {
    return oldSpinePos;
  }

  public int getCurrentSpinePos() {
    return navigator.getCurrentSpinePos();
  }

  public String getCurrentFragmentId() {
    return navigator.getCurrentFragmentId();
  }

  public boolean isBookChanged() {
    if (oldBook == null) {
      return true;
    }
    return oldBook != navigator.getBook();
  }

  public boolean isSpinePosChanged() {
    return getOldSpinePos() != getCurrentSpinePos();
  }

  public boolean isFragmentChanged() {
    return StringUtil.equals(getOldFragmentId(), getCurrentFragmentId());
  }

  public Resource getOldResource() {
    return oldResource;
  }

  public Resource getCurrentResource() {
    return navigator.getCurrentResource();
  }

  public void setOldResource(Resource oldResource) {
    this.oldResource = oldResource;
  }


  public void setOldSpinePos(int oldSpinePos) {
    this.oldSpinePos = oldSpinePos;
  }


  public void setNavigator(Navigator navigator) {
    this.navigator = navigator;
  }


    public void setOldBook(EpubBook oldBook) {
        this.oldBook = oldBook;
    }

    public EpubBook getCurrentBook() {
        return getNavigator().getBook();
    }

  public boolean isResourceChanged() {
    return oldResource != getCurrentResource();
  }

  @SuppressWarnings("NullableProblems")
  public String toString() {
    return StringUtil.toString(
        "oldSectionPos", oldSectionPos,
        "oldResource", oldResource,
        "oldBook", oldBook,
        "oldFragmentId", oldFragmentId,
        "oldSpinePos", oldSpinePos,
        "currentPagePos", getCurrentSectionPos(),
        "currentResource", getCurrentResource(),
        "currentBook", getCurrentBook(),
        "currentFragmentId", getCurrentFragmentId(),
        "currentSpinePos", getCurrentSpinePos()
    );
  }

  public boolean isSectionPosChanged() {
    return oldSectionPos != getCurrentSectionPos();
  }
}
