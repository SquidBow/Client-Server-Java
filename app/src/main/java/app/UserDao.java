package practice1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final List<User> db = new ArrayList<>();
    private int idAutoIncrement = 0;

    public int add(User user) {
        user.setId(++idAutoIncrement);
        db.add(user);
        return idAutoIncrement;
    }

    public Optional<User> getById(int id) {
        return db.stream()
            .filter(u -> u.getId() == id)
            .findFirst();
    }

    public void removeById(int id) {
        db.removeIf(u -> u.getId() == id);
    }

    public int count() {
        return db.size();
    }

    public void reset() {
        db.clear();
        idAutoIncrement = 0;
    }
}