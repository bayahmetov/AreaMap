package app.organicmaps.safety;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import app.organicmaps.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Small app-local language layer for the AreaMap shell. */
public final class AreaMapLocale
{
  private static final String PREFS = "areamap_locale";
  private static final String KEY_LANGUAGE = "language";

  private AreaMapLocale() {}

  public static void applySaved(@NonNull Context context)
  {
    final String tag = prefs(context).getString(KEY_LANGUAGE, "");
    AppCompatDelegate.setApplicationLocales(
        tag == null || tag.isEmpty() ? LocaleListCompat.getEmptyLocaleList()
                                     : LocaleListCompat.forLanguageTags(tag));
  }

  @NonNull
  public static String selectedTag(@NonNull Context context)
  {
    return prefs(context).getString(KEY_LANGUAGE, "");
  }

  @NonNull
  public static String selectedLabel(@NonNull Context context)
  {
    final String tag = selectedTag(context);
    if ("ru".equals(tag))
      return context.getString(R.string.areamap_language_russian);
    if ("en".equals(tag))
      return context.getString(R.string.areamap_language_english);
    return context.getString(R.string.areamap_language_system);
  }

  public static void showPicker(@NonNull Activity activity)
  {
    final String[] tags = {"", "ru", "en"};
    final String[] labels = {
        activity.getString(R.string.areamap_language_system),
        activity.getString(R.string.areamap_language_russian),
        activity.getString(R.string.areamap_language_english)
    };
    final String current = selectedTag(activity);
    int checked = 0;
    for (int i = 0; i < tags.length; i++)
      if (tags[i].equals(current))
        checked = i;

    final int initial = checked;
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.areamap_language_title)
        .setSingleChoiceItems(labels, initial, (dialog, which) -> {
          final String tag = tags[which];
          prefs(activity).edit().putString(KEY_LANGUAGE, tag).apply();
          AppCompatDelegate.setApplicationLocales(
              tag.isEmpty() ? LocaleListCompat.getEmptyLocaleList() : LocaleListCompat.forLanguageTags(tag));
          dialog.dismiss();
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  @NonNull
  private static SharedPreferences prefs(@NonNull Context context)
  {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }
}
