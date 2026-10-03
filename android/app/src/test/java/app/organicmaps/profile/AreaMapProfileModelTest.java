package app.organicmaps.profile;

import static org.junit.Assert.*;
import org.junit.Test;

public class AreaMapProfileModelTest
{
  @Test public void identityIsIndependentOfOsmAndNormalizesInput()
  {
    AreaMapUserProfile profile = new AreaMapUserProfile("  A traveller  ", "  Mountains  ", " Almaty ", "/private/avatar.jpg", 42);
    assertEquals("A traveller", profile.displayName);
    assertEquals("Mountains", profile.bio);
    assertEquals("Almaty", profile.homeRegion);
    assertEquals("/private/avatar.jpg", profile.avatarPath);
    assertEquals(42, profile.createdAt);
  }

  @Test public void missingTrackingNeverUnlocksBadgesOrInventsDates()
  {
    Achievement achievement = new Achievement("10_hikes", 0, 0, 0, "completed_hikes", 10, 0, 123);
    assertFalse(achievement.isUnlocked());
    assertEquals(0, achievement.progress);
    assertEquals(0, achievement.unlockedAt);
  }

  @Test public void progressIsBoundedAndUnlockRequiresTarget()
  {
    Achievement partial = new Achievement("safe_hiker", 0, 0, 0, "safety_preparation", 2, 1, 456);
    assertFalse(partial.isUnlocked());
    assertEquals(0, partial.unlockedAt);
    Achievement ready = new Achievement("safe_hiker", 0, 0, 0, "safety_preparation", 2, 3, 456);
    assertTrue(ready.isUnlocked());
    assertEquals(2, ready.progress);
    assertEquals(456, ready.unlockedAt);
  }
}
