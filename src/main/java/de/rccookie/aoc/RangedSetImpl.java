package de.rccookie.aoc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.rccookie.util.EmptyIteratorException;
import de.rccookie.util.ListStream;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

class RangedSetImpl implements RangedSet {

    final ArrayList<long[]> ranges;

    public RangedSetImpl() {
        ranges = new ArrayList<>();
    }

    public RangedSetImpl(Collection<? extends Long> copy) {
        if(copy instanceof RangedSetImpl) {
            List<long[]> ranges = ((RangedSetImpl) copy).ranges;
            this.ranges = new ArrayList<>(ranges.size());
            for(long[] r : ranges)
                this.ranges.add(r.clone());
        }
        else if(copy instanceof RangedSet)
            this.ranges = new ArrayList<>(((RangedSet) copy).ranges());
        else {
            this.ranges = new ArrayList<>();
            addAll(copy);
        }
    }




    static String toString(RangedSet set) {
        Stream<long[]> ranges = set instanceof RangedSetImpl ? ((RangedSetImpl) set).ranges.stream() : set.ranges();
        return "{" + ranges.map(r -> r[0] != r[1] ? r[0]+":"+r[1] : r[0]+"").collect(Collectors.joining(", ")) + "}";
    }

    static int hashCode(RangedSet set) {
        if(!(set instanceof RangedSetImpl))
            return (int) set.ranges().mapToLong(r -> (r[1] * (r[1]+1) - r[0] * (r[0]+1)) / 2).sum();
        long hash = 0;
        for(long[] range : ((RangedSetImpl) set).ranges)
            hash += (range[1] * (range[1]+1) - range[0] * (range[0]+1)) / 2;
        return (int) hash;
    }

    static boolean equals(RangedSet set, Object obj) {
        if(obj == set)
            return true;
        if(!(obj instanceof RangedSet))
            return set.longSize() <= Integer.MAX_VALUE && Utils.equals(set, obj);
        List<long[]> ranges = set instanceof RangedSetImpl ? ((RangedSetImpl) set).ranges : set.ranges().useAsList();
        List<long[]> otherRanges = obj instanceof RangedSetImpl ? ((RangedSetImpl) obj).ranges : ((RangedSet) obj).ranges().useAsList();
        if(ranges.size() != otherRanges.size())
            return false;
        for(int i=0; i<ranges.size(); i++) {
            long[] a = ranges.get(i), b = otherRanges.get(i);
            if(a[0] != b[0] || a[1] != b[1])
                return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return toString(this);
    }

    @Override
    public int hashCode() {
        return hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return equals(this, obj);
    }

    @Override
    public @NotNull RangedSetImpl clone() {
        return new RangedSetImpl(this);
    }

    private long longSize0() {
        long size = 0;
        for(long[] r : ranges)
            size += r[1] - r[0] + 1;
        return size;
    }

    @Override
    public BigInteger exactSize() {
        return new BigInteger(Long.toUnsignedString(longSize0()));
    }

    @Override
    public long longSize() {
        long size = longSize0();
        return size < 0 ? Long.MAX_VALUE : size;
    }

    @Override
    public int rangeCount() {
        return ranges.size();
    }

    @Override
    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    private int rangeIndex(long value) {
        int low = 0;
        int high = ranges.size() - 1;

        while(low <= high) {
            int mid = (low + high) >>> 1;
            long[] midVal = ranges.get(mid);

            if(midVal[1] < value)
                low = mid + 1;
            else if(midVal[0] > value)
                high = mid - 1;
            else
                return mid;
        }
        return -(low + 1); // Not in any range
    }

    private int removeRanges(int fromIndex, int toIndex) {
        if(fromIndex >= toIndex)
            return 0;
        if(fromIndex == 0 && toIndex == ranges.size())
            ranges.clear();
        else if(fromIndex == toIndex + 1)
            ranges.remove(fromIndex);
        else ranges.subList(fromIndex, toIndex).clear();
        return toIndex - fromIndex;
    }

    @Override
    public boolean contains(long value) {
        return rangeIndex(value) >= 0;
    }

    @Override
    public boolean containsInclusiveRange(long start, long end) {
        if(start > end)
            return true;
        int index = rangeIndex(start);
        return index >= 0 && ranges.get(index)[1] >= end;
    }

    @Override
    public boolean containsAnyInInclusiveRange(long start, long end) {
        if(start > end)
            return false;
        int startIndex = rangeIndex(start);
        return startIndex >= 0 || startIndex != rangeIndex(end);
    }

    @Override
    public @NotNull PrimitiveIterator.OfLong iterator() {
        return new RangeIterator(this);
    }

    @Override
    public ListStream<long[]> ranges() {
        return ListStream.of(ranges).map(long[]::clone);
    }

    @Override
    public long @NotNull[] toLongArray() {
        long size = longSize();
        if(size > Integer.MAX_VALUE)
            throw new OutOfMemoryError("Cannot allocate array longer than Integer.MAX_VALUE");
        long[] arr = new long[(int) size];
        int index = 0;
        for(long[] range : ranges) {
            for(long i=range[0]; i<range[1]; i++)
                arr[index++] = i;
            arr[index++] = range[1];
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        long size = longSize();
        if(size > Integer.MAX_VALUE)
            throw new OutOfMemoryError("Cannot allocate array longer than Integer.MAX_VALUE");
        if(a.length < size)
            a = Arrays.copyOf(a, (int) size);
        int index = 0;
        for(long[] range : ranges) {
            for(long i=range[0]; i<range[1]; i++)
                a[index++] = (T) (Long) i;
            a[index++] = (T) (Long) range[1];
        }
        if(a.length > size)
            a[(int) size] = null;
        return a;
    }

    @Override
    public boolean add(long value) {
        int index = rangeIndex(value);
        if(index >= 0)
            return false;
        index = -index - 1;

        boolean before = index > 0 && ranges.get(index)[1] == value - 1;
        boolean after = index < ranges.size() && ranges.get(index)[0] == value + 1;

        if(before) {
            if(after)
                ranges.get(index - 1)[1] = ranges.remove(index)[1];
            else ranges.get(index - 1)[1] = value;
        }
        else {
            if(after)
                ranges.get(index)[0] = value;
            else ranges.add(index, new long[] { value, value });
        }
        return true;
    }

    @Override
    public boolean addInclusiveRange(long start, long end) {
        if(start > end)
            return false;

        int startIndex = rangeIndex(start);
        if(startIndex < 0) {
            startIndex = -startIndex - 1;
            if(startIndex == ranges.size() || ranges.get(startIndex)[0] - 1 > end) {
                ranges.add(startIndex, new long[] { start, end });
                return true;
            }
            if(startIndex > 0 && ranges.get(startIndex - 1)[1] + 1 == start)
                startIndex--;
        }

        long[] range = ranges.get(startIndex);
        if(range[0] <= start && end <= range[1])
            return false;

        int removeFrom = startIndex + 1;
        int removeTo = startIndex + 1;
        for(; removeTo < ranges.size(); removeTo++) {
            long[] r = ranges.get(removeTo);
            if(end < r[0] - 1)
                break;
        }

        range[0] = Math.min(range[0], start);
        range[1] = Math.max(Math.max(range[1], end), ranges.get(removeTo - 1)[1]);
        removeRanges(removeFrom, removeTo);

        return true;
    }

    @Override
    public boolean remove(long value) {
        int index = rangeIndex(value);
        if(index < 0)
            return false;

        long[] range = ranges.get(index);
        boolean before = range[0] < value;
        boolean after = range[1] > value;

        if(before) {
            if(after) {
                ranges.add(index + 1, new long[] { value + 1, range[1] });
                range[1] = value - 1;
            }
            else range[1]--;
        }
        else {
            if(after)
                range[0]++;
            else ranges.remove(index);
        }
        return true;
    }

    @Override
    public boolean removeInclusiveRange(long start, long end) {
        if(start > end)
            return false;

        int startIndex = rangeIndex(start);
        if(startIndex < 0) {
            startIndex = -startIndex - 1;
            if(startIndex == ranges.size())
                return false;
        }

        int endIndex = startIndex;
        while(endIndex + 1 < ranges.size() && ranges.get(endIndex + 1)[0] <= end)
            endIndex++;

        long[] startRange = ranges.get(startIndex);
        long[] endRange = ranges.get(endIndex);

        boolean startRemains = start > startRange[0];
        boolean endRemains = end < endRange[1];
        if(startRemains && endRemains && startIndex == endIndex) {
            ranges.add(new long[] { end + 1, startRange[1] });
            startRange[1] = start - 1;
            return true;
        }

        if(startRemains) {
            startRange[1] = start - 1;
            startIndex++;
        }
        if(endRemains) {
            endRange[0] = end + 1;
            endIndex--;
        }

        removeRanges(startIndex, endIndex + 1);
        return true;
    }

    @Override
    public boolean retainInclusiveRange(long start, long end) {
        if(isEmpty())
            return false;
        if(start > end) {
            clear();
            return true;
        }

        boolean changed = false;

        int endIndex = rangeIndex(end);
        if(endIndex < 0)
            endIndex = -endIndex - 1;
        else {
            long[] range = ranges.get(endIndex);
            changed |= range[1] != (range[1] = end);
        }
        changed |= removeRanges(-endIndex - 1, ranges.size()) != 0;

        int startIndex = rangeIndex(start);
        if(startIndex < 0)
            startIndex = -startIndex - 1;
        else {
            long[] range = ranges.get(startIndex);
            changed |= range[0] != (range[0] = start);
        }
        changed |= removeRanges(0, -startIndex - 1) != 0;

        return changed;
    }

    @Override
    public void invert() {
        if(ranges.isEmpty()) {
            ranges.add(new long[] { Long.MIN_VALUE, Long.MAX_VALUE });
            return;
        }

        long[] firstRange = ranges.get(0);

        long lastEnd;
        int off;
        if(firstRange[0] == Long.MIN_VALUE) {
            lastEnd = firstRange[1];
            off = 1;
        }
        else {
            lastEnd = Long.MAX_VALUE; // === Long.MIN_VALUE - 1;
            off = 0;
        }

        for(int i=0; i<ranges.size()-off; i++) {
            long[] range = ranges.get(i);
            long[] other = ranges.get(i + off);
            long start = lastEnd + 1, end = other[0] - 1;
            lastEnd = other[1];
            range[0] = start;
            range[1] = end;
        }

        long[] lastRange = ranges.get(ranges.size() - 1);
        if(lastRange[1] == Long.MAX_VALUE) {
            if(off != 0)
                ranges.remove(ranges.size() - 1);
            else {
                lastRange[1] = lastRange[0] - 1;
                lastRange[0] = lastEnd + 1;
            }
        }
        else {
            if(off != 0) {
                lastRange[1] = lastRange[0] + 1;
                lastRange[0] = Long.MAX_VALUE;
            }
            else ranges.add(new long[] { lastEnd + 1, Long.MAX_VALUE });
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if(c instanceof RangedSetImpl)
            return containsAllRangesInclusively(((RangedSetImpl) c).ranges);
        return RangedSet.super.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        if(c instanceof RangedSetImpl)
            return addAllRangesInclusively(((RangedSetImpl) c).ranges);
        return RangedSet.super.addAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        RangedSet complement;
        if(c instanceof RangedSet)
            complement = new RangedSetImpl((RangedSet) c);
        else {
            complement = new RangedSetImpl();
            for(Object o : c)
                if(o instanceof Long)
                    complement.add((long) o);
        }
        complement.invert();
        return removeAll(complement);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if(c instanceof RangedSetImpl)
            return removeAllRangesInclusively(((RangedSetImpl) c).ranges);
        return RangedSet.super.removeAll(c);
    }

    @Override
    public void clear() {
        ranges.clear();
    }

    @Override
    public Long first() {
        if(ranges.isEmpty())
            throw new NoSuchElementException();
        return ranges.get(0)[0];
    }

    @Override
    public Long last() {
        if(ranges.isEmpty())
            throw new NoSuchElementException();
        return ranges.get(ranges.size() - 1)[1];
    }

    @Override
    public Spliterator.@NotNull OfLong spliterator() {
        return new Splitter(ranges);
    }

    static class Splitter implements Spliterator.OfLong {

        final List<long[]> ranges;
        int fromRange;
        final int toRange;
        RangeSplitter currentRange;
        boolean sized = false;

        Splitter(List<long[]> ranges) {
            this.ranges = ranges;
            this.fromRange = 0;
            this.toRange = ranges.size();
            if(fromRange < toRange)
                currentRange = new RangeSplitter(ranges.get(fromRange));
        }

        private Splitter(List<long[]> ranges, int fromRange, int toRange, RangeSplitter currentRange) {
            this.ranges = ranges;
            this.fromRange = fromRange;
            this.toRange = toRange;
            this.currentRange = currentRange;
        }

        @Override
        public OfLong trySplit() {
            if(fromRange >= toRange)
                return null;
            if(toRange - fromRange == 1)
                return currentRange.trySplit();
            int split = fromRange + (toRange - fromRange) / 2;
            Splitter splitter = new Splitter(ranges, fromRange, split, currentRange);
            fromRange = split;
            currentRange = new RangeSplitter(ranges.get(fromRange));
            return splitter;
        }

        @Override
        public long estimateSize() {
            if(currentRange == null)
                return 0;
            long size = currentRange.end - currentRange.start + 1;
            for(int i=fromRange+1; i<toRange; i++) {
                long[] range = ranges.get(i);
                size += range[1] - range[0] + 1;
            }
            if(size <= 0 && (!currentRange.done || fromRange + 1 < toRange))
                return Long.MAX_VALUE;
            return size;
        }

        @Override
        public long getExactSizeIfKnown() {
            long size = estimateSize();
            return size == Long.MAX_VALUE ? -1 : size;
        }

        @Override
        public int characteristics() {
            if(!sized)
                sized = estimateSize() != Long.MAX_VALUE;
            return Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SORTED | (sized ? Spliterator.SIZED | Spliterator.SUBSIZED : 0);
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(fromRange >= toRange)
                return false;
            while(!currentRange.tryAdvance(action)) {
                if(++fromRange >= toRange)
                    return false;
                long[] range = ranges.get(fromRange);
                currentRange = new RangeSplitter(range[0], range[1]);
            }
            return true;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }
    }

    private static class RangeSplitter implements Spliterator.OfLong {
        private long start;
        private final long end;
        private boolean done;

        RangeSplitter(long[] range) {
            this(range[0], range[1]);
        }

        RangeSplitter(long start, long end) {
            this.start = start;
            this.end = end;
            this.done = start > end;
        }

        @Override
        public OfLong trySplit() {
            if(start == end || start + 1 == end)
                return null;
            long split = start + (end - start) / 2;
            RangeSplitter prefix = new RangeSplitter(start, split);
            start = split + 1;
            return prefix;
        }

        @Override
        public long estimateSize() {
            if(done)
                return 0;
            long size = end - start + 1;
            return size <= 0 ? Long.MAX_VALUE : size;
        }

        @Override
        public int characteristics() {
            int sized = done || end - start + 1 > 0 ? Spliterator.SIZED | Spliterator.SUBSIZED : 0;
            return Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SORTED | sized;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(done)
                return false;
            done = start == end;
            action.accept(start++);
            return true;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }
    }



    static final class RangeIterator implements PrimitiveIterator.OfLong {
        final RangedSet set;
        final List<long[]> ranges;
        int rangeIndex = -1;
        long next = 1;
        long rangeEnd = 0;
        long last;
        boolean canRemove = false;

        private RangeIterator(RangedSetImpl set) {
            this.set = set;
            this.ranges = set.ranges;
        }

        RangeIterator(RangedSet set) {
            this.set = set;
            this.ranges = set.ranges();
        }

        @Override
        public long nextLong() {
            if(!hasNext())
                throw new EmptyIteratorException();
            canRemove = true;
            return last = next++;
        }

        @Override
        public boolean hasNext() {
            if(next <= rangeEnd)
                return true;
            if(++rangeIndex >= ranges.size())
                return false;
            long[] range = ranges.get(rangeIndex);
            next = range[0];
            rangeEnd = range[1];
            return true;
        }

        @Override
        public void remove() {
            if(!canRemove)
                throw new IllegalStateException("Cannot call remove() without previous call to next()");
            canRemove = false;

            int size = ranges.size();
            set.remove(last);
            rangeIndex -= ranges.size() - size;
        }
    };
}
