package de.rccookie.aoc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import de.rccookie.util.Arguments;
import de.rccookie.util.Cloneable;
import de.rccookie.util.ListStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A set of long values that provides efficient representation and operations for
 * intervals (ranges) of values.
 */
public interface RangedSet extends SortedSet<Long>, Cloneable<RangedSet> {

    @SuppressWarnings("override")
    @NotNull RangedSet clone();

    /**
     * Returns the exact number of items in this set, which might be up to (and
     * including) 2^64.
     *
     * @return The number of values in this set
     */
    BigInteger exactSize();

    /**
     * Returns the number of items in this set, or {@link Long#MAX_VALUE} if the
     * size is larger than the maximum long value.
     *
     * @return The number of values in this set, or {@link Long#MAX_VALUE} if more
     */
    long longSize();

    @Override
    default int size() {
        long size = longSize();
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
    }

    /**
     * Returns the number of continuous ranges in this set.
     */
    int rangeCount();

    @Override
    boolean isEmpty();

    @Override
    default boolean contains(Object o) {
        return o instanceof Long && contains((long) (Long) o);
    }

    /**
     * Returns whether the given value is contained in this set.
     *
     * @param value The value to test
     * @return Whether the value is contained
     */
    boolean contains(long value);

    /**
     * Returns whether given range is completely contained within this set.
     * If the range is empty or negative, the result is <code>true</code>.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (exclusive)
     * @return Whether all values range[0] <= x < range[1] are contained
     */
    default boolean containsRange(long[] range) {
        checkRange(range);
        return containsRange(range[0], range[1]);
    }

    /**
     * Returns whether given range is completely contained within this set.
     * If the range is empty or negative, the result is <code>true</code>.
     *
     * @param start The (inclusive) start of the range
     * @param end The (exclusive) end of the range
     * @return Whether all values start <= x < end are contained
     */
    default boolean containsRange(long start, long end) {
        return end == Long.MIN_VALUE || containsInclusiveRange(start, end - 1);
    }

    /**
     * Returns whether given range is completely contained within this set.
     * If the range is empty or negative, the result is <code>true</code>.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (inclusive)
     * @return Whether all values range[0] <= x <= range[1] are contained
     */
    default boolean containsInclusiveRange(long[] range) {
        checkRange(range);
        return containsInclusiveRange(range[0], range[1]);
    }

    /**
     * Returns whether given range is completely contained within this set.
     * If the range is empty or negative, the result is <code>true</code>.
     *
     * @param start The (inclusive) start of the range
     * @param end The (inclusive) end of the range
     * @return Whether all values start <= x <= end are contained
     */
    boolean containsInclusiveRange(long start, long end);


    /**
     * Returns whether given range is partially contained within this set,
     * i.e. there is an overlap of the range with this set. If the range is
     * empty or negative, the result is <code>false</code>.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (exclusive)
     * @return Whether there is any value range[0] <= x < range[1] contained
     *         in this set
     */
    default boolean containsAnyInRange(long[] range) {
        checkRange(range);
        return containsAnyInRange(range[0], range[1]);
    }

    /**
     * Returns whether given range is partially contained within this set,
     * i.e. there is an overlap of the range with this set. If the range is
     * empty or negative, the result is <code>false</code>.
     *
     * @param start The (inclusive) start of the range
     * @param end The (exclusive) end of the range
     * @return Whether there is any value start <= x < end contained
     *         in this set
     */
    default boolean containsAnyInRange(long start, long end) {
        return end != Long.MIN_VALUE && containsAnyInInclusiveRange(start, end - 1);
    }

    /**
     * Returns whether given range is partially contained within this set,
     * i.e. there is an overlap of the range with this set. If the range is
     * empty or negative, the result is <code>false</code>.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (inclusive)
     * @return Whether there is any value range[0] <= x <= range[1] contained
     *         in this set
     */
    default boolean containsAnyInInclusiveRange(long[] range) {
        checkRange(range);
        return containsAnyInInclusiveRange(range[0], range[1]);
    }

    /**
     * Returns whether given range is partially contained within this set,
     * i.e. there is an overlap of the range with this set. If the range is
     * empty or negative, the result is <code>false</code>.
     *
     * @param start The (inclusive) start of the range
     * @param end The (inclusive) end of the range
     * @return Whether there is any value start <= x <= end contained
     *         in this set
     */
    boolean containsAnyInInclusiveRange(long start, long end);

    @Override
    @NotNull PrimitiveIterator.OfLong iterator();

    /**
     * Returns a list of all continuous ranges contained in this set, in
     * ascending order (only the largest continuous ranges).
     *
     * @return All ranges contained in this set
     */
    ListStream<long[]> ranges();

    default void forEachLong(LongConsumer action) {
        for(PrimitiveIterator.OfLong it = iterator(); it.hasNext(); )
            action.accept(it.nextLong());
    }

    /**
     * Returns a new array containing all values in this set. Throws an
     * {@link OutOfMemoryError} if the array would be too large (because of
     * actual memory constraints, or because the size is greater than
     * {@link Integer#MAX_VALUE}.
     *
     * @return An array with all values in this set
     */
    long @NotNull[] toLongArray();

    @Override
    default @NotNull Object @NotNull [] toArray() {
        return toArray(new Long[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    <T> T @NotNull [] toArray(T @NotNull [] a);

    @Override
    default <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
        long size = longSize();
        if(size > Integer.MAX_VALUE)
            throw new OutOfMemoryError("Cannot allocate array longer than Integer.MAX_VALUE");
        return toArray(generator.apply((int) size));
    }

    @Override
    default boolean add(Long value) {
        return add((long) value);
    }

    /**
     * Adds the given value to this set, if not already contained.
     *
     * @param value The value to add
     * @return Whether the set changed, i.e. the value was not yet present
     */
    boolean add(long value);

    /**
     * Adds all values in the given range to this set, if not already contained.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (exclusive)
     * @return Whether the set changed, i.e. any values were added
     */
    default boolean addRange(long[] range) {
        checkRange(range);
        return addRange(range[0], range[1]);
    }

    /**
     * Adds all values in the given range to this set, if not already contained.
     *
     * @param start The (inclusive) start of the range
     * @param end The (exclusive) end of the range
     * @return Whether the set changed, i.e. any values were added
     */
    default boolean addRange(long start, long end) {
        return addInclusiveRange(start, end - 1);
    }

    /**
     * Adds all values in the given range to this set, if not already contained.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (inclusive)
     * @return Whether the set changed, i.e. any values were added
     */
    default boolean addInclusiveRange(long[] range) {
        checkRange(range);
        return addInclusiveRange(range[0], range[1]);
    }

    /**
     * Adds all values in the given range to this set, if not already contained.
     *
     * @param start The (inclusive) start of the range
     * @param end The (inclusive) end of the range
     * @return Whether the set changed, i.e. any values were added
     */
    boolean addInclusiveRange(long start, long end);

    @Override
    default boolean remove(Object o) {
        if(!(o instanceof Long))
            return false;
        return remove((long) o);
    }

    /**
     * Removes the given value from this set, if it was contained.
     *
     * @param value The value to be removed
     * @return Whether the set changed, i.e. whether the value was previously present
     */
    boolean remove(long value);


    /**
     * Removes all values from the given range from this set that were contained.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (exclusive)
     * @return Whether the set changed, i.e. whether any elements in the range were contained
     */
    default boolean removeRange(long[] range) {
        checkRange(range);
        return removeRange(range[0], range[1]);
    }

    /**
     * Removes all values from the given range from this set that were contained.
     *
     * @param start The (inclusive) start of the range
     * @param end The (exclusive) end of the range
     * @return Whether the set changed, i.e. whether any elements in the range were contained
     */
    default boolean removeRange(long start, long end) {
        return end != Long.MIN_VALUE && removeInclusiveRange(start, end - 1);
    }

    /**
     * Removes all values from the given range from this set that were contained.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (inclusive)
     * @return Whether the set changed, i.e. whether any elements in the range were contained
     */
    default boolean removeInclusiveRange(long[] range) {
        checkRange(range);
        return removeInclusiveRange(range[0], range[1]);
    }

    /**
     * Removes all values from the given range from this set that were contained.
     *
     * @param start The (inclusive) start of the range
     * @param end The (inclusive) end of the range
     * @return Whether the set changed, i.e. whether any elements in the range were contained
     */
    boolean removeInclusiveRange(long start, long end);


    /**
     * Removes all values outside the given range from this set.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (exclusive)
     * @return Whether the set changed, i.e. whether it had any elements outside the range
     */
    default boolean retainRange(long[] range) {
        checkRange(range);
        return retainRange(range[0], range[1]);
    }

    /**
     * Removes all values outside the given range from this set.
     *
     * @param start The (inclusive) start of the range
     * @param end The (exclusive) end of the range
     * @return Whether the set changed, i.e. whether it had any elements outside the range
     */
    default boolean retainRange(long start, long end) {
        if(end == Long.MIN_VALUE) {
            boolean changed = !isEmpty();
            clear();
            return changed;
        }
        return retainInclusiveRange(start, end - 1);
    }

    /**
     * Removes all values outside the given range from this set.
     *
     * @param range A long array with exactly two elements; the range start
     *              (inclusive) and the range end (inclusive)
     * @return Whether the set changed, i.e. whether it had any elements outside the range
     */
    default boolean retainInclusiveRange(long[] range) {
        checkRange(range);
        return retainInclusiveRange(range[0], range[1]);
    }

    /**
     * Removes all values outside the given range from this set.
     *
     * @param start The (inclusive) start of the range
     * @param end The (inclusive) end of the range
     * @return Whether the set changed, i.e. whether it had any elements outside the range
     */
    boolean retainInclusiveRange(long start, long end);

    /**
     * Sets this set to the complement of itself (i.e. in-place), that is, all
     * values previously contained will not be contained anymore, but all values
     * previously not contained will now be contained.
     */
    void invert();

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        if(c instanceof RangedSetImpl)
            return containsAllRangesInclusively(((RangedSetImpl) c).ranges());
        for(Object o : c)
            if(!contains(o))
                return false;
        return true;
    }

    /**
     * Returns the complement of this, i.e. the set that contains exactly those
     * values not contained in this set.
     *
     * @return The complement of this set
     */
    default RangedSet complement() {
        RangedSet complement = clone();
        complement.invert();
        return complement;
    }


    /**
     * Returns whether all given ranges are fully contained in this set.
     *
     * @param ranges The list of ranges to check
     * @return Whether all ranges are contained in this set
     * @see #containsRange(long[])
     */
    default boolean containsAllRanges(long[]... ranges) {
        return containsAllRanges(Arrays.asList(ranges));
    }

    /**
     * Returns whether all given ranges are fully contained in this set.
     *
     * @param ranges The list of ranges to check
     * @return Whether all ranges are contained in this set
     * @see #containsRange(long[])
     */
    default boolean containsAllRanges(Collection<? extends long[]> ranges) {
        for(long[] range : ranges)
            if(!containsRange(range))
                return false;
        return true;
    }

    /**
     * Returns whether all given ranges are fully contained in this set.
     *
     * @param ranges The list of ranges to check, whether both ends are treated as inclusive
     * @return Whether all ranges are contained in this set
     * @see #containsInclusiveRange(long[])
     */
    default boolean containsAllRangesInclusively(long[]... ranges) {
        return containsAllRangesInclusively(Arrays.asList(ranges));
    }

    /**
     * Returns whether all given ranges are fully contained in this set.
     *
     * @param ranges The list of ranges to check, whether both ends are treated as inclusive
     * @return Whether all ranges are contained in this set
     * @see #containsInclusiveRange(long[])
     */
    default boolean containsAllRangesInclusively(Collection<? extends long[]> ranges) {
        for(long[] range : ranges)
            if(!containsInclusiveRange(range))
                return false;
        return true;
    }


    /**
     * Adds all the given values to this set, that aren't already contained.
     *
     * @param values The values to add
     * @return Whether the set changed, i.e. any value was not yet contained
     */
    default boolean addAll(long... values) {
        boolean changed = false;
        for(long l : values)
            changed |= add(l);
        return changed;
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends Long> c) {
        if(c instanceof RangedSet)
            return addAllRangesInclusively(((RangedSet) c).ranges());
        boolean changed = false;
        for(long l : c)
            changed |= add(l);
        return changed;
    }

    /**
     * Adds all values from all given ranges to this set, if they weren't yet contained.
     *
     * @param ranges The ranges to add to this set
     * @return Whether the set changed, i.e. whether any range contained any value not
     *         yet in this set
     * @see #addRange(long[])
     */
    default boolean addAllRanges(long[]... ranges) {
        return addAllRanges(Arrays.asList(ranges));
    }

    /**
     * Adds all values from all given ranges to this set, if they weren't yet contained.
     *
     * @param ranges The ranges to add to this set
     * @return Whether the set changed, i.e. whether any range contained any value not
     *         yet in this set
     * @see #addRange(long[])
     */
    default boolean addAllRanges(Collection<? extends long[]> ranges) {
        boolean changed = false;
        for(long[] range : ranges)
            changed |= addRange(range);
        return changed;
    }

    /**
     * Adds all values from all given ranges to this set, if they weren't yet contained.
     *
     * @param ranges The ranges to add to this set, with both ends treated inclusively
     * @return Whether the set changed, i.e. whether any range contained any value not
     *         yet in this set
     * @see #addInclusiveRange(long[])
     */
    default boolean addAllRangesInclusively(long[]... ranges) {
        return addAllRangesInclusively(Arrays.asList(ranges));
    }

    /**
     * Adds all values from all given ranges to this set, if they weren't yet contained.
     *
     * @param ranges The ranges to add to this set, with both ends treated inclusively
     * @return Whether the set changed, i.e. whether any range contained any value not
     *         yet in this set
     * @see #addInclusiveRange(long[])
     */
    default boolean addAllRangesInclusively(Collection<? extends long[]> ranges) {
        boolean changed = false;
        for(long[] range : ranges)
            changed |= addInclusiveRange(range);
        return changed;
    }

    @Override
    boolean retainAll(@NotNull Collection<?> c);

    /**
     * Removes all given values from this set that were previously contained.
     *
     * @param values The values to be removed
     * @return Whether the set changed, i.e. whether any of the values was previously
     *         in the set
     */
    default boolean removeAll(long... values) {
        boolean changed = false;
        for(long l : values)
            changed |= remove(l);
        return changed;
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        if(c instanceof RangedSet)
            return removeAllRangesInclusively(((RangedSet) c).ranges());
        boolean changed = false;
        for(Object o : c)
            changed |= remove(o);
        return changed;
    }

    /**
     * Removes all values of all given ranges from this set that were previously contained.
     *
     * @param ranges The list of ranges to remove from this set
     * @return Whether this set changed, i.e. whether any value from any of the ranges was
     *         previously in this set
     * @see #removeRange(long[])
     */
    default boolean removeAllRanges(long[]... ranges) {
        return removeAllRanges(Arrays.asList(ranges));
    }

    /**
     * Removes all values of all given ranges from this set that were previously contained.
     *
     * @param ranges The list of ranges to remove from this set
     * @return Whether this set changed, i.e. whether any value from any of the ranges was
     *         previously in this set
     * @see #removeRange(long[])
     */
    default boolean removeAllRanges(Collection<? extends long[]> ranges) {
        boolean changed = false;
        for(long[] range : ranges)
            changed |= removeRange(range);
        return changed;
    }

    /**
     * Removes all values of all given ranges from this set that were previously contained.
     *
     * @param ranges The list of ranges to remove from this set, where both ends are treated
     *               inclusively
     * @return Whether this set changed, i.e. whether any value from any of the ranges was
     *         previously in this set
     * @see #removeInclusiveRange(long[])
     */
    default boolean removeAllRangesInclusively(long[]... ranges) {
        return removeAllRangesInclusively(Arrays.asList(ranges));
    }

    /**
     * Removes all values of all given ranges from this set that were previously contained.
     *
     * @param ranges The list of ranges to remove from this set, where both ends are treated
     *               inclusively
     * @return Whether this set changed, i.e. whether any value from any of the ranges was
     *         previously in this set
     * @see #removeInclusiveRange(long[])
     */
    default boolean removeAllRangesInclusively(Collection<? extends long[]> ranges) {
        boolean changed = false;
        for(long[] range : ranges)
            changed |= removeInclusiveRange(range);
        return changed;
    }

    @Override
    default @Nullable Comparator<? super Long> comparator() {
        return null;
    }

    @Override
    default @NotNull RangedSet subSet(Long fromElement, Long toElement) {
        if(fromElement >= toElement)
            return RangedSubset.EMPTY;
        return new RangedSubset(this, fromElement, toElement - 1);
    }

    @Override
    default @NotNull RangedSet headSet(Long toElement) {
        if(toElement == Long.MIN_VALUE)
            return RangedSubset.EMPTY;
        return new RangedSubset(this, 0, toElement - 1);
    }

    @Override
    default @NotNull RangedSet tailSet(Long fromElement) {
        if(fromElement == Long.MIN_VALUE)
            return this;
        return new RangedSubset(this, fromElement, Long.MAX_VALUE);
    }

    @Override
    Spliterator.@NotNull OfLong spliterator();;

    default LongStream longStream() {
        return StreamSupport.longStream(spliterator(), false);
    }

    default LongStream parallelLongStream() {
        return StreamSupport.longStream(spliterator(), true);
    }


    private static void checkRange(long[] range) {
        if(Arguments.checkNull(range, "range").length != 2)
            throw new IllegalArgumentException("Range must be array with exactly 2 elements");
    }


    static RangedSet allIn(long[] range) {
        checkRange(range);
        return allBetween(range[0], range[1]);
    }

    static RangedSet allBetween(long start, long end) {
        if(end == Long.MIN_VALUE)
            return none();
        return allBetweenInclusive(start, end - 1);
    }

    static RangedSet allInInclusive(long[] range) {
        checkRange(range);
        return allBetweenInclusive(range[0], range[1]);
    }

    static RangedSet allBetweenInclusive(long start, long end) {
        RangedSet set = none();
        set.addInclusiveRange(start, end);
        return set;
    }

    static RangedSet allIn(long[]... ranges) {
        return allIn(Arrays.asList(ranges));
    }

    static RangedSet allIn(Collection<? extends long[]> ranges) {
        RangedSet set = none();
        for(long[] range : ranges)
            set.addRange(range);
        return set;
    }

    static RangedSet allInInclusive(long[]... ranges) {
        return allInInclusive(Arrays.asList(ranges));
    }

    static RangedSet allInInclusive(Collection<? extends long[]> ranges) {
        RangedSet set = none();
        for(long[] range : ranges)
            set.addInclusiveRange(range);
        return set;
    }

    static RangedSet allExcept(long start, long end) {
        return allExceptInclusive(start, end);
    }

    static RangedSet allExceptInclusive(long start, long end) {
        RangedSet set = all();
        set.removeInclusiveRange(start, end);
        return set;
    }

    static RangedSet ofDiscrete(long... values) {
        RangedSetImpl set = new RangedSetImpl();
        set.ranges.ensureCapacity(values.length);
        set.addAll(values);
        return set;
    }

    static RangedSet ofDiscrete(Collection<? extends Long> values) {
        return new RangedSetImpl(values);
    }

    static RangedSet allExcept(long... values) {
        RangedSetImpl set = (RangedSetImpl) all();
        set.ranges.ensureCapacity(values.length + 1);
        set.removeAll(values);
        return set;
    }

    static RangedSet allExcept(Collection<? extends Long> values) {
        RangedSet set = ofDiscrete(values);
        set.invert();
        return set;
    }

    static RangedSet none() {
        return new RangedSetImpl();
    }

    static RangedSet all() {
        RangedSet set = new RangedSetImpl();
        set.addInclusiveRange(Long.MIN_VALUE, Long.MAX_VALUE);
        return set;
    }


    static RangedSet union(RangedSet... sets) {
        return union(Arrays.asList(sets));
    }

    static RangedSet union(Collection<? extends RangedSet> sets) {
        RangedSet union = none();
        for(RangedSet set : sets)
            union.addAll(set);
        return union;
    }

    static RangedSet intersection(RangedSet... sets) {
        return intersection(Arrays.asList(sets));
    }

    static RangedSet intersection(Collection<? extends RangedSet> sets) {
        RangedSet intersection = all();
        for(RangedSet set : sets)
            intersection.retainAll(set);
        return intersection;
    }

    static RangedSet difference(RangedSet a, RangedSet b) {
        RangedSet difference = union(a);
        difference.removeAll(intersection(a,b));
        return difference;
    }






    static Collector<long[], ?, RangedSet> union() {
        return UnionCollector.EXCLUSIVE;
    }

    static Collector<long[], ?, RangedSet> inclusiveUnion() {
        return UnionCollector.INCLUSIVE;
    }

    static Collector<long[], ?, RangedSet> intersection() {
        return IntersectionCollector.EXCLUSIVE;
    }

    static Collector<long[], ?, RangedSet> inclusiveIntersection() {
        return IntersectionCollector.INCLUSIVE;
    }
}

final class UnionCollector implements Collector<long[], RangedSet, RangedSet> {

    private static final Supplier<RangedSet> SUPPLIER = RangedSet::none;
    private static final BinaryOperator<RangedSet> COMBINER = (a, b) -> {
        if(a.rangeCount() < b.rangeCount()) {
            RangedSet tmp = a;
            a = b;
            b = tmp;
        }
        a.addAll(b);
        return a;
    };

    static final UnionCollector EXCLUSIVE = new UnionCollector(RangedSet::addRange);
    static final UnionCollector INCLUSIVE = new UnionCollector(RangedSet::addInclusiveRange);

    private final BiConsumer<RangedSet, long[]> accumulator;

    private UnionCollector(BiConsumer<RangedSet, long[]> accumulator) {
        this.accumulator = accumulator;
    }

    @Override
    public Supplier<RangedSet> supplier() {
        return SUPPLIER;
    }

    @Override
    public BiConsumer<RangedSet, long[]> accumulator() {
        return accumulator;
    }

    @Override
    public BinaryOperator<RangedSet> combiner() {
        return COMBINER;
    }

    @Override
    public Function<RangedSet, RangedSet> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(
                Characteristics.UNORDERED,
                Characteristics.IDENTITY_FINISH
        );
    }
}

final class IntersectionCollector implements Collector<long[], RangedSet, RangedSet> {

    private static final Supplier<RangedSet> SUPPLIER = RangedSet::all;
    private static final BinaryOperator<RangedSet> COMBINER = (a, b) -> {
        if(a.rangeCount() < b.rangeCount()) {
            RangedSet tmp = a;
            a = b;
            b = tmp;
        }
        a.retainAll(b);
        return a;
    };

    static final IntersectionCollector EXCLUSIVE = new IntersectionCollector(RangedSet::retainRange);
    static final IntersectionCollector INCLUSIVE = new IntersectionCollector(RangedSet::retainInclusiveRange);

    private final BiConsumer<RangedSet, long[]> accumulator;

    private IntersectionCollector(BiConsumer<RangedSet, long[]> accumulator) {
        this.accumulator = accumulator;
    }

    @Override
    public Supplier<RangedSet> supplier() {
        return SUPPLIER;
    }

    @Override
    public BiConsumer<RangedSet, long[]> accumulator() {
        return accumulator;
    }

    @Override
    public BinaryOperator<RangedSet> combiner() {
        return COMBINER;
    }

    @Override
    public Function<RangedSet, RangedSet> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(
                Characteristics.UNORDERED,
                Characteristics.IDENTITY_FINISH
        );
    }
}
