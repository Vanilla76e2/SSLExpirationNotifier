import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.util.concurrent.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;

public class SSLExpirationNotifier {
    private static Config config = new Config();
    private static final String CONFIG_FILE = "SSLExpirationNotifier.cfg";
    private static ScheduledExecutorService scheduler;
    private static final String guide =
            """
                    
                    Для обновления сертификата следуйте инструкциям ниже:
                    
                    1. Чтобы получить сертификат нужно иметь: свободные порты 443 и 80; доменное имя.
                    
                    2. Если порты 443 и 80 на микротике заняты, временно освободите их и создайте правило для вашего ip адреса по портам 443 и 80.
                    
                    3. Введите команду в терминал certbot certonly --nginx --register-unsafely-without-email
                    
                    4. Обратите внимание на IMPORTANT NOTES, там будут выведены директории в которых хранятся сертификат и ключ.
                    
                    5. После получения файлов сертификата и фала ключа убедитесь что они лежат в директории /etc/letsencrypt/live/radionoyabrsk.ru/
                    
                    6. Затем при необходимости измените ссылки на файлы в default.txt в настройках Nginx""";

    public static void main(String[] args) {
        loadConfig();
        startScheduler();
        handleConsoleInput();
    }

    private static void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(SSLExpirationNotifier::checkDate, 0, 1, TimeUnit.DAYS);
    }

    private static void checkDate() {
        try {
            if (config.SSLExpiryDate == null) return;

            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), config.SSLExpiryDate);
            if (daysLeft == 30 || daysLeft == 15 || daysLeft == 7 || daysLeft == 1) {

                sendNotification("Сертификат истекает через " + daysLeft + " дней!\n" + guide);
            }
        }
        catch (Exception e)
        {
            System.out.println("Ошибка проверки даты " + e.getMessage());
        }

    }

    private static void sendNotification(String message) {
        if (config.senderEmail == null || config.senderPassword == null || config.getterEmail == null) {
            System.out.println("Настройки email не выполнены!");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.beget.com");
        props.put("mail.smtp.port", "2525");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected  PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.senderEmail, config.senderPassword);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress((config.senderEmail)));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getterEmail));
            msg.setSubject("Напоминание о SSL сертификате");
            msg.setText(message);
            Transport.send(msg);
            System.out.println("Уведомление отправлено.");
        } catch (MessagingException e) {
            System.out.println("Ошибка отправки:" + e.getMessage());
        }
    }

    private static void handleConsoleInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("SSLExpirationNotifier запущен.\nВведите 'help' для списка команд.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ");
            String command = parts[0].toLowerCase();
            try {
                switch (command) {
                    case "test":
                        System.out.println("Тестовая отправка псимьа...");
                        sendNotification("Тестовое сообщение для проверки подключения к почте.");
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "clear":
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                        System.out.println("SSLExpirationNotifier запущен.\nВведите 'help' для списка команд.");
                        break;
                    case "set":
                        handleSetCommand(parts);
                        break;
                    case "get":
                        if (parts.length == 2 && parts[1].equalsIgnoreCase("date")) {
                            System.out.println("Дата окончания: " + config.SSLExpiryDate);
                        }
                        break;
                    case "date":
                        if (parts.length == 2 && parts[1].equalsIgnoreCase("reset")) {
                            config.SSLExpiryDate = LocalDate.now().plusDays(config.delay);
                            saveConfig();
                            System.out.println("Дата сброшена на +" + config.delay + " дней.");
                        }
                        break;
                    case "days":
                        if (parts.length == 2 && parts[1].equalsIgnoreCase("left")) {
                            if (config.SSLExpiryDate == null) {
                                System.out.println("Дата не установлена.");
                            }
                            else {
                                long days = ChronoUnit.DAYS.between(LocalDate.now(), config.SSLExpiryDate);

                                System.out.println("Осталось дней: " + days);
                            }
                        }
                        break;
                    case "exit":
                        System.out.println("Завершение работы...");
                        scheduler.shutdown();
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Неизвестная команда. Используйте help для просмотра списка команд.");
                }
            }
            catch (Exception e) {
                System.out.println("Ошибка:" + e.getMessage());
            }
        }
    }

    private static void  handleSetCommand (String[] parts) {
        if (parts.length < 3) {
            System.out.println("Неверный формат команды.");
            return;
        }

        String type = parts[1].toLowerCase();

        switch (type) {
            case "sender":
                if (parts.length == 4 && parts[2].equalsIgnoreCase("email")) {
                    config.senderEmail = parts[3];
                }
                else if (parts.length == 4 && parts[2].equalsIgnoreCase("password")) {
                    config.senderPassword = parts[3];
                }
                break;
            case "getter":
                if (parts.length == 4 && parts[2].equalsIgnoreCase("email")) {
                    config.getterEmail = parts[3];
                }
                break;
            case "date":
                try {
                    config.SSLExpiryDate = LocalDate.parse(parts[2]);
                }
                catch (DateTimeParseException e) {
                    System.out.println("Неверный формат даты! Используйте ГГГГ-ММ-ДД.");
                    return;
                }
                break;
            case "delay":
                try {
                    config.delay = Integer.parseInt(parts[2]);
                }
                catch (NumberFormatException e) {
                    System.out.println("Эта команда принимает только числовые занчения!");
                }
                break;
            default:
                System.out.println("Неизвестная команда. Используйте help для просмотра списка команд.");
                return;

        }
        saveConfig();
        System.out.println("Настройки обновлены.");
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("help - показать это сообщение");
        System.out.println("test - отправить тестовое сообщение");
        System.out.println("set sender email [адрес] - установить email отправителя");
        System.out.println("set sender password [пароль] - установить пароль отправителя");
        System.out.println("set getter email [адрес] - установить пароль получателя");
        System.out.println("set date [ГГГГ-ММ-ДД] - установить дату окончания");
        System.out.println("set delay [количество дней] - установить значение отсрочки");
        System.out.println("get date - показать дату окончания");
        System.out.println("date reset - сбросить дату на текущую + отсрочка");
        System.out.println("days left - показать количество дней до истчения срока");
        System.out.println("clear - очистить консоль");
        System.out.println("exit - выйти из приложения");
    }

    private static  void loadConfig() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            config = (Config) ois.readObject();
        }
        catch (Exception e) {
            System.out.println("Конфигурационный файл не найден. Создан новый конфигурационный файл.");
        }
    }

    private static void saveConfig() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            oos.writeObject(config);
        }
        catch (Exception e) {
            System.out.println("Ошибка сохранения конфигурационного файла: " + e.getMessage());
        }
    }

    public static class Config implements Serializable {
        String senderEmail;
        String senderPassword;
        String getterEmail;
        Integer delay;
        LocalDate SSLExpiryDate;

        public Config() {}
    }
}
