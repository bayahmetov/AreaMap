package app.organicmaps.safety;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;

public class TripWeatherDestinationTest
{
  private static final double LAT = 43.127825;
  private static final double LON = 77.013213;

  @Test
  public void offlineDestinationCacheDoesNotTouchTripCache()
  {
    final Context context = mock(Context.class);
    final SharedPreferences destination = mock(SharedPreferences.class);
    when(context.getSharedPreferences("areamap_weather_destination_tri_bratya", Context.MODE_PRIVATE))
        .thenReturn(destination);
    when(destination.getString("forecast_lat", "999")).thenReturn(Double.toString(LAT));
    when(destination.getString("forecast_lon", "999")).thenReturn(Double.toString(LON));
    when(destination.getInt("forecast_alt", -10000)).thenReturn(2860);
    when(destination.contains("forecast_json")).thenReturn(true);
    assertTrue(TripWeatherRepository.refreshDestination(context, "tri_bratya", LAT, LON, 2860, true));
    verify(context, never()).getSharedPreferences("areamap_weather", Context.MODE_PRIVATE);
    verify(destination, never()).edit();
  }

  @Test
  public void destinationCacheCannotBeShownForDifferentPoint()
  {
    final Context context = mock(Context.class);
    final SharedPreferences destination = mock(SharedPreferences.class);
    when(context.getSharedPreferences("areamap_weather_destination_tri_bratya", Context.MODE_PRIVATE))
        .thenReturn(destination);
    when(destination.contains("forecast_json")).thenReturn(true);
    when(destination.getString("forecast_lat", "999")).thenReturn("43.5");
    when(destination.getString("forecast_lon", "999")).thenReturn("77.5");
    assertTrue(TripWeatherRepository.destinationHours(context, "tri_bratya", LAT, LON, 2860).isEmpty());
    verify(destination, never()).getString("forecast_json", "");
  }

  @Test
  public void unavailableDestinationDoesNotBorrowActiveTripForecast()
  {
    final Context context = mock(Context.class);
    final SharedPreferences destination = mock(SharedPreferences.class);
    when(context.getSharedPreferences("areamap_weather_destination_tri_bratya", Context.MODE_PRIVATE))
        .thenReturn(destination);
    when(destination.getString("forecast_lat", "999")).thenReturn("999");
    when(destination.getString("forecast_lon", "999")).thenReturn("999");
    assertFalse(TripWeatherRepository.refreshDestination(context, "tri_bratya", LAT, LON, 2860, false));
    assertTrue(TripWeatherRepository.destinationHours(context, "tri_bratya", LAT, LON, 2860).isEmpty());
    verify(context, never()).getSharedPreferences("areamap_weather", Context.MODE_PRIVATE);
  }
}
