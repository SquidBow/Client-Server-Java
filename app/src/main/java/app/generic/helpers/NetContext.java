package app.generic.helpers;

import com.sun.net.httpserver.HttpExchange;
// import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class NetContext {

    public Socket socket = null;
    // public DatagramSocket dsocket;
    public InetAddress address = null;
    public int port;

    public HttpExchange exchange = null;

    public NetContext(Socket socket) {
        this.socket = socket;
    }

    public NetContext(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public NetContext(HttpExchange exchange) {
        this.exchange = exchange;
    }

    // public Context(InetAddress address, int port, DatagramSocket dsocket) {
    // this.address = address;
    // this.port = port;
    // this.dsocket = dsocket;
    // }
}
