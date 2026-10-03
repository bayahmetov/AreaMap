package app.organicmaps.profile;

/** Local AreaMap identity, independent of OpenStreetMap authentication. */
public final class AreaMapUserProfile
{
  public final String displayName, bio, homeRegion, avatarPath;
  public final long createdAt;

  public AreaMapUserProfile(String name, String bio, String region, String avatarPath, long createdAt)
  {
    this.displayName = name.trim();
    this.bio = bio.trim();
    this.homeRegion = region.trim();
    this.avatarPath = avatarPath;
    this.createdAt = createdAt;
  }
}
