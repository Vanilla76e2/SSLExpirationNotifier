# SSLExpirationNotifier

Интерактивная консольная утилита для мониторинга SSL сертификатов.  
Позволяет:

- Проверять дату окончания SSL сертификата.
- Отправлять уведомления по Email и Telegram.
- Автоматически отправлять уведомления за 30, 14, 7, и 1 день до истечения сертификата.
- Обновлять сертификат через certbot.

---

## Требования

- Java 17+
- Maven (или Gradle)
- certbot (если планируется использование обновления сертификатов)
- Доступ к SMTP серверу (для email уведомлений)
- Telegram бот (для уведомлений через Telegram)

---

## Установка

1. Клонируйте репозиторий:

```bash
git clone https://github.com/yourusername/SSLExpirationNotifier.git
cd SSLExpirationNotifier
```

2. Соберите проект:

```bash
mvn clean package
```

3. Настройте конфиг через REPL или вручную редактируя SSLExpirationNotifier.cfg.

---

## Использование

Запуск приложения:

```bash
java -jar target/SSLExpirationNotifier.jar
```

Основные команды REPL

- help — показать список команд

- set — изменить настройки (email, Telegram, SSL путь, SMTP)

- get — вывести текущие значения конфигурации

- notify — принудительно отправить уведомление

- renew — обновить SSL сертификат через certbot

- test — отправить тестовое уведомление

- exit — выйти из приложения

Пример:

```bash
> set emailSender sender@gmail.com
> set emailPassword mypassword
> set emailReciever reciever@gmail.com
> set sslPath /etc/letsencrypt/live/example.com/cert.pem
> notify --send
```

---

## Лицензия

Проект распространяется под лицензией GNU GPLv3.