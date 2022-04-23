import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * This is a Reliable UDP server which echoes the messages it receives back to the client.
 */
public class RudpServer extends Thread {
    private final DatagramSocket socket;      // udp socket
    private boolean connected;                // flag for connection status
    private boolean running;                  // flag for running status

    public RudpServer() throws SocketException {
        socket = new DatagramSocket(Constants.PORT);
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Creates a UDP packet for acknowledging a connection request from the server
     *
     * @param connectionRequestPacket the packet received on the connection request
     * @return the UDP packet
     */
    public DatagramPacket createServerAckPacket(DatagramPacket connectionRequestPacket) {
        // create new packet
        DatagramPacket newPacket = RudpDatagramPacket.copyPacket(connectionRequestPacket);
        // create the reliable header for connection request
        byte[] reliableHeader = RudpDatagramPacket.getReliableHeader(newPacket.getData());
        // set the ACK flag
        RudpDatagramPacket.setACK(reliableHeader);
        // set the acknowledgement number
        RudpDatagramPacket.setACKNumber(reliableHeader, (short) (RudpDatagramPacket.getACKNumber(reliableHeader) + 1));
        // create random sequence number
        RudpDatagramPacket.setSequenceNumber(reliableHeader, (short) (Math.random() * Short.MAX_VALUE));
        // add the new reliable header to the data
        RudpDatagramPacket.modifyReliableHeader(reliableHeader, newPacket.getData());
        // return the rudp datagram packet
        return newPacket;
    }

    /**
     * Creates a UDP packet for acknowledging a connection close request from the server
     *
     * @param connectionCloseRequestPacket the packet received on the connection close request
     * @return the UDP packet
     */
    public DatagramPacket createServerCloseAckPacket(DatagramPacket connectionCloseRequestPacket) {
        // create new packet
        DatagramPacket newPacket = RudpDatagramPacket.copyPacket(connectionCloseRequestPacket);
        // create the reliable header for connection request
        byte[] reliableHeader = RudpDatagramPacket.getReliableHeader(newPacket.getData());
        // set the FIN flag
        RudpDatagramPacket.setFIN(reliableHeader);
        // set the acknowledgement number
        RudpDatagramPacket.setACKNumber(reliableHeader, (short) (RudpDatagramPacket.getACKNumber(reliableHeader) + 1));
        // create random sequence number
        RudpDatagramPacket.setSequenceNumber(reliableHeader, (short) (Math.random() * Short.MAX_VALUE));
        // add the new reliable header to the data
        RudpDatagramPacket.modifyReliableHeader(reliableHeader, newPacket.getData());
        // return the rudp datagram packet
        return newPacket;
    }

    /**
     * Accept a connection from the client
     */
    public void acceptConnection() throws IOException {
        // receive connection request from client
        DatagramPacket requestPacket = RudpDatagramPacket.createEmptyPacket();
        socket.receive(requestPacket);
        System.out.println("Received connection request from " + requestPacket.getAddress().getHostAddress() + ":" +
                requestPacket.getPort());
        // send acknowledgement to client
        socket.send(createServerAckPacket(requestPacket));
        System.out.println("Sent acknowledgement to " + requestPacket.getAddress().getHostAddress() + ":" +
                requestPacket.getPort());
        socket.receive(requestPacket);
        System.out.println("Received acknowledgement from " + requestPacket.getAddress().getHostAddress() + ":");
        connected = true;
        running = true;
    }

    /**
     * Accept a connection close request from the client
     *
     * @param packet the packet received
     */
    public void closeConnection(DatagramPacket packet) throws IOException {
        // send acknowledgement to client
        socket.send(createServerCloseAckPacket(packet));
        // close the socket
        connected = false;
        running = false;
        socket.close();
    }

    @Override
    public void run() {
        try {
            // accept connection from client
            acceptConnection();

            while (running) {
                // create an empty datagram packet to receive data
                DatagramPacket packet = RudpDatagramPacket.createEmptyPacket();

                // if client is not connected, accept the connection
                if (!connected) {
                    acceptConnection();
                }

                // receive data from client
                socket.receive(packet);
                System.out.println(
                        "Received data from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " +
                                packet.getLength());
                // shut down the server if the client sends the end communication message
                if (RudpDatagramPacket.isDisconnectRequest(packet)) {
                    closeConnection(packet);
                    continue;
                }

                // send the data back to the client
                socket.send(packet);
                System.out.println(
                        "Sent data to " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " +
                                packet.getLength());
            }

            // close the socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}