import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;

@CommandLine.Command(name = "renew", description = "Обновить SSL сертификат с помощью certbot")
class RenewCommand implements Runnable {

    @Override
    public void run() {
        if (SSLExpirationNotifier.config.sslCertPath == null) {
            System.out.println("Путь к сертификату не установлен!");
            return;
        }

        try {
            // Здесь запускаем команду certbot для nginx
            ProcessBuilder pb = new ProcessBuilder("sudo", "certbot", "certonly", "--nginx", "--register-unsafely-without-email");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Читаем вывод команды
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Обновление сертификата прошло успешно.");

                // После обновления можно обновить дату в конфиге
                LocalDate expiry = SSLExpirationNotifier.getExpiryDateFromCert(
                        SSLExpirationNotifier.config.sslCertPath);
                if (expiry != null) {
                    System.out.println("Новая дата окончания: " + expiry);
                }

                // Отправляем уведомление о продлении на настроенные каналы
                String message = "SSL сертификат успешно обновлён!\n" +
                        "Новая дата окончания: " + expiry + "\n" +
                        SSLExpirationNotifier.guide;

                if (SSLExpirationNotifier.config.emailSender != null &&
                        SSLExpirationNotifier.config.emailPassword != null &&
                        SSLExpirationNotifier.config.emailReceiver != null) {
                    SSLExpirationNotifier.sendNotification(message);
                }

                if (SSLExpirationNotifier.config.telegramToken != null &&
                        SSLExpirationNotifier.config.telegramChatId != null) {
                    SSLExpirationNotifier.sendTelegram(message);
                }

            } else {
                System.out.println("Обновление сертификата завершилось с ошибкой. Код: " + exitCode);
            }
        } catch (Exception e) {
            System.out.println("Ошибка при обновлении сертификата: " + e.getMessage());
        }
    }
}
