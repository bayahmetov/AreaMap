package app.organicmaps.profile;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.bookmarks.BookmarkCategoriesActivity;
import app.organicmaps.bookmarks.BookmarkListActivity;
import app.organicmaps.bookmarks.BookmarksListFragment;
import app.organicmaps.editor.OsmLoginActivity;
import app.organicmaps.editor.ProfileActivity;
import app.organicmaps.safety.GuideListActivity;
import app.organicmaps.safety.TripSafety;
import app.organicmaps.safety.TripSafetyActivity;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.editor.OsmOAuth;
import app.organicmaps.settings.SettingsActivity;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Personal outdoor hub. All identity stays on-device; OSM is an optional integration. */
public class AreaMapProfileFragment extends BaseMwmFragment implements BookmarkManager.BookmarksLoadingListener
{
  private AreaMapProfileRepository mRepository;
  private View mRoot;
  private LinearLayout mContent;
  private String mPage = "profile";
  private Bundle mDraft;
  private boolean mListening;
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener mProfileChanged =
      (preferences, key) -> { if (mRoot != null) render(); };
  private AlertDialog mEditor;
  private EditText mName, mBio, mRegion;
  private ImageView mAvatar;
  private final ExecutorService mImages = Executors.newSingleThreadExecutor();
  private final ActivityResultLauncher<PickVisualMediaRequest> mPicker = registerForActivityResult(
      new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri == null) return;
        final android.content.Context context = requireContext().getApplicationContext();
        final AreaMapProfileRepository repository = new AreaMapProfileRepository(context);
        mImages.execute(() -> {
          boolean success;
          try { repository.importAvatar(uri); success = true; }
          catch (Exception error) { success = false; }
          final boolean saved = success;
          new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (!isAdded() || mRoot == null) return;
            if (saved) { if (mAvatar != null) loadAvatar(mAvatar); }
            else new AlertDialog.Builder(requireContext()).setMessage(R.string.p_avatar_error)
                .setPositiveButton(android.R.string.ok, null).show();
          });
        });
      });

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_areamap_profile, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    mRoot = view;
    mContent = view.findViewById(R.id.profile_content);
    mRepository = new AreaMapProfileRepository(requireContext());
    if (state != null) { mPage = state.getString("page", "profile"); mDraft = state.getBundle("draft"); }
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      final Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
          | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    ViewCompat.requestApplyInsets(view);
    setupNavigation();
    render();
    if (mDraft != null) view.post(this::editProfile);
  }

  private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private int color(int id) { return ContextCompat.getColor(requireContext(), id); }
  private LinearLayout column()
  {
    LinearLayout layout = new LinearLayout(requireContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return layout;
  }
  private LinearLayout row()
  {
    LinearLayout layout = column(); layout.setOrientation(LinearLayout.HORIZONTAL);
    layout.setGravity(Gravity.CENTER_VERTICAL); return layout;
  }
  private GradientDrawable surface()
  {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color(R.color.profile_surface));
    drawable.setCornerRadius(dp(20));
    drawable.setStroke(dp(1), color(R.color.profile_border));
    return drawable;
  }
  private TextView text(String value, int size, boolean bold)
  {
    TextView text = new TextView(requireContext()); text.setText(value); text.setTextSize(size);
    text.setTextColor(color(bold ? R.color.profile_text : R.color.profile_secondary));
    if (bold) text.setTypeface(null, Typeface.BOLD);
    text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    text.setPadding(0, dp(3), 0, dp(3)); return text;
  }
  private TextView action(int title, Runnable click)
  {
    TextView button = text(getString(title), 15, true);
    button.setTextColor(color(R.color.profile_accent)); button.setMinHeight(dp(48));
    button.setGravity(Gravity.CENTER_VERTICAL); button.setOnClickListener(v -> click.run());
    button.setBackground(surface()); button.setPadding(dp(14), dp(8), dp(14), dp(8));
    button.setFocusable(true); return button;
  }
  private ImageView icon(int drawable, int size)
  {
    ImageView image = new ImageView(requireContext()); image.setImageResource(drawable);
    image.setColorFilter(color(R.color.profile_accent));
    image.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return image;
  }
  private void section(View view)
  {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = dp(16); view.setLayoutParams(params); mContent.addView(view);
  }
  private void header()
  {
    LinearLayout header = row();
    header.addView(icon(R.drawable.home_logo, 34));
    TextView brand = text(getString(R.string.home_brand), 23, true);
    brand.setPadding(dp(8), 0, 0, 0);
    brand.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); header.addView(brand);
    for (int i = 0; i < 2; i++)
    {
      final boolean share = i == 0;
      ImageButton button = new ImageButton(requireContext());
      button.setImageResource(share ? R.drawable.ic_share : R.drawable.ic_settings);
      button.setColorFilter(color(R.color.profile_text)); button.setBackground(surface()); button.setPadding(dp(12), dp(12), dp(12), dp(12));
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48)); params.leftMargin = dp(8); button.setLayoutParams(params);
      button.setContentDescription(getString(share ? R.string.p_share : R.string.p_settings));
      button.setOnClickListener(v -> { if (share) share(); else startActivity(new Intent(requireContext(), SettingsActivity.class)); });
      header.addView(button);
    }
    mContent.addView(header);
  }
  private String displayName(AreaMapUserProfile profile)
  { return profile.displayName.isEmpty() ? getString(R.string.p_default_name) : profile.displayName; }
  private void loadAvatar(ImageView image)
  {
    final String path = mRepository.profile().avatarPath;
    final android.graphics.Bitmap bitmap = path.isEmpty() ? null : BitmapFactory.decodeFile(path);
    image.setColorFilter(null);
    if (bitmap == null) { image.setImageResource(R.drawable.home_profile); image.setColorFilter(color(R.color.profile_accent)); }
    else image.setImageBitmap(bitmap);
  }
  private void identity()
  {
    AreaMapUserProfile profile = mRepository.profile();
    LinearLayout identity = row(); identity.setGravity(Gravity.TOP);
    mAvatar = new ImageView(requireContext());
    mAvatar.setLayoutParams(new LinearLayout.LayoutParams(dp(88), dp(88)));
    mAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP); mAvatar.setBackground(surface());
    mAvatar.setOutlineProvider(new ViewOutlineProvider() {
      @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); }
    });
    mAvatar.setClipToOutline(true); mAvatar.setContentDescription(getString(R.string.p_avatar));
    loadAvatar(mAvatar); identity.addView(mAvatar);
    LinearLayout details = column(); details.setPadding(dp(16), 0, 0, 0);
    details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    details.addView(text(displayName(profile), 25, true));
    details.addView(text(profile.bio.isEmpty() ? getString(R.string.p_default_bio) : profile.bio, 15, false));
    details.addView(text(profile.homeRegion.isEmpty() ? getString(R.string.p_default_region) : profile.homeRegion, 14, false));
    details.addView(action(R.string.p_edit, this::editProfile)); identity.addView(details); section(identity);
  }
  private void statistics()
  {
    final AreaMapProfileRepository.Statistics stats = mRepository.statistics();
    int[] values = {stats.peaks, stats.trips, stats.routes, stats.offlineRegions};
    int[] labels = {R.string.p_peaks, R.string.p_trips, R.string.p_routes, R.string.p_offline};
    int[] icons = {R.drawable.home_logo, R.drawable.ic_areamap_hike, R.drawable.ic_manage_route, R.drawable.ic_layers};
    boolean wide = getResources().getConfiguration().screenWidthDp >= 600 && getResources().getConfiguration().fontScale <= 1.3f;
    int columns = wide ? 4 : 2;
    LinearLayout grid = column();
    for (int start = 0; start < 4; start += columns)
    {
      LinearLayout row = row(); row.setGravity(Gravity.TOP);
      for (int j = start; j < start + columns; j++)
      {
        LinearLayout card = column(); card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(surface());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3)); card.setLayoutParams(params);
        card.addView(icon(icons[j], 30)); card.addView(text(Integer.toString(values[j]), 24, true));
        card.addView(text(getString(labels[j]), 14, false)); row.addView(card);
      }
      grid.addView(row);
    }
    section(grid);
  }
  private void menuRow(LinearLayout group, int title, int subtitle, int drawable, Runnable click)
  {
    if (group.getChildCount() > 0)
    {
      View line = new View(requireContext()); line.setBackgroundColor(color(R.color.profile_border));
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
      params.setMargins(dp(16), 0, dp(16), 0); group.addView(line, params);
    }
    LinearLayout item = row(); item.setPadding(dp(16), dp(16), dp(16), dp(16)); item.setMinimumHeight(dp(76));
    item.addView(icon(drawable, 32));
    LinearLayout details = column(); details.setPadding(dp(14), 0, dp(8), 0);
    details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    details.addView(text(getString(title), 18, true)); details.addView(text(getString(subtitle), 14, false)); item.addView(details);
    TextView chevron = text("›", 30, false); chevron.setLayoutParams(new LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.WRAP_CONTENT));
    item.addView(chevron); item.setOnClickListener(v -> click.run()); item.setFocusable(true); group.addView(item);
  }
  private void openPage(String page)
  {
    mPage = page; render(); ((androidx.core.widget.NestedScrollView) mRoot.findViewById(R.id.profile_scroll)).scrollTo(0, 0);
  }
  private void render()
  {
    if (mContent == null) return;
    mContent.removeAllViews(); header();
    if (!mPage.equals("profile")) section(action(R.string.p_back, () -> openPage("profile")));
    switch (mPage)
    {
      case "achievements": achievements(false); break;
      case "history":
        section(text(getString(R.string.p_empty_trips), 24, true));
        section(text(getString(R.string.p_history_note), 15, false));
        section(action(R.string.p_start_hike, () -> returnToMap("route"))); break;
      default:
        identity(); statistics();
        LinearLayout menu = column(); menu.setBackground(surface());
        menuRow(menu, R.string.p_favorites, R.string.p_favorites_desc, R.drawable.ic_bookmarks_on, () -> openBookmarks(false));
        menuRow(menu, R.string.p_my_routes, R.string.p_routes_desc, R.drawable.ic_manage_route, () -> openBookmarks(true));
        menuRow(menu, R.string.p_achievements, R.string.p_achievements_desc, R.drawable.profile_badge_peak, () -> openPage("achievements"));
        menuRow(menu, R.string.p_history, R.string.p_history_desc, R.drawable.profile_badge_hike, () -> openPage("history"));
        menuRow(menu, R.string.p_safety, R.string.p_safety_desc, R.drawable.profile_badge_safety, this::safety);
        section(menu); achievements(true); osm(); break;
    }
  }
  private void openBookmarks(boolean routes)
  {
    int count = 0;
    for (BookmarkCategory category : BookmarkManager.INSTANCE.getCategories())
      count += routes ? category.getTracksCount() : category.size();
    if (count == 0)
      new AlertDialog.Builder(requireContext()).setMessage(routes ? R.string.p_empty_routes : R.string.p_empty_favorites)
          .setPositiveButton(R.string.p_open_lists, (d, w) -> BookmarkCategoriesActivity.start(requireActivity()))
          .setNegativeButton(android.R.string.cancel, null).show();
    else if (!routes) BookmarkCategoriesActivity.start(requireActivity());
    else
    {
      final java.util.List<BookmarkCategory> categories = new java.util.ArrayList<>();
      for (BookmarkCategory category : BookmarkManager.INSTANCE.getCategories())
        if (category.getTracksCount() > 0) categories.add(category);
      final String[] titles = new String[categories.size()];
      for (int i = 0; i < titles.length; i++) titles[i] = categories.get(i).getName();
      new AlertDialog.Builder(requireContext()).setTitle(R.string.p_my_routes).setItems(titles, (d, index) ->
          startActivity(new Intent(requireContext(), BookmarkListActivity.class)
              .putExtra(BookmarksListFragment.EXTRA_CATEGORY, categories.get(index))))
          .setNegativeButton(android.R.string.cancel, null).show();
    }
  }
  private void achievements(boolean preview)
  {
    LinearLayout title = row();
    TextView heading = text(getString(R.string.p_my_achievements), 23, true);
    heading.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); title.addView(heading);
    if (preview)
    {
      TextView all = action(R.string.p_all_achievements, () -> openPage("achievements"));
      all.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
      title.addView(all);
    }
    section(title);
    // Two columns on phones, four on wide screens; natural height respects large fonts.
    int columns = getResources().getConfiguration().screenWidthDp >= 700 ? 4 : 2;
    LinearLayout grid = column(); LinearLayout current = null; int index = 0;
    for (Achievement achievement : mRepository.achievements())
    {
      if (index++ % columns == 0) { current = row(); current.setGravity(Gravity.TOP); grid.addView(current); }
      LinearLayout card = column(); card.setGravity(Gravity.CENTER_HORIZONTAL); card.setPadding(dp(12), dp(16), dp(12), dp(16));
      card.setBackground(surface()); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
      params.setMargins(dp(3), dp(3), dp(3), dp(3)); card.setLayoutParams(params);
      ImageView badge = new ImageView(requireContext()); badge.setImageResource(achievement.icon);
      badge.setLayoutParams(new LinearLayout.LayoutParams(dp(88), dp(88))); badge.setAlpha(achievement.isUnlocked() ? 1f : 0.55f);
      badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); card.addView(badge);
      TextView name = text(getString(achievement.title), 17, true); name.setGravity(Gravity.CENTER); card.addView(name);
      TextView description = text(getString(achievement.description), 13, false); description.setGravity(Gravity.CENTER); card.addView(description);
      String status = achievement.isUnlocked() ? getString(R.string.p_unlocked,
          DateFormat.getDateInstance().format(new Date(achievement.unlockedAt)))
          : getString(R.string.p_locked, achievement.progress, achievement.target);
      TextView progress = text(status, 13, false); progress.setGravity(Gravity.CENTER); card.addView(progress);
      if (preview) { card.setOnClickListener(v -> openPage("achievements")); card.setFocusable(true); }
      current.addView(card);
    }
    section(grid);
    if (!preview) section(text(getString(R.string.p_tracking_note), 14, false));
  }
  private void osm()
  {
    LinearLayout card = column(); card.setBackground(surface()); card.setPadding(dp(16), dp(12), dp(16), dp(12));
    card.addView(text(getString(R.string.p_osm), 18, true));
    card.addView(text(OsmOAuth.isAuthorized() ? getString(R.string.p_osm_connected, OsmOAuth.getUsername())
        : getString(R.string.p_osm_disconnected), 14, false));
    card.setOnClickListener(v -> {
      final Intent account = new Intent(requireContext(), OsmOAuth.isAuthorized() ? ProfileActivity.class : OsmLoginActivity.class);
      if (!OsmOAuth.isAuthorized()) account.putExtra(ProfileActivity.EXTRA_REDIRECT_TO_PROFILE, true);
      startActivity(account);
    });
    card.setFocusable(true); section(card);
  }
  private void share()
  {
    AreaMapUserProfile profile = mRepository.profile(); AreaMapProfileRepository.Statistics stats = mRepository.statistics();
    Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
        getString(R.string.p_share_summary, displayName(profile), profile.bio, profile.homeRegion, stats.routes, stats.offlineRegions));
    startActivity(Intent.createChooser(share, getString(R.string.p_share)));
  }
  private EditText field(LinearLayout parent, int label, String value, int limit, boolean multiline)
  {
    parent.addView(text(getString(label), 14, true));
    EditText input = new EditText(requireContext()); input.setText(value); input.setTextColor(color(R.color.profile_text));
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (multiline ? android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE : android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
    input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(limit)});
    input.setMinHeight(dp(48)); input.setMaxLines(multiline ? 5 : 1); parent.addView(input); return input;
  }
  private void editProfile()
  {
    if (mEditor != null && mEditor.isShowing()) return;
    AreaMapUserProfile profile = mRepository.profile(); LinearLayout fields = column(); fields.setPadding(dp(20), dp(8), dp(20), dp(16));
    mName = field(fields, R.string.p_name, mDraft == null ? profile.displayName : mDraft.getString("name", ""), 80, false);
    mBio = field(fields, R.string.p_bio, mDraft == null ? profile.bio : mDraft.getString("bio", ""), 280, true);
    mRegion = field(fields, R.string.p_region, mDraft == null ? profile.homeRegion : mDraft.getString("region", ""), 120, false);
    fields.addView(action(R.string.p_avatar, () -> {
      mPicker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }));
    ScrollView scroll = new ScrollView(requireContext()); scroll.addView(fields);
    mEditor = new AlertDialog.Builder(requireContext()).setTitle(R.string.p_edit).setView(scroll)
        .setPositiveButton(R.string.p_save, (d, w) -> {
          mRepository.save(mName.getText().toString(), mBio.getText().toString(), mRegion.getText().toString()); render();
        }).setNegativeButton(android.R.string.cancel, null).create();
    mEditor.setOnDismissListener(d -> { mDraft = null; mEditor = null; mName = null; mBio = null; mRegion = null; });
    mEditor.show();
  }
  private void safety()
  {
    final TripSafety safety = TripSafety.get(requireContext()); final TripSafety.Profile profile = safety.getProfile();
    LinearLayout fields = column(); fields.setPadding(dp(20), dp(8), dp(20), dp(16));
    EditText name = field(fields, R.string.p_name, profile.name, 80, false);
    EditText phone = field(fields, R.string.p_phone, profile.phone, 40, false); phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
    EditText contact = field(fields, R.string.p_emergency_name, profile.emergencyName, 80, false);
    EditText contactPhone = field(fields, R.string.p_emergency_phone, profile.emergencyPhone, 40, false);
    contactPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
    fields.addView(action(R.string.p_open_safety, () -> startActivity(new Intent(requireContext(), TripSafetyActivity.class))));
    ScrollView scroll = new ScrollView(requireContext()); scroll.addView(fields);
    new AlertDialog.Builder(requireContext()).setTitle(R.string.p_safety).setView(scroll)
        .setPositiveButton(R.string.p_save, (d, w) -> {
          safety.saveProfile(new TripSafety.Profile(name.getText().toString(), phone.getText().toString(), profile.groupSize,
              contact.getText().toString(), contactPhone.getText().toString())); render();
        }).setNegativeButton(android.R.string.cancel, null).show();
  }
  private void setupNavigation()
  {
    final int[] ids = {R.id.areamap_nav_search, R.id.areamap_nav_route, R.id.areamap_nav_trip,
        R.id.areamap_nav_guides, R.id.areamap_nav_profile};
    final String[] actions = {"search", "route", "trip", "guides", "profile"};
    for (int i = 0; i < ids.length; i++)
    {
      final String destination = actions[i]; LinearLayout item = mRoot.findViewById(ids[i]);
      final boolean selected = ids[i] == R.id.areamap_nav_profile;
      int tint = color(selected ? R.color.profile_accent : R.color.profile_secondary);
      ((ImageView) item.getChildAt(0)).setColorFilter(tint);
      ((TextView) item.getChildAt(1)).setTextColor(tint);
      item.getChildAt(2).setVisibility(selected ? View.VISIBLE : View.INVISIBLE); item.setSelected(selected);
      item.setOnClickListener(v -> {
        if (destination.equals("profile")) openPage("profile");
        else if (destination.equals("guides")) {
          startActivity(new Intent(requireContext(), GuideListActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
          requireActivity().finish();
        }
        else returnToMap(destination);
      });
    }
    mRoot.findViewById(R.id.areamap_bottom_nav_bar).setBackground(surface());
  }
  private void returnToMap(String action)
  {
    startActivity(new Intent(requireContext(), MwmActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MwmActivity.EXTRA_GUIDES_ACTION, action)); requireActivity().finish();
  }
  @Override public boolean onBackPressed()
  { if (mPage.equals("profile")) return false; openPage("profile"); return true; }
  @Override public void onStart()
  { super.onStart(); BookmarkManager.INSTANCE.addLoadingListener(this); mRepository.addListener(mProfileChanged); mListening = true; }
  @Override public void onStop()
  { if (mListening) { BookmarkManager.INSTANCE.removeLoadingListener(this); mRepository.removeListener(mProfileChanged); } mListening = false; super.onStop(); }
  @Override public void onResume() { super.onResume(); render(); }
  @Override public void onBookmarksLoadingFinished() { render(); }
  @Override public void onBookmarksFileImportSuccessful() { render(); }
  @Override public void onSaveInstanceState(@NonNull Bundle state)
  {
    super.onSaveInstanceState(state); state.putString("page", mPage);
    if (mEditor != null && mName != null)
    {
      mDraft = new Bundle(); mDraft.putString("name", mName.getText().toString());
      mDraft.putString("bio", mBio.getText().toString()); mDraft.putString("region", mRegion.getText().toString());
    }
    if (mDraft != null) state.putBundle("draft", mDraft);
  }
  @Override public void onDestroyView()
  {
    if (mEditor != null) { mEditor.setOnDismissListener(null); mEditor.dismiss(); mEditor = null; }
    mName = null; mBio = null; mRegion = null; mAvatar = null; mRoot = null; mContent = null;
    super.onDestroyView();
  }
  @Override public void onDestroy() { mImages.shutdown(); super.onDestroy(); }
}
