package app.organicmaps.safety;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.util.WindowInsetUtils.PaddingInsetsListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class TripSafetyFragment extends BaseMwmFragment
{
  private TripSafety mSos;
  private LinearLayout mArticles;
  private List<GuideArticles.Article> mAllArticles;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_trip_safety, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    ViewCompat.setOnApplyWindowInsetsListener(view, PaddingInsetsListener.excludeTop());
    mSos = TripSafety.get(requireContext());

    mArticles = view.findViewById(R.id.guide_articles);
    mAllArticles = GuideArticles.all();
    renderArticles("");

    final EditText search = view.findViewById(R.id.guide_search);
    search.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        renderArticles(s == null ? "" : s.toString());
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    view.findViewById(R.id.guide_import)
        .setOnClickListener(v -> startActivity(new Intent(requireContext(), RouteImportActivity.class)));

    view.findViewById(R.id.trip_sos).setOnClickListener(v -> showSos());
    refreshLastLocation(view);

    if (requireActivity().getIntent().getBooleanExtra(TripSafetyActivity.EXTRA_SHOW_SOS, false))
    {
      requireActivity().getIntent().removeExtra(TripSafetyActivity.EXTRA_SHOW_SOS);
      view.post(this::showSos);
    }
  }

  private void renderArticles(@NonNull String query)
  {
    mArticles.removeAllViews();
    for (GuideArticles.Article article : mAllArticles)
    {
      if (!article.matches(query))
        continue;

      final Button card = new Button(requireContext());
      card.setAllCaps(false);
      card.setText(article.title);
      card.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
      card.setOnClickListener(v -> openArticle(article));
      mArticles.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
    }
  }

  private void openArticle(@NonNull GuideArticles.Article article)
  {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(article.title)
        .setMessage(article.body)
        .setPositiveButton(R.string.areamap_close, null)
        .show();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    final View view = getView();
    if (view != null)
      refreshLastLocation(view);
  }

  private void refreshLastLocation(@NonNull View view)
  {
    final Location last = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (last != null)
      mSos.save(last);
    ((TextView) view.findViewById(R.id.trip_coordinates)).setText(mSos.coordinates());
  }

  private void showSos()
  {
    final Location last = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (last != null)
      mSos.save(last);

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_sos)
        .setMessage(mSos.card())
        .setNegativeButton(R.string.areamap_close, null)
        .setPositiveButton(R.string.areamap_dial,
                           (dialog, which) -> openIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))))
        .setNeutralButton(R.string.areamap_share,
                          (dialog, which) -> {
                            final Intent send = new Intent(Intent.ACTION_SEND)
                                                    .setType("text/plain")
                                                    .putExtra(Intent.EXTRA_TEXT, mSos.card());
                            openIntent(Intent.createChooser(send, getString(R.string.areamap_share)));
                          })
        .show();
  }

  private void openIntent(@NonNull Intent intent)
  {
    try
    {
      startActivity(intent);
    }
    catch (ActivityNotFoundException e)
    {
      Toast.makeText(requireContext(), R.string.areamap_no_handler, Toast.LENGTH_LONG).show();
    }
  }
}
