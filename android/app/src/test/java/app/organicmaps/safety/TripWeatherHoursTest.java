package app.organicmaps.safety;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class TripWeatherHoursTest
{
  private String payload(Object temperature) throws Exception
  {
    final JSONObject hourly = new JSONObject();
    hourly.put("time", new JSONArray(new long[] {3600, 7200, 10800}));
    hourly.put("temperature_2m", new JSONArray().put(temperature).put(8).put(7));
    hourly.put("precipitation_probability", new JSONArray(new int[] {10, 40, 60}));
    hourly.put("precipitation", new JSONArray(new double[] {0, 0.5, 1}));
    hourly.put("weather_code", new JSONArray(new int[] {0, 61, 61}));
    hourly.put("wind_speed_10m", new JSONArray(new double[] {9, 12, 15}));
    hourly.put("wind_gusts_10m", new JSONArray(new double[] {12, 18, 20}));
    return new JSONObject().put("hourly", hourly).toString();
  }

  @Test
  public void currentHourAndFutureValuesComeFromCache() throws Exception
  {
    final var hours = TripWeatherRepository.parseHours(payload(9), 5400000, 8);
    assertEquals(3, hours.size());
    assertEquals(3600000L, hours.get(0).timeMillis);
    assertEquals(9, hours.get(0).temperatureC, 0);
    assertEquals(40, hours.get(1).precipitationProbability);
    assertEquals(12, hours.get(1).windKmh, 0);
  }

  @Test
  public void expiredCacheAndLimitsDoNotCreateHours() throws Exception
  {
    assertEquals(1, TripWeatherRepository.parseHours(payload(9), 5400000, 1).size());
    assertTrue(TripWeatherRepository.parseHours(payload(9), 14400000, 8).isEmpty());
    assertTrue(TripWeatherRepository.parseHours("", 5400000, 8).isEmpty());
  }

  @Test
  public void missingTemperatureIsNotPresentedAsZeroDegrees() throws Exception
  {
    final var hours = TripWeatherRepository.parseHours(payload(JSONObject.NULL), 5400000, 8);
    assertEquals(2, hours.size());
    assertEquals(7200000L, hours.get(0).timeMillis);
  }
}
