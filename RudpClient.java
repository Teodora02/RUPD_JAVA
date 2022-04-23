import java.io.IOException;
import java.net.*;

/**
 * This is a Reliable UDP client which sends messages to the echo server and prints the response
 */
public class RudpClient {
    private final DatagramSocket socket;  // udp socket
    private final InetAddress serverAddress;    // ipv4 address of the server
    private final int serverPort;
    private boolean connected;

    public RudpClient() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(Constants.SERVER_NAME);
        serverPort = Constants.PORT;
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Creates a UDP packet for requesting a connection
     *
     * @return the UDP packet
     */
    public DatagramPacket createConnectionRequestPacket() {
        // create the reliable header for connection request
        byte[] reliableHeader = RudpDatagramPacket.createEmptyHeader();
        // set the SYN flag
        RudpDatagramPacket.setSYN(reliableHeader);
        // create random sequence number
        RudpDatagramPacket.setSequenceNumber(reliableHeader, (short) (Math.random() * Short.MAX_VALUE));
        // data is empty
        byte[] data = new byte[0];
        // assemble the payload
        byte[] payload = RudpDatagramPacket.assemblePayload(reliableHeader, data);
        // create the rudp datagram packet
        return new DatagramPacket(payload, payload.length, serverAddress, serverPort);
    }

    /**
     * Creates a UDP packet for acknowledging the server's acknowledgement (from the client)
     *
     * @param serverAckPacket the packet received on the server's acknowledgement
     * @return the UDP packet
     */
    public DatagramPacket createClientAckPacket(DatagramPacket serverAckPacket) {
        // create new packet
        DatagramPacket ackPacket = RudpDatagramPacket.copyPacket(serverAckPacket);
        // create the reliable header for connection request
        byte[] reliableHeader = RudpDatagramPacket.getReliableHeader(ackPacket.getData());
        // reset the SYN flag
        RudpDatagramPacket.resetSYN(reliableHeader);
        // set the ACK flag
        RudpDatagramPacket.setACK(reliableHeader);
        // set the acknowledgement number
        RudpDatagramPacket.setACKNumber(reliableHeader,
                (short) (RudpDatagramPacket.getSequenceNumber(reliableHeader) + 1));
        // reset the sequence number
        RudpDatagramPacket.setSequenceNumber(reliableHeader, (short) 0);
        // add the new reliable header to the data
        RudpDatagramPacket.modifyReliableHeader(reliableHeader, ackPacket.getData());
        // set address and port
        ackPacket.setAddress(serverAddress);
        ackPacket.setPort(serverPort);
        // return the rudp datagram packet
        return ackPacket;
    }

    /**
     * Connect to the server using 3-way handshake.
     */
    public void connectToServer() throws IOException {
        // create the connection request
        DatagramPacket requestPacket = createConnectionRequestPacket();
        socket.send(requestPacket);
        System.out.println("Connection request sent.");
        // receive the acknowledgement
        DatagramPacket ackPacket = RudpDatagramPacket.createEmptyPacket();
        // verify the acknowledgement
        socket.receive(ackPacket);
        System.out.println("Connection acknowledgement received.");
        // send the acknowledgement
        socket.send(createClientAckPacket(ackPacket));
        System.out.println("Connection acknowledgement sent.");
        // set the connected flag
        connected = true;
    }

    /**
     * Send a message to the server, waiting for a response.
     *
     * @param msg content of the message
     * @return the response from the server
     */
    public String sendEcho(String msg) throws IOException {
        // extract the message bytes
        byte[] data = msg.getBytes();
        // create a datagram packet with the reliable header
        DatagramPacket packet = RudpDatagramPacket.createDataPacket(data, serverAddress, serverPort);
        // send the packet
        socket.send(packet);
        System.out.println("Message sent. " + packet.getLength());
        // receive the response
        socket.receive(packet);
        System.out.println("Message received. " + packet.getLength());
        // return the response message
        byte[] receivedMsg = RudpDatagramPacket.getData(packet);
        return new String(receivedMsg, 0, receivedMsg.length);
    }

    /**
     * Close the connection to the server.
     */
    public void closeConnection() throws IOException {
        // send the disconnection request
        socket.send(RudpDatagramPacket.createDisconnectRequestPacket(serverAddress, serverPort));
        // receive the acknowledgement from the server
        DatagramPacket packet = RudpDatagramPacket.createEmptyPacket();
        socket.receive(packet);
        // send the acknowledgement to the server
        socket.send(createClientAckPacket(packet));
        // close the socket
        connected = false;
        socket.close();
    }
}
