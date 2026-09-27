package app.organicmaps.search;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds offline-friendly fallback queries while keeping Organic Maps as the actual search engine. */
final class SearchQueryVariants
{
  private static final Map<String, String[]> ALIASES = new LinkedHashMap<>();
  private static final Map<String, String> TYPE_TO_ENGLISH = new LinkedHashMap<>();
  private static final Set<String> GENERIC_TYPES = new LinkedHashSet<>();

  static
  {
    // Common local spellings/abbreviations are aliases only, never hard-coded coordinates.
    alias(new String[] {"бао", "большое алматинское озеро", "big almaty lake", "үлкен алматы көлі"},
          "Большое Алматинское озеро", "Big Almaty Lake", "Үлкен Алматы көлі");
    alias(new String[] {"фурмановка", "пик фурманова", "furmanov peak", "furmanovka"},
          "Пик Фурманова", "Furmanov Peak", "Furmanovka");
    alias(new String[] {"шымбулак", "шымбұлақ", "чимбулак", "shymbulak"},
          "Шымбұлақ", "Шымбулак", "Чимбулак", "Shymbulak");
    alias(new String[] {"медео", "медеу", "medeu"}, "Медеу", "Медео", "Medeu");
    alias(new String[] {"кимасар", "kimasar"}, "Кимасар", "Kimasar");
    alias(new String[] {"кок жайлау", "кокжайлау", "көк жайлау", "көкжайлау", "kok zhailau", "kok-zhailau"},
          "Көкжайлау", "Кок-Жайлау", "Kok Zhailau");
    alias(new String[] {"бутаковка", "butakovka"}, "Бутаковка", "Butakovka");

    type("пик", "peak");
    type("вершина", "peak");
    type("гора", "mountain");
    type("озеро", "lake");
    type("ущелье", "gorge");
    type("каньон", "canyon");
    type("водопад", "waterfall");
    type("перевал", "pass");
    type("ледник", "glacier");
    type("тропа", "trail");
    type("маршрут", "trail");
    type("шың", "peak");
    type("шыңы", "peak");
    type("тау", "mountain");
    type("көлі", "lake");
    type("шатқал", "gorge");
    type("шатқалы", "gorge");
    type("сарқырама", "waterfall");
    type("асу", "pass");
    type("асуы", "pass");
    type("мұздық", "glacier");
    type("соқпақ", "trail");

    for (String english : new String[] {"peak", "summit", "mount", "mountain", "lake", "gorge", "canyon",
                                         "waterfall", "pass", "glacier", "trail"})
      GENERIC_TYPES.add(english);
  }

  private SearchQueryVariants() {}

  private static void alias(@NonNull String[] keys, @NonNull String... variants)
  {
    for (String key : keys)
      ALIASES.put(normalize(key), variants);
  }

  private static void type(@NonNull String source, @NonNull String english)
  {
    GENERIC_TYPES.add(source);
    TYPE_TO_ENGLISH.put(source, english);
  }

  @NonNull
  static List<String> build(@NonNull String query)
  {
    final LinkedHashSet<String> variants = new LinkedHashSet<>();
    final String clean = clean(query);
    if (clean.isEmpty())
      return new ArrayList<>();

    add(variants, clean);

    final String[] aliases = ALIASES.get(normalize(clean));
    if (aliases != null)
      for (String alias : aliases)
        add(variants, alias);

    add(variants, stripGenericTypes(clean));
    add(variants, moveLeadingTypeToEnd(clean));

    final ArrayList<String> snapshot = new ArrayList<>(variants);
    for (String variant : snapshot)
    {
      add(variants, translateGenericTypes(variant));
      add(variants, transliterate(variant));
      add(variants, transliterate(translateGenericTypes(variant)));
    }
    return new ArrayList<>(variants);
  }

  static boolean hasAlias(@NonNull String query)
  {
    return ALIASES.containsKey(normalize(query));
  }

  static boolean hasGenericType(@NonNull String query)
  {
    for (String token : tokens(query))
      if (GENERIC_TYPES.contains(token))
        return true;
    return false;
  }

  @NonNull
  static List<String> significantTokens(@NonNull String query)
  {
    final ArrayList<String> result = new ArrayList<>();
    for (String token : tokens(query))
      if (token.length() >= 2 && !GENERIC_TYPES.contains(token))
        result.add(token);
    return result;
  }

  @NonNull
  static String normalize(@NonNull String value)
  {
    return clean(value).toLowerCase(Locale.ROOT).replace('ё', 'е');
  }

  private static void add(@NonNull LinkedHashSet<String> variants, @NonNull String value)
  {
    final String clean = clean(value);
    if (!clean.isEmpty())
      variants.add(clean);
  }

  @NonNull
  private static String stripGenericTypes(@NonNull String query)
  {
    final StringBuilder result = new StringBuilder();
    for (String raw : query.split("\\s+"))
    {
      if (GENERIC_TYPES.contains(normalize(raw)))
        continue;
      if (result.length() > 0)
        result.append(' ');
      result.append(raw);
    }
    return result.toString();
  }

  @NonNull
  private static String moveLeadingTypeToEnd(@NonNull String query)
  {
    final String[] words = query.split("\\s+");
    if (words.length < 2 || !GENERIC_TYPES.contains(normalize(words[0])))
      return query;

    final StringBuilder result = new StringBuilder();
    for (int i = 1; i < words.length; i++)
    {
      if (result.length() > 0)
        result.append(' ');
      result.append(words[i]);
    }
    result.append(' ').append(words[0]);
    return result.toString();
  }

  @NonNull
  private static String translateGenericTypes(@NonNull String query)
  {
    final StringBuilder result = new StringBuilder();
    for (String raw : query.split("\\s+"))
    {
      final String replacement = TYPE_TO_ENGLISH.get(normalize(raw));
      if (result.length() > 0)
        result.append(' ');
      result.append(replacement != null ? replacement : raw);
    }
    return result.toString();
  }

  @NonNull
  private static List<String> tokens(@NonNull String value)
  {
    final String normalized = normalize(value).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    if (normalized.isEmpty())
      return new ArrayList<>();
    return Arrays.asList(normalized.split("\\s+"));
  }

  @NonNull
  private static String clean(@NonNull String value)
  {
    return value.trim().replaceAll("\\s+", " ");
  }

  @NonNull
  private static String transliterate(@NonNull String value)
  {
    final StringBuilder out = new StringBuilder(value.length() * 2);
    for (int i = 0; i < value.length(); i++)
    {
      final char c = value.charAt(i);
      final boolean upper = Character.isUpperCase(c);
      final String lower = switch (Character.toLowerCase(c))
      {
        case 'а' -> "a"; case 'б' -> "b"; case 'в' -> "v"; case 'г' -> "g";
        case 'д' -> "d"; case 'е', 'э' -> "e"; case 'ё' -> "yo"; case 'ж' -> "zh";
        case 'з' -> "z"; case 'и', 'і' -> "i"; case 'й' -> "y"; case 'к', 'қ' -> "k";
        case 'л' -> "l"; case 'м' -> "m"; case 'н' -> "n"; case 'ң' -> "ng";
        case 'о' -> "o"; case 'ө' -> "o"; case 'п' -> "p"; case 'р' -> "r";
        case 'с' -> "s"; case 'т' -> "t"; case 'у', 'ұ' -> "u"; case 'ү' -> "u";
        case 'ф' -> "f"; case 'х', 'һ' -> "h"; case 'ц' -> "ts"; case 'ч' -> "ch";
        case 'ш' -> "sh"; case 'щ' -> "sch"; case 'ы' -> "y"; case 'ь', 'ъ' -> "";
        case 'ю' -> "yu"; case 'я' -> "ya"; case 'ә' -> "a";
        default -> null;
      };
      if (lower == null)
      {
        out.append(c);
        continue;
      }
      if (upper && !lower.isEmpty())
        out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
      else
        out.append(lower);
    }
    return out.toString();
  }
}
