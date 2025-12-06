package de.rccookie.aoc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.LongConsumer;

import de.rccookie.util.ArgumentOutOfRangeException;
import de.rccookie.util.EmptyIteratorException;
import de.rccookie.util.ListStream;
import org.jetbrains.annotations.NotNull;

final class RangedSubset implements RangedSet {

    public static final RangedSet EMPTY = new RangedSet() {
        @Override
        public String toString() {
            return "{}";
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Set<?> && ((Set<?>) obj).isEmpty();
        }

        @Override
        public @NotNull RangedSet clone() {
            return RangedSet.none();
        }

        @Override
        public BigInteger exactSize() {
            return BigInteger.ZERO;
        }

        @Override
        public long longSize() {
            return 0;
        }

        @Override
        public int rangeCount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(long value) {
            return false;
        }

        @Override
        public boolean containsInclusiveRange(long start, long end) {
            return start > end;
        }

        @Override
        public boolean containsAnyInInclusiveRange(long start, long end) {
            return false;
        }

        @Override
        public @NotNull PrimitiveIterator.OfLong iterator() {
            return new PrimitiveIterator.OfLong() {
                @Override
                public long nextLong() {
                    throw new EmptyIteratorException();
                }

                @Override
                public boolean hasNext() {
                    return false;
                }
            };
        }

        @Override
        public ListStream<long[]> ranges() {
            return ListStream.empty();
        }

        @Override
        public long @NotNull [] toLongArray() {
            return new long[0];
        }

        @Override
        public <T> T @NotNull [] toArray(T @NotNull [] a) {
            if(a.length != 0)
                a[0] = null;
            return a;
        }

        @Override
        public boolean add(long value) {
            throw new ArgumentOutOfRangeException("Cannot add value outside of subset range");
        }

        @Override
        public boolean addInclusiveRange(long start, long end) {
            if(start > end)
                return false;
            throw new ArgumentOutOfRangeException("Cannot add value outside of subset range");
        }

        @Override
        public boolean remove(long value) {
            return false;
        }

        @Override
        public boolean removeInclusiveRange(long start, long end) {
            return false;
        }

        @Override
        public boolean retainInclusiveRange(long start, long end) {
            return false;
        }

        @Override
        public void invert() {
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return false;
        }

        @Override
        public Spliterator.@NotNull OfLong spliterator() {
            return new Spliterator.OfLong() {
                @Override
                public OfLong trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return 0;
                }

                @Override
                public int characteristics() {
                    return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.SORTED | Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE;
                }

                @Override
                public boolean tryAdvance(LongConsumer action) {
                    return false;
                }
            };
        }

        @Override
        public Long first() {
            throw new NoSuchElementException();
        }

        @Override
        public Long last() {
            throw new NoSuchElementException();
        }

        @Override
        public void clear() {
        }
    };


    final RangedSet base;
    final long start, end;

    RangedSubset(RangedSet base, long start, long end) {
        this.base = base;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return RangedSetImpl.toString(this);
    }

    @Override
    public int hashCode() {
        return RangedSetImpl.hashCode(this);
    }

    public boolean equals(Object obj) {
        return RangedSetImpl.equals(this, obj);
    }

    @Override
    public @NotNull RangedSet clone() {
        return RangedSet.ofDiscrete(this);
    }

    private long size0() {
        return ranges().mapToLong(r -> r[1] - r[0] + 1).sum();
    }

    @Override
    public BigInteger exactSize() {
        return new BigInteger(Long.toUnsignedString(size0()));
    }

    @Override
    public long longSize() {
        long size = size0();
        if(size == 0 && isEmpty())
            return 0;
        return size <= 0 ? Long.MAX_VALUE : size;
    }

    @Override
    public int rangeCount() {
        return (int) ranges().count();
    }

    @Override
    public boolean isEmpty() {
        return !base.containsAnyInInclusiveRange(start, end);
    }

    @Override
    public boolean contains(long value) {
        return start <= value && value <= end && base.contains(value);
    }

    @Override
    public boolean containsInclusiveRange(long start, long end) {
        return (start > end) || (this.start <= start && end <= this.end && base.containsInclusiveRange(start, end));
    }

    @Override
    public boolean containsAnyInInclusiveRange(long start, long end) {
        if(start > end)
            return false;
        return base.containsAnyInInclusiveRange(Math.max(this.start, start), Math.min(this.end, end));
    }

    @Override
    public @NotNull PrimitiveIterator.OfLong iterator() {
        return new RangedSetImpl.RangeIterator(this);
    }

    @Override
    public ListStream<long[]> ranges() {
        return base.ranges().filter(r ->
                (r[0] <= start && start <= r[1]) ||
                (r[0] <= end && end <= r[1]) ||
                (start <= r[0] && r[0] <= end)
        ).peek(r -> {
            r[0] = Math.max(r[0], start);
            r[1] = Math.min(r[1], end);
        });
    }

    @Override
    public long @NotNull [] toLongArray() {
        long size = longSize();
        if(size > Integer.MAX_VALUE)
            throw new OutOfMemoryError("Cannot allocate array longer than Integer.MAX_VALUE");
        long[] arr = new long[(int) size];
        int index = 0;
        for(long[] range : ranges()) {
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
        for(long[] range : ranges()) {
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
        if(value < start || value > end)
            throw new ArgumentOutOfRangeException("Value outside of subset range");
        return base.add(value);
    }

    @Override
    public boolean addInclusiveRange(long start, long end) {
        if(start < this.start || end > this.end)
            throw new ArgumentOutOfRangeException("Value outside of subset range");
        return base.addInclusiveRange(start, end);
    }

    @Override
    public boolean remove(long value) {
        return start <= value && value <= end && base.remove(value);
    }

    @Override
    public boolean removeInclusiveRange(long start, long end) {
        return base.removeInclusiveRange(Math.max(this.start, start), Math.min(this.end, end));
    }

    @Override
    public boolean retainInclusiveRange(long start, long end) {
        return retain0(RangedSet.allBetweenInclusive(start, end));
    }

    @Override
    public void invert() {
        RangedSet newInRange = base.clone();
        newInRange.invert();
        newInRange.retainInclusiveRange(start, end);
        base.removeInclusiveRange(start, end);
        base.addAll(newInRange);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        RangedSet retain;
        if(c instanceof RangedSet)
            retain = ((RangedSet) c).clone();
        else {
            retain = RangedSet.none();
            for(Object o : c)
                if(o instanceof Long)
                    retain.add((Long) o);
        }
        return base.removeAll(retain);
    }

    private boolean retain0(RangedSet retainCopy) {
        retainCopy.addRange(0, start);
        if(end != Long.MAX_VALUE)
            retainCopy.addInclusiveRange(end + 1, Long.MAX_VALUE);
        retainCopy.invert();
        return base.removeAll(retainCopy);
    }

    @Override
    public void clear() {
        base.removeInclusiveRange(start, end);
    }

    @Override
    public @NotNull RangedSet subSet(Long fromElement, Long toElement) {
        if(fromElement >= toElement)
            return RangedSubset.EMPTY;
        if(fromElement <= start && end < toElement)
            return this;
        return new RangedSubset(base, Math.max(start, fromElement), Math.min(end, toElement - 1));
    }

    @Override
    public @NotNull RangedSet headSet(Long toElement) {
        if(toElement > end)
            return this;
        if(toElement == Long.MIN_VALUE)
            return EMPTY;
        return new RangedSubset(base, Long.MIN_VALUE, end);
    }

    @Override
    public @NotNull RangedSet tailSet(Long fromElement) {
        if(fromElement <= start)
            return this;
        return new RangedSubset(base, start, Long.MAX_VALUE);
    }

    @Override
    public Long first() {
        return ranges().findFirst().get()[0];
    }

    @Override
    public Long last() {
        List<long[]> ranges = ranges();
        return ranges.get(ranges.size() - 1)[1];
    }

    @Override
    public Spliterator.@NotNull OfLong spliterator() {
        return new RangedSetImpl.Splitter(ranges());
    }
}
