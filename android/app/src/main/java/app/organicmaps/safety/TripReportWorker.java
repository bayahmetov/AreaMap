package app.organicmaps.safety;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import app.organicmaps.BuildConfig;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Network failures keep the original message, coordinates and event ID for retry. */
public final class TripReportWorker extends Worker
{
  private static final String TELEGRAM_CHAT_ID = "-5581444104";

  public TripReportWorker(@NonNull Context context, @NonNull WorkerParameters params)
  {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork()
  {
    final Context context = getApplicationContext();
    final String id = getInputData().getString("event_id");
    if (id == null)
      return Result.failure();
    final ReportDelivery.Outcome outcome = ReportDelivery.attempt(
        TripReportSender.queue(context), id,
        entry -> TripReportSender.isConfigured() ? deliver(entry) : ReportDelivery.Outcome.RETRY);
    TripReportSender.notifyStateChanged(context);
    if (outcome == ReportDelivery.Outcome.DONE)
      return Result.success();
    if (outcome == ReportDelivery.Outcome.BLOCKED)
      return Result.failure();
    return Result.retry();
  }

  private ReportDelivery.Outcome deliver(ReportQueue.Entry entry) throws Exception
  {
    HttpURLConnection connection = null;
    try
    {
      connection =
          (HttpURLConnection) new URL("https://api.telegram.org/bot" + BuildConfig.AREAMAP_DEMO_KEY + "/sendMessage")
              .openConnection();
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      final String report = entry.body + "\nEvent ID: " + entry.id;
      final byte[] body =
          ("chat_id=" + encode(TELEGRAM_CHAT_ID) + "&text=" + encode(report) + "&disable_web_page_preview=true")
              .getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(body.length);
      try (OutputStream output = connection.getOutputStream())
      {
        output.write(body);
      }
      final int code = connection.getResponseCode();
      if (code == 429 || code >= 500)
        return ReportDelivery.Outcome.RETRY;
      if (code == 400 || code == 401 || code == 403 || code == 404)
        return ReportDelivery.Outcome.BLOCKED;
      if (code < 200 || code >= 300)
        return ReportDelivery.Outcome.RETRY;
      final JSONObject response = new JSONObject(read(connection.getInputStream()));
      if (!response.optBoolean("ok") || response.optJSONObject("result") == null
          || response.getJSONObject("result").optLong("message_id", 0) == 0)
        return ReportDelivery.Outcome.RETRY;
      return ReportDelivery.Outcome.DONE;
    }
    finally
    {
      if (connection != null)
        connection.disconnect();
    }
  }

  private static String encode(String value) throws Exception
  {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
  }

  private static String read(InputStream stream) throws Exception
  {
    final StringBuilder out = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = reader.readLine()) != null)
        out.append(line);
    }
    return out.toString();
  }
}
