import picocli.CommandLine;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@CommandLine.Command(name = "notify", description = "Отправить напоминание о SSL сертификате вручную")
class NotifyCommand implements Runnable {

    @CommandLine.Option(names = "--send", description = "Отправить уведомление на все настроенные каналы")
    boolean send;

    @CommandLine.Option(names = "--email", description = "Отправить уведомление только по Email")
    boolean emailOnly;

    @CommandLine.Option(names = "--telegram", description = "Отправить уведомление только через Telegram")
    boolean telegramOnly;

    @Override
    public void run() {
        if (SSLExpirationNotifier.config.sslCertPath == null) {
            System.out.println("Путь к сертификату не установлен!");
            return;
        }

        LocalDate expiry = SSLExpirationNotifier.getExpiryDateFromCert(SSLExpirationNotifier.config.sslCertPath);
        if (expiry == null) {
            System.out.println("Не удалось прочитать сертификат.");
            return;
        }

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiry) - 1;

        String message = "Напоминание о SSL сертификате!\n" +
                "Срок истекает: " + expiry + "\n" +
                "Осталось дней: " + daysLeft + "\n" +
                SSLExpirationNotifier.guide;

        // Если не указано никаких флагов, по умолчанию --send
        if (!send && !emailOnly && !telegramOnly) send = true;

        if (send || emailOnly) {
            if (SSLExpirationNotifier.config.emailSender != null &&
                    SSLExpirationNotifier.config.emailPassword != null &&
                    SSLExpirationNotifier.config.emailReceiver != null) {
                SSLExpirationNotifier.sendNotification(message);
                System.out.println("Email уведомление отправлено.");
            } else {
                System.out.println("Email не настроен.");
            }
        }

        if (send || telegramOnly) {
            if (SSLExpirationNotifier.config.telegramToken != null &&
                    SSLExpirationNotifier.config.telegramChatId != null) {
                SSLExpirationNotifier.sendTelegram(message);
                System.out.println("Telegram уведомление отправлено.");
            } else {
                System.out.println("Telegram не настроен.");
            }
        }
    }
}
