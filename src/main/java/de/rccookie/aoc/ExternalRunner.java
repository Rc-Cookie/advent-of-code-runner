package de.rccookie.aoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.IntSummaryStatistics;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.json.Default;
import de.rccookie.json.Json;
import de.rccookie.util.ArgsParser;
import de.rccookie.util.Console;
import de.rccookie.util.Options;
import de.rccookie.util.Stopwatch;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;
import org.jetbrains.annotations.NotNull;

public final class ExternalRunner {

    private ExternalRunner() { }

    private static String getInputStats(String input) {
        IntSummaryStatistics lengths = input.lines().mapToInt(String::length).filter(i -> i != 0).summaryStatistics();
        return "Input statistics: Lines: "+input.lines().count()
               +" | Chars: "+input.toCharArray().length
               +" | Blank lines: "+input.lines().filter(String::isBlank).count()
               +" | Line lengths: "+lengths.getMin()+(lengths.getMin() == lengths.getMax() ? "" : " - "+lengths.getMax());
    }

    private static String run(String command, int task, int day, int year, String token, boolean inputStats) throws Solution.InvalidInputException {

        // Validate token syntax
        if(token.length() != 128)
            throw new Solution.InvalidInputException("Invalid token, expected 128 characters");

        // Get date of puzzle if not already given
        if(day <= 0)
            day = Solution.CALENDAR.get(Calendar.DAY_OF_MONTH);
        if(year <= 0)
            year = Solution.CALENDAR.get(Calendar.YEAR);
        int _day = day, _year = year;

        Wrapper<String[]> solutions = new Wrapper<>(null);
        if(task > 0) {
            // In the background, find which parts of the puzzle were already solved and get their solutions
            new Thread(() -> {
                String[] s = Solution.getSolutions(_day, _year, token);
                synchronized(solutions) {
                    solutions.value = s;
                    solutions.notifyAll();
                }
            }, "Solution fetcher").start();
        }
        else {
            // We need to know which parts are already solved to find out which task to execute. Get the solutions
            // to them in the same request
            solutions.value = Solution.getSolutions(day, year, token);
            task = solutions.value.length + 1;
        }

        // Read input file or fetch from website and store
        Path inputFile = Path.of("input", year+"", day+".txt");
        String input = Solution.getInput(day, year, token, inputFile);

        if(inputStats)
            Console.log(getInputStats(input));
        Console.log("Running task {} of puzzle {}{}", task, day, year != Solution.CALENDAR.get(Calendar.YEAR) ? " from year "+year : "");

        String[] cmd = buildCommand(command, task, day, year, inputFile);

        Stopwatch watch = new Stopwatch().start();
        String result = null;
        int exitCode;
        try {
            System.out.println("----------- Program output -----------");
            Process p = new ProcessBuilder(cmd)
                    .redirectInput(inputFile.toFile())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

            try(BufferedReader output = p.inputReader()) {
                String line = output.readLine();
                while(line != null) {
                    System.out.println(line);
                    result = line;
                    line = output.readLine();
                }
            }
            exitCode = p.waitFor();
        } catch(IOException|InterruptedException e) {
            throw Utils.rethrow(e);
        }

        if(exitCode != 0)
            System.exit(exitCode);

        // Print results
        Console.map("Result", Console.colored(result != null ? result : "null", Attribute.BOLD()));
        Console.map("Duration", watch.getPassedNanos() / 1000000.0 + "ms");
        if(result == null || result.isBlank() || result.length() > 30)
            // null, blank string or long output won't be the solution, so just exit
            return result;

        return Solution.maybeSubmit(task, day, year, token, solutions, result, t -> run(command, t, _day, _year, token, inputStats));
    }


    private static String[] buildCommand(String template, int task, int day, int year, Path inputFile) {
        // Build command from pattern
        String[] cmd = new String[3];
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        if(win) {
            cmd[0] = "powershell.exe";
            cmd[1] = "-Command";
        }
        else {
            cmd[0] = "bash";
            cmd[1] = "-c";
        }
        cmd[2] = template.replace("{day}", ""+day)
                .replace("{0_day}", String.format("%02d", day))
                .replace("{year}", String.format("%02d", year % 100))
                .replace("{full_year}", ""+year)
                .replace("{task}", ""+task)
                .replace("{file}", ""+inputFile)
                .replace("{abs_file}", ""+inputFile.toAbsolutePath())
                .replace("{input}", win ? "$(Get-Content \""+inputFile+"\" -Raw)" : "\"$(<"+inputFile+")\"");
        return cmd;
    }

    public static void main(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.addDefaults();
        parser.addOption('t', "task", true, "Specify a specific task that should be ran, rather than running task 2 iff task 1 is completed otherwise task 1");
        parser.addOption('d', "day", true, "Specify a specific day of month whose task should be executed, rather than today's task");
        parser.addOption('y', "year", true, "Specify a specific year (yy or yyyy) whose task should be executed, rather than running this year's tasks");
        parser.addOption('c', "config", true, "Path to config file, default is config.json");
        parser.addOption(null, "token", true, "Overrides config; access token used to authenticate on adventofcode.com");
        parser.addOption(null, "cmd", true, """
                        Overrides config; command (executable in bash or powershell, depending on your system) to execute to run your program, where specific patterns will be replaced (e.g. in file name or params):
                         - {day}: day of puzzle
                         - {0_day}: day of puzzle, padded with a zero if necessary
                         - {year}: year of puzzle mod 100 (the last two digits)
                         - {full_year}: year of puzzle
                         - {task}: Task number (1 or 2) describing the subtask of the puzzle to run
                         - {file}: Filename of the input file (note that the input file is also piped in as stdin
                         - {abs_file}: Absolute filename of the input file
                         - {input}: The contents of the input file (to be passed as parameter to the program) (note that the input file is also piped in as stdin)""");
        parser.addOption('s', "inputStats", true, "Overrides config; boolean value ('true' for true) whether to print input stats before execution, default is true");
        Options options = parser.parse(args);

        try {
            // Load config file only on demand; if we never use it we don't need to complain about errors
            Config config = null;
            String configPath = options.getOr("config", "config.json");

            String token;
            String command;
            boolean inputStats;

            if(!options.is("token")) {
                config = Config.read(configPath);
                token = config.tokenValue();
            }
            else token = options.get("token");

            if(!options.is("cmd")) {
                if(config == null)
                    config = Config.read(configPath);
                command = config.command();
            }
            else command = options.get("cmd");

            if(!options.is("inputStats")) {
                if(config == null)
                    config = Config.read(configPath);
                inputStats = config.showInputStats();
            }
            else inputStats = options.get("inputStats").equalsIgnoreCase("true");

            int year = options.getIntOr("year", -1);
            if(year >= 0 && year < 100)
                year += 2000;

            run(command, options.getIntOr("task", -1), options.getIntOr("day", -1), year, token, inputStats);
        } catch(Solution.InvalidInputException e) {
            Console.error(e.getMessage());
        }
    }


    /**
     * Represents the config file for an external program.
     */
    private record Config(@Default(value = "token.txt", string = true) Path token,
                      String command,
                      @Default("true") boolean showInputStats) {
        @NotNull
        private static Config read(String path) {
            try {
                return Json.load("config.json").as(Config.class);
            } catch(Exception e) {
                throw new Solution.InvalidInputException("Failed to parse " + path + (e.getMessage() != null ? ": " + e.getMessage() : "") + "\nCheck the README.md file to get detail on the structure of the config file.", e);
            }
        }

        public String tokenValue() {
            try {
                return Files.readString(token()).trim();
            } catch(IOException e) {
                throw new Solution.InvalidInputException("Could not read '" + token() + "': " + e.getMessage(), e);
            }
        }

        @Override
        @NotNull
        public String command() {
            if(command == null)
                throw new Solution.InvalidInputException("'command' field missing in config.json, which should be the command pattern to run your program for a specific puzzle task. See README.md for more info on patterns.");
            return command;
        }
    }
}
