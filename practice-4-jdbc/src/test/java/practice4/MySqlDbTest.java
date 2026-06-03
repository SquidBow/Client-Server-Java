package practice4;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDbTest extends BaseMySqlTest {

    private Db mySqlDb;

    @BeforeEach
    void setup() {
        mySqlDb = new MySqlDb(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());

        mySqlDb.insert(new Student("test1", "test1", 1));
        mySqlDb.insert(new Student("test2", "test2", 2));
        mySqlDb.insert(new Student("test3", "test3", 3));
    }

    @AfterEach
    void cleanUp() {
        int number = mySqlDb.deleteAll();
        System.out.printf("Removed %s students", number);
    }

    @Test
    void shouldIncreaseCountAfterInsert() {
        int countBefore = mySqlDb.count();

        mySqlDb.insert(new Student("test4", "test4", 4));

        assertThat(mySqlDb.count())
            .isEqualTo(countBefore + 1);
    }

    @Test
    void shouldGetStudentById() {
        int id = mySqlDb.insert(new Student("test4", "test4", 4));

        assertThat(mySqlDb.getById(id))
            .isPresent()
            .get()
            .isEqualTo(new Student(id, "test4", "test4", 4));
    }


}