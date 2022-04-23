import jdk.jfr.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class RudpTest {
    RudpClient client;

    @Before
    public void setup() throws SocketException, UnknownHostException {
        new RudpServer().start();
        client = new RudpClient();
    }

    @Test
    @Description("Test connection with server")
    public void whenRequestConnection_ThenConnectionIsEstablished() throws IOException {
        client.connectToServer();
        assertTrue(client.isConnected());
    }

    @Test
    @Description("Test sending and receiving data")
    public void whenCanSendAndReceivePacket_thenCorrect() throws IOException {
        client.connectToServer();
        assertTrue(client.isConnected());
        String echo = client.sendEcho("hello server");
        assertEquals("hello server", echo);
        echo = client.sendEcho("server is working");
        assertNotEquals("hello server", echo);
    }

    @After
    public void shutDown() throws IOException {
        client.closeConnection();
    }
}