package app.organicmaps.safety;

/** One user decision per confirmation, including cancellation during lifecycle changes. */
final class SosConfirmationGate
{
  private boolean mClosed;

  synchronized boolean tryConfirm()
  {
    if (mClosed)
      return false;
    mClosed = true;
    return true;
  }

  synchronized void cancel()
  {
    mClosed = true;
  }
}
