package de.rccookie.aoc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.graph.Graph;
import de.rccookie.graph.ReadableGraph;
import de.rccookie.graph.SimpleHashGraph;
import de.rccookie.math.Mathf;
import de.rccookie.math.constInt2;
import de.rccookie.math.int2;
import de.rccookie.util.Console;
import de.rccookie.util.IterableIterator;
import de.rccookie.util.ListStream;
import de.rccookie.util.MappingIterator;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A helper class for working with grid-shaped inputs in Advent of Code.
 */
public final class Grid {

    private final char[][] grid;
    /**
     * The width and height of the grid.
     */
    public final constInt2 size;
    /**
     * The dimensions of the grid.
     */
    public final int width, height;
    private long[] marking = null;
    private char padding = 0;
    private int intPadding = -1;

    /**
     * Creates a new grid from the given char grid. Shorter lines will be
     * replaced with longer arrays padded with whitespaces.
     *
     * @param grid The data for the grid
     */
    public Grid(char[][] grid) {
        this.grid = grid;
        size = new constInt2(Mathf.max(grid, l -> l.length), grid.length);
        width = size.x();
        height = size.y();

        for(int i=0; i<grid.length; i++) {
            if(grid[i].length == width) continue;
            char[] padded = Arrays.copyOf(grid[i], width);
            Arrays.fill(padded, grid[i].length, padded.length, ' ');
            grid[i] = padded;
        }
    }

    /**
     * Creates a new grid from the given (multiline) string. Shorter lines will be
     * padded with whitespaces for a consistent width.
     *
     * @param lines The string data to represent in this grid. Line breaks will be
     *              removed when splitting into the rows of the grid, as will empty
     *              lines.
     */
    public Grid(String lines) {
        this(lines.lines().filter(l -> !l.isEmpty()).map(String::toCharArray).toArray(char[][]::new));
    }

    /**
     * Creates a new grid from the given file's contents. Shorter lines will be
     * padded with whitespaces for a consistent width.
     *
     * @param file The file from which to load the grid string. Line breaks will be
     *             removed when splitting into the rows of the grid, as will empty
     *              lines.
     */
    public Grid(Path file) {
        this(loadFile(file));
    }

    /**
     * Constructs a new, empty grid.
     *
     * @param size The size for the grid, component-wise non-negative
     * @param initialValue The initial character to assign to all cells
     */
    public Grid(constInt2 size, char initialValue) {
        this(size.x(), size.y(), initialValue);
    }

    /**
     * Constructs a new, empty grid.
     *
     * @param width The width for the grid, non-negative
     * @param height The height for the grid, non-negative
     * @param initialValue The initial character to assign to all cells
     */
    public Grid(int width, int height, char initialValue) {
        this.width = width;
        this.height = height;
        size = new constInt2(width, height);

        this.grid = new char[height][width];
        for(char[] row : grid)
            Arrays.fill(row, initialValue);
    }

    private static char[][] loadFile(Path file) {
        try(Stream<String> lines = Files.lines(file)) {
            return lines.filter(l -> !l.isEmpty()).map(String::toCharArray).toArray(char[][]::new);
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
    }

    /**
     * Returns the lines of this grid joined by newlines, with marked characters being rendered
     * bold (using ANSI escape sequences).
     *
     * @return The contents of this grid, joined with newlines
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for(int y=0; y<height; y++) {
            for(int x=0; x<width; x++)
                str.append(marked(x,y) ? Console.colored(grid[y][x], Attribute.BOLD(), Attribute.RED_TEXT()) : grid[y][x]);
            if(y+1 != height)
                str.append("\n");
        }
        return str.toString();
    }

    /**
     * Returns {@link #toString()}, but without any ANSI escape sequences marking bold characters.
     *
     * @return The contents of this grid, joined with newlines
     */
    public String toPlainString() {
        return Arrays.stream(grid).map(String::new).collect(Collectors.joining("\n"));
    }

    /**
     * Returns the padding; the char returned when requesting a position out of bounds,
     * as set by {@link #setPadding(char)} (defaults to '\0').
     *
     * @return The current padding
     */
    public char getPadding() {
        return padding;
    }

    /**
     * Returns the int padding; the number returned when requesting a position out of bounds,
     * as set by {@link #setIntPadding(int)} (defaults to -1).
     *
     * @return The current int padding
     */
    public int getIntPadding() {
        return intPadding;
    }

    /**
     * Sets the padding; the char returned when requesting a position out of bounds.
     *
     * @param padding The padding char to use
     */
    public void setPadding(char padding) {
        this.padding = padding;
    }

    /**
     * Sets the int padding; the number returned when requesting a position out of bounds.
     *
     * @param intPadding The padding integer to use
     */
    public void setIntPadding(int intPadding) {
        this.intPadding = intPadding;
    }

    /**
     * Sets the padding; the char/integer returned when requesting a position out of bounds.
     *
     * @param padding The padding char to use
     * @param intPadding The padding integer to use
     */
    public void setPadding(char padding, int intPadding) {
        this.padding = padding;
        this.intPadding = intPadding;
    }

    /**
     * Returns whether the given coordinates represent a valid index in the grid.
     *
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @return <code>true</code> iff {@link #charAtRaw(int,int)} would succeed with
     *         the same coordinates
     */
    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Returns whether the given coordinates represent a valid index in the grid.
     *
     * @param xy The coordinates to check
     * @return <code>true</code> iff {@link #charAtRaw(constInt2)} would succeed with
     *         the same coordinates
     */
    public boolean inside(constInt2 xy) {
        return xy.geq(int2.zero) && xy.less(size);
    }

    /**
     * Returns whether the given coordinates represent an invalid index in the grid.
     *
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @return <code>true</code> iff {@link #charAtRaw(int,int)} would fail with
     *         the same coordinates
     */
    public boolean outside(int x, int y) {
        return x < 0 || y < 0 || x >= width || y >= height;
    }

    /**
     * Returns whether the given coordinates represent an invalid index in the grid.
     *
     * @param xy The coordinates to check
     * @return <code>true</code> iff {@link #charAtRaw(constInt2)} would fail with
     *         the same coordinates
     */
    public boolean outside(constInt2 xy) {
        return !xy.geq(int2.zero) || !xy.less(size);
    }

    /**
     * Returns the char at the given coordinates, throwing an {@link ArrayIndexOutOfBoundsException}
     * for invalid bounds.
     *
     * @param x The x coordinate of the char to read
     * @param y The y coordinate of the char to read
     * @return The character at that position in the grid
     */
    public char charAtRaw(int x, int y) {
        return grid[y][x];
    }

    /**
     * Returns the digit value of the character at the given coordinates, throwing an
     * {@link ArrayIndexOutOfBoundsException} for invalid bounds.
     *
     * @param x The x coordinate of the char to read
     * @param y The y coordinate of the char to read
     * @return The digit at that position in the grid
     * @apiNote The digit value is calculated from <code>charAtRaw(x,y) - '0'</code> and
     *          will produce arbitrary results when applied to a character which is not
     *          an ascii digit.
     */
    public int intAtRaw(int x, int y) {
        return grid[y][x] - '0';
    }

    /**
     * Returns the char at the given coordinates, throwing an {@link ArrayIndexOutOfBoundsException}
     * for invalid bounds.
     *
     * @param xy The coordinates of the char to read
     * @return The character at that position in the grid
     */
    public char charAtRaw(constInt2 xy) {
        return grid[xy.y()][xy.x()];
    }

    /**
     * Returns the digit value of the character at the given coordinates, throwing an
     * {@link ArrayIndexOutOfBoundsException} for invalid bounds.
     *
     * @param xy The coordinates of the char to read
     * @return The digit at that position in the grid
     * @apiNote The digit value is calculated from <code>charAtRaw(xy) - '0'</code> and
     *          will produce arbitrary results when applied to a character which is not
     *          an ascii digit.
     */
    public int intAtRaw(constInt2 xy) {
        return grid[xy.y()][xy.x()] - '0';
    }

    /**
     * Returns the character at the given coordinates in the grid, or the configured
     * padding char ('\0' by default) when out of bounds.
     *
     * @param x The x coordinate of the char to read
     * @param y The y coordinate of the char to read
     * @return The character at that position, or the padding character when out of bounds
     */
    public char charAt(int x, int y) {
        if(x < 0 || y < 0 || x >= width || y >= height)
            return padding;
        return grid[y][x];
    }

    /**
     * Returns the digit value of the character at the given at the given coordinates
     * in the grid, or the configured padding integer (-1 by default) when out of bounds.
     *
     * @param x The x coordinate of the digit to read
     * @param y The y coordinate of the digit to read
     * @return The digit at that position, or the padding integer when out of bounds
     * @apiNote The digit value is calculated from <code>charAtRaw(x,y) - '0'</code> and
     *          will produce arbitrary results when applied to a character which is not
     *          an ascii digit.
     */
    public int intAt(int x, int y) {
        if(x < 0 || y < 0 || x >= width || y >= height)
            return intPadding;
        return grid[y][x] - '0';
    }

    /**
     * Returns the character at the given coordinates in the grid, or the configured
     * padding char ('\0' by default) when out of bounds.
     *
     * @param xy The coordinates of the char to read
     * @return The character at that position, or the padding character when out of bounds
     */
    public char charAt(constInt2 xy) {
        return charAt(xy.x(), xy.y());
    }

    /**
     * Returns the digit value of the character at the given at the given coordinates
     * in the grid, or the configured padding integer (-1 by default) when out of bounds.
     *
     * @param xy The coordinates of the digit to read
     * @return The digit at that position, or the padding integer when out of bounds
     * @apiNote The digit value is calculated from <code>charAtRaw(xy) - '0'</code> and
     *          will produce arbitrary results when applied to a character which is not
     *          an ascii digit.
     */
    public int intAt(constInt2 xy) {
        return intAt(xy.x(), xy.y());
    }

    /**
     * Modifies the contents of the grid by setting the character at the given coordinates
     * to the specified value.
     *
     * @param x The x coordinate where to write the character
     * @param y The y coordinate where to write the character
     * @param c The character to write
     * @return <code>c</code>
     */
    public char set(int x, int y, char c) {
        return grid[y][x] = c;
    }

    /**
     * Modifies the contents of the grid by setting the character at the given coordinates
     * to the specified value.
     *
     * @param xy The coordinates where to write the character
     * @param c The character to write
     * @return <code>c</code>
     */
    public char set(constInt2 xy, char c) {
        return set(xy.x(), xy.y(), c);
    }

    /**
     * Returns the coordinates of the given char in the grid.
     *
     * @param c The character to search for
     * @return The coordinates of the character
     * @throws NullPointerException If the given character is not present in the grid
     */
    @NotNull
    public int2 find(char c) {
        return Objects.requireNonNull(tryFind(c), "'"+c+"' not found in input");
    }

    /**
     * Returns the first coordinates fulfilling the given filter.
     *
     * @param filter The filter to find a truthy position for
     * @return The coordinates where the filter returned <code>true</code>
     * @throws NullPointerException If the filter did not accept any coordinates in
     *                              the grid
     */
    @NotNull
    public int2 find(Predicate<? super int2> filter) {
        return Objects.requireNonNull(tryFind(filter), "No matching position found in input");
    }

    /**
     * Returns the coordinates of the given char in the grid, or <code>null</code> if
     * not present.
     *
     * @param c The character to search for
     * @return The coordinates of the character, or <code>null</code>
     */
    @Nullable
    public int2 tryFind(char c) {
        for(constInt2 p : size.iterateConst())
            if(charAtRaw(p) == c)
                return p.clone();
        return null;
    }

    /**
     * Returns the first coordinates fulfilling the given filter, or <code>null</code>
     * if none match the filter.
     *
     * @param filter The filter to find a truthy position for
     * @return The coordinates where the filter returned <code>true</code>, or <code>null</code>
     */
    @Nullable
    public int2 tryFind(Predicate<? super int2> filter) {
        for(int2 p : size)
            if(filter.test(p))
                return p;
        return null;
    }

    /**
     * Marks the given coordinates in the grid, this can later be read with {@link #marked(constInt2)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param xy The coordinates to mark
     * @return Whether the marking changed, that is, it was previously unmarked
     */
    public boolean mark(constInt2 xy) {
        return mark(xy.x(), xy.y());
    }

    /**
     * Marks the given coordinates in the grid, this can later be read with {@link #marked(constInt2)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param x The x coordinate to mark
     * @param y The y coordinate to mark
     * @return Whether the marking changed, that is, it was previously unmarked
     */
    public boolean mark(int x, int y) {
        if(marking == null)
            marking = new long[(size.product() + 63) / 64];
        return mark(marking, x, y, width);
    }

    private static boolean mark(long[] data, int x, int y, int width) {
        int i = x + width * y;
        return data[i >>> 6] != (data[i >>> 6] |= 1L << (i & 63));
    }

    /**
     * Unmarks the given coordinates in the grid, this can later be read with {@link #marked(constInt2)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param xy The coordinates to unmark
     * @return Whether the marking changed, that is, it was previously marked
     */
    public boolean unmark(constInt2 xy) {
        return unmark(xy.x(), xy.y());
    }

    /**
     * Unmarks the given coordinates in the grid, this can later be read with {@link #marked(int,int)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param x The x coordinate to unmark
     * @param y The y coordinate to unmark
     * @return Whether the marking changed, that is, it was previously marked
     */
    public boolean unmark(int x, int y) {
        if(marking == null)
            return false;
        int i = x + width * y;
        return marking[i >>> 6] != (marking[i >>> 6] &= ~(1L << (i & 63)));
    }

    /**
     * Toggles the marking state of the given coordinates in the grid. This may be faster and more
     * convenient than a combination of {@link #marked(constInt2)} and {@link #mark(constInt2, boolean)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param xy The coordinates to toggle the marked state of
     * @return Whether the coordinates are now marked
     */
    public boolean toggleMarking(constInt2 xy) {
        return toggleMarking(xy.x(), xy.y());
    }

    /**
     * Toggles the marking state of the given coordinates in the grid. This may be faster and more
     * convenient than a combination of {@link #marked(int, int)} and {@link #mark(int, int, boolean)}.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates may have an
     * arbitrary effect.
     *
     * @param x The x coordinate to toggle the marked state of
     * @param y The y coordinate to toggle the marked state of
     * @return Whether the coordinates are now marked
     */
    public boolean toggleMarking(int x, int y) {
        if(marking == null)
            marking = new long[(size.product() + 63) / 64];
        int i = x + width * y;
        long m = 1L << (i & 63);
        return ((marking[i >>> 6] ^= m) & m) != 0;
    }

    /**
     * Marks or unmarks the given coordinates in the grid, depending on the given value.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates
     * may have an arbitrary effect.
     *
     * @param xy The coordinates to mark
     * @param marked Whether to set the coordinates to be marked or unmarked
     * @return Whether the marking changed, that is, it was previously <code>!marked</code>
     */
    public boolean mark(constInt2 xy, boolean marked) {
        return mark(xy.x(), xy.y(), marked);
    }

    /**
     * Marks or unmarks the given coordinates in the grid, depending on the given value.
     * Marking is only allowed for valid coordinates in grid, writing other coordinates
     * may have an arbitrary effect.
     *
     * @param x The x coordinate to mark
     * @param y The y coordinate to mark
     * @param marked Whether to set the coordinates to be marked or unmarked
     * @return Whether the marking changed, that is, it was previously <code>!marked</code>
     */
    public boolean mark(int x, int y, boolean marked) {
        if(marked)
            return mark(x,y);
        else return unmark(x,y);
    }

    /**
     * Returns whether the given coordinates in the grid are marked, e.g. by {@link #mark(constInt2)}.
     * Marking has no internal function, it is intended to be used e.g. with breath first search or
     * similar. Note that only valid coordinates are allowed to be marked, reading or writing other
     * coordinates may result in arbitrary results.
     *
     * @param xy The coordinates to determine the marked state of
     * @return Whether the given coordinates are marked
     */
    public boolean marked(constInt2 xy) {
        return marked(xy.x(), xy.y());
    }

    /**
     * Returns whether the given coordinates in the grid are marked, e.g. by {@link #mark(int, int)}.
     * Marking has no internal function, it is intended to be used e.g. with breath first search or
     * similar. Note that only valid coordinates are allowed to be marked, reading or writing other
     * coordinates may result in arbitrary results.
     *
     * @param x The x coordinate to determine the marked state of
     * @param y The y coordinate to determine the marked state of
     * @return Whether the given coordinates are marked
     */
    public boolean marked(int x, int y) {
        return marking != null && marked(marking, x, y, width);
    }

    private static boolean marked(long[] data, int x, int y, int width) {
        int i = x + width * y;
        return (data[i >>> 6] & 1L << (i & 63)) != 0;
    }

    /**
     * Sets all coordinates to be not marked.
     */
    public void clearMarked() {
        if(marking != null)
            Arrays.fill(marking, 0);
    }

    /**
     * Returns the number of coordinates currently marked in the grid.
     *
     * @return The number of marked coordinates
     */
    public int countMarked() {
        if(marking == null)
            return 0;

        int count = 0;
        for(long m : marking)
            count += Long.bitCount(m);
        return count;
    }

    /**
     * Evaluates the given predicate once for every valid coordinate in this grid and
     * returns the number of times it evaluated to <code>true</code>.
     *
     * @param filter The filter to determine whether to count a coordinate or not
     * @return The number of selected coordinates
     */
    public int count(Predicate<? super int2> filter) {
        int count = 0;
        for(int2 p : size)
            if(filter.test(p))
                count++;
        return count;
    }

    /**
     * Evaluates the given predicate once for the character of every valid coordinate
     * in this grid and returns the number of times it evaluated to <code>true</code>.
     *
     * @param filter The filter to determine whether to count a character or not
     * @return The number of times the filter returned <code>true</code>
     */
    public int countChars(CharPredicate filter) {
        int count = 0;
        for(char[] row : grid) for(char val : row)
            if(filter.test(val))
                count++;
        return count;
    }

    /**
     * Returns the number of occurrences of the given char in this grid.
     *
     * @param c The character to count
     * @return The number of times the given character occurs in this grid
     */
    public int count(char c) {
        int count = 0;
        for(char[] row : grid) for(char val : row)
            if(val == c)
                count++;
        return count;
    }

    /**
     * Returns the sum of applying the given function to each coordinate in the grid.
     *
     * @param positionValue The function to determine the value for a single position
     * @return The sum of all evaluations
     */
    public long sum(ToLongFunction<? super int2> positionValue) {
        return Mathf.sumL(size, positionValue);
    }

    /**
     * Returns the product of applying the given function to each coordinate in the grid.
     *
     * @param positionValue The function to determine the value for a single position
     * @return The product of all evaluations
     */
    public long product(ToLongFunction<? super int2> positionValue) {
        long product = 1;
        for(int2 p : size)
            product *= positionValue.applyAsLong(p);
        return product;
    }

    /**
     * Returns an iterator iterating over all valid coordinates in this grid, with the
     * given insets to skip.
     *
     * @param inset The inset on all sides, e.g. 1 to iterate from [1,1] to [width-1,height-1]
     *              (exclusive)
     * @return An iterator over the coordinates in this grid with the given insets
     */
    public IterableIterator<int2> withInsets(int inset) {
        return withInsets(inset, inset);
    }

    /**
     * Returns an iterator iterating over all valid coordinates in this grid, with the
     * given insets to skip.
     *
     * @param horizontal The left and right insets to skip for iteration
     * @param vertical The top and bottom insets to skip for iteration
     * @return An iterator over the coordinates in this grid with the given insets
     */
    public IterableIterator<int2> withInsets(int horizontal, int vertical) {
        return withInsets(horizontal, horizontal, vertical, vertical);
    }

    /**
     * Returns an iterator iterating over all valid coordinates in this grid, with the
     * given insets to skip.
     *
     * @param left The left inset to skip for iteration
     * @param right The right inset to skip for iteration
     * @param top The top inset to skip for iteration
     * @param bottom The bottom inset to skip for iteration
     * @return An iterator over the coordinates in this grid with the given insets
     */
    public IterableIterator<int2> withInsets(int left, int right, int top, int bottom) {
        return new MappingIterator<>(size.subed(left + right, top + bottom), p -> p.add(left, top));
    }

    /**
     * Returns the 9 adjacent characters at the given coordinate (including
     * at the coordinate itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj9(constInt2 xy) {
        return adj9(xy.x(), xy.y());
    }

    /**
     * Returns the 9 adjacent characters at the given coordinate (including
     * at the coordinate itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    @SuppressWarnings("DuplicatedCode")
    public char[] adj9(int x, int y) {
        char[] adj = new char[9];
        int i = 0;
        if(y > 0) {
            if(x > 0) adj[i++] = grid[y-1][x-1];
            adj[i++] = grid[y-1][x];
            if(x+1 < width) adj[i++] = grid[y-1][x+1];
        }
        if(x > 0)
            adj[i++] = grid[y][x-1];
        adj[i++] = grid[y][x];
        if(x+1 < width)
            adj[i++] = grid[y][x+1];
        if(y+1 < height) {
            if(x > 0) adj[i++] = grid[y+1][x-1];
            adj[i++] = grid[y+1][x];
            if(x+1 < width) adj[i++] = grid[y+1][x+1];
        }
        return i < 9 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 8 adjacent characters at the given coordinate (without
     * at the coordinate itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj8(constInt2 xy) {
        return adj8(xy.x(), xy.y());
    }

    /**
     * Returns the 8 adjacent characters at the given coordinate (without
     * at the coordinate itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    @SuppressWarnings("DuplicatedCode")
    public char[] adj8(int x, int y) {
        char[] adj = new char[8];
        int i = 0;
        if(y > 0) {
            if(x > 0) adj[i++] = grid[y-1][x-1];
            adj[i++] = grid[y-1][x];
            if(x+1 < width) adj[i++] = grid[y-1][x+1];
        }
        if(x > 0)
            adj[i++] = grid[y][x-1];
        if(x+1 < width)
            adj[i++] = grid[y][x+1];
        if(y+1 < height) {
            if(x > 0) adj[i++] = grid[y+1][x-1];
            adj[i++] = grid[y+1][x];
            if(x+1 < width) adj[i++] = grid[y+1][x+1];
        }
        return i < 8 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 5 directly adjacent characters at the given coordinate (left, right,
     * top, bottom and at the position itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj5(constInt2 xy) {
        return adj5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 directly adjacent characters at the given coordinate (left, right,
     * top, bottom and at the position itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj5(int x, int y) {
        char[] adj = new char[5];
        int i = 0;
        if(y > 0)
            adj[i++] = grid[y-1][x];
        if(x > 0)
            adj[i++] = grid[y-1][x-1];
        adj[i++] = grid[y][x];
        if(x+1 < width)
            adj[i++] = grid[y-1][x+1];
        if(y+1 < height)
            adj[i++] = grid[y+1][x];
        return i < 5 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 4 directly adjacent characters at the given coordinate (left, right,
     * top and bottom), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj4(constInt2 xy) {
        return adj4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 directly adjacent characters at the given coordinate (left, right,
     * top and bottom), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] adj4(int x, int y) {
        char[] adj = new char[4];
        int i = 0;
        if(y > 0)
            adj[i++] = grid[y-1][x];
        if(x > 0)
            adj[i++] = grid[y-1][x-1];
        if(x+1 < width)
            adj[i++] = grid[y-1][x+1];
        if(y+1 < height)
            adj[i++] = grid[y+1][x];
        return i < 4 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 5 diagonally adjacent characters at the given coordinate (and the character
     * at the position itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] diag5(constInt2 xy) {
        return diag5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 diagonally adjacent characters at the given coordinate (and the character
     * at the position itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    @SuppressWarnings("DuplicatedCode")
    public char[] diag5(int x, int y) {
        char[] adj = new char[5];
        int i = 0;
        if(y > 0) {
            if(x > 0) adj[i++] = grid[y-1][x-1];
            if(x+1 < width) adj[i++] = grid[y-1][x+1];
        }
        adj[i++] = grid[y][x];
        if(y+1 < height) {
            if(x > 0) adj[i++] = grid[y+1][x-1];
            if(x+1 < width) adj[i++] = grid[y+1][x+1];
        }
        return i < 5 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 4 diagonally adjacent characters at the given coordinate (without the character
     * at the position itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] diag4(constInt2 xy) {
        return diag4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 diagonally adjacent characters at the given coordinate (without the character
     * at the position itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent characters
     * @param y The y coordinate where to query the adjacent characters
     * @return The adjacent characters
     */
    public char[] diag4(int x, int y) {
        char[] adj = new char[4];
        int i = 0;
        if(y > 0) {
            if(x > 0) adj[i++] = grid[y-1][x-1];
            if(x+1 < width) adj[i++] = grid[y-1][x+1];
        }
        adj[i++] = grid[y][x];
        if(y+1 < height) {
            if(x > 0) adj[i++] = grid[y+1][x-1];
            if(x+1 < width) adj[i++] = grid[y+1][x+1];
        }
        return i < 4 ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns the 9 adjacent positions at the given coordinate (including the position
     * itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos9(constInt2 xy) {
        return adjPos9(xy.x(), xy.y());
    }

    /**
     * Returns the 9 adjacent positions at the given coordinate (including the position
     * itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos9(int x, int y) {
        return adjPos(x, y, true, true, true);
    }

    /**
     * Returns the 8 adjacent positions at the given coordinate (excluding the position
     * itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos8(constInt2 xy) {
        return adjPos8(xy.x(), xy.y());
    }

    /**
     * Returns the 8 adjacent positions at the given coordinate (excluding the position
     * itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos8(int x, int y) {
        return adjPos(x, y, false, true, true);
    }

    /**
     * Returns the 5 directly adjacent positions at the given coordinate (left, right,
     * top, bottom and the position itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos5(constInt2 xy) {
        return adjPos5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 directly adjacent positions at the given coordinate (left, right,
     * top, bottom and the position itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos5(int x, int y) {
        return adjPos(x, y, true, true, false);
    }

    /**
     * Returns the 4 directly adjacent positions at the given coordinate (left, right,
     * top and bottom), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos4(constInt2 xy) {
        return adjPos4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 directly adjacent positions at the given coordinate (left, right,
     * top and bottom), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] adjPos4(int x, int y) {
        return adjPos(x, y, false, true, false);
    }

    /**
     * Returns the 5 diagonally adjacent positions at the given coordinate (including
     * the point itself), excluding entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] diagPos5(constInt2 xy) {
        return diagPos5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 diagonally adjacent positions at the given coordinate (including
     * the point itself), excluding entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] diagPos5(int x, int y) {
        return adjPos(x, y, true, false, true);
    }

    /**
     * Returns the 4 diagonally adjacent positions at the given coordinate, excluding
     * entries that would be out of bounds.
     *
     * @param xy The coordinates where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] diagPos4(constInt2 xy) {
        return diagPos4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 diagonally adjacent positions at the given coordinate, excluding
     * entries that would be out of bounds.
     *
     * @param x The x coordinate where to query the adjacent coordinates
     * @param y The y coordinate where to query the adjacent coordinates
     * @return The adjacent coordinates
     */
    public int2[] diagPos4(int x, int y) {
        return adjPos(x, y, false, false, true);
    }

    private int2[] adjPos(int x, int y, boolean self, boolean flat, boolean diag) {
        int2[] pos = adjOffsets(x, y, self, flat, diag);
        for(int2 p : pos)
            p.add(x,y);
        return pos;
    }

    /**
     * Returns the 9 offsets to the adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets9(constInt2 xy) {
        return adjOffsets9(xy.x(), xy.y());
    }

    /**
     * Returns the 9 offsets to the adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets9(int x, int y) {
        return adjOffsets(x, y, true, true, true);
    }

    /**
     * Returns the 8 offsets to the adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets8(constInt2 xy) {
        return adjOffsets8(xy.x(), xy.y());
    }

    /**
     * Returns the 8 offsets to the adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets8(int x, int y) {
        return adjOffsets(x, y, false, true, true);
    }

    /**
     * Returns the 5 offsets to the directly adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets5(constInt2 xy) {
        return adjOffsets5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 offsets to the directly adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets5(int x, int y) {
        return adjOffsets(x, y, true, true, false);
    }

    /**
     * Returns the 4 offsets to the directly adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets4(constInt2 xy) {
        return adjOffsets4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 offsets to the directly adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] adjOffsets4(int x, int y) {
        return adjOffsets(x, y, false, true, false);
    }

    /**
     * Returns the 5 offsets to the diagonally adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] diagOffsets5(constInt2 xy) {
        return diagOffsets5(xy.x(), xy.y());
    }

    /**
     * Returns the 5 offsets to the diagonally adjacent positions of the given coordinate
     * (in the range -1 to 1, including 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] diagOffsets5(int x, int y) {
        return adjOffsets(x, y, true, false, true);
    }

    /**
     * Returns the 4 offsets to the diagonally adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param xy The coordinates where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] diagOffsets4(constInt2 xy) {
        return diagOffsets4(xy.x(), xy.y());
    }

    /**
     * Returns the 4 offsets to the diagonally adjacent positions of the given coordinate
     * (in the range -1 to 1, excluding 0 for the position itself), excluding entries
     * that would, when added to the given coordinates, lay out of bounds.
     *
     * @param x The x coordinate where to query the offsets to adjacent coordinates
     * @param y The y coordinate where to query the offsets to adjacent coordinates
     * @return The offsets to adjacent coordinates
     */
    public int2[] diagOffsets4(int x, int y) {
        return adjOffsets(x, y, false, false, true);
    }

    private int2[] adjOffsets(int x, int y, boolean self, boolean flat, boolean diag) {
        int max = (self?1:0) + (flat?4:0) + (diag?4:0);
        int2[] adj = new int2[max];
        int i = 0;
        if(y > 0) {
            if(diag && x > 0)
                adj[i++] = new int2(-1, -1);
            if(flat)
                adj[i++] = new int2(0, -1);
            if(diag && x+1 < width)
                adj[i++] = new int2(1, -1);
        }
        if(flat && x > 0)
            adj[i++] = new int2(-1, 0);
        if(self)
            adj[i++] = int2.zero();
        if(flat && x+1 < width)
            adj[i++] = new int2(1, 0);
        if(y+1 < height) {
            if(diag && x > 0)
                adj[i++] = new int2(-1, 1);
            if(flat)
                adj[i++] = new int2(0, 1);
            if(diag && x+1 < width)
                adj[i++] = new int2(1, 1);
        }
        return i < max ? Arrays.copyOf(adj, i) : adj;
    }

    /**
     * Returns a graph with every coordinate of this grid, where adjacent nodes are connected
     * iff the given filter returns <code>true</code> for them.
     *
     * @param diagonals Whether to also test (and then connect) diagonally adjacent coordinates
     * @param filter The filter to test whether two chars should be connected. Will be evaluated
     *               in both directions for each pair of nodes, possibly resulting in a directed
     *               graph.
     * @return The resulting graph
     */
    public Graph<int2, Integer> graphFromChars(boolean diagonals, BiCharPredicate filter) {
        return graphFromChars(diagonals, true, filter);
    }

    /**
     * Returns a graph with every coordinate of this grid, where adjacent nodes are connected
     * iff the given filter returns <code>true</code> for them.
     *
     * @param diagonals Whether to also test (and then connect) diagonally adjacent coordinates
     * @param bothDirections If <code>true</code>, the filter will be tested in both directions
     *                       for each pair of characters. Otherwise, it will only be evaluated
     *                       in one direction, definitely resulting in a directed graph. If an
     *                       undirected graph is needed, either use a symmetric condition or wrap
     *                       the result in <code>new HashGraph&lt;>(grid.graphFromChars(...), false)</code>.
     * @param filter The filter to test whether two chars should be connected.
     * @return The resulting graph
     */
    public Graph<int2, Integer> graphFromChars(boolean diagonals, boolean bothDirections, BiCharPredicate filter) {
        return graphFromPoints(diagonals, bothDirections, (a, b) -> filter.test(charAt(a), charAt(b)));
    }

    /**
     * Returns a graph with every coordinate of this grid, where adjacent nodes are connected
     * iff the given filter returns <code>true</code> for them.
     *
     * @param diagonals Whether to also test (and then connect) diagonally adjacent coordinates
     * @param filter The filter to test whether two coordinates should be connected. Will be evaluated
     *               in both directions for each pair of nodes, possibly resulting in a directed
     *               graph.
     * @return The resulting graph
     */
    public Graph<int2, Integer> graphFromPoints(boolean diagonals, BiPredicate<constInt2, constInt2> filter) {
        return graphFromPoints(diagonals, true, filter);
    }

    /**
     * Returns a graph with every coordinate of this grid, where adjacent nodes are connected
     * iff the given filter returns <code>true</code> for them.
     *
     * @param diagonals Whether to also test (and then connect) diagonally adjacent coordinates
     * @param bothDirections If <code>true</code>, the filter will be tested in both directions
     *                       for each pair of coordinates. Otherwise, it will only be evaluated
     *                       in one direction, definitely resulting in a directed graph. If an
     *                       undirected graph is needed, either use a symmetric condition or wrap
     *                       the result in <code>new HashGraph&lt;>(grid.graphFromPoints(...), false)</code>.
     * @param filter The filter to test whether two coordinates should be connected. Will be evaluated
     *               in both directions for each pair of nodes, possibly resulting in a directed
     *               graph.
     * @return The resulting graph
     */
    public Graph<int2, Integer> graphFromPoints(boolean diagonals, boolean bothDirections, BiPredicate<constInt2, constInt2> filter) {
        Graph<int2, Integer> g = new SimpleHashGraph<>();

        for(int y=0; y<height; y++) for(int x=0; x<width; x++) {
            g.add(new int2(x,y));
            if(x+1 < width) {
                if(filter.test(new int2(x,y), new int2(x+1, y)))
                    g.connect(new int2(x,y), new int2(x+1, y));
                if(bothDirections && filter.test(new int2(x+1,y), new int2(x, y)))
                    g.connect(new int2(x+1,y), new int2(x, y));
            }
            if(y+1 < width) {
                if(filter.test(new int2(x,y), new int2(x,y+1)))
                    g.connect(new int2(x,y), new int2(x,y+1));
                if(bothDirections && filter.test(new int2(x,y+1), new int2(x, y)))
                    g.connect(new int2(x,y+1), new int2(x, y));
            }
            if(diagonals) {
                if(x+1 < width && y+1 < height) {
                    if(filter.test(new int2(x,y), new int2(x+1, y+1)))
                        g.connect(new int2(x,y), new int2(x+1, y+1));
                    if(bothDirections && filter.test(new int2(x+1, y+1), new int2(x, y)))
                        g.connect(new int2(x+1, y+1), new int2(x, y));
                }
                if(x+1 < width && y > 0) {
                    if(filter.test(new int2(x,y), new int2(x+1, y-1)))
                        g.connect(new int2(x,y), new int2(x+1, y-1));
                    if(bothDirections && filter.test(new int2(x+1, y-1), new int2(x, y)))
                        g.connect(new int2(x+1, y-1), new int2(x, y));
                }
            }
        }

        return g;
    }

    private static final String[][] HORIZONTAL_BARS = { { " ", "→" }, { "←", "-" } };
    private static final String[][] VERTICAL_BARS = { { " ", "↑" }, { "↓", "|" } };
    private static final String[][][][] DIAGONAL_BARS = { { { { " ", "↗" }, { "↙", "/" } }, { { "↖", "⤧" }, { "⤪", "y" } } },
                                                          { { { "↘", "⤨" }, { "⤩", "⤰" } }, { {"\\", "⤯" }, { "λ", "X" } } } };

    /**
     * Produces an ascii art string for the given graph assuming it has at most straight and diagonal
     * edges and the size of this grid. The nodes will display as the character at that coordinate.
     *
     * @param grid The grid to render in ascii art
     * @return An ascii art representation of the graph
     */
    public String render(ReadableGraph<? extends constInt2, ?> grid) {
        return render(grid, this::charAt);
    }

    /**
     * Produces an ascii art string for the given graph assuming it has at most straight and diagonal
     * edges and the size of this grid.
     *
     * @param grid The grid to render in ascii art
     * @param renderer A function to determine what to show as nodes. The result will be wrapped
     *                 in {@link Objects#toString(Object)}.
     * @return An ascii art representation of the graph
     */
    public String render(ReadableGraph<? extends constInt2,?> grid, Function<constInt2, ?> renderer) {

        if(size.product() == 0)
            return "∅";

        String[][] rows = new String[height][width];
        int[][] lengths = new int[height][width];
        for(int2 p : size)
            lengths[p.y()][p.x()] = length(rows[p.y()][p.x()] = grid.contains(p) ? Objects.toString(renderer.apply(p)) : " ");

        for(int x=0; x<width; x++) {
            int x0 = x;
            int width = Mathf.max(lengths, r -> r[x0]);
            for(int y=0; y<rows.length; y++) {
                int l = width - lengths[y][x];
                rows[y][x] = " ".repeat((l+1)/2) + rows[y][x] + " ".repeat(l);
            }
        }

        StringBuilder str = new StringBuilder();

        for(int y=0; y<height; y++) {
            for(int x=0; x<width; x++) {
                str.append(rows[y][x]);

                if(x+1 < width) {
                    int2 a = new int2(x,y), b = new int2(x+1,y);
                    str.append(HORIZONTAL_BARS[grid.connected(a,b)?1:0][grid.connected(b,a)?1:0]);
                }
            }
            if(y+1 < height) {
                str.append('\n');
                for(int x=0; x<width; x++) {
                    int2 a = new int2(x,y), c = new int2(x,y+1);
                    str.append(VERTICAL_BARS[grid.connected(a,c)?1:0][grid.connected(c,a)?1:0]);

                    if(x+1 < width) {
                        int2 b = new int2(x+1,y), d = new int2(x+1,y+1);
                        str.append(DIAGONAL_BARS[grid.connected(a,d)?1:0][grid.connected(d,a)?1:0][grid.connected(b,c)?1:0][grid.connected(c,b)?1:0]);
                    }
                }
                str.append('\n');
            }
        }

        return str.toString();
    }

    private static final Pattern ANSI_ESCAPE_SEQUENCE_PAT = Pattern.compile("\u001b\\[\\d+(;\\d+)*m");
    private static int length(String str) {
        return ANSI_ESCAPE_SEQUENCE_PAT.matcher(str).replaceAll("").length();
    }


    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point and recursively extending to all adjacent points with the same character value,
     * using breath first search. The point itself is always included in the result. The fill
     * only considers the 4 directly adjacent neighbors, not the diagonal ones.
     *
     * @param src The starting point from which to execute the flood fill search
     * @return A stream over all the coordinates with the same character and connected to the
     *         starting point using a path only leading across points with that character,
     *         including <code>src</code>.
     */
    public ListStream<int2> floodFill(constInt2 src) {
        return floodFill(src, FloodFillMode.BFS, false);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point and recursively extending to all adjacent points with the same character value,
     * using breath first search. The point itself is always included in the result.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param diagonals Whether diagonal connections are allowed
     * @return A stream over all the coordinates with the same character and connected to the
     *         starting point using a path only leading across points with that character,
     *         including <code>src</code>.
     */
    public ListStream<int2> floodFill(constInt2 src, boolean diagonals) {
        return floodFill(src, FloodFillMode.BFS, diagonals);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point and recursively extending to all adjacent points with the same character value.
     * The point itself is always included in the result.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param mode The algorithm to use, may impact iteration order
     * @param diagonals Whether diagonal connections are allowed
     * @return A stream over all the coordinates with the same character and connected to the
     *         starting point using a path only leading across points with that character,
     *         including <code>src</code>.
     */
    public ListStream<int2> floodFill(constInt2 src, FloodFillMode mode, boolean diagonals) {

        char c = charAt(src);

        boolean dfs = mode == FloodFillMode.DFS;

        Deque<int2> todo = new ArrayDeque<>();
        if(dfs) todo.push(src.clone());
        else todo.add(src.clone());

        long[] dejavu = new long[(width * height + 63) / 64];
        mark(dejavu, src.x(), src.y(), width);

        constInt2[] adj = diagonals ? Solution.ADJ8 : Solution.ADJ4;

        return ListStream.of(new IterableIterator<>() {
            @Override
            public boolean hasNext() {
                return !todo.isEmpty();
            }

            @Override
            public int2 next() {
                int2 v = todo.remove();
                for(constInt2 off : adj) {
                    int x = v.x() + off.x(), y = v.y() + off.y();
                    if(x < 0 || y < 0 || x >= width || y >= height || grid[y][x] != c || !mark(dejavu, x, y, width))
                        continue;
                    if(dfs) todo.push(new int2(x,y));
                    else todo.add(new int2(x,y));
                }
                return v;
            }
        });
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point using breath first search. The point itself is always included in the result. The
     * fill only considers the 4 directly adjacent neighbors, not the diagonal ones. This
     * method differs from {@link #floodFill(constInt2, boolean, BiPredicate)} in that it
     * supplies the chars rather than the coordinates to the connectivity predicate, for
     * convenience.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param connectivity A predicate to determine for two given chars determines if they
     *                     lie in the same area, that is, if the first char is within the
     *                     flood fill, whether the second char should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFillChars(constInt2 src, Grid.BiCharPredicate connectivity) {
        return floodFillChars(src, false, connectivity);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point using breath first search. The point itself is always included in the result.
     * This method differs from {@link #floodFill(constInt2, boolean, BiPredicate)} in that it
     * supplies the chars rather than the coordinates to the connectivity predicate, for
     * convenience.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param diagonals Whether diagonal connections are allowed
     * @param connectivity A predicate to determine for two given chars determines if they
     *                     lie in the same area, that is, if the first char is within the
     *                     flood fill, whether the second char should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFillChars(constInt2 src, boolean diagonals, Grid.BiCharPredicate connectivity) {
        return floodFillChars(src, FloodFillMode.BFS, diagonals, connectivity);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point. The point itself is always included in the result. This method differs from
     * {@link #floodFill(constInt2, FloodFillMode, boolean, BiPredicate)} in that it supplies
     * the chars rather than the coordinates to the connectivity predicate, for convenience.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param mode The algorithm to use, may impact iteration order
     * @param diagonals Whether diagonal connections are allowed
     * @param connectivity A predicate to determine for two given chars determines if they
     *                     lie in the same area, that is, if the first char is within the
     *                     flood fill, whether the second char should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFillChars(constInt2 src, FloodFillMode mode, boolean diagonals, Grid.BiCharPredicate connectivity) {
        return floodFill(src, mode, diagonals, (a,b) -> connectivity.test(charAtRaw(a), charAtRaw(b)));
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point using breath first search. The point itself is always included in the result. The
     * fill only considers the 4 directly adjacent neighbors, not the diagonal ones.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param connectivity A predicate to determine for two given points determines if they
     *                     lie in the same area, that is, if the first point is within the
     *                     flood fill, whether the second point should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFill(constInt2 src, BiPredicate<int2, int2> connectivity) {
        return floodFill(src, false, connectivity);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point using breath first search. The point itself is always included in the result.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param diagonals Whether diagonal connections are allowed
     * @param connectivity A predicate to determine for two given points determines if they
     *                     lie in the same area, that is, if the first point is within the
     *                     flood fill, whether the second point should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFill(constInt2 src, boolean diagonals, BiPredicate<int2, int2> connectivity) {
        return floodFill(src, FloodFillMode.BFS, diagonals, connectivity);
    }

    /**
     * Returns a stream over all points in the flood fill of this grid starting at the given
     * point. The point itself is always included in the result.
     *
     * @param src The starting point from which to execute the flood fill search
     * @param mode The algorithm to use, may impact iteration order
     * @param diagonals Whether diagonal connections are allowed
     * @param connectivity A predicate to determine for two given points determines if they
     *                     lie in the same area, that is, if the first point is within the
     *                     flood fill, whether the second point should also do so. This relation
     *                     must be symmetric, otherwise the result may depend on the iteration
     *                     order. Usually, it should in fact be an equivalence relation, i.e.
     *                     it should be transitive (reflexivity is irrelevant). However, if
     *                     the input has specific properties, this may not be necessary in all
     *                     cases.
     * @return A stream over all the coordinates in the fill, including <code>src</code>
     */
    public ListStream<int2> floodFill(constInt2 src, FloodFillMode mode, boolean diagonals, BiPredicate<int2, int2> connectivity) {

        boolean dfs = mode == FloodFillMode.DFS;

        Deque<int2> todo = new ArrayDeque<>();
        if(dfs) todo.push(src.clone());
        else todo.add(src.clone());

        long[] dejavu = new long[(size.product() + 63) / 64];
        mark(dejavu, src.x(), src.y(), width);

        constInt2[] adj = diagonals ? Solution.ADJ8 : Solution.ADJ4;

        return ListStream.of(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !todo.isEmpty();
            }

            @Override
            public int2 next() {
                int2 v = todo.remove();
                for(constInt2 off : adj) {
                    int x = v.x() + off.x(), y = v.y() + off.y();
                    int2 w;
                    if(x < 0 || y < 0 || x >= width || y >= height || marked(dejavu, x, y, width) || !connectivity.test(v, w = new int2(x,y)))
                        continue;
                    mark(dejavu, x, y, width);
                    if(dfs) todo.push(w);
                    else todo.add(w);
                }
                return v;
            }
        });
    }


    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     * '.' will be treated as empty space, where obstacles may be pushed.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movableChars A string containing all the characters that are considered
     *                     movable obstacles, in arbitrary order. Any character that is
     *                     neither a movable obstacle nor <code>empty</code> will be
     *                     considered an immovable wall.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, String movableChars) {
        return movableChars.length() == 1 ? push(from, dir, movableChars.charAt(0)) : push(from, dir, c -> movableChars.indexOf(c) >= 0);
    }

    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     * '.' will be treated as empty space, where obstacles may be pushed.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movable A predicate determining whether a given character represents a movable
     *                object, an "obstacle". Any character that is neither a movable
     *                obstacle nor <code>empty</code> will be considered an immovable wall.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, Grid.CharPredicate movable) {
        return push(from, dir, movable, '.');
    }

    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movableChars A string containing all the characters that are considered
     *                     movable obstacles, in arbitrary order. Any character that is
     *                     neither a movable obstacle nor <code>empty</code> will be
     *                     considered an immovable wall.
     * @param empty Character used to represent empty space, the background if you will.
     *              If this character is found in the way of an obstacle, the obstacle will
     *              move at its position, and the cell where that (or another obstacle, if
     *              multiple were pushed at once) was will be set to this character.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, String movableChars, char empty) {
        return movableChars.length() == 1 ? push(from, dir, movableChars.charAt(0), empty) : push(from, dir, c -> movableChars.indexOf(c) >= 0, empty);
    }

    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movable A predicate determining whether a given character represents a movable
     *                object, an "obstacle". Any character that is neither a movable obstacle
     *                nor <code>empty</code> will be considered an immovable wall.
     * @param empty Character used to represent empty space, the background if you will.
     *              If this character is found in the way of an obstacle, the obstacle will
     *              move at its position, and the cell where that (or another obstacle, if
     *              multiple were pushed at once) was will be set to this character.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, Grid.CharPredicate movable, char empty) {
        if(dir.isZero())
            throw new IllegalArgumentException("Pushing direction may not be 0");

        int obstacleCount = 0;
        int2 cursor = from.added(dir);

        char c;
        while(movable.test(c = charAt(cursor))) {
            cursor.add(dir);
            ++obstacleCount;
        }
        if(c != empty)
            return -1;

        if(obstacleCount != 0) {
            int2 cursorBefore = cursor.subed(dir);

            if(inside(cursor))
                set(cursor, charAt(cursorBefore));
            cursor.set(cursorBefore);
            cursorBefore.sub(dir);

            while(!from.equals(cursorBefore)) {
                set(cursor, charAt(cursorBefore));
                cursor.set(cursorBefore);
                cursorBefore.sub(dir);
            }
            set(cursor, empty);
        }
        return obstacleCount;
    }

    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     * '.' will be treated as empty space, where obstacles may be pushed.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movable The character that represents a movable obstacle. Any character that
     *                is neither a movable obstacle nor <code>empty</code> will be
     *                considered an immovable wall.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, char movable) {
        return push(from, dir, movable, '.');
    }

    /**
     * Simulates pushing 0 or more 1x1 sized obstacles once in a specific direction.
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of obstacles
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param movable The character that represents a movable obstacle. Any character that
     *                is neither a movable obstacle nor <code>empty</code> will be
     *                considered an immovable wall.
     * @param empty Character used to represent empty space, the background if you will.
     *              If this character is found in the way of an obstacle, the obstacle will
     *              move at its position, and the cell where that (or another obstacle, if
     *              multiple were pushed at once) was will be set to this character.
     * @return The number of obstacles pushed (0 or more), or -1 if an immovable wall is in
     *         the way
     */
    public int push(constInt2 from, constInt2 dir, char movable, char empty) {
        if(dir.isZero())
            throw new IllegalArgumentException("Pushing direction may not be 0");

        int obstacleCount = 0;
        int2 cursor = from.added(dir);

        char c;
        while((c = charAt(cursor)) == movable) {
            cursor.add(dir);
            ++obstacleCount;
        }
        if(c != empty)
            return -1;

        if(obstacleCount != 0) {
            if(inside(cursor))
                set(cursor, movable);
            if(inside(cursor.set(from).add(dir)))
                set(cursor, empty);
        }
        return obstacleCount;
    }


    /**
     * Simulates pushing 0 or more of instances of a specific shape once in a specific
     * direction.
     * <p>The shape may have arbitrary form, including holes or gaps. The shape is
     * specified in ascii art; as n equally long lines delimited by '\n'. Holes are
     * specified using whitespaces ' ' (each line still needs to have the same number of
     * chars, even if it has trailing whitespaces). <b>All non-whitespace chars need to
     * be distinct</b> such that the position of the shape can uniquely be identified given
     * a single character. For example, <code>"[]"</code> would specify a simple 2x1 box,
     * <code>"F7\nLJ"</code> may represent a 2x2 box, or <code>"[ ]"</code> could represent
     * two small objects which are somehow entangled with each other.</p>
     *
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of distinct objects
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     * '.' will be treated as empty space, where obstacles may be pushed.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param shape A string specifying the shape to be pushed, as described above
     * @return The number of distinct obstacles pushed (0 or more), or -1 if an immovable wall
     *         is in the way
     */
    public int pushShape(constInt2 from, constInt2 dir, String shape) {
        return pushShape(from, dir, shape, '.');
    }

    /**
     * Simulates pushing 0 or more of instances of a specific shape once in a specific
     * direction.
     * <p>The shape may have arbitrary form, including holes or gaps. The shape is
     * specified in ascii art; as n equally long lines delimited by '\n'. Holes are
     * specified using whitespaces ' ' (each line still needs to have the same number of
     * chars, even if it has trailing whitespaces). <b>All non-whitespace chars need to
     * be distinct</b> such that the position of the shape can uniquely be identified given
     * a single character. For example, <code>"[]"</code> would specify a simple 2x1 box,
     * <code>"F7\nLJ"</code> may represent a 2x2 box, or <code>"[ ]"</code> could represent
     * two small objects which are somehow entangled with each other.</p>
     *
     * If the required space is found, all (possibly 0 if none are in the way) obstacles
     * will be pushed over accordingly, and the method returns the number of distinct objects
     * pushed. Otherwise, the grid content will remain unchanged and the method returns -1.
     *
     * @param from The position from which to push. The first considered obstacles lays
     *             next to it, at <code>from + dir</code>.
     * @param dir The direction in which to push. In pretty much all use cases this should
     *            be a vector with x and y each between -1 and 1, at least one of which
     *            non-zero. Passing 0 will result in an exception. Passing e.g. [2,0] will
     *            result in only every other cell being sampled for obstacles, and when
     *            pushed the obstacles will move 2 cells at once, with anything in between
     *            unaffected. Usually undesirable.
     * @param shape A string specifying the shape to be pushed, as described above
     * @param empty Character used to represent empty space, the background if you will.
     *              If this character is found in the way of an obstacle, the obstacle will
     *              move at its position, and the cell where that (or another obstacle, if
     *              multiple were pushed at once) was will be set to this character.
     * @return The number of distinct obstacles pushed (0 or more), or -1 if an immovable wall
     *         is in the way
     */
    public int pushShape(constInt2 from, constInt2 dir, String shape, char empty) {
        if(dir.isZero())
            throw new IllegalArgumentException("Pushing direction may not be 0");

        int width1 = shape.indexOf('\n') + 1;
        if(width1 <= 0)
            width1 = shape.length() + 1;
        int height = (shape.length()+1) / width1;

        Set<int2> moved = new HashSet<>();
        if(!findMovedObjects(from.added(dir), dir, empty, shape, width1-1, height, moved))
            return -1;

        for(int2 obj : moved) {
            for(int i=0; i<width1-1; i++) for(int j=0; j<height; j++) {
                if(shape.charAt(i + width1 * j) != ' ')
                    grid[obj.y() + j][obj.x() + i] = empty;
            }
        }
        for(int2 obj : moved) {
            obj.add(dir);
            for(int i=0; i<width1-1; i++) for(int j=0; j<height; j++) {
                char c = shape.charAt(i + width1 * j);
                if(c != ' ')
                    grid[obj.y() + j][obj.x() + i] = c;
            }
        }

        return moved.size();
    }

    private boolean findMovedObjects(int2 pos, constInt2 dir, char empty, String shape, int width, int height, Set<int2> objectsOut) {
        char c = charAt(pos);
        if(c == empty)
            return true;

        int index = shape.indexOf(c);
        if(index < 0)
            return false;

        int x = index % (width+1), y = index / (width+1);
        pos.sub(x,y);

        if(!objectsOut.add(pos))
            return true;

        for(int i=0; i<width; i++) for(int j=0; j<height; j++)
            if(shape.charAt(i + (width+1) * j) != ' ' && !findMovedObjects(pos.added(i,j).add(dir), dir, empty, shape, width, height, objectsOut))
                return false;

        return true;
    }


    /**
     * Possible algorithms determining the iteration order when using floodfill.
     */
    public enum FloodFillMode {
        /** Breath first search. */
        BFS,
        /** Depth first search. */
        DFS
    }


    public interface CharPredicate {
        boolean test(char c);
    }

    public interface BiCharPredicate {
        boolean test(char a, char b);
    }
}
