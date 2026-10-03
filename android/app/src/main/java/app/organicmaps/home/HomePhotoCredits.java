package app.organicmaps.home;

import android.content.Context;
import android.text.util.Linkify;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import app.organicmaps.R;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Offline attribution travels with the photographs and is accessible from HOME. */
public final class HomePhotoCredits
{
  private HomePhotoCredits() {}

  public static void show(Context context)
  {
    final String credits;
    try (InputStream input = context.getAssets().open("areamap/hikes/PHOTO_CREDITS.txt"))
    {
      final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
      final byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1)
        output.write(buffer, 0, count);
      credits = new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
    catch (IOException error)
    {
      android.util.Log.e("HomePhotoCredits", "Cannot read bundled photo credits", error);
      return;
    }
    final TextView text = new TextView(context);
    final int padding = context.getResources().getDimensionPixelSize(R.dimen.home_padding);
    text.setPadding(padding, padding, padding, padding);
    text.setText(credits);
    text.setTextIsSelectable(true);
    Linkify.addLinks(text, Linkify.WEB_URLS);
    final ScrollView scroll = new ScrollView(context);
    scroll.addView(text);
    new AlertDialog.Builder(context).setTitle(R.string.home_photo_credits).setView(scroll)
        .setPositiveButton(android.R.string.ok, null).show();
  }
}
