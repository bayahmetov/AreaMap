package app.organicmaps.safety;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import app.organicmaps.R;

/**
 * Prototype DCHS transport.
 *
 * For now the app hands the already-confirmed report to Telegram. A production integration should
 * replace this class with a server-side relay; a Telegram bot token must never be shipped in the APK.
 */
public final class TripReportSender
{
  private TripReportSender() {}

  public static void shareToTelegram(@NonNull Activity activity, @NonNull String report)
  {
    final Intent telegram = new Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, report)
        .setPackage("org.telegram.messenger");
    try
    {
      activity.startActivity(telegram);
      return;
    }
    catch (ActivityNotFoundException ignored)
    {
      // Telegram may be missing or use a different package. Fall back to the system share sheet.
    }

    final Intent share = new Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, report);
    try
    {
      activity.startActivity(Intent.createChooser(share, activity.getString(R.string.areamap_send_trip_report)));
    }
    catch (ActivityNotFoundException e)
    {
      Toast.makeText(activity, R.string.areamap_no_handler, Toast.LENGTH_LONG).show();
    }
  }
}
