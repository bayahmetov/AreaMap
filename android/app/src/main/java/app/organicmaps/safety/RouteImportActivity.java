package app.organicmaps.safety;

import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import app.organicmaps.MwmApplication;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.util.StorageUtils;
import app.organicmaps.sdk.util.concurrency.ThreadPool;
import java.io.File;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RouteImportActivity extends AppCompatActivity
{
  public static class ImportModel extends ViewModel
  {
    final MutableLiveData<ImportResult> result = new MutableLiveData<>();
  }

  private static final class ImportResult
  {
    final boolean accepted;
    final GpxTrack track;
    ImportResult(boolean accepted, GpxTrack track)
    {
      this.accepted = accepted;
      this.track = track;
    }
  }

  private ImportModel mModel;
  private final ActivityResultLauncher<String[]> mPicker =
      registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onDocument);

  @Override
  protected void onCreate(@Nullable Bundle state)
  {
    super.onCreate(state);
    mModel = new ViewModelProvider(this).get(ImportModel.class);
    mModel.result.observe(this, result -> showResult(result.accepted, result.track));
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

    final MwmApplication application = MwmApplication.from(this);
    final File tempDir = new File(StorageUtils.getTempPath(application));
    final ImportModel model = mModel;
    ThreadPool.getStorage().execute(() -> {
      final boolean accepted = BookmarkManager.INSTANCE.importBookmarksFile(application.getContentResolver(), uri, tempDir);
      final GpxTrack track = accepted ? GpxNavigation.read(application.getContentResolver(), uri) : null;
      model.result.postValue(new ImportResult(accepted, track));
    });
  }

  private void showResult(boolean accepted, @Nullable GpxTrack track)
  {
    if (isFinishing() || isDestroyed())
      return;
    if (!accepted)
    {
      Toast.makeText(this, R.string.areamap_import_failed, Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.areamap_import_success)
        .setNegativeButton(R.string.areamap_close, (d, w) -> finish())
        .setOnCancelListener(d -> finish());
    if (track == null)
      dialog.setMessage(R.string.areamap_gpx_unsupported);
    else
    {
      final int minutes = HikingTiming.conservativeSeconds(0, track.length, track.ascent) / 60;
      dialog.setMessage(getString(R.string.areamap_gpx_preview, track.length / 1000, minutes / 60, minutes % 60)
          + "\n\n" + getString(R.string.areamap_gpx_explanation))
          .setPositiveButton(R.string.areamap_follow_track, (d, w) -> {
            GpxNavigation.current = new GpxNavigation(track);
            startActivity(new Intent(this, MwmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
          });
    }
    dialog.show();
  }
}
