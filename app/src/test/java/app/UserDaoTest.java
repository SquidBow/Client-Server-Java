package practice1;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDaoTest {
    private final UserDao sut = new UserDao();

    @BeforeEach
    void setUp() {
        sut.add(new User("John Smith", "John.Smith@mail.com"));
        sut.add(new User("Rebecca Roffe", "Rebecca.Roffe@mail.com"));
    }

    @AfterEach
    void cleanUp() {
        sut.reset();
    }

    @Test
    void shouldAddUserToDb() {
        int countBefore = sut.count();
        sut.add(new User("New User", "new.user@email.com"));

        Assertions.assertThat(sut.count())
            .isEqualTo(countBefore + 1);
    }

    @Test
    void shouldGetUserById() {
        int id = sut.add(new User("My New User", "my.new.user@email.com"));

        Assertions.assertThat(sut.getById(id))
            .isPresent()
            .get()
            .returns("My New User", User::getName)
            .returns("my.new.user@email.com", User::getEmail);
    }
}