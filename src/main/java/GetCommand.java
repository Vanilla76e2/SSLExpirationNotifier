import picocli.CommandLine;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@CommandLine.Command(name = "get", description = "Показать значения конфигурации")
class GetCommand implements Runnable {

    @CommandLine.Option(names = "--sslDate", description = "Показать дату окончания SSL сертификата и количество дней до истечения")
    boolean sslDate;

    @CommandLine.Option(names = "--emailSender", description = "Показать email отправителя")
    boolean emailSender;

    @CommandLine.Option(names = "--emailGetter", description = "Показать email получателя")
    boolean emailReceiver;

    @CommandLine.Option(names = "--sslPath", description = "Показать путь к SSL сертификату")
    boolean sslPath;

    @CommandLine.Option(names = "--telegram", description = "Показать токен Telegram и chat ID")
    boolean telegram;

    @Override
    public void run() {
        if (sslDate) {
            if (SSLExpirationNotifier.config.sslCertPath == null) {
                System.out.println("Путь к сертификату не установлен.");
            } else {
                LocalDate expiry = SSLExpirationNotifier.getExpiryDateFromCert(
                        SSLExpirationNotifier.config.sslCertPath);
                if (expiry != null) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiry) - 1;
                    System.out.println("Срок сертификата: " + expiry);
                    System.out.println("Осталось дней: " + daysLeft);
                } else {
                    System.out.println("Не удалось прочитать сертификат.");
                }
            }
        }

        if (emailSender) {
            System.out.println("Email отправителя: " + SSLExpirationNotifier.config.emailSender);
        }

        if (emailReceiver) {
            System.out.println("Email получателя: " + SSLExpirationNotifier.config.emailReceiver);
        }

        if (sslPath) {
            System.out.println("Путь к SSL сертификату: " + SSLExpirationNotifier.config.sslCertPath);
        }

        if (telegram) {
            System.out.println("Telegram Token: " + SSLExpirationNotifier.config.telegramToken);
            System.out.println("Telegram Chat ID: " + SSLExpirationNotifier.config.telegramChatId);
        }

        // Если не передали ни одной опции, можно выводить все значения сразу
        if (!sslDate && !emailSender && !emailReceiver && !sslPath && !telegram) {
            System.out.println("Конфигурация:");
            System.out.println("Sender email: " + SSLExpirationNotifier.config.emailSender);
            System.out.println("Getter email: " + SSLExpirationNotifier.config.emailReceiver);
            System.out.println("SSL path: " + SSLExpirationNotifier.config.sslCertPath);
            System.out.println("Telegram token: " + SSLExpirationNotifier.config.telegramToken);
            System.out.println("Telegram chat id: " + SSLExpirationNotifier.config.telegramChatId);
        }
    }
}