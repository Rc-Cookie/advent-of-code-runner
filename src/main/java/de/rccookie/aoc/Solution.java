package de.rccookie.aoc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntFunction;
import java.util.function.ToLongFunction;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.json.Default;
import de.rccookie.json.Json;
import de.rccookie.math.Mathf;
import de.rccookie.util.ArgsParser;
import de.rccookie.util.Console;
import de.rccookie.util.ListStream;
import de.rccookie.util.Options;
import de.rccookie.util.Stopwatch;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;
import de.rccookie.xml.Node;
import de.rccookie.xml.XML;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * A base class for classes that implement a solution for a day of Advent of Code,
 * which eases the development and automatically loads and submits inputs and outputs.
 * An implementation only needs to implement the task1() method, the class will
 * automatically detect that task2() is not yet implemented and will run task 1.
 * Once task2() gets overridden (the base method does not need / shouldn't be called),
 * that task will automatically be run instead of task 1. In any case only one of
 * task1() and task2() will be executed on the same solution instance.
 *
 * <p>To function properly, a "config.json" file should be present in the working directory
 * of the program. Alternatively, a config file can be passed to the program with the
 * "--config" option. The config file should contain the following fields:</p>
 * <ul>
 *     <li>"classPattern": The fully qualified name pattern of your solution classes
 *     that you want to use. The name can include {day}, {0_day}, {year} and {full_year},
 *     which will be replaced with the day of month, day of month padded with 0 if needed,
 *     the last two digits of the year, or the full year number, respectively. This pattern
 *     will be used to find and initiate an instance of your solution class for a specific
 *     puzzle. For example, the pattern "de.rccookie.aoc._{year}.Solution{day}" would match
 *     the class de.rccookie.aoc._23.Solution24, if the date was the 24th december 2023.</li>
 *     <li>Optional: "token": The file that contains the session token used to authenticate
 *     on adventofcode.com. This token can be found by logging in on the website and then
 *     opening the browser devtools. In the category "storage" you can see the cookies of
 *     the website, which should include the "session" cookie. The default value of this is
 *     "token.txt".</li>
 *     <li>Optional: "showInputStats": Boolean, which can be used to control whether to
 *     print a short input summary at the beginning of execution. Enabled by default.</li>
 * </ul>
 *
 * Implementations of this class must provide a parameter-less constructor, such that an
 * instance of the class can be created on demand.
 *
 * @author RcCookie
 */
public abstract class Solution {

    /**
     * The raw input string.
     */
    protected String input;
    /**
     * All characters from the input string (including newlines).
     */
    protected char[] chars;
    /**
     * A mutable list containing all characters from the input string (including newlines).
     */
    protected List<Character> charList;
    /**
     * A {@link ListStream} that is buffered (can be queried multiple times) containing all
     * the lines from the input string (without newlines).
     */
    protected ListStream<String> lines;
    /**
     * All the lines from the input string (without newlines).
     */
    protected String[] linesArr;
    /**
     * All the lines from the input string as char arrays (without newlines).
     */
    protected char[][] charTable;


    /**
     * Loads common things the solution may need for both of the tasks.
     * Called before task1() / task2() is executed.
     * <p>The default implementation does nothing.</p>
     */
    public void load() { }

    /**
     * Computes the solution to the first task of this puzzle, using the input data
     * exposed by the class.
     *
     * @return The solution to the puzzle. Can be any value. If an instance of {@link Optional}
     *         will be passed, it will be extracted (with <code>null</code> being used if not
     *         present). Unless the result is <code>null</code> or a blank string, the result
     *         can then be submitted.
     */
    public abstract Object task1();

    /**
     * Computes the solution to the second task of this puzzle, using the input data
     * exposed by the class.
     * <p>This method should only be overridden if task 1 is completed, because once overridden
     * task 2 will be executed instead of task 1. The default implementation does nothing and
     * returns <code>null</code>.</p>
     *
     * @return The solution to the puzzle. Can be any value. If an instance of {@link Optional}
     *         will be passed, it will be extracted (with <code>null</code> being used if not
     *         present). Unless the result is <code>null</code> or a blank string, the result
     *         can then be submitted.
     */
    public Object task2() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2]; // [2] is the calling frame: [0] = getStackTrace(), [1] = task2()
        if(caller.getClassName().equals(Solution.class.getName()))
            throw new NotImplemented();
        return null;
    }


    /**
     * Utility method which computes the sum over all lines, using the given function
     * to compute a number for a given input line.
     *
     * @param lineFunction A function which maps a value to each line, then those values will
     *                     be summed up
     * @return The sum of all return values of the function after being applied to each line,
     *         in proper order
     */
    protected long sum(ToLongFunction<String> lineFunction) {
        return lines.mapToLong(lineFunction).sum();
    }

    /**
     * Returns a buffered list stream over the result of splitting the input string
     * around the given regular expression (analogous to {@link String#split(String)}).
     *
     * @param regex The regular expression to split the string around
     * @return A list stream over the split parts of the input string
     */
    protected ListStream<String> split(String regex) {
        return ListStream.of(input.split(regex)).useAsList();
    }

    /**
     * Returns the input lines as char arrays, with an additional line before, after,
     * and a char before and after each line ("around the outside") of the specified
     * padding character. If some lines are shorter than others, they will receive
     * additional padding to match the length of the other lines. Thus, the returned
     * array will have the length <code>charTable.length + 2</code> and all lines will
     * have the length <code>max(i: charTable[i].length) + 2</code>.
     *
     * @param padding The char to pad all lines with
     * @return The input lines as char arrays padded to have equal length with additional
     *         padding in front, behind, above and below
     */
    protected char[][] charTable(char padding) {
        int maxLength = Mathf.max(charTable, l -> l.length);
        char[][] table = new char[charTable.length][maxLength + 2];
        Arrays.fill(table[0], padding);
        Arrays.fill(table[table.length-1], padding);
        for(int i=0; i<charTable.length; i++) {
            table[i+1][0] = padding;
            System.arraycopy(charTable[i], 0, table[i+1], 1, charTable[i].length);
            Arrays.fill(table[i+1], charTable[i].length + 1, table[i+1].length, padding);
        }
        return table;
    }

    /**
     * Return some statistics about the input data into the console. This function
     * may be modified to print custom statistics.
     */
    @SuppressWarnings("DataFlowIssue")
    protected String getInputStats() {
        IntSummaryStatistics lengths = lines.mapToInt(String::length).filter(i -> i != 0).summaryStatistics();
        return "Input statistics: Lines: "+linesArr.length
               +" | Chars: "+chars.length
               +" | Blank lines: "+lines.filter(String::isBlank).count()
               +" | Line lengths: "+lengths.getMin()+(lengths.getMin() == lengths.getMax() ? "" : " - "+lengths.getMax());
    }


    /**
     * Initialize the other inputs based on {@link #input}.
     */
    private void initInput() {
        chars = input.toCharArray();
        charList = new ArrayList<>(input.chars().mapToObj(c -> (char) c).toList());
        lines = ListStream.of(input.lines()).useAsList();
        linesArr = lines.toArray(String[]::new);
        //noinspection DataFlowIssue
        charTable = lines.map(String::toCharArray).toArray(char[][]::new);
    }


    /**
     * Runs a task using the given Solution implementation and returns the result.
     *
     * @param type The solution class that should be used to solve the puzzle
     * @param task 1 or 2 to do that specific task, for any other value task 2 will be executed
     *             iff overridden, otherwise task 1
     * @param day If positive the day of month of the puzzle, otherwise the current date
     * @param year If positive the year of the puzzle, otherwise the current year
     * @param token Session token for adventofcode.com
     * @param exampleInput Whether to use the example input instead of the real input
     * @param inputStats Whether to print input stats
     * @return The result computed by the solution class (may be wrong)
     * @throws InvalidInputException If some input given is invalid
     */
    private static String run(Class<? extends Solution> type, int task, int day, int year, String token, boolean exampleInput, boolean inputStats) throws InvalidInputException {

        // Validate token syntax (don't need a token for example input - not for input download, and won't submit)
        if(!exampleInput && token.length() != 128)
            throw new InvalidInputException("Invalid token, expected 128 characters");

        // Get date of puzzle if not already given
        if(day <= 0)
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if(year <= 0)
            year = Calendar.getInstance().get(Calendar.YEAR);

        // Create instance of solution
        Solution solution;
        try {
            Constructor<? extends Solution> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            solution = ctor.newInstance();
        } catch(NoSuchMethodException e) {
            throw new InvalidInputException(type.getName()+" does not have a parameter-less constructor. Please declare one.", e);
        } catch(InstantiationException e) {
            throw new InvalidInputException(type.getName()+" is not instantiatable, possibly because it is an abstract class or an interface.", e);
        } catch(InvocationTargetException e) {
            throw Utils.rethrow(e.getCause());
        } catch(IllegalAccessException e) {
            throw new AssertionError();
        }

        // In the background, find which parts of the puzzle were already solved and get their solutions
        Wrapper<String[]> solutions = new Wrapper<>(null);
        int _day = day, _year = year;
        new Thread(() -> {
            String[] s = getSolutions(_day, _year, token);
            synchronized(solutions) {
                solutions.value = s;
                solutions.notifyAll();
            }
        }, "Solution fetcher").start();

        // Read input file or fetch from website and store
        if(exampleInput)
            solution.input = getExampleInput(day, year, Path.of("input", year+"", "examples", day+".txt"));
        else solution.input = getInput(day, year, token, Path.of("input", year+"", day+".txt"));

        // Initialize other fields
        solution.initInput();
        if(inputStats)
            Console.log(solution.getInputStats());

        Console.log("Running puzzle {}{}", day, year != Calendar.getInstance().get(Calendar.YEAR) ? " from year "+year : "");

        // Give the solution a chance to do some preparation
        solution.load();

        // Actually execute the task
        Object resultObj;
        Stopwatch watch = new Stopwatch().start();
        if(task < 1 || task > 2) {
            // We don't know which one, so we execute task2(). If we get a NotImplemented error,
            // we know it cannot have been overridden by the user. Thus, then simply execute task1().
            try {
                resultObj = solution.task2();
                task = 2;
            } catch(NotImplemented n) {
                watch.restart();
                resultObj = solution.task1();
                task = 1;
            }
        }
        else if(task == 1)
            resultObj = solution.task1();
        else {
            try {
                resultObj = solution.task2();
            } catch(NotImplemented n) {
                // This happens if task2() wasn't overridden but the user specified to run task 2 explicitly
                throw new InvalidInputException("Task 2 is not yet implemented. Override the 'public Object task2() { }' method in "+type.getSimpleName());
            }
        }
        watch.stop();

        // Recursively unpack optionals
        while(resultObj != null) {
            if(resultObj instanceof Optional<?>)
                resultObj = ((Optional<?>) resultObj).orElse(null);
            else if(resultObj instanceof OptionalInt)
                resultObj = ((OptionalInt) resultObj).isPresent() ? ((OptionalInt) resultObj).getAsInt() : null;
            else if(resultObj instanceof OptionalLong)
                resultObj = ((OptionalLong) resultObj).isPresent() ? ((OptionalLong) resultObj).getAsLong() : null;
            else if(resultObj instanceof OptionalDouble)
                resultObj = ((OptionalDouble) resultObj).isPresent() ? ((OptionalDouble) resultObj).getAsDouble() : null;
            else break;
        }

        // Print results
        String result = Objects.toString(resultObj);
        Console.map("Result (task "+task+")", Console.colored(result, Attribute.BOLD()));
        Console.map("Duration", watch.getPassedNanos() / 1000000.0 + "ms");
        if(exampleInput || resultObj == null || result.isBlank())
            // null or blank string won't be the solution, so just exit
            return resultObj == null ? null : result;

        return maybeSubmit(task, day, year, token, solutions, result, t -> run(type, t, _day, _year, token, false, false));
    }

    /**
     * Waits until the solutions wrapper is filled, then prompts the user to submit and submits the result
     * if the user can actually submit for that task. If it's already done, the solution will be compared
     * with the given result and printed. If the task is not yet unlocked, the user will be prompted to
     * confirm, and then the next available task will be executed instead.
     *
     * @param task The puzzle task that the result belongs to
     * @param day The day of the puzzle
     * @param year The year of the puzzle
     * @param token The login token
     * @param solutions Either already filled, or filled later and then notified: An array containing one entry
     *                  per already known solution (because already submitted and correct)
     * @param result The result to maybe submit
     * @param runTask A function that runs the same configuration again, except the given task number
     * @return The result given, or the one computed if executed a second time
     */
    static String maybeSubmit(int task, int day, int year, String token, Wrapper<String[]> solutions, String result, IntFunction<String> runTask) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(solutions) {
            // Wait until we know which tasks are already done
            while(solutions.value == null) try {
                solutions.wait();
            } catch(InterruptedException e) {
                throw Utils.rethrow(e);
            }
        }

        if(solutions.value.length != task - 1) {
            if(solutions.value.length == 0) {
                // Trying to submit solution for a later subtask, but hasn't solved a previous subtask yet.
                // We can just run the other task instead.
                int todo = solutions.value.length + 1;
                Console.log("Cannot submit solution for task {} before task {} is completed.", task, todo);
                if(Console.input("Execute task {} instead? >", todo).isBlank())
                    return runTask.apply(todo);
            }
            else {
                // The correct answer has already been submitted, now we can compare
                // ourselves with the correct answer
                if(solutions.value[task - 1].equals(result))
                    Console.log("That is the correct answer!");
                else Console.log("That is not the correct answer, the expected answer is {}.", Console.colored(solutions.value[task - 1], Attribute.ITALIC()));
                Console.log("(You have already solved this puzzle before)");
            }
            return result;
        }

        // Ask whether the user want's to submit this answer
        if(!"".equals(Console.input("Submit? >")))
            return result;

        // Send the data, which gets HTML as response
        Console.log("Submitting answer...");
        String info = submit(day, year, task, token, result);
        if(info.length() > 1000)
            throw new InvalidInputException("Invalid session token");

        // Filter out readable text and put the first sentence on an extra line
        info = info.substring(0, info.contains(day + "!") ? info.indexOf(day + "!") + (day + "!").length() : info.indexOf("[") - 1)
                .replaceAll("\\s+", " ")
                .replace(" ,", ",")
                .replace(" .", ".")
                .replaceFirst("([.!?])\\s*", "$1\n");
        Console.log(info);

        return result;
    }

    /**
     * Runs a task using the given Solution implementation and returns the result.
     *
     * @param classPattern The name pattern for the solution classes, which should be used to find
     *                     the actual solution class for the date selected
     * @param task 1 or 2 to do that specific task, for any other value task 2 will be executed
     *             iff overridden, otherwise task 1
     * @param day If positive the day of month of the puzzle, otherwise the current date
     * @param year If positive the year of the puzzle, otherwise the current year
     * @param token Session token for adventofcode.com
     * @param exampleInput Whether to use the example input instead of the real input
     * @param inputStats Whether to print input stats
     * @return The result computed by the solution class (may be wrong)
     * @throws InvalidInputException If some input given is invalid
     */
    private static String run(String classPattern, int task, int day, int year, String token, boolean exampleInput, boolean inputStats) throws InvalidInputException {
        // Get date if not given
        if(day <= 0)
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if(year <= 0)
            year = Calendar.getInstance().get(Calendar.YEAR);

        // Build concrete class name from pattern
        String className = classPattern.replace("{day}", ""+day)
                                       .replace("{0_day}", String.format("%02d", day))
                                       .replace("{year}", String.format("%02d", year % 100))
                                       .replace("{full_year}", ""+year);

        Class<? extends Solution> type;
        try {
            //noinspection unchecked
            type = (Class<? extends Solution>) Class.forName(className);
        } catch(ClassNotFoundException e) {
            throw new InvalidInputException("Could not find solution class '"+className+"'", e);
        }
        if(!Solution.class.isAssignableFrom(type))
            throw new InvalidInputException("Your solution class must extend "+Solution.class.getName()+", "+type.getName()+" does not extend it");

        // Run with the found class
        return run(type, task, day, year, token, exampleInput, inputStats);
    }

    /**
     * Loads or reads the cache for the puzzle input for a specific puzzle
     * and for a specific user.
     *
     * @param day The day of the puzzle to get the input for
     * @param year The year of the puzzle to get the input for
     * @param token The token of the user to get the input for
     * @param cacheFile The file to use as cache. May or may not already be present
     * @return The input for that puzzle for that user
     */
    public static String getInput(int day, int year, String token, Path cacheFile) {
        try {
            // Keep a file with the hashed session token in the directory of the input files. If the
            // token changes, we have to reload the input files as the user may have changed.
            // Hash the token to prevent a user accidentally publishing their token to the public
            // by uploading the input folder.
            Path userFile = cacheFile.resolveSibling("user");
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes());
            if(Files.exists(userFile) && !Arrays.equals(Files.readAllBytes(userFile), hash)) {
                // Delete all files in directory
                try(var files = Files.list(userFile.resolve(".."))) {
                    for(Path file : Utils.iterate(files.filter(Files::isRegularFile)))
                        Files.delete(file);
                }
            }
            Files.createDirectories(cacheFile.resolve(".."));
            Files.write(userFile, hash);

            // Does the input cache (still) exist?
            if(Files.exists(cacheFile))
                return Files.readString(cacheFile);
            Console.log("Fetching input...");
            String input = HttpRequest.get("https://adventofcode.com/"+year+"/day/"+day+"/input")
                    .addCookie("session", token)
                    .send().text();
            if(input.startsWith("Puzzle inputs differ per user"))
                throw new InvalidInputException("Invalid token, cannot receive input data");
            // Store the input, so we don't have to load it every time
            Files.createDirectories(cacheFile.resolve(".."));
            Files.writeString(cacheFile, input);
            return input;
        } catch(IOException e) {
            throw Utils.rethrow(e);
        } catch(NoSuchAlgorithmException e) {
            throw new InvalidInputException("Why is SHA-256 not available on your system???", e);
        }
    }

    /**
     * Loads or reads the cache for the example puzzle input for a specific
     * puzzle. The example is detected at best effort, but may not actually be the
     * example if there are multiple code blocks.
     *
     * @param day The day of the puzzle to get the example input for
     * @param year The year of the puzzle to get the example input for
     * @param cacheFile The file to use as cache. May or may not already be present
     * @return The example input for that puzzle
     */
    public static String getExampleInput(int day, int year, Path cacheFile) {
        try {
            // No need to check for specific user - same for everyone

            // Does the input cache (still) exist?
            if(Files.exists(cacheFile))
                return Files.readString(cacheFile);

            Console.log("Fetching example input...");
            Node article = HttpRequest.get("https://adventofcode.com/" + year + "/day/" + day)
                    .send().body()
                    .xml(XML.HTML | XML.PRESERVE_WHITESPACES)
                    .getElementByTag("article");

            int i = 0;
            while(!article.child(i).text().contains("(your puzzle input)")) i++;
            i++;
            while(i < article.children.size() && !article.child(i).tag.equals("pre")) i++;

            String input = (i < article.children.size() ? article.child(i) : article.getElementByTag("pre")).text();

            // Store the input, so we don't have to load it every time
            Files.createDirectories(cacheFile.resolve(".."));
            Files.writeString(cacheFile, input);
            return input;
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
    }

    /**
     * Fetches the solutions to the puzzle which have already been revealed (because the user
     * has already solved them). The returned array has one element per solved puzzle part. If
     * nothing has been solved yet, or the puzzle is not yet unlocked, an empty array will be
     * returned.
     *
     * @param day The day of the puzzle to get the solutions for
     * @param year The year of the puzzle to get the solutions for
     * @param token The session token used for authentication on adventofcode.com
     * @return One element per known solution
     */
    public static String[] getSolutions(int day, int year, String token) {
        return HttpRequest.get("https://adventofcode.com/"+year+"/day/"+day)
                .addCookie("session", token)
                .send()
                .html()
                .getElementsByTag("p")
                .filter(p -> p.text().contains("Your puzzle answer was"))
                .map(p -> p.getElementByTag("code").text())
                .toArray(String[]::new);
    }

    /**
     * Attempts to submit a specific solution for a specific puzzle and returns the response text.
     *
     * @param day The day of the puzzle
     * @param year The year of the puzzle
     * @param task 1 for the first subtask, 2 for the second
     * @param token The session token used for authentication on adventofcode.com
     * @param answer The answer to submit
     * @return A message explaining whether the result was correct
     */
    public static String submit(int day, int year, @Range(from = 1, to = 2) int task, String token, String answer) {
        // The response is an HTML page
        return HttpRequest.post("https://adventofcode.com/"+year+"/day/"+day+"/answer")
                .addCookie("session", token)
                .setContentType(ContentType.URL_ENCODED)
                .setData("level="+task+"&answer="+URLEncoder.encode(answer, StandardCharsets.UTF_8)) // Yes, the data is sent in the body as URL encoded
                .send()
                .html()
                .getElementByTag("article")
                .text();
    }

    /**
     * Runs the solution for the puzzle of this day, running task 2 iff task2() was
     * overridden, otherwise task 1.
     *
     * @param type The solution class to use to solve the puzzle
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(Class<? extends Solution> type) throws InvalidInputException {
        return run(type, -1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of this day.
     *
     * @param type The solution class to use to solve the puzzle
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(Class<? extends Solution> type, int task) throws InvalidInputException {
        return run(type, task, -1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific day of this year.
     *
     * @param type The solution class to use to solve the puzzle
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(Class<? extends Solution> type, int task, int day) throws InvalidInputException {
        return run(type, task, day, -1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific date.
     *
     * @param type The solution class to use to solve the puzzle
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @param year The year of the puzzle to solve
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(Class<? extends Solution> type, int task, int day, int year) throws InvalidInputException {
        return run(type, task, day, year, false);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific date.
     *
     * @param type The solution class to use to solve the puzzle
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @param year The year of the puzzle to solve
     * @param exampleInput Whether to use the example input instead of the real input
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(Class<? extends Solution> type, int task, int day, int year, boolean exampleInput) throws InvalidInputException {
        Config config = Config.read("config.json");
        return run(type, task, day, year, config.tokenValue(), exampleInput, config.showInputStats());
    }

    /**
     * Runs the solution for the puzzle of this day, running task 2 iff task2() was
     * overridden, otherwise task 1.
     *
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run() throws InvalidInputException {
        return run(-1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of this day.
     *
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(int task) throws InvalidInputException {
        return run(task, -1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific day of this year.
     *
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(int task, int day) throws InvalidInputException {
        return run(day, day, -1);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific date.
     *
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @param year The year of the puzzle to solve
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(int task, int day, int year) throws InvalidInputException {
        return run(task, day, year, false);
    }

    /**
     * Runs the specified task of the solution for the puzzle of a specific date.
     *
     * @param task 1 or 2 to run that specific part of the puzzle, otherwise find out automatically
     * @param day The day of the puzzle to solve
     * @param year The year of the puzzle to solve
     * @param exampleInput Whether to use the example input instead of the real input
     * @return The result for one part of the puzzle
     * @throws InvalidInputException If some input in the config file is invalid
     */
    public static String run(int task, int day, int year, boolean exampleInput) throws InvalidInputException {
        Config config = Config.read("config.json");
        return run(config.classPattern(), task, day, year, config.tokenValue(), exampleInput, config.showInputStats());
    }


    /**
     * Runs a puzzle solution for a date. If not differently specified, this method does
     * exactly the same as {@link #run()}; the date of the puzzle is today's date and
     * the pattern from the config file will be used to find the solution class to use
     * to solve the puzzle, of which the task number will be automatically detected,
     * depending on whether task2() was overridden.
     * <p>Many parameters from the config file or other parameters can be overridden
     * using command line parameters, use --help to display a complete list.</p>
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.addDefaults();
        parser.addOption('t', "task", true, "Specify a specific task that should be ran, rather than running task 2 iff implemented otherwise task 1");
        parser.addOption('d', "day", true, "Specify a specific day of month whose task should be executed, rather than today's task");
        parser.addOption('y', "year", true, "Specify a specific year (yy or yyyy) whose task should be executed, rather than running this year's tasks");
        parser.addOption('c', "config", true, "Path to config file, default is config.json");
        parser.addOption(null, "token", true, "Overrides config; access token used to authenticate on adventofcode.com");
        parser.addOption(null, "cls", true, "Overrides config; fully qualified name pattern of your solution class, where {day}, {0_day}, {year} and {full_year} will be replaced with the day of month, the day of month padded with a 0 if needed, the last two digits of the year or the full year number, respectively.");
        parser.addOption('s', "inputStats", true, "Overrides config; boolean value ('true' for true) whether to print input stats before execution, default is true");
        parser.addOption('x', "example", false, "Use example input instead of real input. (Note that the detection for example input may not always be correct)");
        Options options = parser.parse(args);

        try {
            // Load config file only on demand; if we never use it we don't need to complain about errors
            Config config = null;
            String configPath = options.getOr("config", "config.json");

            String token;
            String classPattern;
            boolean inputStats;

            if(!options.is("token")) {
                config = Config.read(configPath);
                token = config.tokenValue();
            }
            else token = options.get("token");

            if(!options.is("cls")) {
                if(config == null)
                    config = Config.read(configPath);
                classPattern = config.classPattern();
            }
            else classPattern = options.get("cls");

            if(!options.is("inputStats")) {
                if(config == null)
                    config = Config.read(configPath);
                inputStats = config.showInputStats();
            }
            else inputStats = options.get("inputStats").equalsIgnoreCase("true");

            int year = options.getIntOr("year", -1);
            if(year >= 0 && year < 100)
                year += 2000;

            run(classPattern, options.getIntOr("task", -1), options.getIntOr("day", -1), year, token, options.is("example"), inputStats);
        } catch(InvalidInputException e) {
            Console.error(e.getMessage());
        }
    }


    /**
     * Control flow exception. Used to indicate that the method wasn't ever overridden.
     * This class is an error rather than an exception to prevent it from being caught
     * accidentally by "catch(Exception e) { }" statements.
     */
    private static final class NotImplemented extends Error {
        private NotImplemented() {
            // No stacktrace needed -> faster
            super(null, null, false, false);
        }
    }

    /**
     * Represents the config file.
     */
    private record Config(@Default(value = "token.txt", string = true) Path token,
                          String classPattern,
                          @Default("true") boolean showInputStats) {
        @NotNull
        private static Config read(String path) {
            try {
                return Json.load("config.json").as(Config.class);
            } catch(Exception e) {
                throw new InvalidInputException("Failed to parse "+path + (e.getMessage() != null ? ": "+e.getMessage() : "")+"\nCheck the README.md file to get detail on the structure of the config file.", e);
            }
        }

        public String tokenValue() {
            try {
                return Files.readString(token()).trim();
            } catch(IOException e) {
                throw new InvalidInputException("Could not read '"+token()+"': "+e.getMessage(), e);
            }
        }

        @Override
        @NotNull
        public String classPattern() {
            if(classPattern == null)
                throw new InvalidInputException("'classPattern' field missing in config.json, which should be the fully qualified name pattern of your solution classes. See README.md for more info on patterns.");
            return classPattern;
        }
    }

    /**
     * Thrown to indicate that the supplied input (parameters or config file) are
     * invalid.
     */
    public static final class InvalidInputException extends RuntimeException {
        public InvalidInputException() {
            super();
        }

        public InvalidInputException(String message) {
            super(message);
        }

        public InvalidInputException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidInputException(Throwable cause) {
            super(cause);
        }

        public InvalidInputException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
