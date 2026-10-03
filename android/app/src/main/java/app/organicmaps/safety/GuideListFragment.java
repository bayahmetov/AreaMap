package app.organicmaps.safety;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import java.util.ArrayList;
import java.util.List;

/** Existing Guides destination, now an offline knowledge hub with actual category/search filtering. */
public class GuideListFragment extends BaseMwmFragment
{
  private List<GuideArticles.Article> mArticles;
  private GuideRepository mRepository;
  private String mListing = "";
  private GuideArticleAdapter mPopular;
  private GuideArticleAdapter mRoute;
  private View mRoot;
  private EditText mSearch;
  private String mCategory = "";
  private boolean mFavoritesOnly;
  private int mSlide;

  private GuidePhotos mPhotos;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_guide_list, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    mRoot = view;
    mRepository = GuideRepository.create(requireContext());
    mArticles = mRepository.all();
    mListing = state == null ? "" : state.getString("listing", "");
    mPhotos = new GuidePhotos(requireContext());
    mCategory = state == null ? "" : state.getString("category", "");
    mFavoritesOnly = state != null && state.getBoolean("favorites");
    mSlide = state == null ? 0 : state.getInt("slide", 0);
    mSearch = view.findViewById(R.id.guide_search);
    mPopular = new GuideArticleAdapter(requireContext(), false, this::openArticle, this::render);
    mRoute = new GuideArticleAdapter(requireContext(), true, this::openArticle, this::render);
    carousel(R.id.guide_popular, mPopular);
    carousel(R.id.guide_route_articles, mRoute);
    final RecyclerView categories = view.findViewById(R.id.guide_categories);
    final boolean wide = getResources().getConfiguration().screenWidthDp >= 600;
    final GridLayoutManager grid = new GridLayoutManager(requireContext(), wide ? 12 : 2);
    if (wide) grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override public int getSpanSize(int position) { return position < 4 ? 3 : 4; }
    });
    categories.setLayoutManager(grid);
    categories.setAdapter(new GuideCategoryAdapter(requireContext(), mArticles, category -> {
      android.util.Log.d("Guides", "Category opened: " + category);
      mCategory = category; mFavoritesOnly = false; mSearch.setText(""); openListing("category");
    }));
    view.findViewById(R.id.guide_filters).setOnClickListener(v -> showFilters());
    view.findViewById(R.id.guide_all_categories).setOnClickListener(v -> openAll());
    view.findViewById(R.id.guide_all_articles).setOnClickListener(v -> openAll());
    view.findViewById(R.id.guide_explore).setOnClickListener(v -> {
      final List<GuideArticles.Article> featured = mRepository.featured();
      if (!featured.isEmpty()) openArticle(featured.get(mSlide));
    });
    view.findViewById(R.id.guide_route_all).setOnClickListener(v -> {
      if (hasRoute()) { mCategory = ""; mFavoritesOnly = false; mSearch.setText(""); openListing("route"); }
      else returnToMap("route");
    });
    view.findViewById(R.id.guide_clear_search).setOnClickListener(v -> openAll());
    view.findViewById(R.id.guide_route_choose).setOnClickListener(v -> returnToMap("route"));
    view.findViewById(R.id.guide_credits).setOnClickListener(v -> GuidePhotos.credits(requireContext()));
    GuidePhotos.round(view.findViewById(R.id.guide_hero));
    setupHero();
    setupNavigation();
    mSearch.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void afterTextChanged(Editable text) {
        if (!text.toString().trim().isEmpty() && mListing.isEmpty()) mListing = "all";
        render();
      }
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    });
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      final Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
          | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    if (getResources().getConfiguration().screenWidthDp < 400)
      view.findViewById(R.id.guide_brand).setVisibility(View.GONE);
    ViewCompat.requestApplyInsets(view);
    render();
  }

  private void carousel(int id, GuideArticleAdapter adapter)
  {
    final RecyclerView recycler = mRoot.findViewById(id);
    recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
    recycler.setAdapter(adapter);
  }

  private boolean hasRoute() { return GuideRouteState.read(requireContext()).present; }

  private void render()
  {
    if (mRoot == null || mPopular == null) return;
    final RouteGuideContext context = GuideRouteState.read(requireContext());
    android.util.Log.d("Guides", "Route context source=" + context.source + " tags=" + context.tags);
    final List<GuideArticles.Article> recommended = mRepository.forRoute(context);
    final List<GuideArticles.Article> visible = new ArrayList<>();
    for (GuideArticles.Article article : mRepository.search(mSearch.getText().toString(), mCategory))
      if ((!mFavoritesOnly || mPopular.isFavorite(article))
          && (!mListing.equals("route") || recommended.contains(article))) visible.add(article);
    if (mListing.equals("route")) visible.sort(java.util.Comparator.comparingInt(recommended::indexOf));
    // Editorial cards first, while the same canonical emergency articles remain in every full listing.
    if (mListing.isEmpty()) visible.sort(java.util.Comparator.comparing(a -> a.sourceType.equals("migrated")));
    mPopular.setListing(!mListing.isEmpty());
    mPopular.submit(visible);
    mRoot.findViewById(R.id.guide_empty).setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    mRoot.findViewById(R.id.guide_clear_search).setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    ((TextView) mRoot.findViewById(R.id.guide_popular_title)).setText(!mCategory.isEmpty()
        ? getString(GuideCategory.find(mCategory).title)
        : getString(mFavoritesOnly ? R.string.g_favorites : mListing.equals("route") ? R.string.g_route
            : mListing.isEmpty() ? R.string.g_popular : R.string.g_all));
    ((TextView) mRoot.findViewById(R.id.guide_route_subtitle)).setText(!context.present ? R.string.g_route_none
        : context.tags.contains("almaty") ? R.string.g_route_almaty : R.string.g_route_general);
    mRoot.findViewById(R.id.guide_route_choose).setVisibility(context.present ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.guide_route_articles).setVisibility(context.present ? View.VISIBLE : View.GONE);
    mRoute.submit(recommended);
    for (int id : new int[] {R.id.guide_hero, R.id.guide_categories, R.id.guide_categories_row,
        R.id.guide_route_row, R.id.guide_route_subtitle, R.id.guide_route_choose, R.id.guide_route_articles})
      if (!mListing.isEmpty()) mRoot.findViewById(id).setVisibility(View.GONE);
      else if (id != R.id.guide_route_choose && id != R.id.guide_route_articles)
        mRoot.findViewById(id).setVisibility(View.VISIBLE);
    mRoot.findViewById(R.id.guide_all_articles).setVisibility(mListing.isEmpty() ? View.VISIBLE : View.GONE);
    final RecyclerView recycler = mRoot.findViewById(R.id.guide_popular);
    final int orientation = mListing.isEmpty() ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL;
    if (((LinearLayoutManager) recycler.getLayoutManager()).getOrientation() != orientation)
      recycler.setLayoutManager(new LinearLayoutManager(requireContext(), orientation, false));
  }

  private void openListing(String mode)
  {
    mListing = mode; render(); scrollToArticles();
  }

  private void openAll()
  {
    mCategory = ""; mFavoritesOnly = false; mSearch.setText(""); openListing("all");
  }

  @Override public boolean onBackPressed()
  {
    if (mListing.isEmpty()) return false;
    reset(); return true;
  }

  private void showFilters()
  {
    final List<GuideCategory> categories = GuideCategory.all();
    final String[] labels = new String[categories.size() + 2];
    labels[0] = getString(R.string.g_all); labels[1] = getString(R.string.g_favorites);
    for (int i = 0; i < categories.size(); i++) labels[i + 2] = getString(categories.get(i).title);
    new AlertDialog.Builder(requireContext()).setTitle(R.string.g_filters).setItems(labels, (d, index) -> {
      mFavoritesOnly = index == 1; mCategory = index < 2 ? "" : categories.get(index - 2).id;
      openListing("category");
    }).setNeutralButton(R.string.g_reset, (d, w) -> reset())
        .setNegativeButton(R.string.cancel, null).show();
  }

  private void reset()
  {
    mCategory = ""; mFavoritesOnly = false; mSearch.setText(""); mListing = ""; render();
    scrollTo(mRoot.findViewById(R.id.guide_hero));
  }
  private void scrollToArticles() { scrollTo(mRoot.findViewById(R.id.guide_popular_title)); }
  private void scrollTo(View target)
  {
    mSearch.clearFocus();
    final android.view.inputmethod.InputMethodManager keyboard =
        (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
    if (keyboard != null) keyboard.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
    mRoot.post(() -> {
      if (mRoot != null)
        ((androidx.core.widget.NestedScrollView) mRoot.findViewById(R.id.guide_scroll))
            .smoothScrollTo(0, target.getTop());
    });
  }

  private void openArticle(GuideArticles.Article article)
  {
    android.util.Log.d("Guides", "Guide opened: guideId=" + article.id);
    startActivity(new Intent(requireContext(), GuideArticleActivity.class)
        .putExtra(GuideArticleActivity.EXTRA_GUIDE_ID, article.id));
  }

  private void setupHero()
  {
    final List<GuideArticles.Article> featured = mRepository.featured();
    if (featured.isEmpty()) { mRoot.findViewById(R.id.guide_hero).setVisibility(View.GONE); return; }
    mSlide = Math.max(0, Math.min(mSlide, featured.size() - 1));
    final GuideArticles.Article article = featured.get(mSlide);
    mPhotos.load(mRoot.findViewById(R.id.guide_hero_photo), article.imageAsset);
    ((TextView) mRoot.findViewById(R.id.guide_hero_title)).setText(article.title);
    ((TextView) mRoot.findViewById(R.id.guide_hero_subtitle)).setText(
        getString(GuideCategory.find(article.category).title) + " · "
        + getString(R.string.g_minutes, article.readingTime, getString(R.string.g_level)));
    final LinearLayout indicators = mRoot.findViewById(R.id.guide_hero_indicators);
    indicators.removeAllViews();
    for (int i = 0; i < featured.size(); i++)
    {
      final int index = i;
      final TextView indicator = new TextView(requireContext());
      final int size = Math.round(40 * getResources().getDisplayMetrics().density);
      indicator.setLayoutParams(new LinearLayout.LayoutParams(size, size));
      indicator.setText("●"); indicator.setGravity(android.view.Gravity.CENTER);
      indicator.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),
          i == mSlide ? R.color.areamap_green : R.color.areamap_text_muted));
      indicator.setContentDescription(getString(R.string.g_slide, i + 1, featured.size()));
      indicator.setOnClickListener(v -> { mSlide = index; setupHero(); });
      indicators.addView(indicator);
    }

  }

  private void setupNavigation()
  {
    final int[] ids = {R.id.areamap_nav_search, R.id.areamap_nav_route, R.id.areamap_nav_trip,
                       R.id.areamap_nav_guides, R.id.areamap_nav_profile};
    for (int id : ids)
    {
      final LinearLayout item = mRoot.findViewById(id);
      final int color = androidx.core.content.ContextCompat.getColor(requireContext(),
          id == R.id.areamap_nav_guides ? R.color.areamap_green : R.color.areamap_text_secondary);
      ((ImageView) item.getChildAt(0)).setColorFilter(color);
      ((TextView) item.getChildAt(1)).setTextColor(color);
      item.getChildAt(2).setVisibility(id == R.id.areamap_nav_guides ? View.VISIBLE : View.INVISIBLE);
      item.setSelected(id == R.id.areamap_nav_guides);
    }
    mRoot.findViewById(R.id.areamap_nav_search).setOnClickListener(v -> returnToMap("search"));
    mRoot.findViewById(R.id.areamap_nav_route).setOnClickListener(v -> returnToMap("route"));
    mRoot.findViewById(R.id.areamap_nav_trip).setOnClickListener(v -> returnToMap("trip"));
    mRoot.findViewById(R.id.areamap_nav_guides).setOnClickListener(v -> reset());
    mRoot.findViewById(R.id.areamap_nav_profile).setOnClickListener(v -> returnToMap("profile"));
  }
  private void returnToMap(String action)
  {
    startActivity(new Intent(requireContext(), MwmActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MwmActivity.EXTRA_GUIDES_ACTION, action));
    requireActivity().finish();
  }
  @Override public void onResume() { super.onResume(); render(); }
  @Override public void onSaveInstanceState(@NonNull Bundle state)
  {
    super.onSaveInstanceState(state); state.putString("category", mCategory);
    state.putString("listing", mListing); state.putBoolean("favorites", mFavoritesOnly); state.putInt("slide", mSlide);
  }
  @Override public void onDestroyView()
  {
    mRoot = null; mSearch = null; mPopular = null; mRoute = null; mPhotos = null;
    super.onDestroyView();
  }
}
