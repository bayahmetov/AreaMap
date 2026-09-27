package app.organicmaps.safety;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TripSafetyScheduleTest
{
  @Test
  public void delayBucketsAvoidNotificationSpam()
  {
    assertEquals(0, TripSafety.scheduleAlertBucket(0));
    assertEquals(0, TripSafety.scheduleAlertBucket(9));
    assertEquals(1, TripSafety.scheduleAlertBucket(10));
    assertEquals(1, TripSafety.scheduleAlertBucket(19));
    assertEquals(2, TripSafety.scheduleAlertBucket(20));
    assertEquals(2, TripSafety.scheduleAlertBucket(34));
    assertEquals(3, TripSafety.scheduleAlertBucket(35));
    assertEquals(4, TripSafety.scheduleAlertBucket(50));
  }
}
