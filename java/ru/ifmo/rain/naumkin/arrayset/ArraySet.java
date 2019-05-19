package ru.ifmo.rain.naumkin.arrayset;

import java.util.*;

public class ArraySet<T extends Comparable<? super T>> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> base;
    private final Comparator<? super T> comparator;

    public ArraySet(Collection<T> collection, Comparator<? super T> comp) {
        Objects.requireNonNull(collection);
        this.comparator = comp;

        if (!collection.isEmpty()) {
            TreeSet<T> ts = new TreeSet<>(comp);
            ts.addAll(collection);
            this.base = Collections.unmodifiableList(new ArrayList<T>(ts));
        } else {
            this.base = Collections.emptyList();
        }
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<T> collection) {
        this(collection, null);
    }

    public ArraySet(Comparator<? super T> comp) {
        this(Collections.emptyList(), comp);
    }

    private ArraySet(List<T> list, Comparator<? super T> comp) {
        this.base = list;
        this.comparator = comp;
    }


    @Override
    public T lower(T e) {
        Objects.requireNonNull(e);
        int i = indexOfLower(e);
        return i == -1 ? null : base.get(i);
    }

    private int indexOfLower(T e) {
        int i = Collections.binarySearch(base, e, comparator);
        if (i >= 0) {
            return i - 1;
        }
        return -(i + 1) - 1;
    }

    @Override
    public T floor(T e) {
        Objects.requireNonNull(e);
        int i = indexOfFloor(e);
        return i == -1 ? null : base.get(i);
    }

    private int indexOfFloor(T e) {
        int i = Collections.binarySearch(base, e, comparator);
        if (i < 0) {
            i = -(i + 1);
            return i - 1;
        } else {
            return i;
        }
    }

    @Override
    public T ceiling(T e) {
        Objects.requireNonNull(e);
        int i = indexOfCeiling(e);
        return i == base.size() ? null : base.get(i);
    }

    private int indexOfCeiling(T e) {
        int i = Collections.binarySearch(base, e, comparator);
        if (i < 0) {
            i = -(i + 1);
        }
        return i;
    }

    @Override
    public T higher(T e) {
        Objects.requireNonNull(e);
        int i = indexOfHigher(e);
        return i == base.size() ? null : base.get(i);
    }

    private int indexOfHigher(T e) {
        int i = Collections.binarySearch(base, e, comparator);
        if (i >= 0) {
            return i + 1;
        } else {
            i = -(i + 1);
            return i;
        }
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return base.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        if (base instanceof ArraySet.ReverseList) {
            return new ArraySet<T>(new ReverseList<>(((ReverseList<T>) base)), Collections.reverseOrder(comparator));
        } else {
            return new ArraySet<>(new ReverseList<>(base, true), Collections.reverseOrder(comparator));
        }
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        Objects.requireNonNull(fromElement);
        Objects.requireNonNull(toElement);
        int from = fromInclusive ? indexOfCeiling(fromElement) : indexOfHigher(fromElement);
        int to = toInclusive ? indexOfFloor(toElement) : indexOfLower(toElement);
        Comparator<? super T> comp = comparator != null ? comparator : Comparator.naturalOrder();
        if (comp.compare(fromElement, toElement) > 0) throw new IllegalArgumentException();
        if (comp.compare(fromElement, toElement) == 0 && (!fromInclusive || !toInclusive)) {
            return new ArraySet<T>(Collections.emptyList(), comparator);
        }
        return new ArraySet<>(base.subList(from, to + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return new ArraySet<>(base.subList(0, (inclusive ? indexOfFloor(toElement) : indexOfLower(toElement)) + 1), comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return new ArraySet<>(base.subList(inclusive ? indexOfCeiling(fromElement) : indexOfHigher(fromElement), base.size()), comparator);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
        return base.get(0);
    }

    @Override
    public T last() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
        return base.get(base.size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(base, (T) o, comparator) >= 0;
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    class ReverseList<T1> extends AbstractList<T1> {
        private List<T1> list;
        private boolean reversed;

        ReverseList(ReverseList<T1> reverseList) {
            list = reverseList.list;
            reversed = !reverseList.reversed;
        }

        ReverseList(List<T1> list, boolean reversed) {
            this.list = list;
            this.reversed = reversed;
        }

        @Override
        public T1 get(int index) {
            return reversed ? list.get(list.size() - index - 1) : list.get(index);
        }

        @Override
        public int size() {
            return list.size();
        }
    }
}