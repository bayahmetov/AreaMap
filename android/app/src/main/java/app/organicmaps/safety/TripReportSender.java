package app.organicmaps.safety;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
import app.organicmaps.R;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Prototype DCHS transport.
 *
 * Prototype delivery for the AreaMap demo. The destination is fixed in this build and the private
 * demo key is injected from ignored local.properties.
 */
public final class TripReportSender
{
  // Prototype destination.
  private static final String TELEGRAM_CHAT_ID = "5581444104";

  private TripReportSender() {}

  public static void shareToTelegram(@NonNull Activity activity, @NonNull String report)
  {
    if (!isBotConfigured())
    {
      shareWithTelegramApp(activity, report);
      return;
    }

    Toast.makeText(activity, R.string.areamap_telegram_sending, Toast.LENGTH_SHORT).show();
    new Thread(() -> {
      boolean success = false;
      try
      {
        final URL url = new URL("https://api.telegram.org/bot" + BuildConfig.AREAMAP_DEMO_KEY + "/sendMessage");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        final String body =
            "chat_id=" + encode(TELEGRAM_CHAT_ID) + "&text=" + encode(report) + "&disable_web_page_preview=true";
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream())
        {
          output.write(bytes);
        }

        final int code = connection.getResponseCode();
        success = code >= 200 && code < 300;
        connection.disconnect();
      }
      catch (Exception ignored)
      {
        success = false;
      }

      final boolean sent = success;
      activity.runOnUiThread(() -> {
        if (activity.isFinishing() || activity.isDestroyed())
          return;
        if (sent)
          Toast.makeText(activity, R.string.areamap_telegram_sent, Toast.LENGTH_LONG).show();
        else
        {
          Toast.makeText(activity, R.string.areamap_telegram_failed, Toast.LENGTH_LONG).show();
          shareWithTelegramApp(activity, report);
        }
      });
    }, "AreaMapTelegramReport").start();
  }

  private static boolean isBotConfigured()
  {
    return !BuildConfig.AREAMAP_DEMO_KEY.trim().isEmpty() && !TELEGRAM_CHAT_ID.trim().isEmpty();
  }

  @NonNull
  private static String encode(@NonNull String value) throws Exception
  {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
  }

  private static void shareWithTelegramApp(@NonNull Activity activity, @NonNull String report)
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
