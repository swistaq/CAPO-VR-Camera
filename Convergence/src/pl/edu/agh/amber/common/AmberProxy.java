package pl.edu.agh.amber.common;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import pl.edu.agh.amber.common.proto.CommonProto.DriverHdr;
import pl.edu.agh.amber.common.proto.CommonProto.DriverMsg;

import com.google.protobuf.ExtensionRegistry;

/**
 * Abstract class used to create proxies that connects to robot's devices.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 * 
 */
public abstract class AmberProxy {

	protected final AmberClient amberClient;
	protected final int deviceType;
	protected final int deviceID;

	protected Logger logger;

	/**
	 * Generic proxy constructor. Must be invoked from subclasses.
	 * 
	 * @param deviceType
	 *            device type ID
	 * @param deviceID
	 *            device instance ID
	 * @param amberClient
	 *            {@link AmberClient} instance
	 * @param logger
	 *            {@link Logger} instance
	 */
	public AmberProxy(int deviceType, int deviceID, AmberClient amberClient,
			Logger logger) {
		this.deviceType = deviceType;
		this.deviceID = deviceID;
		this.amberClient = amberClient;
		this.logger = logger;

		amberClient.registerClient(deviceType, deviceID, this);
	}

	public abstract void handleDataMsg(DriverHdr header, DriverMsg message);

	/**
	 * Invoked to handle incoming ping message.
	 * 
	 * @param header
	 *            Protobuf's {@link DriverHdr} object, with message header
	 * @param message
	 *            Protobuf's {@link DriverMsg} object, with message contents
	 */
	public void handlePingMessage(DriverHdr header, DriverMsg message) {
	};

	/**
	 * Invoked to handle incoming pong message.
	 * 
	 * @param header
	 *            Protobuf's {@link DriverHdr} object, with message header
	 * @param message
	 *            Protobuf's {@link DriverMsg} object, with message contents
	 */
	public void handlePongMessage(DriverHdr header, DriverMsg message) {
	};

	/**
	 * Invoked to handle incoming "driver died" message.
	 * 
	 * @param header
	 *            Protobuf's {@link DriverHdr} object, with message header
	 * @param message
	 *            Protobuf's {@link DriverMsg} object, with message contents
	 */
	public void handleDriverDiedMessage(DriverHdr header, DriverMsg message) {
	};

	/**
	 * Must be overriden by proxy and return {@link ExtensionRegistry} with all
	 * extensions registered.
	 * 
	 * @return {@link ExtensionRegistry} object.
	 */
	public abstract ExtensionRegistry getExtensionRegistry();

	protected DriverHdr buildHeader() {
		DriverHdr.Builder driverHdrBuilder = DriverHdr.newBuilder();
		driverHdrBuilder.setDeviceType(deviceType);
		driverHdrBuilder.setDeviceID(deviceID);

		return driverHdrBuilder.build();
	}

	/**
	 * Sends "client died" message and terminates the proxy.
	 */
	public void terminateProxy() {
		logger.info("Terminating proxy.");

		DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
		driverMsgBuilder.setType(DriverMsg.MsgType.CLIENT_DIED);

		try {
			amberClient.sendMessage(buildHeader(), driverMsgBuilder.build());
		} catch (IOException e) {
			logger.warning("Error in sending terminate message");
		}
	}

}
