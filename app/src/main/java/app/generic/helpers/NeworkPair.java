package app.generic.helpers;

public class NeworkPair<T> {

    public final T data;
    public final NetContext context;

    public NeworkPair(T data, NetContext context) {
        this.data = data;
        this.context = context;
    }
}

// Get data = byte[]
// Decrypt
// Get data = Message (with string embeded_message)
// Process
// Get data = Message
// Encrypt
// Get data = byte[]
// Send
