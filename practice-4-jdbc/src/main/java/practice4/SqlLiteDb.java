package practice4;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlLiteDb implements Db {

    private final Connection connection;

    public SqlLiteDb(String dbName) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create SQLite DB", e);
        }

        init();
    }

    @Override
    public int insert(Student student) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO student(first_name, last_name, course) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, student.getFirstName());
            ps.setString(2, student.getLastName());
            ps.setInt(3, student.getCourse());

            int inserted = ps.executeUpdate();
            if (inserted < 1) {
                throw new RuntimeException("Insert failed");
            }

            ResultSet generatedKeys = ps.getGeneratedKeys();
            return generatedKeys.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Can't insert student: " + student, e);
        }
    }

    @Override
    public int count() {
        try (PreparedStatement ps = connection.prepareStatement("select count(*) from student")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count students", e);
        }
    }

    @Override
    public List<Student> getAll() {
        try (PreparedStatement ps = connection.prepareStatement("select * from student")) {
            List<Student> students = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(new Student(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getInt("course")));
                }
            }

            return students;
        } catch (SQLException e) {
            throw new RuntimeException("Can't get students", e);
        }
    }

    @Override
    public Optional<Student> getById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("select * from student where id = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Student(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getInt("course")));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't get student by id: " + id, e);
        }
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("delete from student")) {
            int numberOfRows = ps.executeUpdate();
            return numberOfRows;
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete students", e);
        }
    }

    private void init() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS student (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name VARCHAR(30) not null,
                    last_name VARCHAR(30) not null,
                    course int(11) not null
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while DB init", e);
        }
    }

}
