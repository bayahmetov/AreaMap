package app.organicmaps.home;

/** Geometry and ownership rules shared by all HOME configurations. */
public final class HomeLayoutPolicy
{
  private HomeLayoutPolicy() {}

  public static boolean canShow(boolean dockVisible, boolean searching, boolean gpxActive, boolean choosingPoint)
  {
    return dockVisible && !searching && !gpxActive && !choosingPoint;
  }

  public static boolean canShowDock(boolean regularOrPreview, boolean searching, boolean placePage,
                                    boolean choosingPoint, boolean fullscreen, boolean activeHike)
  {
    return (regularOrPreview || activeHike) && !searching && !placePage && !choosingPoint
 && (!fullscreen || activeHike);
  }

  public static boolean isChoosingPoint(boolean observableActive, boolean nativeActive)
  {
    return observableActive || nativeActive;
  }

  public static int sheetHeight(int rootHeight, int expandedTop)
  {
    return Math.max(1, rootHeight - expandedTop);
  }

  public static int visiblePeek(int target, int safeBottom, int mapTop, int expandedTop)
  {
    return Math.min(peekHeight(target, Math.max(1, safeBottom - mapTop)),
                    Math.max(1, safeBottom - expandedTop));
  }

  public static int collapsedTop(int safeBottom, int visiblePeek)
  {
    return safeBottom - visiblePeek;
  }

  public static int peekHeight(int target, int usableHeight)
  {
    return Math.min(target, Math.max(1, Math.max(1, usableHeight) * 62 / 100));
  }
}
