package app;

import static org.junit.jupiter.api.Assertions.*;

import app.client.network.http.AppClientHTTP;
import app.generic.helpers.Message;
import app.server.logic.QueueManager;
import app.server.network.http.StoreServerHTTP;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpServerTest {

    StoreServerHTTP server;
    AppClientHTTP client;

    @BeforeEach
    void setUp() {
        QueueManager queue = new QueueManager();
        server = new StoreServerHTTP(queue);
        client = new AppClientHTTP();
    }

    @AfterEach
    void cleanUp() {
        server.stopServer();
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        Message loginMsg = new Message(0, 0, "Admin Admin%%%admin");
        Message response = client.sendRequest(loginMsg);
        assertNotNull(response);
        assertTrue(response.message.equals("Manager"));
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {
        Message loginMsg = new Message(0, 0, "Admin Admin%%%wrong");
        Message response = client.sendRequest(loginMsg);
        assertNull(response);
    }
}
