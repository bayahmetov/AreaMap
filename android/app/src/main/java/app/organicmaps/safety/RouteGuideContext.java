package app.organicmaps.safety;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Non-identifying route relevance only. Coordinates and personal trip data are never logged. */
public final class RouteGuideContext
{
  public final boolean present;
  public final String source;
  public final Set<String> tags;

  public RouteGuideContext(boolean present, String source, Set<String> tags)
  {
    this.present = present; this.source = source;
    this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
  }

  static void addLocation(Set<String> tags, double lat, double lon)
  {
    // Geographic region, not an inferred peak name or unverified route identity.
    if (lat >= 42.8 && lat <= 43.5 && lon >= 76.4 && lon <= 77.6) tags.add("almaty");
  }
}
