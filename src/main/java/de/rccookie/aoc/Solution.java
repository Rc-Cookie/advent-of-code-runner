package de.rccookie.aoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.json.Default;
import de.rccookie.json.Json;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.math.Mathf;
import de.rccookie.math.constInt2;
import de.rccookie.math.int2;
import de.rccookie.math.int3;
import de.rccookie.math.intN;
import de.rccookie.util.ArgsParser;
import de.rccookie.util.ArgumentOutOfRangeException;
import de.rccookie.util.Console;
import de.rccookie.util.ListStream;
import de.rccookie.util.Options;
import de.rccookie.util.Stopwatch;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;
import de.rccookie.util.text.Alignment;
import de.rccookie.util.text.TableRenderer;
import de.rccookie.xml.Node;
import de.rccookie.xml.XML;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.MagicConstant;
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
 *     <li>Optional: "inputStats": Boolean, which can be used to control whether to
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
     * The time zone that is being used for puzzle releases.
     */
    static final ZoneId TIMEZONE = ZoneId.of("America/Toronto");


    /**
     * The vector constants left, right, up and down.
     */
    protected static final constInt2[] ADJ4 = { new constInt2(-1,0), int2.X, new constInt2(0,-1), int2.Y };
    /**
     * The vector constants top left, top right, bottom left and bottom right.
     */
    protected static final constInt2[] DIAGONALS = { int2.minusOne, new constInt2(1,-1), new constInt2(-1,1), int2.one };
    /**
     * The vector constants between [-1,-1] and [1,1] (inclusive), exluding [0,0].
     */
    protected static final constInt2[] ADJ8 = {
            int2.minusOne, new constInt2(0,-1), new constInt2(1,-1),
            new constInt2(-1,0), int2.X,
            new constInt2(-1,1), int2.Y, int2.one
    };
    /**
     * A vector constant describing the unit vector pointing to the left.
     */
    protected static final constInt2 LEFT = new constInt2(-1,0);
    /**
     * A vector constant describing the unit vector pointing to the right.
     */
    protected static final constInt2 RIGHT = constInt2.X;
    /**
     * A vector constant describing the unit vector pointing up.
     */
    protected static final constInt2 UP = new constInt2(0,-1);
    /**
     * A vector constant describing the unit vector pointing down.
     */
    protected static final constInt2 DOWN = constInt2.Y;

    /**
     * The pattern used to split the input into sections; matches one or more whitespaces with
     * only spaces in the lines.
     */
    private static final Pattern SECTION_DELIMITER = Pattern.compile("\\n\\s*\\n");


    //#region Input

    /**
     * The raw input data, hidden from subclasses such that implementations
     * can't modify it.
     */
    private byte[] originalInput;
    /**
     * The raw input string.
     */
    protected String input;
    /**
     * The raw input data.
     */
    protected byte[] bytes;
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
     * The input string split around blank lines.
     */
    protected String[] sections;
    /**
     * All the lines from the input string as char arrays (without newlines).
     */
    protected char[][] charTable;
    /**
     * The dimensions of {@link #charTable}. If some lines are
     * longer than others, the maximum width is used.
     */
    protected constInt2 size;
    /**
     * A {@link Grid} instance containing {@link #charTable}; a utility
     * class with many helpful methods for grid-formed Advent of Code input.
     */
    protected Grid grid;


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
     * Utility method which computes the sum over all lines from a specific section of the input,
     * using the given function to compute a number for a given input line. Sections are separated
     * by blank lines.
     *
     * @param section The index of the section to evaluate the lines of
     * @param lineFunction A function which maps a value to each line, then those values will
     *                     be summed up
     * @return The sum of all return values of the function after being applied to each line,
     *         in proper order
     */
    protected long sum(int section, ToLongFunction<String> lineFunction) {
        return lines(section).mapToLong(lineFunction).sum();
    }

    /**
     * Evaluates the given predicate once for every element in the iterable and
     * returns the number of times it returned <code>true</code>.
     *
     * @param data The data to evaluate
     * @param filter The filter to test for every data entry
     * @return The number of times the filter evaluated to <code>true</code>
     */
    public <T> int count(Iterable<? extends T> data, Predicate<? super T> filter) {
        return Mathf.sum(data, t -> filter.test(t) ? 1 : 0);
    }

    /**
     * Evaluates the given predicate once for every element in the array and
     * returns the number of times it returned <code>true</code>.
     *
     * @param data The data to evaluate
     * @param filter The filter to test for every data entry
     * @return The number of times the filter evaluated to <code>true</code>
     */
    public <T> int count(T[] data, Predicate<? super T> filter) {
        return Mathf.sum(data, t -> filter.test(t) ? 1 : 0);
    }

    /**
     * Returns the sum of applying the given function to all elements from the
     * iterable.
     *
     * @param data The data to evaluate
     * @param counter The function to determine the value of a given object
     * @return The sum of all data's values
     */
    public <T> long sum(Iterable<? extends T> data, ToLongFunction<? super T> counter) {
        return Mathf.sumL(data, counter);
    }

    /**
     * Returns the sum of applying the given function to all elements from the
     * array.
     *
     * @param data The data to evaluate
     * @param counter The function to determine the value of a given object
     * @return The sum of all data's values
     */
    public <T> long sum(T[] data, ToLongFunction<? super T> counter) {
        return Mathf.sumL(data, counter);
    }

    /**
     * Returns a buffered {@link ListStream} containing each line of the given section of
     * the input, where a sections are separated by blank lines.
     *
     * @param section The index of the section of whose lines to obtain
     * @return The lines of that section
     */
    protected ListStream<String> lines(int section) {
        return lines0(section).useAsList();
    }

    private ListStream<String> lines0(int section) {
        return ListStream.of(sections[section].lines());
    }

    /**
     * Returns a buffered list stream over the result of splitting the input string
     * around the given regular expression (analogous to {@link String#split(String)}).
     *
     * @param regex The regular expression to split the string around
     * @return A list stream over the split parts of the input string
     */
    protected ListStream<String> split(@Language("regexp") String regex) {
        return ListStream.of(input.split(regex)).useAsList();
    }

    /**
     * Returns a buffered list stream over the result of splitting a specific section
     * of the input around the given regular expression (analogous to {@link String#split(String)}).
     * Sections are separated by blank lines.
     *
     * @param section Index of the section that is to be split
     * @param regex The regular expression to split the string around
     * @return A list stream over the split parts of the specified input string section
     */
    protected ListStream<String> split(int section, @Language("regexp") String regex) {
        return ListStream.of(sections[section].split(regex)).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line the numbers were extracted into
     * an array using {@link #parseLongs(String)}.
     *
     * @return A list stream over all numbers in the input, grouped by line
     * @see #parseLongs(String)
     */
    protected ListStream<long[]> arrays() {
        return lines.map(Solution::parseLongs).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line of the specified section the numbers
     * were extracted into an array using {@link #parseLongs(String)}.
     *
     * @return A list stream over all numbers in the given section, grouped by line
     * @see #parseLongs(String)
     * @see #sections
     */
    protected ListStream<long[]> arrays(int section) {
        return lines0(section).map(Solution::parseLongs).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line the numbers were extracted into
     * an {@link ArrayList} using {@link #parseLongs(String)}.
     *
     * @return A list stream over all numbers in the input, grouped by line in (mutable) lists
     * @see #parseLongs(String)
     */
    protected ListStream<List<Long>> lists() {
        return lines.map(Solution::parseLongs).map(arr -> {
            List<Long> list = new ArrayList<>(arr.length);
            for(long x : arr)
                list.add(x);
            return list;
        }).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line of the specified section the numbers
     * were extracted into an {@link ArrayList} using {@link #parseLongs(String)}.
     *
     * @return A list stream over all numbers in the given section, grouped by line in (mutable) lists
     * @see #parseLongs(String)
     * @see #sections
     */
    protected ListStream<List<Long>> lists(int section) {
        return lines0(section).map(Solution::parseLongs).map(arr -> {
            List<Long> list = new ArrayList<>(arr.length);
            for(long x : arr)
                list.add(x);
            return list;
        }).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line the numbers were extracted into
     * an {@link int2} using {@link #parseLongs(String)}. Each line must contain exactly 2 numbers.
     *
     * @return A list stream over all 2d vectors in the input
     * @see #parseLongs(String)
     */
    protected ListStream<int2> vecs() {
        return lines.map(Solution::parseInts).map(int2::fromArray).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line of the specified section the numbers
     * were extracted into an {@link int2} using {@link #parseLongs(String)}. Each line must
     * contain exactly 2 numbers.
     *
     * @return A list stream over all 2d vectors in the given section
     * @see #parseLongs(String)
     * @see #sections
     */
    protected ListStream<int2> vecs(int section) {
        return lines0(section).map(Solution::parseInts).map(int2::fromArray).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line the numbers were extracted into
     * an {@link int3} using {@link #parseLongs(String)}. Each line must contain exactly 3 numbers.
     *
     * @return A list stream over all 3d vectors in the input
     * @see #parseLongs(String)
     */
    protected ListStream<int3> vec3s() {
        return lines.map(Solution::parseInts).map(int3::fromArray).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line of the specified section the numbers
     * were extracted into an {@link int3} using {@link #parseLongs(String)}. Each line must
     * contain exactly 3 numbers.
     *
     * @return A list stream over all 3d vectors in the given section
     * @see #parseLongs(String)
     * @see #sections
     */
    protected ListStream<int3> vec3s(int section) {
        return lines0(section).map(Solution::parseInts).map(int3::fromArray).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line the numbers were extracted into
     * an {@link intN} using {@link #parseLongs(String)}. Each line must contain at least 1
     * number.
     *
     * @return A list stream over all vectors in the input, where the dimensions of the vector
     *         may vary from line to line
     * @see #parseLongs(String)
     */
    protected ListStream<intN> vecNs() {
        return lines.map(Solution::parseInts).map(intN::ref).useAsList();
    }

    /**
     * Returns a buffered list stream where from each line of the specified section the numbers
     * were extracted into an {@link intN} using {@link #parseLongs(String)}. Each line must
     * contain at least 1 number.
     *
     * @return A list stream over all vectors in the given section, where the dimensions of the
     *         vector may vary from line to line
     * @see #parseLongs(String)
     * @see #sections
     */
    protected ListStream<intN> vecNs(int section) {
        return lines0(section).map(Solution::parseInts).map(intN::ref).useAsList();
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
        char[][] table = new char[charTable.length + 2][maxLength + 2];
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
     * Returns the character from {@link #charTable} at the given
     * coordinates.
     *
     * @param pos The x and y coordinate of the char to get
     * @return The char at that position
     */
    protected char charAt(constInt2 pos) {
        return charTable[pos.y()][pos.x()];
    }

    /**
     * Returns the character from the given 2D character array at the given
     * coordinates, that is <code>table[pos.y()][pos.x()]</code>.
     *
     * @param pos The x and y coordinate of the char to get
     * @return The char at that position
     */
    protected char charAt(char[][] table, constInt2 pos) {
        return table[pos.y()][pos.x()];
    }

    /**
     * Returns the character from {@link #charTable} at the given
     * coordinates, or the default value if the position would be
     * out of bounds.
     *
     * @param pos The x and y coordinate of the char to get
     * @param defaultValue The value to return if x or y are out of bounds
     * @return The char at the given position, or <code>defaultValue</code>
     */
    protected char charAt(constInt2 pos, char defaultValue) {
        if(pos.x() < 0 || pos.y() < 0 || pos.y() >= charTable.length || pos.x() >= charTable[pos.y()].length)
            return defaultValue;
        return charTable[pos.y()][pos.x()];
    }

    /**
     * Return some statistics about the input data into the console. This function
     * may be modified to print custom statistics.
     */
    @SuppressWarnings("DataFlowIssue")
    protected String getInputStats() {
        IntSummaryStatistics lengths = lines.mapToInt(String::length).filter(i -> i != 0).summaryStatistics();
        return "Input statistics: Lines: "+linesArr.length
               +" | Chars: "+chars.length+" ("+input.chars().filter(c -> !Character.isWhitespace(c)).count()+" non-empty)"
               +" | Blank lines: "+lines.filter(String::isBlank).count()
               +" | Line lengths: "+lengths.getMin()+(lengths.getMin() == lengths.getMax() ? "" : " - "+lengths.getMax());
    }


    /**
     * Initialize the other inputs.
     *
     * @param input The raw input string
     */
    public void initInput(byte[] input) {
        bytes = input;
        this.input = new String(input);
        chars = this.input.toCharArray();
        charList = new ArrayList<>(this.input.chars().mapToObj(c -> (char) c).toList());
        lines = ListStream.of(this.input.lines()).useAsList();
        linesArr = lines.toArray(String[]::new);
        sections = SECTION_DELIMITER.split(this.input);
        //noinspection DataFlowIssue
        charTable = lines.map(String::toCharArray).toArray(char[][]::new);
        size = new constInt2(Mathf.max(linesArr, String::length), linesArr.length);
        grid = new Grid(charTable);
    }


    //#region Static utilities


    /**
     * An array, where at index i is the base-36 value of <code>(char) i</code> (both
     * upper and lower case for each alphabetical letter), 0 elsewhere.
     */
    protected static final int[] CHAR_VALUE = new int[128];
    /**
     * An array, where at index i is the value <code>true</code> iff <code>(char) i</code>
     * is a valid hexadecimal digit (allowing both lowercase and uppercase letters).
     */
    protected static final boolean[] IS_HEX = new boolean[128];
    /**
     * An array, where at index i is the value <code>10^i</code>, large enough to contain
     * all non-overflowing results.
     */
    protected static final long[] POW10 = new long[(""+Long.MAX_VALUE).length()];
    static {
        for(int i=0; i<10; i++) {
            CHAR_VALUE['0' + i] = i;
            IS_HEX['0'+i] = true;
        }
        for(int i=0; i<26; i++) {
            CHAR_VALUE['a' + i] = CHAR_VALUE['A' + i] = 10 + i;
            IS_HEX['a'+i] = IS_HEX['A'+i] = i < 6;
        }
        POW10[0] = 1;
        for(int i=1; i<POW10.length; i++)
            POW10[i] = 10 * POW10[i-1];
    }

    /**
     * Returns one of {@link #LEFT}, {@link #RIGHT}, {@link #UP} or {@link #DOWN} for
     * the respective value of '&lt;', '>', '^' or 'v'.
     *
     * @param dir One of &lt;>^v
     * @return The respective unit direction vector
     */
    protected static constInt2 directionVec(@MagicConstant(intValues = {'<', '>', '^', 'v'}) char dir) {
        return switch(dir) {
            case '<' -> LEFT;
            case '>' -> RIGHT;
            case '^' -> UP;
            case 'v' -> DOWN;
            default -> throw new IllegalArgumentException(Json.escape(dir)+" is not a valid direction (only one of <,>,^,v are allowed)");
        };
    }

    /**
     * Parses a list of integers with arbitrary delimiters (except '-' which is treated as minus symbol
     * when directly in front, but not directly behind a number) into an array. Leading and trailing
     * "stuff" will be ignored.
     *
     * @param str The string to parse the list from
     * @return The list of integers
     */
    @SuppressWarnings("DuplicatedCode")
    public static int[] parseInts(String str) {
        int[] ints = new int[2];
        int intsLen = 0;
        int i = 0, x, len = str.length();
        char c;
        boolean minus;
        outer: while(true) {
            do if(i >= len) break outer;
            while(((c = str.charAt(i++)) < '0' || c > '9') && c != '-');

            //noinspection AssignmentUsedAsCondition
            if(minus = c == '-') {
                if(i >= len) break;
                if((c = str.charAt(i++)) < '0' || c > '9') continue;
            }

            x = c - '0';
            if(i < len) {
                while((c = str.charAt(i)) >= '0' && c <= '9') {
                    x = 10 * x + c - '0';
                    if(++i >= len)
                        break;
                }
            }
            if(ints.length == intsLen)
                ints = Arrays.copyOf(ints, intsLen << 1);
            ints[intsLen++] = minus ? -x : x;
            i++;
        }
        return ints.length == intsLen ? ints : Arrays.copyOf(ints, intsLen);
    }

    /**
     * Parses a list of integers with arbitrary delimiters (except '-' which is treated as minus symbol
     * when directly in front, but not directly behind a number) into an array using 64-bit signed integers.
     * Leading and trailing "stuff" will be ignored.
     *
     * @param str The string to parse the list from
     * @return The list of longs
     */
    @SuppressWarnings("DuplicatedCode")
    public static long[] parseLongs(String str) {
        long[] longs = new long[2];
        int longsLen = 0;
        int i = 0, len = str.length();
        long x;
        char c;
        boolean minus;
        outer: while(true) {
            do if(i >= len) break outer;
            while(((c = str.charAt(i++)) < '0' || c > '9') && c != '-');

            //noinspection AssignmentUsedAsCondition
            if(minus = c == '-') {
                if(i >= len) break;
                if((c = str.charAt(i++)) < '0' || c > '9') continue;
            }

            x = c - '0';
            if(i < len) {
                while((c = str.charAt(i)) >= '0' && c <= '9') {
                    x = 10 * x + c - '0';
                    if(++i >= len)
                        break;
                }
            }
            if(longs.length == longsLen)
                longs = Arrays.copyOf(longs, longsLen << 1);
            longs[longsLen++] = minus ? -x : x;
            i++;
        }
        return longs.length == longsLen ? longs : Arrays.copyOf(longs, longsLen);
    }

    /**
     * Returns <code>10^e</code>, throwing an exception on overflow or negative argument.
     *
     * @param e The exponent to raise to. Must be non-negative and less or equal to 19 (otherwise
     *          an overflow would occur)
     * @return 10 raised to the given power (1 if e is 0)
     */
    public static long pow10(int e) {
        return POW10[e];
    }

    /**
     * Returns <code>floor(log_10(x))</code>. For values less or equal to 0 the result is -1.
     *
     * @param x The value to compute the logarithm base 10 for.
     * @return The greatest y such that pow10(y) <= x, or -1 if there is no such number (i.e. x <= 0)
     */
    public static int log10(long x) {
        for(int i=0; i<POW10.length; i++)
            if(x < POW10[i])
                return i - 1;
        return POW10.length;
    }

    /**
     * Creates a new 2D array that is the transpose of the given 2D array, padded such that all rows
     * have the same length, where missing values are set to the default value of the 2D-array's content
     * type (i.e. <code>null</code> for objects, <code>false</code> for booleans, and <code>0</code>
     * for all other primitives)
     *
     * @param array The array to transpose; not modified. Must be at least a 2D array (could be more)
     * @return A 2-level deep copy of the array, transposed
     * @param <T> An array type
     * @apiNote The signature is not <code>T[][]</code> to allow for primitive arrays. An exception will
     *          occur when passing a 1D array.
     */

    public static <T> T[] transpose(T[] array) {
        return transpose(array, null);
    }

    /**
     * Creates a new 2D array that is the transpose of the given 2D array, padded such that all rows
     * have the same length, where missing values are set to the specified value.
     *
     * @param array The array to transpose; not modified. Must be at least a 2D array (could be more)
     * @param fill The value to set missing cells to, or <code>null</code> for the type's default value
     *             (i.e. <code>null</code> for objects, <code>false</code> for booleans, and <code>0</code>
     *             for all other primitives)
     * @return A 2-level deep copy of the array, transposed
     * @param <T> An array type
     * @apiNote The signature is not <code>T[][]</code> to allow for primitive arrays. An exception will
     *          occur when passing a 1D array.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] transpose(T[] array, Object fill) {
        int width = Mathf.max(array, Array::getLength);
        int height = array.length;

        T[] transpose = (T[]) Array.newInstance(array.getClass().getComponentType().getComponentType(), width, height);
        for(int y=0; y<height; y++) {
            int l = Array.getLength(array[y]);
            for(int x=0; x<l; x++)
                Array.set(transpose[x], y, Array.get(array[y], x));
            if(fill != null)
                for(int x=l; x<width; x++)
                    Array.set(transpose[x], y, fill);
        }
        return transpose;
    }

    //#endregion


    //#region Execution

    /**
     * Runs all tasks of all days up to (and including) a specific day (only tasks from that year)
     * and displays timing information.
     *
     * @param classPattern The pattern to use to resolve the solution classes
     * @param day The day up to which the puzzle solutions should be executed (inclusive)
     * @param year The year of the puzzles to solve
     * @param token The token used for authentication
     * @param repeats How many times to run each task repeatedly, then average the runtime duration
     * @param checkResults Whether to validate the results of the tasks (if the task has already been solved before)
     * @throws InvalidInputException If some input given is invalid
     */
    private static void runAll(String classPattern, int day, int year, String token, int warmup, int repeats, boolean checkResults) throws InvalidInputException {
        // Validate token syntax
        if(token.length() != 128)
            throw new InvalidInputException("Invalid token, expected 128 characters");

        // Execute at least once
        if(repeats < 1)
            throw new InvalidInputException("Repeat count < 1");
        if(warmup < 0)
            throw new InvalidInputException("Warmup count < 0");

        // Get date of puzzle if not already given
        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        if(day <= 0) {
            if(now.getMonth() != Month.DECEMBER)
                throw new InvalidInputException("Day of month is required; it is not december");
            day = now.getDayOfMonth();
        }
        if(year <= 0)
            year = now.getYear();

        // Create instances of solutions and initialize
        Solution[] solutions = new Solution[day * 2];
        String[] correctSolutions = new String[day * 2];
        int _year = year;
        if(checkResults)
            Console.log("Loading puzzle solutions...");
        // Run in parallel -> can load multiple inputs at once
        Stream.iterate(0, i->i+1).limit(day).parallel().forEach(i ->  {
            Class<? extends Solution> type;
            try {
                type = resolveType(classPattern, i + 1, _year);
            } catch(InvalidInputException e) {
                synchronized(Console.class) {
                    Console.error("Day", i + 1, "not implemented:");
                    Console.error(e.getMessage());
                }
                return;
            }
            solutions[2*i] = createInstance(type);
            solutions[2*i+1] = createInstance(type);

            solutions[2*i].originalInput = solutions[2*i+1].originalInput = getInput(i+1, _year, token, Path.of("input", _year+"", (i+1)+".txt"));

            if(checkResults) {
                String[] correct = getSolutions(i + 1, _year, token);
                System.arraycopy(correct, 0, correctSolutions, 2 * i, correct.length);
            }
        });

        long[] durations = new long[solutions.length];
        boolean allCorrect = true;
        boolean[] correct = new boolean[solutions.length];
        Arrays.fill(correct, true);

        for(int i=0; i<solutions.length; i++) try {
            if(solutions[i] == null) continue;

            Console.log("Running day {} task {}...", i/2 + 1, i%2 + 1);
            Stopwatch watch = new Stopwatch().reset();
            Object resultObj = runTask(solutions[i], i%2 + 1, warmup, repeats, watch);
            durations[i] = watch.getPassedNanos() / repeats;

            if(checkResults && correctSolutions[i] != null) {
                String result = unpackResult(resultObj);
                if(!result.equals(correctSolutions[i])) {
                    Console.error("Incorrect result, expected {} but got {}", correctSolutions[i], result);
                    correct[i] = allCorrect = false;
                }
            }

        } catch(Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        } catch(NotImplemented e) {
            Console.error("Not implemented");
        }

        System.out.println();
        Console.map("Duration"+(repeats>1?" (average of "+repeats+" runs)":""), (Mathf.sumL(durations) / 1000000.0) + "ms");

        // Show the duration of each individual task in a table
        System.out.println(createTable(durations, correct));
        if(!allCorrect)
            Console.log("(*): Puzzle solution was incorrect");
        else if(checkResults)
            Console.log("All results correct");
    }

    /**
     * Runs a specific subtask of the given solution repeatedly.
     *
     * @param solution The solution of which to execute a task
     * @param task The number of the task to execute; 1 or 2
     * @param warmup The number of untimed warmup rounds to run
     * @param repeats The number of timed runs to execute
     * @param watch The stopwatch to be used for timing, should be
     *              paused when calling this method and will be paused
     *              when returning from this method
     * @return The value returned from the task in the last iteration
     */
    private static Object runTask(Solution solution, int task, int warmup, int repeats, Stopwatch watch) {
        Object res = null;
        for(int i = 0; i < warmup + repeats; i++) {
            solution.initInput(solution.originalInput.clone());
            solution.load();
            if(i >= warmup)
                watch.start();

            res = switch(task) {
                case 1 -> solution.task1();
                case 2 -> solution.task2();
                default -> throw new ArgumentOutOfRangeException("No task #"+task);
            };

            if(i >= warmup)
                watch.stop();
        }
        return res;
    }

    /**
     * Creates a table mapping days and tasks to the duration of their solution. If
     * a duration is 0, it will be displayed as "N/A".
     *
     * @param durations The durations, in task order: 1st is day 1 task 1, then day 1 task 2, then day 2 task 1 etc.
     * @return A table renderer to show the given data as a table
     */
    @NotNull
    static TableRenderer createTable(long[] durations, boolean[] correct) {
        TableRenderer table = new TableRenderer();
        table.style(TableRenderer.Style.DOUBLE_LINES_AFTER_LABELS);
        table.horizontalAlignment(Alignment.RIGHT);
        table.columnLabels("Task 1", "Task 2");
        List<String> rowLabels = new ArrayList<>();
        for(int i = 0; i < durations.length / 2; i++) {
            rowLabels.add("Day "+(i+1));
            table.addRow(
                    durations[2 * i] == 0 ? "N/A" : (correct[2*i] ? "" : "(*) ") + String.format(Locale.ROOT, "%.3fms", durations[2 * i] / 1000000.0),
                    durations[2 * i + 1] == 0 ? "N/A" : (correct[2*i+1] ? "" : "(*) ") + String.format(Locale.ROOT, "%.3fms", durations[2*i+1] / 1000000.0)
            );
        }
        table.rowLabels(rowLabels);
        return table;
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
     * @param warmup The number of times to additionally repeat the calculation without measuring
     *               the time
     * @param repeats The number of times to repeat the calculation (for profiling purposes)
     * @param inputStats Whether to print input stats
     * @return The result computed by the solution class (may be wrong)
     * @throws InvalidInputException If some input given is invalid
     */
    private static String run(Class<? extends Solution> type, int task, int day, int year, String token, boolean exampleInput, int warmup, int repeats, boolean inputStats) throws InvalidInputException {

        // Validate token syntax (don't need a token for example input - not for input download, and won't submit)
        if(!exampleInput && token.length() != 128)
            throw new InvalidInputException("Invalid token, expected 128 characters");

        // Execute at least once
        if(repeats < 1)
            throw new InvalidInputException("Repeat count < 1");
        if(warmup < 0)
            throw new Solution.InvalidInputException("Warmup count < 0");

        // Get date of puzzle if not already given
        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        if(day <= 0) {
            if(now.getMonth() != Month.DECEMBER)
                throw new InvalidInputException("Day of month is required; it is not december");
            day = now.getDayOfMonth();
        }
        if(year <= 0)
            year = now.getYear();

        // Create instance of solution
        Solution solution = createInstance(type);

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
            solution.originalInput = getExampleInput(day, year, Path.of("input", year+"", "examples", day+".txt"));
        else solution.originalInput = getInput(day, year, token, Path.of("input", year+"", day+".txt"));

        // Initialize other fields
        solution.initInput(solution.originalInput);
        if(inputStats)
            Console.log(solution.getInputStats());

        Console.log("Running puzzle {}{}", day, year != now.getYear() ? " from year "+year : "");

        // Actually execute the task
        Object resultObj;
        Stopwatch watch = new Stopwatch().reset();
        if(task < 1 || task > 2) {
            // We don't know which one, so we execute task2(). If we get a NotImplemented error,
            // we know it cannot have been overridden by the user. Thus, then simply execute task1().
            try {
                resultObj = runTask(solution, 2, warmup, repeats, watch);
                task = 2;
            } catch(NotImplemented n) {
                resultObj = runTask(solution, 1, warmup, repeats, watch);
                task = 1;
            }
        }
        else if(task == 1) {
            resultObj = runTask(solution, 1, warmup, repeats, watch);
        }
        else {
            try {
                resultObj = runTask(solution, 2, warmup, repeats, watch);
            } catch(NotImplemented n) {
                // This happens if task2() wasn't overridden but the user specified to run task 2 explicitly
                throw new InvalidInputException("Task 2 is not yet implemented. Override the 'public Object task2() { }' method in "+type.getSimpleName());
            }
        }

        // Print results
        String result = unpackResult(resultObj);
        Console.map("Result (task "+task+")", Console.colored(result, Attribute.BOLD()));
        Console.map("Duration"+(repeats>1?" (average of "+repeats+" runs)":""), (watch.getPassedNanos() / repeats / 1000000.0) + "ms");
        if(exampleInput || resultObj == null || result.isBlank())
            // null or blank string won't be the solution, so just exit
            return resultObj == null ? null : result;

        return maybeSubmit(task, day, year, token, solutions, result, t -> run(type, t, _day, _year, token, false, warmup, repeats, false));
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
                    Console.log(Console.colored("That is the correct answer!", Attribute.GREEN_TEXT()));
                else Console.log(Console.colored("That is not the correct answer, the expected answer is", Attribute.RED_TEXT()), Console.colored(solutions.value[task - 1], Attribute.ITALIC(), Attribute.RED_TEXT()) + Console.colored(".", Attribute.RED_TEXT()));
                Console.log(Console.colored("(You have already solved this puzzle before)", Attribute.ITALIC()));
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
        String first = info.lines().findFirst().get();
        Console.log(Console.colored(first, Attribute.BOLD(), first.matches(".*[Tt]hat('s| is) the (right|correct) answer.*") ? Attribute.GREEN_TEXT() : Attribute.RED_TEXT()));
        Console.log(info.lines().skip(1).collect(Collectors.joining("\n")));

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
    private static String run(String classPattern, int task, int day, int year, String token, boolean exampleInput, int warmup, int repeatCount, boolean inputStats) throws InvalidInputException {
        // Get date if not given to resolve class
        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        if(day <= 0) {
            if(now.getMonth() != Month.DECEMBER)
                throw new InvalidInputException("Day of month is required; it is not december");
            day = now.getDayOfMonth();
        }
        if(year <= 0)
            year = now.getYear();

        // Run with the resolved class
        return run(resolveType(classPattern, day, year), task, day, year, token, exampleInput, warmup, repeatCount, inputStats);
    }

    /**
     * Resolves the solution class from a given class pattern for a specific day and year.
     *
     * @param classPattern The pattern name of the class to resolve
     * @param day The day to resolve the class for
     * @param year The year to resolve the class for
     * @return The type of the solution class
     */
    private static Class<? extends Solution> resolveType(String classPattern, int day, int year) {
        // Build concrete class name from pattern
        String className = classPattern.replace("{day}", ""+day)
                .replace("{0_day}", String.format("%02d", day))
                .replace("{year}", String.format("%02d", year % 100))
                .replace("{full_year}", ""+year);

        try {
            //noinspection unchecked
            Class<? extends Solution> type = (Class<? extends Solution>) Class.forName(className);
            // Validate that class actually extends Solution
            if(!Solution.class.isAssignableFrom(type))
                throw new InvalidInputException("Your solution class must extend "+Solution.class.getName()+", "+type.getName()+" does not extend it");
            return type;
        } catch(ClassNotFoundException e) {
            throw new InvalidInputException("Could not find solution class '"+className+"'", e);
        }
    }

    /**
     * Create a new instance of the given type, throwing an exception if there is no parameter-less
     * constructor declared.
     *
     * @param type The class of the solution to instantiate
     * @return A new instance of that type
     */
    private static Solution createInstance(Class<? extends Solution> type) {
        try {
            Constructor<? extends Solution> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch(NoSuchMethodException e) {
            throw new InvalidInputException(type.getName()+" does not have a parameter-less constructor. Please declare one.", e);
        } catch(InstantiationException e) {
            throw new InvalidInputException(type.getName()+" is not instantiatable, possibly because it is an abstract class or an interface.", e);
        } catch(InvocationTargetException e) {
            throw Utils.rethrow(e.getCause());
        } catch(IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    /**
     * Returns the string representation of the given result object.
     *
     * @param result The result to get a string for. Optionals will be automatically
     *               unpacked
     * @return The result as a string
     */
    @NotNull
    private static String unpackResult(Object result) {
        // Recursively unpack optionals
        while(result != null) {
            if(result instanceof Optional<?>)
                result = ((Optional<?>) result).orElse(null);
            else if(result instanceof OptionalInt)
                result = ((OptionalInt) result).isPresent() ? ((OptionalInt) result).getAsInt() : null;
            else if(result instanceof OptionalLong)
                result = ((OptionalLong) result).isPresent() ? ((OptionalLong) result).getAsLong() : null;
            else if(result instanceof OptionalDouble)
                result = ((OptionalDouble) result).isPresent() ? ((OptionalDouble) result).getAsDouble() : null;
            else break;
        }
        return Objects.toString(result);
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
    public static byte[] getInput(int day, int year, String token, Path cacheFile) {
        try {
            checkUserFile(cacheFile.toAbsolutePath().normalize().getParent(), token);

            // Does the input cache (still) exist?
            if(Files.exists(cacheFile))
                return loadCachedInput(cacheFile);

            synchronized(Console.class) {
                Console.log("Fetching input for day {}...", day);
            }
            String input = HttpRequest.get("https://adventofcode.com/"+year+"/day/"+day+"/input")
                    .addCookie("session", token)
                    .send().text();
            if(input.startsWith("Puzzle inputs differ by user"))
                throw new InvalidInputException("Invalid token, cannot receive input data");
            if(input.startsWith("Please don't repeatedly request this endpoint before it unlocks!"))
                throw new InvalidInputException("Puzzle "+day+" is not yet unlocked");
            // Store the input, so we don't have to load it every time
            Files.writeString(cacheFile, input);
            return input.getBytes();
        } catch(IOException e) {
            throw Utils.rethrow(e);
        } catch(NoSuchAlgorithmException e) {
            throw new InvalidInputException("Why is SHA-256 not available on your system???", e);
        }
    }

    private static synchronized void checkUserFile(Path dir, String token) throws IOException, NoSuchAlgorithmException {
        // Keep a file with the hashed session token in the directory of the input files. If the
        // token changes, we have to reload the input files as the user may have changed.
        // Hash the token to prevent a user accidentally publishing their token to the public
        // by uploading the input folder.
        Path userFile = dir.resolve("user");
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes());
        if(Files.exists(userFile) && !Arrays.equals(Files.readAllBytes(userFile), hash)) {
            // Delete all files in directory
            try(var files = Files.list(userFile.toAbsolutePath().normalize().getParent())) {
                for(Path file : Utils.iterate(files.filter(Files::isRegularFile)))
                    Files.delete(file);
            }
        }
        Files.createDirectories(dir);
        Files.write(userFile, hash);
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
    public static byte[] getExampleInput(int day, int year, Path cacheFile) {
        try {
            // No need to check for specific user - same for everyone

            // Does the input cache (still) exist?
            if(Files.exists(cacheFile))
                return loadCachedInput(cacheFile);

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

            System.out.println();
            Console.warn("The following code block was identified as example:");
            System.out.print(Console.colored(input, Attribute.BOLD())); // No printLN because input ends with newline
            Console.warn("If this is incorrect, please paste the correct example input into", cacheFile);

            // Wait a moment for the user to acknowledge in case there follows a lot of output and this will be off the screen
            for(int j=0; j<2; j++) {
                System.out.print(".");
                Thread.sleep(1000);
            }
            System.out.println(".");

            // Store the input, so we don't have to load it every time
            Files.createDirectories(cacheFile.toAbsolutePath().normalize().getParent());
            Files.writeString(cacheFile, input);
            return input.getBytes();
        } catch(IOException|InterruptedException e) {
            throw Utils.rethrow(e);
        }
    }

    /**
     * Loads the given (existing) input file. If the file does not end on a newline or contains \r
     * characters, it will be normalized and the original file overridden. The returned input is always
     * normalized.
     *
     * @param file The file from while to load the input
     * @return The input data, normalized
     */
    private static byte[] loadCachedInput(Path file) throws IOException {
        // If the user pasted some text, he may well have forgotten to include the final newline (which may break his own program).
        // In this case we add the newline back.
        String input = Files.readString(file);
        if(!input.endsWith("\n") || input.contains("\r")) {
            input = input.replace('\r', '\n');
            if(!input.endsWith("\n"))
                input += "\n";
            Files.writeString(file, input);
        }
        return input.getBytes();
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
        return run(type, task, day, year, config.tokenValue(), exampleInput, 0, 1, config.inputStats());
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
        return run(config.classPattern(), task, day, year, config.tokenValue(), exampleInput, 0, 1, config.inputStats());
    }


    /**
     * Creates a java source file which executes all the given solutions once directly after
     * another, without the overhead of reflection and other "nice-to-haves" to produce a single
     * executable which executes all days as fast as possible. The resulting file will contain
     * a main method, and will handle loading input files (but not downloading or normalizing -
     * the files should already be ready to go). It will also measure and log the time of each
     * sub-task individually, together with the respective result.
     *
     * @param solutions The solution (tasks) to include in the benchmark, list may not be empty
     * @param className The class name for the benchmark class. The class will be located in the
     *                  package of the first class in the solutions list.
     * @return The path to the created (or overridden) .java file
     */
    public static Path createBenchmark(List<BenchmarkSolution> solutions, String className) {
        if(solutions.isEmpty())
            throw new IllegalArgumentException("At least one solution is required");

        String prefab;
        try {
            prefab = new String(Solution.class.getResourceAsStream("/Benchmark.java").readAllBytes());
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }

        StringBuilder executions = new StringBuilder();

        for(BenchmarkSolution s : solutions) {
            executions.append("runSolution(new ").append(s.type.getCanonicalName())
                    .append("(), ")
                    .append(s.day)
                    .append(", ")
                    .append(s.year)
                    .append(", ")
                    .append(s.task)
                    .append(");\n        ");
        }

        String pgk = solutions.get(0).type.getPackageName();
        String java = prefab.replace("$package$", pgk).replace("$className$", className).replace("$executions$", executions);

        Path file = Path.of("src/main/java" + (pgk.isBlank() ? "" : "/" + pgk.replace('.', '/'))).resolve(className+".java");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, java);
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
        return file;
    }

    /**
     * Creates a java source file which executes all solutions for the given year found by the
     * specified class pattern once, directly after another, without the overhead of reflection
     * and other "nice-to-haves" to produce a single executable which executes all days as fast
     * as possible. The resulting file will contain a main method, and will handle loading input
     * files (but not downloading or normalizing - the files should already be ready to go). It
     * will also measure and log the time of each sub-task individually, together with the
     * respective result.
     *
     * @param classPattern The class pattern used to find solution classes to include in the benchmark
     * @param year The year to use, will be used to populate the class pattern, or -1 for the
     *             current year
     * @return The path to the created (or overridden) .java file
     */
    public static Path createBenchmark(String classPattern, int year) {
        if(year < 0)
            year = LocalDate.now(TIMEZONE).getYear();

        String prefab;
        try {
            prefab = new String(Solution.class.getResourceAsStream("/YearBenchmark.java").readAllBytes());
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }

        String java = prefab.replace("$className$", "Benchmark").replace("$year$", year+"");

        Class<? extends Solution> anySolution = null;
        for(int day=1; day<=25; day++) {
            try {
                Class<? extends Solution> type = resolveType(classPattern, day, year);
                if(anySolution == null)
                    anySolution = type;
                java = java.replace("$solution"+day+"$", "new "+type.getCanonicalName()+"()");
                Console.log("Found day", day);
            } catch(Exception e) {
                java = java.replace("$solution"+day+"$", "null");
//                Console.warn("Day", day, "not found:", e.toString());
            }
        }

        if(anySolution == null)
            throw new IllegalArgumentException("0 solutions found matching class pattern");
        String pgk = anySolution.getPackageName();
        java = java.replace("$package$", pgk);

        Path file = Path.of("src/main/java" + (pgk.isBlank() ? "" : "/" + pgk.replace('.', '/'))).resolve("Benchmark.java");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, java);
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
        return file;
    }


    /**
     * Creates an args parser with all the parameters in common for java and external program execution.
     *
     * @return A partially configured args parser
     */
    static ArgsParser createGenericArgsParser() {
        ArgsParser parser = new ArgsParser();
        parser.setName("Advent of Code Runner");
        parser.addDefaults();
        parser.setArgsMode(ArgsParser.ArgsMode.NOT_ALLOWED);
        parser.addOption('t', "task", true, "Specify a specific task that should be ran, rather than running task 2 iff task 1 is completed otherwise task 1");
        parser.addOption('d', "day", true, "Specify a specific day of month whose task should be executed, rather than today's task");
        parser.addOption('y', "year", true, "Specify a specific year (yy or yyyy) whose task should be executed, rather than running this year's tasks");
        parser.addOption('c', "config", true, "Path to config file, default is config.json");
        parser.addOption(null, "token", true, "Overrides config; access token used to authenticate on adventofcode.com");
        parser.addOption('s', "inputStats", true, "Overrides config; boolean value ('true' for true) whether to print input stats before execution, default is true");
        parser.addOption('x', "example", false, "Use example input instead of real input. (Note that the detection for example input may not always be correct)");
        parser.addOption('a', "all", false, "Run all tasks up to (including) the tasks of the selected date and measure the computation time");
        parser.addOption(null, "check", false, "Check all results when running all tasks with --all. Ignored when not profiling");
        parser.addOption('r', "repeat", true, "Repeat the execution so many times and take the average time");
        parser.addOption('w', "warmup", true, "Repeat the execution so many times additionally before starting to measure time");
        return parser;
    }

    /**
     * Merges the given options with the config file contents and parses the result
     * into the specific config class. If the file does not exist, it will be created from
     * the given template resource and an exception will be thrown.
     *
     * @param options The command line options overriding config file parameters
     * @param toType The config file type, must be deserializable from json
     * @param defaultConfigResource Path of the resource containing the default config file template for the given config class
     * @return The parsed config class
     */
    static <T> T parseConfig(Options options, Class<T> toType, String defaultConfigResource) {
        Path configFile = Path.of(options.getOr("config", "config.json"));
        // Fix mismatch between cli parameter name and config file field name
        JsonObject jsonObj = (JsonObject) options.toJson();
        if(jsonObj.contains("check"))
            jsonObj.put("checkAll", jsonObj.remove("check"));
        JsonElement json = jsonObj.asElement();
        try {
            if(Files.exists(configFile))
                json = json.merge(Json.load(configFile));
            else {
                try(InputStream defaultConfig = Solution.class.getResourceAsStream(defaultConfigResource)) {
                    //noinspection DataFlowIssue
                    defaultConfig.transferTo(Files.newOutputStream(configFile));
                }
                throw new InvalidInputException(configFile+" not found, template generated. Please adjust the required values.");
            }
        } catch(IOException|UncheckedIOException e) {
            throw new InvalidInputException(e.getMessage(), e);
        }
        return json.as(toType);
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
        ArgsParser parser = createGenericArgsParser();
        parser.setDescription("""
                        Runs your java advent of code solutions.
                        Automatically downloads input or example files and submits your solution.

                        Usage: aoc-run [options]""");
        parser.addOption('p', "classPattern", true, "Overrides config; fully qualified name pattern of your solution class, where {day}, {0_day}, {year} and {full_year} will be replaced with the day of month, the day of month padded with a 0 if needed, the last two digits of the year or the full year number, respectively.");
        parser.addOption(null, "createBenchmark", false, "Create a Benchmark.java file which statically performs runs all solution. Use --year to override the year.");
        Options options = parser.parse(args);

        try {
            Config config = parseConfig(options, Config.class, "/defaultJavaConfig.json");

            if(options.is("createBenchmark")) {
                Console.log("Created", createBenchmark(config.classPattern, config.year));
            }
            else if(config.all)
                runAll(config.classPattern, config.day, config.year, config.tokenValue(), config.warmup, config.repeat, config.checkAll);
            else run(config.classPattern, config.task, config.day, config.year, config.tokenValue(), config.example, config.warmup, config.repeat, config.inputStats);
        } catch(InvalidInputException e) {
            Console.error(e.getMessage());
            if(args.length == 0)
                Console.error("Run --help for more info");
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
                          String tokenValue,
                          String classPattern,
                          @Default("true") boolean inputStats,
                          @Default("false") boolean all,
                          @Default("false") boolean checkAll,
                          @Default("1") int repeat,
                          @Default("0") int warmup,
                          @Default("false") boolean example,
                          @Default("-1") int day,
                          @Default("-1") int year,
                          @Default("-1") int task) {

        @NotNull
        private static Config read(String path) {
            try {
                return Json.load(path).as(Config.class);
            } catch(Exception e) {
                throw new InvalidInputException("Failed to parse "+path + (e.getMessage() != null ? ": "+e.getMessage() : "")+"\nCheck the README.md file to get detail on the structure of the config file.", e);
            }
        }

        @Override
        public String tokenValue() {
            if(tokenValue != null)
                return tokenValue;
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
                throw new InvalidInputException("'classPattern' field missing in config file, which should be the fully qualified name pattern of your solution classes. See README.md for more info on patterns.");
            return classPattern;
        }
    }

    /**
     * Defines a benchmarking target.
     *
     * @param type The solution class to execute. Must have a public, parameterless constructor
     * @param day The day of the puzzle
     * @param year The year of the puzzle
     * @param task 1 or 2, depending on which sub-task to execute
     */
    public record BenchmarkSolution(Class<? extends Solution> type,
                                    @Range(from = 1, to = 25) int day,
                                    int year,
                                    @Range(from = 1, to = 2) int task) {
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

    //#endregion
}
