import picocli.CommandLine;

@CommandLine.Command(name = "test", description = "Отправить тестовое уведомление")
class TestCommand implements Runnable {

    @CommandLine.Option(names = "--email", description = "Отправить тестовое уведомление только по Email")
    boolean email;

    @CommandLine.Option(names = "--telegram", description = "Отправить тестовое уведомление только через Telegram")
    boolean telegram;

    @Override
    public void run() {
        if (email) SSLExpirationNotifier.sendEmail("Тестовое сообщение.", "Тестовое сообщение для проверки Telegram бота.");
        if (telegram) SSLExpirationNotifier.sendTelegram("Тестовое сообщение.");
    }
}
