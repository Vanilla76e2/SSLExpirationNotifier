
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;



public class SSLExpirationNotifier {
    public static Config config = new Config();
    private static final String CONFIG_FILE = "SSLExpirationNotifier.cfg";
    public static ScheduledExecutorService scheduler;
    public static final String guide =
            """
                    
                    Для обновления сертификата следуйте инструкциям ниже:
                    
                    1. Чтобы получить сертификат нужно иметь: свободные порты 443 и 80; доменное имя.
                    
                    2. Если порты 443 и 80 на микротике заняты, временно освободите их и создайте правило для вашего ip адреса по портам 443 и 80.
                    
                    3. Введите команду в терминал certbot certonly --nginx --register-unsafely-without-email
                    
                    4. Обратите внимание на IMPORTANT NOTES, там будут выведены директории в которых хранятся сертификат и ключ.
                    
                    5. После получения файлов сертификата и фала ключа убедитесь что они лежат в директории /etc/letsencrypt/live/radionoyabrsk.ru/
                    
                    6. Затем при необходимости измените ссылки на файлы в default.txt в настройках Nginx""";

    public static void main(String[] args) throws Exception {
        loadConfig();
        startScheduler();
        Shell.start();
    }

    private static void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(SSLExpirationNotifier::checkDate, 0, 1, TimeUnit.DAYS);
    }

    private static void checkDate() {
        try {
            if (config.sslCertPath == null) return;

            LocalDate expiryDate = getExpiryDateFromCert(config.sslCertPath);
            if (expiryDate == null) return;

            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate) - 1;

            if (daysLeft == 30 || daysLeft == 15 || daysLeft == 7 || daysLeft == 1) {
                sendNotification("Сертификат истекает через " + daysLeft + " дней!\n" + guide);
            }
        } catch (Exception e) {
            System.out.println("Ошибка проверки сертификата: " + e.getMessage());
        }
    }

    // Notifications
    public static void sendNotification(String message) {
        boolean sent = false;

        // Email
        if (config.emailSender != null && config.emailPassword != null && config.emailReceiver != null) {
            sendEmail("Напоминание о SSL сертификате", message);
        }

        // Telegram
        if (config.telegramToken != null && config.telegramChatId != null) {
            sendTelegram(message);
            sent = true;
        }

        if (!sent) {
            System.out.println("Не настроены каналы для уведомлений! Сообщение не отправлено.");
        }
    }

    public static void sendEmail(String subject, String messageText) {
        if (config.emailSender == null || config.emailPassword == null || config.emailReceiver == null) {
            System.out.println("Настройки email не выполнены!");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.smtpHost != null ? config.smtpHost : "smtp.beget.com");
            props.put("mail.smtp.port", config.smtpPort != null ? config.smtpPort.toString() : "2525");
            props.put("mail.smtp.auth", config.smtpAuth != null ? config.smtpAuth.toString() : "true");
            props.put("mail.smtp.starttls.enable", config.smtpTLS != null ? config.smtpTLS.toString() : "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.emailSender, config.emailPassword);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(config.emailSender));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.emailReceiver));
            msg.setSubject(subject);
            msg.setText(messageText);

            Transport.send(msg);
            System.out.println("Email отправлен на " + config.emailReceiver);
        } catch (MessagingException e) {
            System.out.println("Ошибка отправки email: " + e.getMessage());
        }
    }

    public static void sendTelegram(String message) {
        if (config.telegramToken == null || config.telegramChatId == null) {
            System.out.println("Telegram не настроен!");
            return;
        }

        try {
            String text = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String urlString = "https://api.telegram.org/bot" + config.telegramToken
                    + "/sendMessage?chat_id=" + config.telegramChatId
                    + "&text=" + text;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.getInputStream().close(); // читаем ответ, чтобы запрос завершился

            System.out.println("Уведомление в Telegram отправлено.");
        } catch (Exception e) {
            System.out.println("Ошибка при отправке в Telegram: " + e.getMessage());
        }
    }


    // Configuration
    private static  void loadConfig() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            config = (Config) ois.readObject();
        }
        catch (Exception e) {
            System.out.println("Конфигурационный файл не найден. Создан новый конфигурационный файл.");
        }
    }

    public static void saveConfig() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            oos.writeObject(config);
        }
        catch (Exception e) {
            System.out.println("Ошибка сохранения конфигурационного файла: " + e.getMessage());
        }
    }

    public static class Config implements Serializable {
        String emailSender;
        String emailPassword;
        String emailReceiver;
        String sslCertPath;
        String telegramToken;
        String telegramChatId;
        String smtpHost;
        Integer smtpPort;
        Boolean smtpTLS;
        Boolean smtpAuth;

        public Config() {}
    }

    // Helper
    public static LocalDate getExpiryDateFromCert(String certPath) {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            return cert.getNotAfter().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception e) {
            System.out.println("Не удалось прочитать сертификат: " + e.getMessage());
            return null;
        }
    }
}