import java.util.*;
import java.util.stream.*;

/**
 * Student Records Data Processor (Pure Java)
 *
 * Loads a set of student records, runs a set of analysis operations
 * over them using the Stream API (map/filter/reduce/sorted), and
 * prints a clearly labeled report to the console. No functions
 * mutate the original student list — each returns new data.
 */
public class StudentRecords {

    // -----------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------

    static class Student {
        final int id;
        final String name;
        final int year;
        final String course;
        final List<Double> grades;
        final boolean enrolled;

        Student(int id, String name, int year, String course,
                List<Double> grades, boolean enrolled) {
            this.id = id;
            this.name = name;
            this.year = year;
            this.course = course;
            // defensive copy so external code can't mutate a student's grades
            this.grades = Collections.unmodifiableList(new ArrayList<>(grades));
            this.enrolled = enrolled;
        }
    }

    // -----------------------------------------------------------------
    // Core analysis functions
    // -----------------------------------------------------------------

    /**
     * Returns a student's average grade. A student with no grades
     * averages to 0 rather than throwing or returning NaN.
     */
    static double getAverageGrade(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("getAverageGrade: student cannot be null.");
        }
        if (student.grades == null || student.grades.isEmpty()) {
            return 0.0;
        }
        return student.grades.stream()
                .mapToDouble(Double::doubleValue)
                .reduce(0.0, Double::sum) / student.grades.size();
    }

    /**
     * Returns the top n students sorted by average grade, descending.
     * Does not mutate the original list.
     */
    static List<Student> getTopStudents(List<Student> students, int n) {
        if (students == null) {
            throw new IllegalArgumentException("getTopStudents: students list cannot be null.");
        }
        if (n < 0) {
            throw new IllegalArgumentException("getTopStudents: n cannot be negative.");
        }

        return students.stream()
                .sorted(Comparator.comparingDouble(StudentRecords::getAverageGrade).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Groups students by their course field.
     */
    static Map<String, List<Student>> groupByCourse(List<Student> students) {
        if (students == null) {
            throw new IllegalArgumentException("groupByCourse: students list cannot be null.");
        }

        return students.stream()
                .collect(Collectors.groupingBy(
                        student -> student.course == null ? "Unspecified" : student.course,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Returns a count of enrolled vs not-enrolled students.
     */
    static Map<String, Integer> getEnrolledCount(List<Student> students) {
        if (students == null) {
            throw new IllegalArgumentException("getEnrolledCount: students list cannot be null.");
        }

        long enrolled = students.stream().filter(student -> student.enrolled).count();
        long notEnrolled = students.size() - enrolled;

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("enrolled", (int) enrolled);
        result.put("notEnrolled", (int) notEnrolled);
        return result;
    }

    /**
     * Case-insensitive search for a student by name.
     * Returns null if no student matches.
     */
    static Student findStudent(List<Student> students, String name) {
        if (students == null) {
            throw new IllegalArgumentException("findStudent: students list cannot be null.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("findStudent: name cannot be null or empty.");
        }

        String target = name.trim();
        return students.stream()
                .filter(student -> student.name != null && student.name.equalsIgnoreCase(target))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns each course's average grade, sorted highest to lowest.
     */
    static Map<String, Double> getCourseAverages(List<Student> students) {
        if (students == null) {
            throw new IllegalArgumentException("getCourseAverages: students list cannot be null.");
        }

        Map<String, List<Student>> grouped = groupByCourse(students);

        Map<String, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Student>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream()
                    .mapToDouble(StudentRecords::getAverageGrade)
                    .average()
                    .orElse(0.0);
            averages.put(entry.getKey(), avg);
        }

        return averages.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Builds a single summary object covering the whole dataset:
     * total students, overall average grade, top-performing student,
     * and a breakdown by course.
     */
    static Map<String, Object> exportSummary(List<Student> students) {
        if (students == null) {
            throw new IllegalArgumentException("exportSummary: students list cannot be null.");
        }

        int totalStudents = students.size();

        double overallAverage = students.isEmpty()
                ? 0.0
                : students.stream()
                    .mapToDouble(StudentRecords::getAverageGrade)
                    .average()
                    .orElse(0.0);

        List<Student> top = getTopStudents(students, 1);
        String topStudentName = top.isEmpty() ? null : top.get(0).name;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", totalStudents);
        summary.put("overallAverageGrade", overallAverage);
        summary.put("topPerformingStudent", topStudentName);
        summary.put("breakdownByCourse", getCourseAverages(students));

        return summary;
    }

    // -----------------------------------------------------------------
    // main()
    // -----------------------------------------------------------------

    public static void main(String[] args) {
        List<Student> students = buildSampleData();

        System.out.println("====================================");
        System.out.println("   STUDENT RECORDS DATA PROCESSOR");
        System.out.println("====================================");

        System.out.println("\n--- TOTAL STUDENTS ---");
        System.out.println("Total Students: " + students.size());

        System.out.println("\n--- OVERALL AVERAGE ---");
        double overallAverage = students.isEmpty() ? 0.0 : students.stream()
                .mapToDouble(StudentRecords::getAverageGrade)
                .average()
                .orElse(0.0);
        System.out.printf("Overall Average: %.2f%n", overallAverage);

        System.out.println("\n--- ENROLLMENT ---");
        Map<String, Integer> enrollment = getEnrolledCount(students);
        System.out.println("Enrolled: " + enrollment.get("enrolled"));
        System.out.println("Not Enrolled: " + enrollment.get("notEnrolled"));

        System.out.println("\n--- TOP 3 STUDENTS ---");
        List<Student> topStudents = getTopStudents(students, 3);
        for (int i = 0; i < topStudents.size(); i++) {
            Student s = topStudents.get(i);
            System.out.printf("%d. %s - %s - Average: %.2f%n",
                    i + 1, s.name, s.course, getAverageGrade(s));
        }

        System.out.println("\n--- STUDENTS BY COURSE ---");
        Map<String, List<Student>> grouped = groupByCourse(students);
        for (Map.Entry<String, List<Student>> entry : grouped.entrySet()) {
            System.out.println("\n" + entry.getKey() + ":");
            entry.getValue().forEach(s -> System.out.println("  - " + s.name));
        }

        System.out.println("\n--- COURSE AVERAGES (highest to lowest) ---");
        Map<String, Double> courseAverages = getCourseAverages(students);
        courseAverages.forEach((course, avg) ->
                System.out.printf("%s: %.2f%n", course, avg));

        System.out.println("\n--- FIND STUDENT ---");
        String[] lookups = { "zairha tauro", "Juan Dela Cruz" };
        for (String name : lookups) {
            Student found = findStudent(students, name);
            if (found != null) {
                System.out.println("Search \"" + name + "\" -> Found: "
                        + found.name + " (" + found.course + ")");
            } else {
                System.out.println("Search \"" + name + "\" -> No matching student found.");
            }
        }

        System.out.println("\n--- EDGE CASE: STUDENT WITH NO GRADES ---");
        Student noGrades = new Student(9999, "No Grades Yet", 1, "BSIT",
                Collections.emptyList(), true);
        List<Student> withEdgeCase = new ArrayList<>(students);
        withEdgeCase.add(noGrades);
        System.out.printf("%s average: %.2f%n", noGrades.name, getAverageGrade(noGrades));

        System.out.println("\n--- SUMMARY ---");
        Map<String, Object> summary = exportSummary(students);
        summary.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n--- EDGE CASE: EMPTY STUDENT LIST ---");
        Map<String, Object> emptySummary = exportSummary(Collections.emptyList());
        emptySummary.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\nReport complete.");
    }

    // -----------------------------------------------------------------
    // Sample data (swap this out for file/database loading if needed)
    // -----------------------------------------------------------------

    static List<Student> buildSampleData() {
        List<Student> students = new ArrayList<>();

        students.add(new Student(1001, "Denmark Day", 2, "BSIT",
                Arrays.asList(90.0, 85.0, 88.0, 92.0), true));

        students.add(new Student(1002, "Zairha Tauro", 3, "BEED",
                Arrays.asList(95.0, 91.0, 89.0, 93.0), true));

        students.add(new Student(1003, "Arcel Dave Alojepan", 1, "BSAB",
                Arrays.asList(80.0, 84.0, 79.0, 85.0), false));

        students.add(new Student(1004, "Joannah Marie Elgario", 3, "BSIT",
                Arrays.asList(94.0, 96.0, 91.0, 95.0), true));

        students.add(new Student(1005, "Mark Allon", 3, "BSIT",
                Arrays.asList(87.0, 90.0, 85.0, 88.0), true));

        return students;
    }
}
