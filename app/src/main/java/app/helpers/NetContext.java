package app.helpers;

// import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class NetContext {

    public Socket socket;
    // public DatagramSocket dsocket;
    public InetAddress address = null;
    public int port;

    public NetContext(Socket socket) {
        this.socket = socket;
    }

    public NetContext(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    // public Context(InetAddress address, int port, DatagramSocket dsocket) {
    //     this.address = address;
    //     this.port = port;
    //     this.dsocket = dsocket;
    // }
}
