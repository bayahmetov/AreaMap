package app.organicmaps.profile;

/** Progress requires recorded evidence; missing tracking never grants a badge. */
public final class Achievement
{
  public final String id;
  public final int title, description, icon, target, progress;
  public final String requirementType;
  public final long unlockedAt;

  public Achievement(String id, int title, int description, int icon, String requirementType,
                     int target, int progress, long unlockedAt)
  {
    this.id = id;
    this.title = title;
    this.description = description;
    this.icon = icon;
    this.requirementType = requirementType;
    this.target = target;
    this.progress = Math.max(0, Math.min(progress, target));
    this.unlockedAt = this.progress >= target ? unlockedAt : 0;
  }

  public boolean isUnlocked() { return progress >= target; }
}
