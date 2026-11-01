# TV Manager Launcher

Лаунчер для Google TV / Android TV: видео-обои со своим звуком, заставка-видео, быстрые кнопки (HDMI, VPN, приложения), список приложений.

## Как получить APK без Android Studio
1. Создайте бесплатный аккаунт на github.com и новый репозиторий.
2. Загрузите в него все файлы этого проекта (включая папку `.github`). Если папка `.github` не загрузилась — Add file → Create new file → имя `.github/workflows/build.yml` и вставьте содержимое из архива.
3. Вкладка **Actions** → дождитесь зелёной галочки → откройте запуск → внизу **Artifacts** → `tv-launcher-apk`. Внутри `app-debug.apk`.

Или откройте папку в Android Studio и выберите Build → Build APK.

## Установка
В ПК-приложении `tvmanager.py` вкладка «Лаунчер ТВ»: установить APK → сделать основным → отправить кнопки/видео.
Видео и настройки лежат на ТВ в `/sdcard/Android/data/com.tvmanager.launcher/files/` (папки `wallpapers`, `screensaver`, `sounds`, `icons`, файл `config.json`).
