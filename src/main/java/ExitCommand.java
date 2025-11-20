import picocli.CommandLine;

@CommandLine.Command(name = "exit", description = "Выйти из приложения")
class ExitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Выход...");
        if (SSLExpirationNotifier.scheduler != null) {
            SSLExpirationNotifier.scheduler.shutdown();
        }
        System.exit(0);
    }
}
