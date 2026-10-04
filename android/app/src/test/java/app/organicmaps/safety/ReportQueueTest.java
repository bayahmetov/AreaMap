package app.organicmaps.safety;

import org.junit.Test;

public class ReportQueueTest
{
  @Test
  public void survivesOfflineDeliveryAndRestartsWithoutDuplicateRegistration() throws Exception
  {
    ReportQueueChecks.runAll();
  }
}
