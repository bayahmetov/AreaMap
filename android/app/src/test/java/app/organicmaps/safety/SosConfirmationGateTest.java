package app.organicmaps.safety;

import org.junit.Test;

public class SosConfirmationGateTest
{
  @Test
  public void confirmationAndCancellationAreSingleDecisions() throws Exception
  {
    SosConfirmationGateChecks.runAll();
  }
}
