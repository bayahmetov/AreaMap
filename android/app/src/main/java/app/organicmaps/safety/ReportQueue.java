package app.organicmaps.safety;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Durable, immutable message snapshots. No network or Android dependencies. */
public final class ReportQueue
{
  public static final String PENDING = "pending";
  public static final String SENT = "sent";
  public static final String BLOCKED = "blocked";

  public static final class Entry
  {
    public final String id;
    public final String kind;
    public final String scope;
    public final String body;
    public final long createdAt;
    public final String state;

    Entry(String id, String kind, String scope, String body, long createdAt, String state)
    {
      this.id = id;
      this.kind = kind;
      this.scope = scope;
      this.body = body;
      this.createdAt = createdAt;
      this.state = state;
    }
  }

  private final File mDirectory;

  public ReportQueue(File directory)
  {
    mDirectory = directory;
  }

  public synchronized Entry enqueue(String kind, String scope, String body, long now) throws IOException
  {
    final List<Entry> entries = entries();
    for (Entry entry : entries)
    {
      if (!kind.equals(entry.kind) || !scope.equals(entry.scope))
        continue;
      // Start/finish belong to one trip, regardless of app launches or repeated taps.
      if ("start".equals(kind) || "finish".equals(kind))
        return entry;
      final long cooldown = "break".equals(kind) ? 1000L : 60000L;
      if (("sos".equals(kind) && !SENT.equals(entry.state))
          || (now >= entry.createdAt && now - entry.createdAt < cooldown))
        return entry;
    }
    final long createdAt = entries.isEmpty() ? now : Math.max(now, entries.get(entries.size() - 1).createdAt + 1);
    final Entry entry = new Entry(UUID.randomUUID().toString(), kind, scope, body, createdAt, PENDING);
    write(entry);
    return entry;
  }

  public synchronized List<Entry> entries() throws IOException
  {
    final List<Entry> entries = new ArrayList<>();
    final File[] files = mDirectory.listFiles((dir, name) -> name.endsWith(".event"));
    if (files == null)
      return entries;
    for (File file : files)
      entries.add(read(file));
    entries.sort(Comparator.comparingLong((Entry entry) -> entry.createdAt).thenComparing(entry -> entry.id));
    return entries;
  }

  public synchronized Entry get(String id) throws IOException
  {
    final File file = file(id);
    return file.exists() ? read(file) : null;
  }

  public synchronized void setState(String id, String state) throws IOException
  {
    final Entry entry = get(id);
    if (entry != null && !SENT.equals(entry.state))
      write(new Entry(entry.id, entry.kind, entry.scope, entry.body, entry.createdAt, state));
  }

  public synchronized boolean waitsForEarlier(Entry entry) throws IOException
  {
    // An emergency must never wait behind an ordinary report.
    if ("sos".equals(entry.kind))
      return false;
    for (Entry earlier : entries())
    {
      if (earlier.id.equals(entry.id))
        return false;
      if (entry.scope.equals(earlier.scope) && !"sos".equals(earlier.kind) && !SENT.equals(earlier.state))
        return true;
    }
    return false;
  }

  private File file(String id)
  {
    if (!id.matches("[a-zA-Z0-9-]+"))
      throw new IllegalArgumentException("Invalid report ID");
    return new File(mDirectory, id + ".event");
  }

  private Entry read(File file) throws IOException
  {
    try (DataInputStream input = new DataInputStream(new FileInputStream(file)))
    {
      if (input.readInt() != 1)
        throw new IOException("Unsupported report version");
      return new Entry(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readLong(),
                       input.readUTF());
    }
  }

  private void write(Entry entry) throws IOException
  {
    if (!mDirectory.isDirectory() && !mDirectory.mkdirs())
      throw new IOException("Cannot create report queue");
    final File target = file(entry.id);
    final File temporary = new File(mDirectory, entry.id + ".tmp");
    try (FileOutputStream stream = new FileOutputStream(temporary);
         DataOutputStream output = new DataOutputStream(stream))
    {
      output.writeInt(1);
      output.writeUTF(entry.id);
      output.writeUTF(entry.kind);
      output.writeUTF(entry.scope);
      output.writeUTF(entry.body);
      output.writeLong(entry.createdAt);
      output.writeUTF(entry.state);
      output.flush();
      stream.getFD().sync();
    }
    // Same-directory rename keeps the preceding valid snapshot intact until replacement.
    if (!temporary.renameTo(target))
      throw new IOException("Cannot commit report queue entry");
  }
}
