package app.organicmaps.safety;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

/** Can also run without Android/Gradle: compile with ReportQueue and ReportDelivery, then run main. */
public final class ReportQueueChecks
{
  private ReportQueueChecks() {}

  public static void main(String[] args) throws Exception
  {
    runAll();
    System.out.println("ReportQueue: 12 checks passed");
  }

  public static void runAll() throws Exception
  {
    final File directory = Files.createTempDirectory("areamap-outbox-test").toFile();
    try
    {
      final ReportQueue queue = new ReportQueue(directory);
      final ReportQueue.Entry sos = queue.enqueue("sos", "trip-1", "SOS: 43.1, 76.2; fix 12:00", 1000);
      check(ReportDelivery.attempt(queue, sos.id, entry -> {
        throw new IOException("airplane mode");
      }) == ReportDelivery.Outcome.RETRY, "Offline send must retry");
      check(ReportQueue.PENDING.equals(queue.get(sos.id).state), "Offline SOS must remain pending");

      final ReportQueue restarted = new ReportQueue(directory);
      check(restarted.get(sos.id).body.equals(sos.body), "Process restart must retain the exact SOS snapshot");
      check(restarted.enqueue("sos", "trip-1", "Repeated tap; different coordinates", 90000).id.equals(sos.id),
            "Repeated offline SOS must not create another message");

      final AtomicInteger calls = new AtomicInteger();
      check(ReportDelivery.attempt(restarted, sos.id, entry -> {
        check(entry.body.equals(sos.body), "Reconnect must send the original coordinates and time");
        calls.incrementAndGet();
        return ReportDelivery.Outcome.DONE;
      }) == ReportDelivery.Outcome.DONE, "Reconnect must complete delivery");
      ReportDelivery.attempt(new ReportQueue(directory), sos.id, entry -> {
        calls.incrementAndGet();
        return ReportDelivery.Outcome.DONE;
      });
      check(calls.get() == 1, "Acknowledged SOS must not be delivered again after restart");

      final ReportQueue.Entry start = queue.enqueue("start", "trip-1", "Trip started", 100000);
      queue.setState(start.id, ReportQueue.SENT);
      check(new ReportQueue(directory).enqueue("start", "trip-1", "Repeated registration", 200000).id.equals(start.id),
            "Resuming must retain the original start event");

      final ReportQueue.Entry otherStart = queue.enqueue("start", "trip-2", "Another trip", 300000);
      final ReportQueue.Entry finish = queue.enqueue("finish", "trip-2", "Returned", 300000);
      check(queue.waitsForEarlier(finish), "Offline finish must wait for that trip's start, including same-ms taps");
      final ReportQueue.Entry urgent = queue.enqueue("sos", "trip-2", "Urgent", 300001);
      check(!queue.waitsForEarlier(urgent), "SOS must bypass unsent ordinary reports");

      ReportDelivery.attempt(queue, otherStart.id, entry -> ReportDelivery.Outcome.BLOCKED);
      check(ReportQueue.BLOCKED.equals(new ReportQueue(directory).get(otherStart.id).state),
            "Bot rejection must retain the message for an explicit retry");
      queue.setState(otherStart.id, ReportQueue.PENDING);
      ReportDelivery.attempt(queue, otherStart.id, entry -> ReportDelivery.Outcome.DONE);
      check(!queue.waitsForEarlier(finish), "Acknowledged start must unblock the saved return");
    }
    finally
    {
      final File[] files = directory.listFiles();
      if (files != null)
        for (File file : files)
          file.delete();
      directory.delete();
    }
  }

  private static void check(boolean condition, String message)
  {
    if (!condition)
      throw new AssertionError(message);
  }
}
