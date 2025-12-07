package de.rccookie.aoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.stream.Stream;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.json.Default;
import de.rccookie.json.Json;
import de.rccookie.math.Mathf;
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
               +" | Chars: "+input.length()+" ("+input.chars().filter(c -> !Character.isWhitespace(c)).count()+" non-empty)"
               +" | Blank lines: "+input.lines().filter(String::isBlank).count()
               +" | Line lengths: "+lengths.getMin()+(lengths.getMin() == lengths.getMax() ? "" : " - "+lengths.getMax());
    }


    private static void runAll(String[] commands, int day, int year, String token, int warmup, int repeatCount, boolean checkResults) throws Solution.InvalidInputException {
        // Validate token syntax
        if(token.length() != 128)
            throw new Solution.InvalidInputException("Invalid token, expected 128 characters");

        // Execute at least once
        if(repeatCount < 1)
            throw new Solution.InvalidInputException("Repeat count < 1");
        if(warmup < 0)
            throw new Solution.InvalidInputException("Warmup count < 0");

        // Get date of puzzle if not already given
        LocalDateTime now = LocalDateTime.now(Solution.TIMEZONE);
        if(day <= 0) {
            if(now.getMonth() != Month.DECEMBER)
                throw new Solution.InvalidInputException("Day of month is required; it is not december");
            day = now.getDayOfMonth();
        }
        if(year <= 0)
            year = now.getYear();

        for(int i=0; i<day; i++)
            if(i >= commands.length || commands[i] == null)
                Console.error("No command specified for day {}, skipping", i+1);

        // Create instances of solutions and initialize
        ProcessBuilder[] processes = new ProcessBuilder[day * 2];
        String[] correctSolutions = new String[day * 2];
        int _year = year;
        if(checkResults)
            Console.log("Loading puzzle solutions...");
        // Run in parallel -> can load multiple inputs at once
        Stream.iterate(0, i -> i + 1)
                .limit(Math.min(day, commands.length))
                .parallel()
                .filter(i -> commands[i] != null)
                .forEach(i ->  {
            Path inputFile = Path.of("input", _year+"", (i+1)+".txt");
            Solution.getInput(i+1, _year, token, inputFile);

            processes[2*i] = createProcess(commands[i], 1, i+1, _year, inputFile);
            processes[2*i+1] = createProcess(commands[i], 2, i+1, _year, inputFile);

            if(checkResults) {
                String[] correct = Solution.getSolutions(i + 1, _year, token);
                System.arraycopy(correct, 0, correctSolutions, 2 * i, correct.length);
            }
        });

        Console.log("Profiling mode, hiding regular program output (errors will be shown)");

        long[] durations = new long[processes.length];
        boolean allCorrect = true;
        boolean[] correct = new boolean[processes.length];
        Arrays.fill(correct, true);

        processLoop: for(int i=0; i<processes.length; i++) try {
            if(processes[i] == null) continue;

            Console.log("Running day {} task {}...", i/2 + 1, i%2 + 1);

            for(int j=0; j<warmup; j++) {
                Process p = processes[i].start();

                StringBuilder savedOutput = new StringBuilder();
                try(BufferedReader output = p.inputReader()) {
                    String line = output.readLine();
                    while(line != null) {
                        savedOutput.append(line).append('\n');
                        line = output.readLine();
                    }
                }
                int exitCode = p.waitFor();
                if(exitCode != 0) {
                    System.out.println(savedOutput);
                    Console.error("Task failed with exit code {}", exitCode);
                    continue processLoop;
                }
            }

            Stopwatch watch = new Stopwatch().start();

            String result = null;
            for(int j=0; j<repeatCount; j++) {
                Process p = processes[i].start();

                StringBuilder savedOutput = new StringBuilder();
                try(BufferedReader output = p.inputReader()) {
                    String line = output.readLine();
                    while(line != null) {
                        savedOutput.append(line).append('\n');
                        result = line;
                        line = output.readLine();
                    }
                }
                int exitCode = p.waitFor();
                if(exitCode != 0) {
                    System.out.println(savedOutput);
                    Console.error("Task failed with exit code {}", exitCode);
                    continue processLoop;
                }
            }
            durations[i] = watch.getPassedNanos() / repeatCount;

            if(checkResults && correctSolutions[i] != null) {
                if(result == null) {
                    Console.error("Incorrect result - no result at all. The last line printed to stdout is treated as result.");
                    correct[i] = allCorrect = false;
                }
                else if(!result.equals(correctSolutions[i])) {
                    Console.error("Incorrect result, expected {} but got {}", correctSolutions[i], result);
                    correct[i] = allCorrect = false;
                }
            }

        } catch(IOException|InterruptedException e) {
            throw Utils.rethrow(e);
        }

        System.out.println();
        Console.map("Duration"+(repeatCount>1?" (average of "+repeatCount+" runs)":""), (Mathf.sumL(durations) / 1000000.0) + "ms");
        if(checkResults && allCorrect)
            Console.log("All results correct");

        // Show the duration of each individual task in a table
        System.out.println(Solution.createTable(durations, correct));
        if(!allCorrect)
            Console.log("(*): Puzzle solution was incorrect");
    }

    private static String run(String[] commands, int task, int day, int year, String token, boolean exampleInput, int warmup, int repeatCount, boolean inputStats) throws Solution.InvalidInputException {

        // Validate token syntax (don't need a token for example input - not for input download, and won't submit)
        if((!exampleInput || task <= 0) && token.length() != 128)
            throw new Solution.InvalidInputException("Invalid token, expected 128 characters");

        // Get date of puzzle if not already given
        LocalDateTime now = LocalDateTime.now(Solution.TIMEZONE);
        if(day <= 0) {
            if(now.getMonth() != Month.DECEMBER)
                throw new Solution.InvalidInputException("Day of month is required; it is not december");
            day = now.getDayOfMonth();
        }
        if(year <= 0)
            year = now.getYear();
        int _day = day, _year = year;

        if(commands.length < day || commands[day-1] == null)
            throw new Solution.InvalidInputException("No command specified for day "+day);

        Wrapper<String[]> solutions = new Wrapper<>(null);
        if(task > 0) {
            // In the background, find which parts of the puzzle were already solved and get their solutions
            new Thread(() -> {
                String[] s = Solution.getSolutions(_day, _year, token);
                Console.mapDebug("Solutions", solutions.value);
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
            task = Math.min(2, solutions.value.length + 1);
            Console.mapDebug("Solutions", solutions.value);
        }

        // Read input file or fetch from website and store
        Path inputFile;
        byte[] input;
        if(exampleInput) {
            inputFile = Path.of("input", year+"", "examples", day+".txt");
            input = Solution.getExampleInput(day, year, inputFile);
        }
        else {
            inputFile = Path.of("input", year+"", day+".txt");
            input = Solution.getInput(day, year, token, inputFile);
        }

        if(inputStats)
            Console.log(getInputStats(new String(input)));
        Console.log("Running task {} of puzzle {}{}", task, day, year != LocalDate.now(Solution.TIMEZONE).getYear() ? " from year " + year : "");

        ProcessBuilder process = createProcess(commands[day-1], task, day, year, inputFile);

        Stopwatch watch = new Stopwatch();
        String result = null;
        int exitCode = 0;
        try {
            System.out.println("----------- Program output -----------");
            for(int j=0; j<warmup; j++) {
                Process p = process.start();
                p.getInputStream().transferTo(System.out);
                exitCode = p.waitFor();
                if(exitCode != 0) break;
            }
            watch.start();
            for(int j=0; j<repeatCount; j++) {
                Process p = process.start();

                try(BufferedReader output = p.inputReader()) {
                    String line = output.readLine();
                    while(line != null) {
                        System.out.println(line);
                        result = line;
                        line = output.readLine();
                    }
                }
                exitCode = p.waitFor();
                if(exitCode != 0) break;
            }
        } catch(IOException|InterruptedException e) {
            throw Utils.rethrow(e);
        }

        if(exitCode != 0)
            System.exit(exitCode);

        // Print results
        Console.map("Result", Console.colored(result != null ? result : "No result found - the last line printed to stdout is treated as result", Attribute.BOLD()));
        Console.map("Duration", watch.getPassedNanos() / 1000000.0 / repeatCount + "ms");
        if(exampleInput || result == null || result.isBlank() || result.length() > 30)
            // null, blank string or long output won't be the solution, so just exit
            return result;

        return Solution.maybeSubmit(task, day, year, token, solutions, result, t -> run(commands, t, _day, _year, token, false, warmup, repeatCount, false));
    }

    private static ProcessBuilder createProcess(String cmdTemplate, int task, int day, int year, Path inputFile) {
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
        cmd[2] = cmdTemplate.replace("{day}", ""+day)
                .replace("{0_day}", String.format("%02d", day))
                .replace("{year}", String.format("%02d", year % 100))
                .replace("{full_year}", ""+year)
                .replace("{task}", ""+task)
                .replace("{file}", ""+inputFile)
                .replace("{abs_file}", ""+inputFile.toAbsolutePath())
                .replace("{input}", win ? "$(Get-Content '"+inputFile.toAbsolutePath()+"' -Raw)" : "\"$(<'"+inputFile.toAbsolutePath()+"')\"");

        synchronized(Console.class) {
            Console.mapDebug("Command for day " + day + " task " + task, cmd[2]);
        }

        return new ProcessBuilder(cmd)
                .redirectInput(inputFile.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT);
    }

    public static void main(String[] args) {
        ArgsParser parser = Solution.createGenericArgsParser();
        parser.setDescription("""
                        Runs your advent of code solutions (which can be coded in any programming language,
                        as long as you can run it from the terminal).
                        Automatically downloads input or example files and submits your solution.

                        Usage: aoc-run [options]""");
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
        Options options = parser.parse(args);

        try {
            Config config = Solution.parseConfig(options, Config.class, "/defaultExternalConfig.json");

            if(config.all)
                runAll(config.command().commands, config.day, config.year, config.tokenValue(), config.warmup, config.repeat, config.checkAll);
            else run(config.command().commands, config.task, config.day, config.year, config.tokenValue(), config.example, config.warmup, config.repeat, config.inputStats);
        } catch(Solution.InvalidInputException e) {
            Console.error(e.getMessage());
            if(Console.isEnabled("debug"))
                Console.error(e);
            if(args.length == 0)
                Console.error("Run --help for more info");
        }
    }


    /**
     * Represents the config file for an external program.
     */
    private record Config(@Default(value = "\"token.txt\"") Path token,
                          String tokenValue,
                          Commands command,
                          @Default("true") boolean inputStats,
                          @Default("false") boolean all,
                          @Default("false") boolean checkAll,
                          @Default("1") int repeat,
                          @Default("0") int warmup,
                          @Default("false") boolean example,
                          @Default("-1") int day,
                          @Default("-1") int year,
                          @Default("-1") int task) {

        @Override
        public String tokenValue() {
            if(tokenValue != null)
                return tokenValue;
            try {
                return Files.readString(token()).trim();
            } catch(IOException e) {
                throw new Solution.InvalidInputException("Could not read '" + token() + "': " + e.getMessage(), e);
            }
        }

        @Override
        @NotNull
        public Commands command() {
            if(command == null)
                throw new Solution.InvalidInputException("'command' field missing in config file, which should be the command pattern to run your program for a specific puzzle task. See README.md for more info on patterns.");
            return command;
        }
    }

    record Commands(String[] commands) {
        static {
            Json.registerDeserializer(Commands.class, json -> {
                if(json.isArray())
                    return new Commands(json.as(String[].class));
                String[] commands = new String[25];
                Arrays.fill(commands, json.asString());
                return new Commands(commands);
            });
        }
    }
}
