import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.shell.jline3.PicocliJLineCompleter;
import picocli.CommandLine;

@CommandLine.Command(
        name = "",
        description = "Интерактивная оболочка SSLExpirationNotifier",
        subcommands = {
                SetCommand.class,
                GetCommand.class,
                TestCommand.class,
                ExitCommand.class,
                NotifyCommand.class,
                RenewCommand.class,
                CommandLine.HelpCommand.class
        }
)
public class Shell implements Runnable {

    @Override
    public void run() {
        System.out.println("SSLExpirationNotifier запущен.");
        System.out.println("Введите help для списка команд.");
    }

    public static void start() throws Exception {

        CommandLine cmd = new CommandLine(new Shell());

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .jna(true)
                .build();

        PicocliJLineCompleter completer =
                new PicocliJLineCompleter(cmd.getCommandSpec());

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .history(new DefaultHistory())
                .build();

        cmd.execute();

        while (true) {
            String line;
            try {
                line = reader.readLine("> ");
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println("Выход...");
                return;
            }

            if (line.trim().isEmpty()) continue;

            try {
                cmd.execute(line.split(" "));
            } catch (Exception ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            }
        }
    }
}