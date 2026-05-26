package app;

import java.net.InetAddress;

public interface ISender {
    void sendMessage(byte[] mess, InetAddress target);
}
