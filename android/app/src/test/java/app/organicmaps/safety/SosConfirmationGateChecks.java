package app.organicmaps.safety;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Host checks also run without Android/Gradle, following ReportQueueChecks. */
public final class SosConfirmationGateChecks
{
  private SosConfirmationGateChecks() {}

  public static void main(String[] args) throws Exception
  {
    runAll();
    System.out.println("SOS confirmation: 5 checks passed");
  }

  public static void runAll() throws Exception
  {
    final AtomicInteger sends = new AtomicInteger();
    final SosConfirmationGate opened = new SosConfirmationGate();
    check(sends.get() == 0, "Opening a confirmation must not send");
    opened.cancel();
    sendIfConfirmed(opened, sends);
    check(sends.get() == 0, "Cancel or Back must prevent a late confirmation callback");

    final SosConfirmationGate confirmed = new SosConfirmationGate();
    sendIfConfirmed(confirmed, sends);
    sendIfConfirmed(confirmed, sends);
    check(sends.get() == 1, "Double-tapping confirmation must send once");

    final SosConfirmationGate beforeRotation = new SosConfirmationGate();
    beforeRotation.cancel();
    sendIfConfirmed(beforeRotation, sends);
    final SosConfirmationGate afterRotation = new SosConfirmationGate();
    afterRotation.cancel();
    check(sends.get() == 1, "Backgrounding or rotation must not replay a pending send");

    final SosConfirmationGate raced = new SosConfirmationGate();
    final CountDownLatch ready = new CountDownLatch(1);
    final Runnable confirm = () -> {
      try
      {
        ready.await();
        sendIfConfirmed(raced, sends);
      }
      catch (InterruptedException e)
      {
        Thread.currentThread().interrupt();
        throw new AssertionError(e);
      }
    };
    final Thread first = new Thread(confirm);
    final Thread second = new Thread(confirm);
    first.start();
    second.start();
    ready.countDown();
    first.join();
    second.join();
    check(sends.get() == 2, "Concurrent confirmation callbacks must send once");
  }

  private static void sendIfConfirmed(SosConfirmationGate gate, AtomicInteger sends)
  {
    if (gate.tryConfirm())
      sends.incrementAndGet();
  }

  private static void check(boolean condition, String message)
  {
    if (!condition)
      throw new AssertionError(message);
  }
}
