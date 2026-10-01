package app.organicmaps.safety;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TripWeatherRepositoryTest
{
  @Test
  public void detectsMountainWeatherHazards()
  {
    assertFalse(TripWeatherRepository.isHazard(
        new TripWeatherRepository.Hour(0L, 8.0, 20, 0.0, 2, 10.0, 20.0)));

    assertTrue(TripWeatherRepository.isHazard(
        new TripWeatherRepository.Hour(0L, 8.0, 70, 0.0, 2, 10.0, 20.0)));

    assertTrue(TripWeatherRepository.isHazard(
        new TripWeatherRepository.Hour(0L, 8.0, 10, 0.0, 2, 20.0, 55.0)));

    assertTrue(TripWeatherRepository.isHazard(
        new TripWeatherRepository.Hour(0L, 5.0, 10, 0.0, 95, 10.0, 20.0)));

    assertTrue(TripWeatherRepository.isHazard(
        new TripWeatherRepository.Hour(0L, 0.0, 30, 0.5, 61, 10.0, 20.0)));
  }
}
