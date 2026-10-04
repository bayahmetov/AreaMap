package app.organicmaps.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeLayoutPolicyTest
{
  @Test
  public void homeYieldsToInteractiveFlows()
  {
    assertTrue(HomeLayoutPolicy.canShow(true, false, false, false));
    assertFalse(HomeLayoutPolicy.canShow(false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShow(true, true, false, false));
    assertFalse(HomeLayoutPolicy.canShow(true, false, true, false));
    assertFalse(HomeLayoutPolicy.canShow(true, false, false, true));
  }

  @Test
  public void sharedDockYieldsToCoveringScreens()
  {
    assertTrue(HomeLayoutPolicy.canShowDock(true, false, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(false, false, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, true, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, true, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, false, true, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, false, false, true, false));
    assertTrue(HomeLayoutPolicy.canShowDock(true, false, false, false, false, true));
    assertTrue(HomeLayoutPolicy.canShowDock(false, false, false, false, false, true));
    assertTrue(HomeLayoutPolicy.canShowDock(false, false, false, false, true, true));
    assertFalse(HomeLayoutPolicy.canShowDock(false, true, false, false, false, true));
    assertFalse(HomeLayoutPolicy.canShowDock(false, false, true, false, false, true));
    assertFalse(HomeLayoutPolicy.canShowDock(false, false, false, true, false, true));
  }

  @Test
  public void chooserOpeningObserverMustHideHomeBeforeNativeModeChanges()
  {
    assertTrue(HomeLayoutPolicy.isChoosingPoint(true, false));
    assertTrue(HomeLayoutPolicy.isChoosingPoint(true, true));
    assertTrue(HomeLayoutPolicy.isChoosingPoint(false, true));
    assertFalse(HomeLayoutPolicy.isChoosingPoint(false, false));
    assertFalse(home(true, false, false, false, false, false, false, false));
    assertTrue(home(false, false, false, false, false, false, false, false));
  }

  private static boolean home(boolean chooser, boolean search, boolean place, boolean fullscreen,
                              boolean planning, boolean preview, boolean hike, boolean gpx)
  {
    boolean dock = HomeLayoutPolicy.canShowDock(true, search, place, chooser, fullscreen, hike);
    return HomeLayoutPolicy.canShow(dock && !planning && !preview && !hike, search, gpx, chooser);
  }

  @Test
  public void idleHomeReturnsAfterEachCoveringFlowCloses()
  {
    assertTrue(home(false, false, false, false, false, false, false, false));
    for (int state = 0; state < 8; state++)
    {
      assertFalse(home(state == 0, state == 1, state == 2, state == 3,
                       state == 4, state == 5, state == 6, state == 7));
      assertTrue(home(false, false, false, false, false, false, false, false));
    }
  }

  @Test
  public void collapsedGeometryReservesDockExactlyOnce()
  {
    // 3x-density phone: 2000px root, 220px dock and controls ending at 1080px.
    int safeBottom = 2000 - 220;
    int expandedTop = 1100;
    int height = HomeLayoutPolicy.sheetHeight(2000, expandedTop);
    int visible = HomeLayoutPolicy.visiblePeek(930, safeBottom, 220, expandedTop);
    int peek = visible + 220;
    int top = HomeLayoutPolicy.collapsedTop(safeBottom, visible);
    assertEquals(900, height);
    assertEquals(680, visible);
    assertEquals(1100, top);
    assertEquals(2000 - peek, top);
    assertEquals(visible, safeBottom - top);
    // Bottom gravity adds a second layout origin before Material adds collapsedOffset.
    assertTrue((2000 - height) + top >= safeBottom);
  }

  @Test
  public void restoredSheetGeometryFitsPortraitAndLandscape()
  {
    for (int[] screen : new int[][] {{2200, 220, 200, 700}, {1000, 180, 160, 350}, {800, 160, 140, 600}})
    {
      int safe = screen[0] - screen[1];
      int visible = HomeLayoutPolicy.visiblePeek(930, safe, screen[2], screen[3]);
      int top = HomeLayoutPolicy.collapsedTop(safe, visible);
      int height = HomeLayoutPolicy.sheetHeight(screen[0], screen[3]);
      assertTrue(visible > 0);
      assertTrue(top >= screen[3]);
      assertTrue(top < safe);
      assertTrue(top + height >= safe);
      assertEquals(screen[0] - (visible + screen[1]), top);
      assertEquals(visible, safe - top);
    }
  }

  @Test
  public void smallScreensRetainUsableMapSpace()
  {
    assertEquals(310, HomeLayoutPolicy.peekHeight(310, 700));
    assertEquals(186, HomeLayoutPolicy.peekHeight(310, 300));
    assertEquals(130, HomeLayoutPolicy.peekHeight(130, 250));
    assertEquals(1, HomeLayoutPolicy.peekHeight(310, 0));
  }
}
