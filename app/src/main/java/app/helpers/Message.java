package app.helpers;

public class Message {

    public int command_id;
    public int user_id;
    public String message;

    public Message() {}

    public Message(int command_id, int user_id, String message) {
        this.command_id = command_id;
        this.user_id = user_id;
        this.message = message;
    }
}
