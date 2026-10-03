package app.organicmaps.safety;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.util.LruCache;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import app.organicmaps.R;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Bounded local bitmap cache. No internet dependency or borrowed location imagery. */
final class GuidePhotos
{
  private final Context mContext;
  private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
    @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
  };
  GuidePhotos(Context context) { mContext = context.getApplicationContext(); }

  void load(ImageView view, String asset)
  {
    Bitmap bitmap = mCache.get(asset);
    if (bitmap == null)
    {
      try (InputStream input = mContext.getAssets().open(asset))
      {
        bitmap = BitmapFactory.decodeStream(input);
        if (bitmap != null) mCache.put(asset, bitmap);
      }
      catch (IOException error) { android.util.Log.e("GuidePhotos", "Missing local photograph: " + asset, error); }
    }
    if (bitmap == null && !asset.equals("areamap/guides/guide_hero_almaty.webp"))
    {
      // Intentional regional illustration; does not pretend to depict a missing specific location.
      load(view, "areamap/guides/guide_hero_almaty.webp");
      return;
    }
    if (bitmap == null) view.setImageResource(R.drawable.home_logo);
    else view.setImageBitmap(bitmap);
  }

  static void round(View view)
  {
    view.setOutlineProvider(new ViewOutlineProvider() {
      @Override public void getOutline(View v, Outline outline)
      {
        outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
            v.getResources().getDimension(R.dimen.home_card_radius));
      }
    });
    view.setClipToOutline(true);
  }

  static void credits(Context context)
  {
    try (InputStream input = context.getAssets().open("areamap/guides/GUIDE_IMAGE_SOURCES.md"))
    {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      final byte[] bytes = new byte[4096]; int count;
      while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
      final TextView text = new TextView(context);
      final int padding = context.getResources().getDimensionPixelSize(R.dimen.home_padding);
      text.setPadding(padding, padding, padding, padding);
      text.setText(new String(output.toByteArray(), StandardCharsets.UTF_8));
      text.setTextIsSelectable(true);
      android.text.util.Linkify.addLinks(text, android.text.util.Linkify.WEB_URLS);
      final ScrollView scroll = new ScrollView(context); scroll.addView(text);
      new AlertDialog.Builder(context).setTitle(R.string.g_credits).setView(scroll)
          .setPositiveButton(android.R.string.ok, null).show();
    }
    catch (IOException error) { android.util.Log.e("GuidePhotos", "Cannot read credits", error); }
  }
}
