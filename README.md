# NotesApp — Простое приложение для заметок

Простое Android-приложение для создания, просмотра и удаления заметок.

## Функции
- Список заметок
- Добавление новой заметки
- Редактирование заметки (долгий тап)
- Удаление заметки (свайп или кнопка)
- Локальное хранение (без интернета)

## Скачать APK

APK собирается автоматически через **GitHub Actions**.

1. Перейдите в вкладку [Actions](https://github.com/ivanivorontsov-sudo/NotesApp/actions)
2. Дождитесь успешного завершения workflow **Build APK**
3. Скачайте артефакт `notes-app-debug` и распакуйте его — внутри будет `app-debug.apk`

Или создайте Release — APK появится в Assets.

## Как собрать самостоятельно

```bash
# Требуется Android SDK и JDK 17+
./gradlew assembleDebug
# APK будет в app/build/outputs/apk/debug/app-debug.apk
```

## Технологии
- Kotlin
- AndroidX
- Material Design
- SharedPreferences (простое хранение)

Создано с помощью Grok (xAI)
