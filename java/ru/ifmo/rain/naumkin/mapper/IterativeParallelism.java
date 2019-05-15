package ru.ifmo.rain.naumkin.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private ParallelMapper parallelMapper;

    public IterativeParallelism(ParallelMapper mapper) {
        parallelMapper = mapper;
    }

    private <T, R> List<R> transform(int threads, List<? extends T> values,
                                     Function<List<? extends T>, ? extends R> f) throws InterruptedException {

        threads = Math.min(threads, values.size());
        int singleThreadDataCount = values.size() / threads;
        int rem = values.size() % threads;

        List<Thread> threadPool = new ArrayList<>(Collections.nCopies(threads, null));
        List<List<? extends T>> result = new LinkedList<>(Collections.nCopies(threads, null));

        int rr = 0;
        for(int i = 0; i < threads; ++i) {
            int blockSize = values.size() / threads + (rem-- > 0 ? 1 : 0);
            final int k = i;
            final int l = rr;
            final int r = l + blockSize;
            rr = r;
            result.set(k, values.subList(l, r));
        }

        return parallelMapper.map(f, result);
    }


    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        if(values.isEmpty()) {
            throw new IllegalArgumentException("No elements in values");
        }
        Function<List<?>, ? extends String> map = (s) -> s.stream().map(Objects::toString).reduce(String::concat).orElse("");
        Function<List<? extends String>, ? extends String> reduce = (s) -> s.stream().collect(Collectors.joining());
        return map.apply(transform(threads, values, map));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        if(values.isEmpty()) {
            throw new IllegalArgumentException("No elements in values");
        }
        Function<List<? extends T>, ? extends List<T>> map = (s) -> s.stream().filter(predicate).collect(Collectors.toList());
        Function<List<? extends List<T>>, ? extends List<T>> reduce = (s) -> s.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return reduce.apply(transform(threads, values, map));
    }

    @Override
    public <T, R> List<R> map(int threads, List<? extends T> values, Function<? super T, ? extends R> f) throws InterruptedException {
        if(values.isEmpty()) {
            throw new IllegalArgumentException("No elements in values");
        }
        Function<List<? extends T>, ? extends List<? extends R>> map = (s) -> s.stream().map(f).collect(Collectors.toList());
        Function<List<? extends List<? extends R>>, ? extends List<R>> reduce = (s) -> s.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return reduce.apply(transform(threads, values, map));
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if(values.isEmpty()) {
            throw new IllegalArgumentException("No elements in values");
        }

        Function<List<? extends T>, ? extends T>  maxComp = (s) -> Collections.max(s, comparator);
        return maxComp.apply(transform(threads, values, maxComp));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        if(values.isEmpty()) {
            throw new IllegalArgumentException("No elements in values");
        }
        Function<List<? extends T>, ? extends Boolean> map = (s) -> s.stream().anyMatch(predicate);
        Function<List<? extends Boolean>, ? extends Boolean> reduce = (s) -> s.stream().anyMatch(Boolean::booleanValue);

        return reduce.apply(transform(threads, values, map));


    }
}
