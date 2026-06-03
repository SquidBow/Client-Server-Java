package practice4;

import java.util.List;
import java.util.Optional;

public interface Db {

    int insert(Student student);

    int count();

    List<Student> getAll();

    Optional<Student> getById(int id);

    int deleteAll();

}
