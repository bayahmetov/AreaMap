package app.organicmaps.safety;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.util.StorageUtils;
import app.organicmaps.sdk.util.concurrency.ThreadPool;
import java.io.File;

public class RouteImportActivity extends AppCompatActivity
{
  private final ActivityResultLauncher<String[]> mPicker =
      registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onDocument);

  @Override
  protected void onCreate(@Nullable Bundle state)
  {
    super.onCreate(state);
    if (state == null)
      mPicker.launch(new String[] {"application/gpx", "application/gpx+xml", "application/vnd.google-earth.kml+xml",
                                   "application/vnd.google-earth.kmz", "application/xml", "text/xml", "*/*"});
  }

  private void onDocument(@Nullable Uri uri)
  {
    if (uri == null)
    {
      finish();
      return;
    }

    final File tempDir = new File(StorageUtils.getTempPath(MwmApplication.from(this)));
    ThreadPool.getStorage().execute(() -> {
      final boolean accepted = BookmarkManager.INSTANCE.importBookmarksFile(getContentResolver(), uri, tempDir);
      runOnUiThread(() -> {
        Toast.makeText(this, accepted ? R.string.areamap_import_success : R.string.areamap_import_failed,
                       Toast.LENGTH_LONG)
            .show();
        finish();
      });
    });
  }
}
