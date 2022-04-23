import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * This class defines operations for creating and modifying reliable UDP packets
 */
public class RudpDatagramPacket {
    public static final int RELIABLE_HEADER_SIZE = 5;
    public static final int SEQ_NUM_BYTE = 0;
    public static final int ACK_NUM_BYTE = 2;
    public static final int FLAGS_BYTE = 4;
    public static final int SYN_MASK = 0x80;
    public static final int SEQ_MASK = 0x40;
    public static final int ACK_MASK = 0x20;
    public static final int PSH_MASK = 0x10;
    public static final int FIN_MASK = 0x08;
    public static int PAYLOAD_MAX_SIZE = 100;       // max number of bytes in a message
    public static int TIMEOUT = 10000;               // timeout in milliseconds
    public static int MAX_RETRIES = 3;              // max number of retries


    /**
     * Creates an empty packet ready to be filled
     *
     * @return the packet
     */
    public static DatagramPacket createEmptyPacket() {
        return new DatagramPacket(new byte[PAYLOAD_MAX_SIZE], PAYLOAD_MAX_SIZE);
    }

    /**
     * Adds the reliable header to the given packet payload
     *
     * @param reliableHeader the reliable header to be added
     * @param data           the data to be added to
     * @return the data with the reliable header added
     */
    public static byte[] assemblePayload(byte[] reliableHeader, byte[] data) {
        byte[] payload = new byte[reliableHeader.length + data.length];
        System.arraycopy(reliableHeader, 0, payload, 0, reliableHeader.length);
        System.arraycopy(data, 0, payload, reliableHeader.length, data.length);
        return payload;
    }

    /**
     * Returns the data from the packet payload (excluding reliable header)
     *
     * @param packet the packet
     * @return the data
     */
    public static byte[] getData(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength() - RELIABLE_HEADER_SIZE];
        System.arraycopy(packet.getData(), RELIABLE_HEADER_SIZE, data, 0, data.length);
        return data;
    }

    /**
     * Modifies the reliable header of the payload given with the
     *
     * @param reliableHeader the new reliable header
     * @param data           the data to be modified
     */
    public static void modifyReliableHeader(byte[] reliableHeader, byte[] data) {
        System.arraycopy(reliableHeader, 0, data, 0, RELIABLE_HEADER_SIZE);
    }

    /**
     * Get the reliable header from the given packet payload
     *
     * @param data the packet payload
     * @return the reliable header
     */
    public static byte[] getReliableHeader(byte[] data) {
        byte[] reliableHeader = new byte[RELIABLE_HEADER_SIZE];
        System.arraycopy(data, 0, reliableHeader, 0, RELIABLE_HEADER_SIZE);
        return reliableHeader;
    }

    /**
     * Creates an empty reliable header with all flags reset
     *
     * @return the reliable header
     */
    public static byte[] createEmptyHeader() {
        return new byte[RELIABLE_HEADER_SIZE];
    }

    public static boolean verifyConnectionRequest(DatagramPacket requestPacket) {
        return isSYN(getReliableHeader(requestPacket.getData()));
    }

    public static boolean verifyClientAck(DatagramPacket ackPacket) {
        return isACK(getReliableHeader(ackPacket.getData()));
    }

    public static boolean verifyServerAck(DatagramPacket requestPacket, DatagramPacket ackPacket) {
        return isSYN(getReliableHeader(ackPacket.getData())) && isACK(getReliableHeader(ackPacket.getData())) &&
                (getSequenceNumber(getReliableHeader(requestPacket.getData())) + 1 ==
                        getACKNumber(getReliableHeader(ackPacket.getData())));
    }

    /**
     * Creates the payload with given data by adding the reliable header
     *
     * @param data               the UDP payload
     * @param destinationAddress the destination address
     * @param destinationPort    the destination port
     * @return the payload with the reliable header
     */
    public static DatagramPacket createDataPacket(byte[] data, InetAddress destinationAddress, int destinationPort) {
        // create reliable header
        byte[] reliableHeader = createEmptyHeader();
        // set PSH flag
        setPSH(reliableHeader);
        // assemble payload
        byte[] payload = assemblePayload(reliableHeader, data);
        // create packet
        return new DatagramPacket(payload, payload.length, destinationAddress, destinationPort);
    }

    public static DatagramPacket copyPacket(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
        return new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
    }

    /**
     * Creates a UDP packet for closing a connection
     *
     * @param destinationAddress destination address
     * @param destinationPort    destination port
     * @return the UDP packet
     */
    public static DatagramPacket createDisconnectRequestPacket(InetAddress destinationAddress, int destinationPort) {
        // create empty packet
        DatagramPacket newPacket = createEmptyPacket();
        // create the reliable header for connection request
        byte[] reliableHeader = RudpDatagramPacket.createEmptyHeader();
        // set the FIN flag
        RudpDatagramPacket.setFIN(reliableHeader);
        // data is empty
        byte[] data = new byte[0];
        // assemble the payload
        newPacket.setData(RudpDatagramPacket.assemblePayload(reliableHeader, data));
        // set the destination address and port
        newPacket.setAddress(destinationAddress);
        newPacket.setPort(destinationPort);
        // return the rudp datagram packet
        return newPacket;
    }

    /**
     * Checks if given packet is a disconnect request
     *
     * @param packet the packet to be checked
     * @return true if the packet is a disconnect request, false otherwise
     */
    public static boolean isDisconnectRequest(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte[] reliableHeader = RudpDatagramPacket.getReliableHeader(data);
        return RudpDatagramPacket.isFIN(reliableHeader);
    }

    /**
     * Sets the sequence number in the reliable header in network byte order.
     *
     * @param reliableHeader the reliable header
     * @param sequenceNumber the sequence number (in host byte order)
     */
    public static void setSequenceNumber(byte[] reliableHeader, short sequenceNumber) {
        reliableHeader[SEQ_NUM_BYTE + 1] = (byte) (sequenceNumber >> 8);
        reliableHeader[SEQ_NUM_BYTE] = (byte) (sequenceNumber & 0xFF);
    }

    /**
     * Gets the sequence number from the reliable header in host byte order.
     *
     * @param reliableHeader the reliable header
     * @return the sequence number (in host byte order)
     */
    public static short getSequenceNumber(byte[] reliableHeader) {
        return (short) ((reliableHeader[SEQ_NUM_BYTE + 1] << 8) | reliableHeader[SEQ_NUM_BYTE]);
    }

    /**
     * Sets the ACK number in the reliable header in network byte order
     *
     * @param reliableHeader the reliable header
     * @param ackNumber      the ACK number (in host byte order)
     */
    public static void setACKNumber(byte[] reliableHeader, short ackNumber) {
        reliableHeader[ACK_NUM_BYTE + 1] = (byte) (ackNumber >> 8);
        reliableHeader[ACK_NUM_BYTE] = (byte) (ackNumber & 0xFF);
    }

    /**
     * Gets the ACK number from the reliable header in host byte order
     *
     * @param reliableHeader the reliable header
     * @return the ACK number (in host byte order)
     */
    public static short getACKNumber(byte[] reliableHeader) {
        return (short) ((reliableHeader[ACK_NUM_BYTE + 1] << 8) | reliableHeader[ACK_NUM_BYTE]);
    }

    public static void setSYN(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] |= SYN_MASK;
    }

    public static void resetSYN(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] &= ~SYN_MASK;
    }

    public static void setSEQ(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] |= SEQ_MASK;
    }

    public static void resetSEQ(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] &= ~SEQ_MASK;
    }

    public static void setACK(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] |= ACK_MASK;
    }

    public static void resetACK(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] &= ~ACK_MASK;
    }

    public static void setPSH(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] |= PSH_MASK;
    }

    public static void resetPSH(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] &= ~PSH_MASK;
    }

    public static void setFIN(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] |= FIN_MASK;
    }

    public static void resetFIN(byte[] reliableHeader) {
        reliableHeader[FLAGS_BYTE] &= ~FIN_MASK;
    }

    public static boolean isSYN(byte[] reliableHeader) {
        return (reliableHeader[FLAGS_BYTE] & SYN_MASK) != 0;
    }

    public static boolean isSEQ(byte[] reliableHeader) {
        return (reliableHeader[FLAGS_BYTE] & SEQ_MASK) != 0;
    }

    public static boolean isACK(byte[] reliableHeader) {
        return (reliableHeader[FLAGS_BYTE] & ACK_MASK) != 0;
    }

    public static boolean isPSH(byte[] reliableHeader) {
        return (reliableHeader[FLAGS_BYTE] & PSH_MASK) != 0;
    }

    public static boolean isFIN(byte[] reliableHeader) {
        return (reliableHeader[FLAGS_BYTE] & FIN_MASK) != 0;
    }
}
