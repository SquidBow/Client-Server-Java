package practice3.tcp.object;

import java.io.Serializable;

public class TestStudent implements Serializable {

    public String name;
    public int course;

    TestStudent(String name, int course) {
        this.name = name;
        this.course = course;
    }

    public String toString() {
        return "Student " + name + " " + course + " course";
    }
}
