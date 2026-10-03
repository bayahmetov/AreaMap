package app.organicmaps.safety;

import android.content.ContentResolver;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Xml;
import androidx.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;

/** Foreground track guidance. It deliberately does not replace GPX geometry with a road route. */
public final class GpxNavigation
{
  private static final String ACTIVE_FILE = "areamap_active_gpx.bin";
  private static final int MAX_POINTS = 20000;

  public static GpxNavigation current;
  public final GpxTrack track;
  private long mStarted;
  private double mPrevious = -1;
  private long mLastFix;
  private double mFirstProgress = -1;
  public double remaining;
  public double offset;
  public int seconds;
  public boolean hasFix;
  public boolean arrived;

  public GpxNavigation(GpxTrack track)
  {
    this.track = track;
    remaining = track.length;
  }

  public static synchronized boolean activate(Context context, GpxTrack track)
  {
    if (!persist(context.getApplicationContext(), track))
      return false;
    context.getSharedPreferences("areamap_gpx_progress", Context.MODE_PRIVATE).edit().clear().commit();
    current = new GpxNavigation(track);
    return true;
  }

  @Nullable
  public static synchronized GpxNavigation restore(Context context)
  {
    if (current != null)
      return current;

    final File file = new File(context.getApplicationContext().getFilesDir(), ACTIVE_FILE);
    if (!file.isFile() && !new File(file.getPath() + ".bak").isFile())
      return null;

    try (DataInputStream input = new DataInputStream(new AtomicFile(file).openRead()))
    {
      final int count = input.readInt();
      if (count < 2 || count > MAX_POINTS)
        throw new IllegalArgumentException("Invalid saved GPX point count");

      final double[][] points = new double[count][3];
      for (int i = 0; i < count; i++)
      {
        points[i][0] = input.readDouble();
        points[i][1] = input.readDouble();
        points[i][2] = input.readDouble();
      }
      current = new GpxNavigation(new GpxTrack(points));
      final android.content.SharedPreferences prefs =
          context.getSharedPreferences("areamap_gpx_progress", Context.MODE_PRIVATE);
      current.mPrevious = Double.longBitsToDouble(prefs.getLong("previous", Double.doubleToLongBits(-1)));
      current.mFirstProgress = Double.longBitsToDouble(prefs.getLong("first", Double.doubleToLongBits(-1)));
      current.remaining =
          Double.longBitsToDouble(prefs.getLong("remaining", Double.doubleToLongBits(current.track.length)));
      current.seconds = prefs.getInt("seconds", 0);
      current.arrived = prefs.getBoolean("arrived", false);
      final long wallNow = System.currentTimeMillis();
      final long elapsedNow = SystemClock.elapsedRealtime();
      final long startWall = prefs.getLong("started_wall", wallNow);
      current.mStarted = elapsedNow - Math.max(0, wallNow - startWall);
      final long fixWall = prefs.getLong("fix_wall", 0);
      current.mLastFix = fixWall == 0 ? 0 : elapsedNow - Math.max(0, wallNow - fixWall);
      // A new process must obtain a fresh position before presenting live guidance.
      current.hasFix = false;
      return current;
    }
    catch (Exception ignored)
    {
      new AtomicFile(file).delete();
      return null;
    }
  }

  public static synchronized void stop(Context context)
  {
    current = null;
    new AtomicFile(new File(context.getApplicationContext().getFilesDir(), ACTIVE_FILE)).delete();
    context.getSharedPreferences("areamap_gpx_progress", Context.MODE_PRIVATE).edit().clear().commit();
  }

  public synchronized void saveProgress(Context context)
  {
    final long elapsedNow = SystemClock.elapsedRealtime();
    final long wallNow = System.currentTimeMillis();
    context.getSharedPreferences("areamap_gpx_progress", Context.MODE_PRIVATE)
        .edit()
        .putLong("previous", Double.doubleToLongBits(mPrevious))
        .putLong("first", Double.doubleToLongBits(mFirstProgress))
        .putLong("remaining", Double.doubleToLongBits(remaining))
        .putInt("seconds", seconds)
        .putBoolean("arrived", arrived)
        .putLong("started_wall", mFirstProgress < 0 ? wallNow : wallNow - (elapsedNow - mStarted))
        .putLong("fix_wall", mLastFix == 0 ? 0 : wallNow - (elapsedNow - mLastFix))
        .apply();
  }

  private static boolean persist(Context context, GpxTrack track)
  {
    final AtomicFile file = new AtomicFile(new File(context.getFilesDir(), ACTIVE_FILE));
    FileOutputStream stream = null;
    try
    {
      stream = file.startWrite();
      final DataOutputStream output = new DataOutputStream(stream);
      output.writeInt(track.points.length);
      for (double[] point : track.points)
      {
        output.writeDouble(point[0]);
        output.writeDouble(point[1]);
        output.writeDouble(point[2]);
      }
      output.flush();
      file.finishWrite(stream);
      return true;
    }
    catch (Exception ignored)
    {
      if (stream != null)
        file.failWrite(stream);
      return false;
    }
  }

  public synchronized void update(Location location)
  {
    final long now = SystemClock.elapsedRealtime();
    hasFix = location != null && location.hasAccuracy() && location.getAccuracy() <= 50
        && now - location.getElapsedRealtimeNanos() / 1000000 < 120000;
    if (!hasFix)
      return;
    final double window = mLastFix == 0 ? Double.POSITIVE_INFINITY
        : Math.max(200, (now - mLastFix) / 1000.0 * 4);
    final GpxTrack.Position position = track.locate(location.getLatitude(), location.getLongitude(), mPrevious, window);
    offset = position.offset;
    if (offset > Math.max(50, location.getAccuracy() * 2))
      return;
    mPrevious = position.progress;
    mLastFix = now;
    if (mFirstProgress < 0)
    {
      mFirstProgress = position.progress;
      mStarted = now;
    }
    remaining = Math.max(0, track.length - position.progress);
    final double walked = position.progress - mFirstProgress;
    final int planned = HikingTiming.conservativeSeconds(0, track.length, track.ascent);
    final double elapsed = (now - mStarted) / 1000.0;
    final double pace = walked > 100 && elapsed > 60 ? elapsed / walked : planned / track.length;
    seconds = (int) Math.min(Integer.MAX_VALUE, remaining * pace);
    arrived = remaining < 30 && offset < 30;
  }

  /** Multi-segment files remain importable; following requires one continuous line. */
  public static GpxTrack read(ContentResolver resolver, Uri uri)
  {
    try (InputStream input = resolver.openInputStream(uri))
    {
      if (input == null)
        return null;
      final XmlPullParser parser = Xml.newPullParser();
      parser.setInput(input, null);
      final ArrayList<double[]> points = new ArrayList<>();
      int segments = 0;
      double[] point = null;
      boolean gpx = false;
      int events = 0;
      for (int event = parser.next(); event != XmlPullParser.END_DOCUMENT; event = parser.next())
      {
        if (++events > 500000)
          return null;
        final String name = parser.getName();
        if (event == XmlPullParser.START_TAG)
        {
          if ("gpx".equals(name))
            gpx = true;
          if ("trkseg".equals(name) || "rte".equals(name))
            if (++segments > 1)
              return null;
          if ("trkpt".equals(name) || "rtept".equals(name))
          {
            if (points.size() >= MAX_POINTS)
              return null;
            point = new double[] {Double.parseDouble(parser.getAttributeValue(null, "lat")),
                                  Double.parseDouble(parser.getAttributeValue(null, "lon")), Double.NaN};
            points.add(point);
          }
          else if ("ele".equals(name) && point != null)
            point[2] = Double.parseDouble(parser.nextText());
        }
        else if (event == XmlPullParser.END_TAG && ("trkpt".equals(name) || "rtept".equals(name)))
          point = null;
      }
      return gpx && points.size() >= 2 ? new GpxTrack(points.toArray(new double[0][])) : null;
    }
    catch (Exception e)
    {
      return null;
    }
  }
}
