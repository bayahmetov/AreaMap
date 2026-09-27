# Выпуск AreaMap на GitHub

Инструкция для ветки `feature/areamap-trip-safety`, проверена по конфигурации
на 27 сентября 2026 года. Публикация релиза и загрузка APK — отдельные действия:
обычный `git push` APK не создаёт.

## 1. Подготовить исходники

В Git Bash, из каталога репозитория:

```bash
git status
git switch feature/areamap-trip-safety
git pull --ff-only origin feature/areamap-trip-safety
git submodule update --init --recursive
git rev-parse HEAD
```

Сохраните свои незакоммиченные изменения до переключения. Если `git status` сообщает
о незавершённом `git am`, сначала разберите его состояние; не накладывайте старый
patch повторно. Сохраните SHA коммита, из которого будете собирать APK.

Для релизной сборки используйте полноценную историю Git: `tools/unix/version.sh`
рассчитывает версию из даты и количества коммитов. Если копия shallow, сначала
выполните `git fetch --unshallow origin`. Проверка: `git rev-parse --is-shallow-repository`.

## 2. Выбрать тестовую или публичную сборку

| Вариант | Назначение | Подпись и идентификатор сейчас |
| --- | --- | --- |
| `fdroidDebug` | Быстро передать команде для проверки | Общий debug-ключ, `app.organicmaps.debug` |
| `fdroidRelease` | Подписанный распространяемый APK | Собственный release-ключ, пока `app.organicmaps` |

Для первого тестового выпуска используйте пометку **Pre-release**. Debug APK можно
раздать для теста, но это не замена подписанному release APK.

Перед публичной самостоятельной сборкой:

- Назначьте уникальный applicationId AreaMap. Сейчас его задаёт `android/build.gradle`.
  Проверьте связанные authorities, deep links, разрешения и шаблон проверки
  package-specific permission в `android/app/build.gradle`.
- Убедитесь, что `AREAMAP_DEMO_KEY` в `android/local.properties` отсутствует либо пуст.
  Непустое значение встраивается в APK. При пустом значении используется передача
  через Telegram-приложение или системный выбор приложения.
- Подготовьте название, значок, контакты поддержки и пользовательское описание
  передачи данных; часть этих параметров пока унаследована от Organic Maps.
- Учтите условия `LICENSE`, `NOTICE`, `DATA_LICENSE.txt` и видимую атрибуцию базы.

## 3. Быстрый APK для команды

В Git Bash:

```bash
cd android
./gradlew :app:assembleFdroidDebug -Parm64
```

В PowerShell: `./gradlew.bat :app:assembleFdroidDebug -Parm64`.
Ищите APK в `android/app/build/outputs/apk/fdroid/debug/` относительно корня репозитория.
ARM64 APK подходит только устройствам с этой архитектурой.

## 4. Подписанный release APK

Откройте каталог `android` в Android Studio. Выберите **Build → Generate Signed
App Bundle or APK → APK**, модуль `app`, затем существующий ключ вашей команды
либо **Create new** для первого выпуска. Выберите вариант **fdroidRelease**,
задайте каталог результата и соберите APK.

Храните keystore, alias и пароли вне репозитория с резервной копией. Следующие
обновления того же приложения должны подписываться тем же ключом.

Для сборки из терминала проект поддерживает `android/app/secure.properties`.
Несмотря на расширение, он подключается как **Groovy script** через `apply from`.
Локальный пример, заполните своими значениями:

```groovy
ext.spropStoreFile = 'C:/AreaMapKeys/areamap-release.jks'
ext.spropStorePassword = 'YOUR_STORE_PASSWORD'
ext.spropKeyAlias = 'areamap'
ext.spropKeyPassword = 'YOUR_KEY_PASSWORD'
```

Относительный путь к ключу считается от `android/app`. Не коммитьте этот файл,
keystore или пароли. Проверьте исключение через `git check-ignore android/app/secure.properties`
из корня репозитория; при необходимости добавьте локальное правило исключения.

Затем из каталога `android`:

```bash
./gradlew :app:assembleFdroidRelease -Parm64
```

Результат: `android/app/build/outputs/apk/fdroid/release/` от корня репозитория.
Без настроенного ключа сборка не гарантирует подписанный устанавливаемый результат.
Имя файла может начинаться с `OrganicMaps`: имя артефакта пока наследуется от базы.
Для загрузки его можно переименовать в `AreaMap-v0.1.0-beta.1-arm64.apk`.
Переименование не меняет applicationId, отображаемое имя или внутреннюю версию.

Чтобы собрать один APK для всех ABI из текущей конфигурации, уберите `-Parm64`.
Такая сборка дольше и больше по размеру. Не называйте ARM64-сборку универсальной.

## 5. Проверить именно публикуемый файл

- Установите готовый APK на телефон, а не только запускайте приложение кнопкой Run.
- Проверьте запуск, геолокацию, офлайн-карту и пешеходный маршрут.
- Проверьте импорт GPX, гайды, SOS и отсутствие сети при отправке отчёта.
- Проверьте интерфейс с крупным шрифтом и после поворота экрана.
- Для обновления проверьте совместимость applicationId и сертификата и увеличение versionCode.

Инструменты `apksigner` и `aapt` находятся в Android SDK Build Tools. Если они доступны
в PATH, выполните рядом с переименованным APK:

```bash
apksigner verify --verbose --print-certs AreaMap-v0.1.0-beta.1-arm64.apk
aapt dump badging AreaMap-v0.1.0-beta.1-arm64.apk
sha256sum AreaMap-v0.1.0-beta.1-arm64.apk > AreaMap-v0.1.0-beta.1-arm64.apk.sha256
```

`aapt` покажет реальный package, versionCode и versionName. Семантический тег GitHub
`v0.1.0-beta.1` не заменяет дату и номер сборки, вычисляемые проектом для Android.

## 6. Создать GitHub Release

1. Откройте [Releases AreaMap](https://github.com/bayahmetov/AreaMap/releases)
   и нажмите **Draft a new release**.
2. Создайте новый тег, например `v0.1.0-beta.1`. Это пример имени; сначала проверьте,
   что такой тег ещё не занят. В **Target** выберите проверенный коммит/ветку AreaMap.
3. Укажите название `AreaMap 0.1.0 Beta 1` и кратко опишите изменения и ограничения.
4. Прикрепите проверенный `.apk` и файл `.sha256` в поле загрузки файлов.
5. Отметьте **Set as a pre-release** для тестовой версии.
6. Сначала сохраните черновик и проверьте файлы и тег, затем нажмите **Publish release**.

Если команда продолжает коммитить в ветку, зафиксируйте именно коммит сборки отдельным
тегом до оформления релиза. Из корня, только когда HEAD совпадает с проверенным SHA:

```bash
git tag -a v0.1.0-beta.1 -m "AreaMap 0.1.0 Beta 1"
git push origin refs/tags/v0.1.0-beta.1
```

После этого выбирайте существующий тег в форме релиза. Не передвигайте опубликованный
тег на другой коммит: для исправления создайте следующую версию.

GitHub автоматически добавит архивы **Source code**. Это исходники, не установщик Android.
Для установки пользователь должен скачать приложенный `.apk`.

Пример описания, заполните результатами своей проверки:

```text
Тестовый выпуск AreaMap на базе Organic Maps.

В этой версии: походный интерфейс, пешеходные маршруты, оценка времени
и высот, гайды, импорт GPX и SOS-карточка.

APK: ARM64. Android: заявленный минимум 5.0; проверено на [устройства/версии].
Коммит: [SHA]. Тип сборки: [fdroidRelease или fdroidDebug].
versionName / versionCode: [из APK].

Ограничения: GPX-сопровождение экспериментальное; отправка требует связи;
подтверждённой интеграции с ДЧС нет. Карты региона нужно скачать заранее.
```

## Почему не запускать имеющийся Android Release без адаптации

`.github/workflows/android-release.yaml` — workflow Organic Maps. Он запрашивает
production-секреты, release SSH key, внешний репозиторий скриншотов и настройки
магазинов. По умолчанию включены несколько направлений публикации.
Для первого AreaMap pre-release проще собрать и проверить APK локально, затем загрузить
его вручную. Отдельный workflow AreaMap можно добавить позже: сборка `fdroidRelease`,
секрет подписи команды, тесты и загрузка APK в черновик релиза.

## Источники

- [GitHub: Managing releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
- [Android: Sign your app](https://developer.android.com/studio/publish/app-signing)
- [Android: Prepare for release](https://developer.android.com/studio/publish/preparing)
- Конфигурация проекта: `android/app/build.gradle`, `android/groovy/application-signing.gradle`,
  `android/sdk/build.gradle`, `tools/unix/version.sh`.
