package app.organicmaps.safety;

import static org.junit.Assert.*;

import org.junit.Test;

public class SafetyEngineTest
{
  private static final long STOP = 15 * 60_000;
  private static final long REPLY = 2 * 60_000;

  private SafetyEngine stopped()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    for (long time = 1000; time <= STOP + 1000; time += 60_000)
      assertTrue(engine.accept(43.2, 77.0, 5, time, time));
    return engine;
  }

  @Test
  public void asksOnlyWithContinuousEvidenceAndThenTimesOut()
  {
    final SafetyEngine engine = stopped();
    assertEquals(SafetyEngine.State.CHECK_IN, engine.state(STOP + 1000));
    assertEquals(SafetyEngine.State.CHECK_IN, engine.state(STOP + REPLY + 999));
    assertEquals(SafetyEngine.State.UNANSWERED, engine.state(STOP + REPLY + 1000));
  }

  @Test
  public void silenceIsNotImmobility()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    engine.accept(43.2, 77, 5, 1000, 1000);
    assertEquals(SafetyEngine.State.NO_LOCATION, engine.state(STOP + 1000));
    engine.accept(43.2, 77, 5, STOP + 2000, STOP + 2000);
    assertEquals(SafetyEngine.State.MOVING, engine.state(STOP + 2000));
  }

  @Test
  public void rejectsInaccurateInvalidStaleFutureAndDuplicateFixes()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    assertFalse(engine.accept(43, 77, 51, 1000, 1000));
    assertFalse(engine.accept(43, 77, 0, 1000, 1000));
    assertFalse(engine.accept(Double.NaN, 77, 5, 1000, 1000));
    assertFalse(engine.accept(91, 77, 5, 1000, 1000));
    assertFalse(engine.accept(43, 181, 5, 1000, 1000));
    assertFalse(engine.accept(43, 77, Double.POSITIVE_INFINITY, 1000, 1000));
    assertFalse(engine.accept(43, 77, 5, 1000, 200_000));
    assertFalse(engine.accept(43, 77, 5, 2000, 1000));
    assertTrue(engine.accept(43, 77, 5, 1000, 1000));
    assertFalse(engine.accept(43, 77, 5, 1000, 2000));
    assertFalse(engine.accept(43, 77, 5, 900, 2000));
  }

  @Test
  public void walkingResetsStationaryWindow()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    for (int minute = 0; minute < 30; minute++)
    {
      long now = 1000 + minute * 60_000;
      engine.accept(43.2 + minute * 0.001, 77, 5, now, now);
      assertEquals(SafetyEngine.State.MOVING, engine.state(now));
    }
  }

  @Test
  public void smallGpsJitterStillAllowsCheckIn()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    for (int minute = 0; minute <= 15; minute++)
    {
      long now = 1000 + minute * 60_000;
      engine.accept(43.2 + (minute % 2) * 0.00005, 77, 10, now, now);
    }
    assertEquals(SafetyEngine.State.CHECK_IN, engine.state(STOP + 1000));
  }

  @Test
  public void acknowledgementClearsQuestionAndRestartsWindow()
  {
    final SafetyEngine engine = stopped();
    engine.acknowledge();
    assertEquals(SafetyEngine.State.MOVING, engine.state(STOP + 2000));
    engine.accept(43.2, 77, 5, STOP + 3000, STOP + 3000);
    assertEquals(SafetyEngine.State.MOVING, engine.state(STOP + 3000));
  }

  @Test
  public void restSuppressesChecksAndDoesNotCountAsStationaryTime()
  {
    final SafetyEngine engine = stopped();
    final long start = STOP + 1000;
    engine.rest(start, 20 * 60_000);
    for (long time = start + 60_000; time < start + 20 * 60_000; time += 60_000)
    {
      engine.accept(43.2, 77, 5, time, time);
      assertEquals(SafetyEngine.State.RESTING, engine.state(time));
    }
    final long end = start + 20 * 60_000;
    engine.accept(43.2, 77, 5, end, end);
    assertEquals(SafetyEngine.State.MOVING, engine.state(end));
  }

  @Test
  public void lossOfGpsDoesNotEraseAnAlreadyUnansweredQuestion()
  {
    final SafetyEngine engine = stopped();
    engine.locationUnavailable();
    assertEquals(SafetyEngine.State.UNANSWERED, engine.state(STOP + REPLY + 1000));
    engine.acknowledge();
    assertEquals(SafetyEngine.State.NO_LOCATION, engine.state(STOP + REPLY + 1000));
  }

  @Test
  public void disabledGpsRequiresNewEvidence()
  {
    final SafetyEngine engine = new SafetyEngine(STOP, REPLY);
    engine.accept(43.2, 77, 5, 1000, 1000);
    engine.locationUnavailable();
    assertEquals(SafetyEngine.State.NO_LOCATION, engine.state(2000));
    engine.accept(43.2, 77, 5, 3000, 3000);
    assertEquals(SafetyEngine.State.MOVING, engine.state(3000));
  }

  @Test
  public void aNewEngineNeverRestoresAStationaryWindowFromOldCoordinates()
  {
    assertEquals(SafetyEngine.State.NO_LOCATION, new SafetyEngine(STOP, REPLY).state(9_000_000));
  }
}
