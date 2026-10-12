КОМПЛЕКТ ЗАПУСКА PULSEUP ДЛЯ ANDROID

1) Сначала создайте репозиторий https://github.com/pulseup0/PulseUp-Android.
2) Загрузите в него всё содержимое папки PulseUp-Android (включая скрытую .github).
3) После commit откройте Actions и дождитесь Build PulseUp APK.
4) Для скачивания участниками создайте Release с тегом v1.0.0. Тег запускает workflow, который прикрепит PulseUp.apk к релизу.
5) Проверьте адрес https://github.com/pulseup0/PulseUp-Android/releases/latest/download/PulseUp.apk.
6) Только после публикации APK загрузите site/index.html в корень основного репозитория https://github.com/pulseup0/PulseUp. В файле есть карточка скачивания для авторизованных пользователей.

ПРЕДУПРЕЖДЕНИЕ: APK ещё не был скомпилирован или проверен на устройстве в этой среде. GitHub Actions выполнит сборку после загрузки проекта. Это debug-подписанный beta APK; для долговременных обновлений потребуется стабильный собственный ключ подписи.
