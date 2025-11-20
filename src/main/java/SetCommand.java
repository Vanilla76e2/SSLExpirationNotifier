import picocli.CommandLine;

@CommandLine.Command(name = "set", description = "Изменить значения конфигурации")
class SetCommand implements Runnable {

    @CommandLine.Option(names = "--emailSender", description = "Установить email отправителя")
    String emailSender;

    @CommandLine.Option(names = "--emailPassword", description = "Установить пароль email отправителя")
    String emailPassword;

    @CommandLine.Option(names = "--emailReceiver", description = "Установить email получателя")
    String emailReceiver;

    @CommandLine.Option(names = "--sslPath", description = "Установить путь к SSL сертификату")
    String sslPath;

    @CommandLine.Option(names = "--tgToken", description = "Установить токен Telegram бота")
    String tgToken;

    @CommandLine.Option(names = "--tgChat", description = "Установить chat ID Telegram")
    String tgChat;

    // Новые опции для SMTP
    @CommandLine.Option(names = "--smtpHost", description = "Установить SMTP хост сервера")
    String smtpHost;

    @CommandLine.Option(names = "--smtpPort", description = "Установить SMTP порт")
    Integer smtpPort;

    @CommandLine.Option(names = "--smtpTLS", description = "Включить/отключить TLS (true/false)")
    Boolean smtpTLS;

    @CommandLine.Option(names = "--smtpAuth", description = "Включить/отключить авторизацию (true/false)")
    Boolean smtpAuth;

    @Override
    public void run() {
        if (emailSender != null) SSLExpirationNotifier.config.emailSender = emailSender;
        if (emailPassword != null) SSLExpirationNotifier.config.emailPassword = emailPassword;
        if (emailReceiver != null) SSLExpirationNotifier.config.emailReceiver = emailReceiver;
        if (sslPath != null) SSLExpirationNotifier.config.sslCertPath = sslPath;
        if (tgToken != null) SSLExpirationNotifier.config.telegramToken = tgToken;
        if (tgChat != null) SSLExpirationNotifier.config.telegramChatId = tgChat;

        if (smtpHost != null) SSLExpirationNotifier.config.smtpHost = smtpHost;
        if (smtpPort != null) SSLExpirationNotifier.config.smtpPort = smtpPort;
        if (smtpTLS != null) SSLExpirationNotifier.config.smtpTLS = smtpTLS;
        if (smtpAuth != null) SSLExpirationNotifier.config.smtpAuth = smtpAuth;

        SSLExpirationNotifier.saveConfig();
        System.out.println("Настройки обновлены.");
    }
}
