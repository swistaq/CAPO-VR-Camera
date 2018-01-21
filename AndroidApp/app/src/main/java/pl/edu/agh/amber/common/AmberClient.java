package pl.edu.agh.amber.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.keyvalue.MultiKey;

import pl.edu.agh.amber.common.proto.CommonProto.DriverHdr;
import pl.edu.agh.amber.common.proto.CommonProto.DriverMsg;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Class used to communicate with robot.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 *  
 */
public class AmberClient {

	private final DatagramSocket socket;
	private final InetAddress address;
	private final int port;

	private boolean terminated = false;

	private final static int RECEIVING_BUFFER_SIZE = 16384;
	private final static int DEFAULT_PORT = 26233;

	private Map<MultiKey, AmberProxy> proxyMap = new HashMap<MultiKey, AmberProxy>();
	private Thread receivingThread;

	private static Logger logger = Logger.getLogger("AmberClient");

	/**
	 * Instantiates AmberClient object.
	 * 
	 * @param hostname
	 *			robot's hostname
	 * @param port
	 *			robot's listening port (most times 26233)
	 * @throws IOException
	 *			 thrown on connection problem.
	 */
	public AmberClient(String hostname, int port) throws IOException {

		this.socket = new DatagramSocket();
		this.address = InetAddress.getByName(hostname);
		this.port = port;

		logger.info(String.format(
				"Starting AmberClient, remote address: %s, port: %d.",
				hostname, port));

		receivingThread = new Thread(this::messageReceivingLoop);

		receivingThread.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				terminate();
			}
		});
	}

	/**
	 * Instantiates AmberClient object.
	 * 
	 * @param hostname
	 *			robot's hostname
	 * @throws IOException
	 *			 thrown on connection problem.
	 */
	public AmberClient(String hostname) throws IOException {
		this(hostname, DEFAULT_PORT);
	}

	/**
	 * Registers {@link AmberProxy} in client.
	 * 
	 * @param deviceType
	 *			device type ID
	 * @param deviceID
	 *			device instance ID
	 * @param proxy
	 *			{@link AmberProxy} object.
	 */
	public void registerClient(int deviceType, int deviceID, AmberProxy proxy) {
		proxyMap.put(new MultiKey(deviceType, deviceID), proxy);
	}

	/**
	 * Sends message to the robot.
	 * 
	 * @param header
	 *			Protobuf's {@link DriverHdr}, message header.
	 * @param message
	 *			Prototobuf's {@link DriverMsg}, message contents.
	 * @throws IOException
	 *			 thrown on connection problem.
	 */
	synchronized public void sendMessage(DriverHdr header, DriverMsg message)
			throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		int len;

		// Header length
		len = header.getSerializedSize();
		outputStream.write((byte) (len >> 8) & 0xff);
		outputStream.write((byte) (len & 0xff));

		// Header
		outputStream.write(header.toByteArray());

		// Message length
		len = message.getSerializedSize();
		outputStream.write((byte) (len >> 8) & 0xff);
		outputStream.write((byte) (len & 0xff));

		// Message
		outputStream.write(message.toByteArray());

		logger.fine(String.format("Sending an UDP packet for (%d: %d).",
				header.getDeviceType(), header.getDeviceID()));

		DatagramPacket packet = new DatagramPacket(outputStream.toByteArray(),
				outputStream.size(), address, port);
		socket.send(packet);
	}

	/**
	 * Terminates client.
	 */
	public void terminate() {
		if (terminated) {
			return;
		}

		terminated = true;

		logger.info("Terminating.");
		terminateProxies();
		socket.close();
		receivingThread.interrupt();
	}

	/**
	 * Terminates all registered proxies.
	 */
	public void terminateProxies() {
		for (AmberProxy proxy : proxyMap.values()) {
			proxy.terminateProxy();
		}
	}

	public static short getShortFromBigEndianRange(byte[] range) {
		return (short) ((range[0] << 8) + (range[1] & 0xff));
	}

	private void messageReceivingLoop() {
		byte[] buf = new byte[RECEIVING_BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buf, RECEIVING_BUFFER_SIZE);
		AmberProxy clientProxy = null;

		while (true) {
			try {
				logger.finest("Entering socket.receive().");
				socket.receive(packet);

				byte[] packetBytes = packet.getData();

				int headerLen = (packetBytes[0] << 8) + (packetBytes[1] & 0xff);
				ByteString headerByteString = ByteString.copyFrom(packet.getData(), 2, headerLen);
				DriverHdr header = DriverHdr.parseFrom(headerByteString);

				int messageLen = (packetBytes[2 + headerLen] << 8) + (packetBytes[2 + headerLen + 1] & 0xff);
				ByteString messageByteString = ByteString.copyFrom(packet.getData(), 2 + headerLen + 2, messageLen);
				DriverMsg message;

				if (!header.hasDeviceType() || !header.hasDeviceID() || header.getDeviceType() == 0) {
					message = DriverMsg.parseFrom(messageByteString);
					handleMessageFromMediator(header, message);

				} else {
					clientProxy = proxyMap.get(new MultiKey(header.getDeviceType(), header.getDeviceID()));

					if (clientProxy == null) {
						logger.warning(String.format(
								"Client proxy with given device type (%d) and ID (%d) not found, ignoring message.",
								header.getDeviceType(), header.getDeviceID()
						));
						continue;
					}

					message = DriverMsg.parseFrom(messageByteString, clientProxy.getExtensionRegistry());
					handleMessageFromDriver(header, message, clientProxy);
				}

			} catch (InvalidProtocolBufferException ex) {
				logger.warning("Error in parsing the message, ignoring.");

			} catch (IOException e) {

				if (socket.isClosed()) {
					logger.fine("Socket closed, exiting.");
					return;
				}

				logger.warning("Error in receiving packet: " + e);
			}
		}
	}

	private void handleMessageFromMediator(DriverHdr header, DriverMsg message) {

		switch (message.getType()) {
		case DATA:
			logger.warning("DATA message came, but device details not set, ignoring.");
			break;

		case PING:
			logger.fine("PING message came, handling.");
			handlePingMessage(header, message);
			break;

		case PONG:
			logger.fine("PONG message came, handling.");
			handlePongMessage(header, message);
			break;

		case DRIVER_DIED:
			logger.warning("DRIVER_DIED message came, but device details not set, ignoring.");
			break;

		default:
			logger.warning(String.format(
					"Unexpected message came: %s, ignoring.", message.getType()
							.toString()));
		}

	}

	private void handleMessageFromDriver(DriverHdr header, DriverMsg message,
			AmberProxy clientProxy) {

		switch (message.getType()) {
		case DATA:
			logger.fine(String.format(
					"DATA message came for (%d: %d), handling.",
					clientProxy.deviceType, clientProxy.deviceID));
			clientProxy.handleDataMsg(header, message);
			break;

		case PING:
			logger.fine(String.format(
					"PING message came for (%d: %d), handling.",
					clientProxy.deviceType, clientProxy.deviceID));
			clientProxy.handlePingMessage(header, message);
			break;

		case PONG:
			logger.fine(String.format(
					"PONG message came for (%d: %d), handling.",
					clientProxy.deviceType, clientProxy.deviceID));
			clientProxy.handlePongMessage(header, message);
			break;

		case DRIVER_DIED:
			logger.fine(String.format(
					"DRIVER_DIED message came dor (%d: %d), handling.",
					clientProxy.deviceType, clientProxy.deviceID));
			clientProxy.handleDriverDiedMessage(header, message);
			break;

		default:
			logger.warning(String.format(
					"Unexpected message came %s for (%d: %d), ignoring.",
					message.getType().toString(), clientProxy.deviceType,
					clientProxy.deviceID));
		}

	}

	private void handlePingMessage(DriverHdr header, DriverMsg message) {

	}

	private void handlePongMessage(DriverHdr header, DriverMsg message) {

	}

}
