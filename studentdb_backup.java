package ru.ifmo.rain.naumkin.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;

import java.util.*;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedStudentGroupQuery {
    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(s -> s.getFirstName() + " " + s.getLastName(),
                        Collectors.mapping(Student::getGroup, Collectors.toSet())))
                .entrySet().stream()
                .max(Comparator
                        .comparingInt((Map.Entry<String, Set<String>> entry) -> entry.getValue().size())
                        .thenComparing(Map.Entry::getKey, String::compareTo))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .stream()
                .map(s -> new Group(s,
                        students.stream()
                                .filter(student -> student
                                        .getGroup()
                                        .equals(s)).sorted(Comparator.comparing(Student::getLastName)
                                                            .thenComparing(Student::getFirstName))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .stream()
                .map(s -> new Group(s,
                        students.stream()
                                .filter(student -> student
                                        .getGroup()
                                        .equals(s)).sorted(Comparator.comparing(Student::getId))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .collect(Collectors.groupingBy(s->s, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .stream()
                .min(Comparator.comparing(Map.Entry::getKey))
                .get()
                .getKey();
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .distinct()
                .collect(Collectors.toList())
                .stream()
                .map(s -> new Group(s,
                        students.stream()
                                .filter(student -> student
                                        .getGroup()
                                        .equals(s))
                                .distinct()
                                .collect(Collectors.toList())))
                .collect(Collectors.groupingBy(s->s, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .stream()
                .min(Comparator.comparing(s -> s.getKey().getName()))
                .get()
                .getKey().getName();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return students.stream()
                .map(Student::getLastName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return students.stream()
                .map(Student::getGroup)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return students.stream()
                .map(student -> student.getFirstName() + " " + student.getLastName())
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getId))
                .collect(Collectors.toList())
                .get(0)
                .getFirstName();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return students.stream()
                .filter(student -> student.getFirstName().equals(name))
                .sorted(Comparator.comparing(Student::getLastName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return students.stream()
                .filter(student -> student.getLastName().equals(name))
                .sorted(Comparator.comparing(Student::getFirstName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .sorted(Comparator.comparing(Student::getLastName)
                        .thenComparing(Student::getFirstName))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return null;
    }

//    @Override
//    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, String group) {
//        return null;
//    }
}


