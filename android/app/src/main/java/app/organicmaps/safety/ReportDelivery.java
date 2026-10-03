package app.organicmaps.safety;

/** A message is acknowledged only after the transport confirms delivery. */
public final class ReportDelivery
{
  public enum Outcome
  {
    DONE,
    RETRY,
    BLOCKED
  }

  public interface Transport
  {
    Outcome send(ReportQueue.Entry entry) throws Exception;
  }

  private ReportDelivery() {}

  public static Outcome attempt(ReportQueue queue, String id, Transport transport)
  {
    try
    {
      final ReportQueue.Entry entry = queue.get(id);
      if (entry == null || ReportQueue.SENT.equals(entry.state))
        return Outcome.DONE;
      if (ReportQueue.BLOCKED.equals(entry.state))
        return Outcome.BLOCKED;
      if (queue.waitsForEarlier(entry))
        return Outcome.RETRY;
      final Outcome outcome = transport.send(entry);
      if (outcome == Outcome.DONE)
        queue.setState(id, ReportQueue.SENT);
      else if (outcome == Outcome.BLOCKED)
        queue.setState(id, ReportQueue.BLOCKED);
      return outcome;
    }
    catch (Exception e)
    {
      return Outcome.RETRY;
    }
  }
}
