package ru.ifmo.rain.naumkin.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements StudentGroupQuery {

    private static List<String> mapStudents(List<Student> students, Function<Student, String> mapper) {
        return students.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    private static String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private static List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private List<Group> sortGroups(Collection<Student> students, Function<List<Student>,
                                                    List<Student>> functor) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream()
                .map(s -> new Group(s.getKey(), functor.apply(s.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private String maxGroupName(Collection<Student> students, Function<List<Student>, Integer> functor) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream()
                .max(Comparator
                .comparingInt((Map.Entry<String, List<Student>> entry) -> functor.apply(entry.getValue()))
                .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    private static <T> T filterSortStudents(Collection<Student> students, Predicate<Student> predicate,
                                                    Collector<Student, ?, T> collector) {
        return students.stream()
                .filter(predicate)
                .sorted(Comparator
                        .comparing(Student::getLastName)
                        .thenComparing(Student::getFirstName)
                        .thenComparing(Student::getId))
                .collect(collector);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudents(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudents(students, Student::getLastName);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudents(students, StudentDB::getFullName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapStudents(students, Student::getGroup);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(mapStudents(students, Student::getFirstName));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getId))
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, Comparator
                .comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparing(Student::getId));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterSortStudents(students, s -> s.getFirstName().equals(name), Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterSortStudents(students, s -> s.getLastName().equals(name), Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterSortStudents(students, s -> s.getGroup().equals(group), Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterSortStudents(students, s -> s.getGroup().equals(group),
                Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    //    @Override
//    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, String group) {
//        return null;

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortGroups(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortGroups(students, this::sortStudentsById);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return maxGroupName(students, List::size);
    }


    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return maxGroupName(students, list -> getDistinctFirstNames(list).size());
    }

}

