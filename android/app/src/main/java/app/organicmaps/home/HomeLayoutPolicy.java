package app.organicmaps.home;

/** Geometry and ownership rules shared by all HOME configurations. */
public final class HomeLayoutPolicy
{
  private HomeLayoutPolicy() {}

  public static boolean canShow(boolean dockVisible, boolean searching, boolean gpxActive, boolean choosingPoint)
  {
    return dockVisible && !searching && !gpxActive && !choosingPoint;
  }

  public static int peekHeight(int target, int usableHeight)
  {
    return Math.min(target, Math.max(1, Math.max(1, usableHeight) * 62 / 100));
  }
}
