package practice4;

public class Main {

    public static void main(String[] args) {
//        Db db = new SqlLiteDb("students.db");
        Db db = new MySqlDb("jdbc:mysql://localhost:3306/my_db", "root", "root"); // requires 'docker compose up' in terminal to start mysql docker container

        System.out.println("Number of students: " + db.count());

        db.insert(new Student("s1", "s1", 1));
        db.insert(new Student("s2", "s2", 2));
        int s3 = db.insert(new Student("s3", "s3", 3));

        System.out.println("Number of students: " + db.count());

        System.out.println("All students: " + db.getAll());
        System.out.println("Student by id: " + db.getById(s3));
        System.out.println("Student by unknown id: " + db.getById(1000000000));
    }

}
