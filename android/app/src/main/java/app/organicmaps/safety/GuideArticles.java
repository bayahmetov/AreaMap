package app.organicmaps.safety;

import android.content.Context;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuideArticles
{
  public static final class Article
  {
    public final String title;
    public final String tags;
    public final String body;
    public final int imageResId;
    public final String id;
    public final String category;
    public final String imageAsset;
    public final int readingTime;
    public final String[] routeTags;
    // No real review dataset exists. UI explicitly marks its rating badge as demo.
    public final double rating = 4.8;

    Article(String id, String title, String tags, String body)
    {
      this.title = title;
      this.tags = tags;
      this.body = body;
      this.imageResId = heroFor(tags);
      this.id = id;
      this.category = tags.contains("storm") || tags.contains("гроза") ? "weather"
          : tags.contains("navigation") || tags.contains("навигация") ? "lost" : "survival";
      this.imageAsset = "areamap/guides/guide_" + (category.equals("weather") ? "weather_storm"
          : category.equals("lost") ? "no_signal" : "hero_almaty") + ".webp";
      this.readingTime = Math.max(1, (body.length() + 899) / 900);
      this.routeTags = new String[] {"hiking"};
    }

    Article(String id, String category, String image, String title, String body, int minutes)
    {
      this.id = id; this.category = category; this.title = title; this.body = body;
      this.tags = category; this.imageAsset = "areamap/guides/guide_" + image + ".webp";
      this.imageResId = R.drawable.guide_hero_mountain;
      this.readingTime = Math.max(1, (body.length() + 899) / 900); this.routeTags = new String[] {"hiking"};
    }

    public boolean matches(@NonNull String query)
    {
      final String q = query.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty())
        return true;
      return (title + " " + tags + " " + body).toLowerCase(Locale.ROOT).contains(q);
    }
  }

  private GuideArticles() {}

  @NonNull
  public static List<Article> all(@NonNull Context context)
  {
    final Locale locale = context.getResources().getConfiguration().getLocales().get(0);
    final List<Article> result = "ru".equals(locale.getLanguage()) ? russian() : english();
    addNew(context, result);
    return result;
  }

  @NonNull
  public static List<Article> all()
  {
    final boolean ru = Locale.getDefault().getLanguage().equals("ru");
    return ru ? russian() : english();
  }

  private static void addNew(Context context, List<Article> result)
  {
    final String[] ids = {"route", "weather", "gear", "signal", "water", "wildlife", "plants"};
    final String[] categories = {"route", "weather", "weather", "lost", "water", "wildlife", "plants"};
    final String[] images = {"kok_zhailau", "weather_storm", "hero_almaty", "no_signal",
                            "butakovsky_waterfall", "wildlife_bear", "plants"};
    final int[] titles = {R.string.g_a_route, R.string.g_a_weather, R.string.g_a_gear, R.string.g_a_signal,
                          R.string.g_a_water, R.string.g_a_wildlife, R.string.g_a_plants};
    final int[] bodies = {R.string.g_b_route, R.string.g_b_weather, R.string.g_b_gear, R.string.g_b_signal,
                          R.string.g_b_water, R.string.g_b_wildlife, R.string.g_b_plants};
    for (int i = 0; i < ids.length; i++)
      result.add(new Article(ids[i], categories[i], images[i], context.getString(titles[i]),
                             context.getString(bodies[i]), 3));
  }

  private static int heroFor(@NonNull String tags)
  {
    final String normalized = tags.toLowerCase(Locale.ROOT);
    if (normalized.contains("cold") || normalized.contains("hypothermia")
        || normalized.contains("холод") || normalized.contains("переох"))
      return R.drawable.guide_hero_cold;
    if (normalized.contains("storm") || normalized.contains("lightning")
        || normalized.contains("гроза") || normalized.contains("молния"))
      return R.drawable.guide_hero_storm;
    if (normalized.contains("first aid") || normalized.contains("bleeding")
        || normalized.contains("fracture") || normalized.contains("head")
        || normalized.contains("cpr") || normalized.contains("первая помощь")
        || normalized.contains("кров") || normalized.contains("перелом")
        || normalized.contains("голова") || normalized.contains("слр"))
      return R.drawable.guide_hero_first_aid;
    return R.drawable.guide_hero_mountain;
  }

  @NonNull
  private static List<Article> russian()
  {
    final List<Article> a = new ArrayList<>();

    a.add(new Article("legacy_0", "Сильное кровотечение", "первая помощь кровь рана",
        "1. Убедитесь, что место безопасно.\n\n"
      + "2. При возможности вызовите 112.\n\n"
      + "3. Плотно прижмите рану чистой тканью, бинтом или одеждой и не отпускайте давление. "
      + "Если материал промок, не снимайте первый слой — добавляйте сверху.\n\n"
      + "4. При массивном кровотечении из руки или ноги, которое не удаётся остановить прямым давлением, "
      + "используйте штатный турникет, если умеете это делать. Зафиксируйте время наложения.\n\n"
      + "5. Уложите человека, сохраняйте тепло, не давайте алкоголь. Следите за сознанием и дыханием.\n\n"
      + "6. Не вытаскивайте глубоко застрявшие предметы из раны — фиксируйте их вокруг.\n\n"
      + "Источник: принципы первой помощи American Red Cross."));

    a.add(new Article("legacy_1", "Перелом, вывих или сильное растяжение", "первая помощь перелом вывих сустав",
        "1. Прекратите нагрузку. Если есть деформация, сильная боль, невозможность опоры или открытая рана — "
      + "считайте травму серьёзной.\n\n"
      + "2. Не пытайтесь вправлять кость или сустав. Зафиксируйте конечность в том положении, в котором она находится, "
      + "подручной шиной только если это не усиливает боль и не ухудшает кровообращение.\n\n"
      + "3. При открытой ране сначала контролируйте кровотечение и прикройте её чистым материалом.\n\n"
      + "4. Холод через ткань можно прикладывать короткими интервалами, если это доступно.\n\n"
      + "5. При онемении, бледности/синюшности конечности, сильной деформации, травме бедра, таза или позвоночника "
      + "нужна срочная эвакуация."));

    a.add(new Article("legacy_2", "Травма головы", "первая помощь голова сотрясение сознание",
        "Опасные признаки: потеря сознания, повторная рвота, нарастающая головная боль, спутанность, судороги, "
      + "слабость в руке/ноге, разные зрачки, кровь или прозрачная жидкость из уха/носа.\n\n"
      + "1. Прекратите маршрут и исключите повторный удар.\n\n"
      + "2. При подозрении на травму шеи не двигайте человека без необходимости.\n\n"
      + "3. Следите за дыханием и сознанием. При ухудшении — 112 и эвакуация.\n\n"
      + "4. Не отправляйте человека продолжать маршрут одного, даже если ему стало лучше."));

    a.add(new Article("legacy_3", "Переохлаждение", "первая помощь холод гипотермия",
        "Признаки: сильная дрожь, затем её исчезновение, неуклюжесть, сонливость, спутанность речи и поведения.\n\n"
      + "1. Уберите человека от ветра и влаги, снимите мокрое и замените сухим, утеплите голову и корпус.\n\n"
      + "2. Согревайте постепенно, в первую очередь грудь и туловище. Не растирайте сильно холодные конечности.\n\n"
      + "3. Если человек полностью в сознании и может глотать — дайте тёплое сладкое безалкогольное питьё.\n\n"
      + "4. При спутанности, слабом дыхании, отсутствии дрожи или невозможности идти — срочная эвакуация.\n\n"
      + "Источник: CDC, рекомендации по гипотермии."));

    a.add(new Article("legacy_4", "Перегрев и тепловой удар", "первая помощь жара перегрев обезвоживание",
        "Тепловое истощение: слабость, головокружение, тошнота, потливость. Тепловой удар: спутанность сознания, "
      + "нарушение координации, очень горячая кожа — это неотложное состояние.\n\n"
      + "1. Перенесите человека в тень, снимите лишние слои, охлаждайте водой и обдувом.\n\n"
      + "2. При тепловом истощении и ясном сознании давайте пить небольшими порциями.\n\n"
      + "3. При спутанности сознания не заставляйте пить; максимально быстро охлаждайте и вызывайте 112."));

    a.add(new Article("legacy_5", "Нет сознания / человек не дышит нормально", "первая помощь слр cpr сердце дыхание",
        "1. Проверьте реакцию и дыхание. Редкие судорожные вдохи не считаются нормальным дыханием.\n\n"
      + "2. Попросите кого-то вызвать 112 и принести AED, если он доступен.\n\n"
      + "3. Если взрослый не дышит нормально — начните нажатия на центр грудной клетки: быстро и сильно, "
      + "примерно 100–120 в минуту, с полным расправлением грудной клетки между нажатиями.\n\n"
      + "4. Если обучены искусственному дыханию — используйте стандартный цикл 30 нажатий / 2 вдоха. "
      + "Если нет — непрерывные нажатия лучше бездействия.\n\n"
      + "5. Продолжайте до появления нормального дыхания, прибытия помощи или физической невозможности продолжать.\n\n"
      + "Источник: American Red Cross."));

    a.add(new Article("legacy_6", "Высотная болезнь", "высота горная болезнь ams hace hape",
        "Ранние симптомы после набора высоты: головная боль плюс тошнота, головокружение, усталость или потеря аппетита.\n\n"
      + "Главное правило: не продолжайте набирать высоту при симптомах.\n\n"
      + "Если состояние ухудшается на той же высоте — спускайтесь. Даже снижение примерно на 300 м часто заметно помогает.\n\n"
      + "Спутанность сознания, шаткость походки, выраженная одышка в покое, кашель с ухудшением дыхания — признаки "
      + "опасных осложнений; нужен срочный спуск и медицинская помощь.\n\n"
      + "Источник: CDC Yellow Book 2026."));

    a.add(new Article("legacy_7", "Если потерялись", "навигация заблудился маршрут",
        "1. Остановитесь. Не пытайтесь «срезать» по незнакомому склону.\n\n"
      + "2. Сохраните текущую точку и сравните её с треком, последним известным участком и рельефом.\n\n"
      + "3. Если точно знаете безопасный путь назад — возвращайтесь по нему. Если нет, оставайтесь в заметном и защищённом месте.\n\n"
      + "4. Экономьте батарею: уменьшите яркость, включите энергосбережение, не выключайте телефон полностью.\n\n"
      + "5. Подготовьте SOS-карточку с координатами до появления связи."));

    a.add(new Article("legacy_8", "Гроза в горах", "гроза молния погода",
        "Уходите с вершины, гребня, металлических конструкций и одиночных высоких объектов. Не стойте в воде и у края обрыва. "
      + "Если группа большая — немного рассредоточьтесь, чтобы один удар не затронул всех. Не прячьтесь под одиночным деревом."));

    return a;
  }

  @NonNull
  private static List<Article> english()
  {
    final List<Article> a = new ArrayList<>();
    a.add(new Article("legacy_0", "Severe bleeding", "first aid bleeding wound",
        "Apply firm continuous direct pressure. Add more material on top if it soaks through. For life-threatening limb "
      + "bleeding not controlled by pressure, use a commercial tourniquet if trained and record the application time. "
      + "Keep the person warm and seek emergency help. Do not remove deeply embedded objects. Source: American Red Cross."));
    a.add(new Article("legacy_1", "Fracture, dislocation or severe sprain", "first aid fracture joint",
        "Stop loading the limb. Do not straighten or relocate a deformed joint or bone. Support it in the position found. "
      + "Control open bleeding first. Numbness, loss of circulation, major deformity, pelvis/femur/spine injury require urgent evacuation."));
    a.add(new Article("legacy_2", "Head injury", "first aid concussion head",
        "Stop the activity. Red flags include loss of consciousness, repeated vomiting, worsening headache, confusion, seizure, "
      + "weakness or unequal pupils. Avoid unnecessary neck movement and seek urgent care if any red flag appears."));
    a.add(new Article("legacy_3", "Hypothermia", "first aid cold hypothermia",
        "Shelter from wind and moisture, replace wet clothing, insulate the torso and head, and warm gradually. Give warm sweet "
      + "non-alcoholic drinks only if fully alert and able to swallow. Confusion, absent shivering or inability to walk require evacuation."));
    a.add(new Article("legacy_4", "Heat illness", "first aid heat dehydration",
        "Move to shade, remove excess layers and cool actively. Give small drinks only when the person is fully alert. Confusion or "
      + "loss of coordination suggests heat stroke: cool rapidly and call emergency services."));
    a.add(new Article("legacy_5", "Unresponsive / not breathing normally", "first aid cpr cardiac arrest",
        "Check responsiveness and normal breathing. Call emergency services/AED. Start chest compressions in the center of the chest "
      + "at about 100–120/min with full recoil. If trained, use 30 compressions to 2 breaths. Source: American Red Cross."));
    a.add(new Article("legacy_6", "Altitude illness", "altitude AMS HACE HAPE",
        "Do not ascend further when symptoms develop. If symptoms worsen at the same altitude, descend; about 300 m can bring rapid "
      + "improvement in acute mountain sickness. Confusion, ataxia or breathlessness at rest require urgent descent and medical care. "
      + "Source: CDC Yellow Book 2026."));
    a.add(new Article("legacy_7", "If you are lost", "navigation lost route",
        "Stop instead of moving blindly. Save your current position and compare it with the track and terrain. Return only on a route "
      + "you know is safe; otherwise stay visible and sheltered. Conserve battery and prepare the SOS card before coverage returns."));
    a.add(new Article("legacy_8", "Thunderstorm in the mountains", "storm lightning weather",
        "Leave summits and exposed ridges, avoid isolated tall objects, metal structures and water. Spread a group out slightly so one "
      + "strike does not affect everyone. Do not shelter under a lone tree."));
    return a;
  }
}
