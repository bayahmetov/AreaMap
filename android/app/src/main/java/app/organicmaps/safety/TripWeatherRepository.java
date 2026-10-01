package app.organicmaps.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Downloads a compact hourly forecast for the active trip weather point and keeps
 * the raw response on-device so the forecast remains readable without coverage.
 */
public final class TripWeatherRepository
{
  private static final String PREFS = "areamap_weather";
  private static final String KEY_JSON = "forecast_json";
  private static final String KEY_FETCHED_AT = "forecast_fetched_at";
  private static final String KEY_LAT = "forecast_lat";
  private static final String KEY_LON = "forecast_lon";
  private static final String KEY_ALT = "forecast_alt";
  private static final long REFRESH_MS = 60 * 60 * 1000L;
  private static final int CONNECT_TIMEOUT_MS = 8_000;
  private static final int READ_TIMEOUT_MS = 10_000;

  public static final class Hour
  {
    public final long timeMillis;
    public final double temperatureC;
    public final int precipitationProbability;
    public final double precipitationMm;
    public final int weatherCode;
    public final double windKmh;
    public final double gustKmh;

    Hour(long timeMillis, double temperatureC, int precipitationProbability,
         double precipitationMm, int weatherCode, double windKmh, double gustKmh)
    {
      this.timeMillis = timeMillis;
      this.temperatureC = temperatureC;
      this.precipitationProbability = precipitationProbability;
      this.precipitationMm = precipitationMm;
      this.weatherCode = weatherCode;
      this.windKmh = windKmh;
      this.gustKmh = gustKmh;
    }
  }

  private TripWeatherRepository() {}

  public static boolean refreshIfNeeded(@NonNull Context context, @NonNull TripSafety safety)
  {
    if (!safety.hasActiveTrip())
      return false;

    final SharedPreferences prefs = prefs(context);
    final long now = System.currentTimeMillis();
    final double lat = safety.weatherLat();
    final double lon = safety.weatherLon();
    final int alt = safety.weatherAltitudeMeters();
    final boolean moved = Math.abs(lat - parseDouble(prefs.getString(KEY_LAT, "999"))) > 0.002
        || Math.abs(lon - parseDouble(prefs.getString(KEY_LON, "999"))) > 0.002
        || Math.abs(alt - prefs.getInt(KEY_ALT, -10000)) > 100;

    if (!moved && prefs.contains(KEY_JSON) && now - prefs.getLong(KEY_FETCHED_AT, 0L) < REFRESH_MS)
      return true;

    if (!isNetworkConnected(context))
      return prefs.contains(KEY_JSON);

    HttpURLConnection connection = null;
    try
    {
      final String altitude = alt >= 0 ? "&elevation=" + alt : "";
      final String endpoint = String.format(Locale.US,
          "https://api.open-meteo.com/v1/forecast"
              + "?latitude=%.6f&longitude=%.6f%s"
              + "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_gusts_10m"
              + "&forecast_days=2&timeformat=unixtime&timezone=GMT",
          lat, lon, altitude);

      connection = (HttpURLConnection) new URL(endpoint).openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.setUseCaches(false);
      connection.setRequestProperty("Accept", "application/json");
      connection.connect();
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
        return false;

      final String body = readAll(connection.getInputStream());
      final JSONObject json = new JSONObject(body);
      final JSONObject hourly = json.optJSONObject("hourly");
      if (hourly == null || hourly.optJSONArray("time") == null)
        return false;

      prefs.edit()
          .putString(KEY_JSON, body)
          .putLong(KEY_FETCHED_AT, now)
          .putString(KEY_LAT, Double.toString(lat))
          .putString(KEY_LON, Double.toString(lon))
          .putInt(KEY_ALT, alt)
          .apply();
      return true;
    }
    catch (Exception ignored)
    {
      // Offline is an expected state in the mountains. Keep the previous forecast.
      return prefs.contains(KEY_JSON);
    }
    finally
    {
      if (connection != null)
        connection.disconnect();
    }
  }

  @Nullable
  public static Hour closestHour(@NonNull Context context, long targetMillis)
  {
    try
    {
      final String raw = prefs(context).getString(KEY_JSON, null);
      if (raw == null)
        return null;
      final JSONObject root = new JSONObject(raw);
      final JSONObject hourly = root.getJSONObject("hourly");
      final JSONArray times = hourly.getJSONArray("time");
      final JSONArray temperatures = hourly.getJSONArray("temperature_2m");
      final JSONArray probabilities = hourly.getJSONArray("precipitation_probability");
      final JSONArray precipitation = hourly.getJSONArray("precipitation");
      final JSONArray codes = hourly.getJSONArray("weather_code");
      final JSONArray winds = hourly.getJSONArray("wind_speed_10m");
      final JSONArray gusts = hourly.getJSONArray("wind_gusts_10m");

      long bestDistance = Long.MAX_VALUE;
      Hour best = null;
      for (int i = 0; i < times.length(); i++)
      {
        final long time = times.getLong(i) * 1000L;
        final long distance = Math.abs(time - targetMillis);
        if (distance >= bestDistance)
          continue;
        bestDistance = distance;
        best = new Hour(time, temperatures.optDouble(i, Double.NaN),
                        probabilities.optInt(i, 0), precipitation.optDouble(i, 0.0),
                        codes.optInt(i, 0), winds.optDouble(i, 0.0), gusts.optDouble(i, 0.0));
      }
      return best;
    }
    catch (Exception ignored)
    {
      return null;
    }
  }

  @Nullable
  public static Hour firstHazard(@NonNull Context context, long fromMillis, long toMillis)
  {
    try
    {
      final String raw = prefs(context).getString(KEY_JSON, null);
      if (raw == null)
        return null;
      final JSONObject root = new JSONObject(raw);
      final JSONObject hourly = root.getJSONObject("hourly");
      final JSONArray times = hourly.getJSONArray("time");
      for (int i = 0; i < times.length(); i++)
      {
        final long time = times.getLong(i) * 1000L;
        if (time < fromMillis || time > toMillis)
          continue;
        final Hour hour = hourAt(hourly, i, time);
        if (isHazard(hour))
          return hour;
      }
    }
    catch (Exception ignored) {}
    return null;
  }

  public static boolean isHazard(@NonNull Hour hour)
  {
    return hour.precipitationProbability >= 60
        || hour.precipitationMm >= 1.0
        || hour.gustKmh >= 50.0
        || hour.weatherCode == 95 || hour.weatherCode == 96 || hour.weatherCode == 99
        || (hour.temperatureC <= 1.0 && hour.precipitationMm > 0.0);
  }

  public static long fetchedAt(@NonNull Context context)
  {
    return prefs(context).getLong(KEY_FETCHED_AT, 0L);
  }

  public static void clearAlertState(@NonNull Context context)
  {
    prefs(context).edit().remove("last_alert_key").apply();
  }

  public static boolean markAlertIfNew(@NonNull Context context, @NonNull Hour hour)
  {
    final String key = hour.timeMillis + ":" + hazardKind(hour);
    final SharedPreferences prefs = prefs(context);
    if (key.equals(prefs.getString("last_alert_key", "")))
      return false;
    prefs.edit().putString("last_alert_key", key).apply();
    return true;
  }

  @NonNull
  public static String hazardKind(@NonNull Hour hour)
  {
    if (hour.weatherCode == 95 || hour.weatherCode == 96 || hour.weatherCode == 99)
      return "storm";
    if (hour.temperatureC <= 1.0 && hour.precipitationMm > 0.0)
      return "ice";
    if (hour.gustKmh >= 50.0)
      return "wind";
    return "rain";
  }

  private static Hour hourAt(@NonNull JSONObject hourly, int i, long time)
  {
    return new Hour(time,
        hourly.optJSONArray("temperature_2m").optDouble(i, Double.NaN),
        hourly.optJSONArray("precipitation_probability").optInt(i, 0),
        hourly.optJSONArray("precipitation").optDouble(i, 0.0),
        hourly.optJSONArray("weather_code").optInt(i, 0),
        hourly.optJSONArray("wind_speed_10m").optDouble(i, 0.0),
        hourly.optJSONArray("wind_gusts_10m").optDouble(i, 0.0));
  }

  private static boolean isNetworkConnected(@NonNull Context context)
  {
    final ConnectivityManager manager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (manager == null)
      return false;
    final Network network = manager.getActiveNetwork();
    if (network == null)
      return false;
    final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
    return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  @NonNull
  private static SharedPreferences prefs(@NonNull Context context)
  {
    return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  private static double parseDouble(@Nullable String value)
  {
    try
    {
      return value == null ? 0.0 : Double.parseDouble(value);
    }
    catch (NumberFormatException ignored)
    {
      return 0.0;
    }
  }

  @NonNull
  private static String readAll(@NonNull InputStream stream) throws Exception
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
