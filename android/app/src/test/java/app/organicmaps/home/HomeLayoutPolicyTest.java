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
  public void sharedDockYieldsToCoveringScreensAndNavigation()
  {
    assertTrue(HomeLayoutPolicy.canShowDock(true, false, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(false, false, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, true, false, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, true, false, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, false, true, false, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, false, false, true, false));
    assertFalse(HomeLayoutPolicy.canShowDock(true, false, false, false, false, true));
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
