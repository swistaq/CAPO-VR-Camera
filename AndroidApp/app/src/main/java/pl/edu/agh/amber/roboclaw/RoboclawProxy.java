package pl.edu.agh.amber.roboclaw;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.common.AmberProxy;
import pl.edu.agh.amber.common.FutureObject;
import pl.edu.agh.amber.common.proto.CommonProto.DriverHdr;
import pl.edu.agh.amber.common.proto.CommonProto.DriverMsg;
import pl.edu.agh.amber.roboclaw.proto.RoboclawProto;
import pl.edu.agh.amber.roboclaw.proto.RoboclawProto.MotorsSpeed;

import com.google.protobuf.ExtensionRegistry;

/**
 * Proxy used to connect to RoboClaw robot's motors controller.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 * 
 */
public class RoboclawProxy extends AmberProxy {

	private final static int DEVICE_TYPE = 2;

	private int synNum = 100;
	private final ExtensionRegistry extensionRegistry;

	private Map<Integer, FutureObject> futureObjectsMap = new ConcurrentHashMap<Integer, FutureObject>();

	/**
	 * Instantiates Roboclaw proxy which is the basic object used to
	 * communication with robot's motors controller.
	 * 
	 * @param amberClient
	 *            {@link AmberClient} instance
	 * @param deviceID
	 *            ID given to particular Roboclaw instance (most times this
	 *            should be 0)
	 */
	public RoboclawProxy(AmberClient amberClient, int deviceID) {
		super(DEVICE_TYPE, deviceID, amberClient, Logger
				.getLogger("RoboclawProxy"));

		logger.info("Starting and registering RoboclawProxy.");

		extensionRegistry = ExtensionRegistry.newInstance();
		RoboclawProto.registerAllExtensions(extensionRegistry);
	}

	/**
	 * Used to send speed command to robot's motors. All values are in mm/s.
	 * 
	 * This request takes about 6ms to complete on robot.
	 * 
	 * @param frontLeftSpeed
	 *            front left motors's speed
	 * @param frontRightSpeed
	 *            front right motors's speed
	 * @param rearLeftSpeed
	 *            rear left motors's speed
	 * @param rearRightSpeed
	 *            rear right motors's speed
	 * @throws IOException
	 *             thrown on connection problems
	 */
	public void sendMotorsCommand(int frontLeftSpeed, int frontRightSpeed,
			int rearLeftSpeed, int rearRightSpeed) throws IOException {
		logger.fine(String.format("Sending MotorsCommand: %d %d %d %d.",
				frontLeftSpeed, frontRightSpeed, rearLeftSpeed, rearRightSpeed));

		DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
		driverMsgBuilder.setType(DriverMsg.MsgType.DATA);

		MotorsSpeed.Builder commandBuilder = MotorsSpeed.newBuilder();

		commandBuilder.setFrontLeftSpeed(frontLeftSpeed);
		commandBuilder.setFrontRightSpeed(frontRightSpeed);
		commandBuilder.setRearLeftSpeed(rearLeftSpeed);
		commandBuilder.setRearRightSpeed(rearRightSpeed);

		MotorsSpeed motorsSpeed = commandBuilder.build();
		driverMsgBuilder.setExtension(RoboclawProto.motorsCommand, motorsSpeed);
		driverMsgBuilder.setSynNum(getNextSynNum());

		amberClient.sendMessage(buildHeader(), driverMsgBuilder.build());
	}

	/**
	 * Gets current robot's motors speed in mm/s.
	 * 
	 * This requests takes about 8ms to complete on robot.
	 * 
	 * @return {@link MotorsCurrentSpeed} object with current speeds.
	 * @throws IOException
	 *             thrown on connection problems.
	 */
	public MotorsCurrentSpeed getCurrentMotorsSpeed() throws IOException {
		logger.fine("Getting current motors speed.");

		int synNum = getNextSynNum();

		DriverMsg currentSpeedRequestMsg = buildCurrentSpeedRequestMsg(synNum);

		MotorsCurrentSpeed motorsCurrentSpeed = new MotorsCurrentSpeed();
		futureObjectsMap.put(synNum, motorsCurrentSpeed);

		amberClient.sendMessage(buildHeader(), currentSpeedRequestMsg);

		return motorsCurrentSpeed;
	}

	@Override
	public void handleDataMsg(DriverHdr header, DriverMsg message) {
		logger.fine("Handling data message");

		if (!message.hasAckNum() || message.getAckNum() == 0) {

		} else {
			int ackNum = message.getAckNum();

			// TODO: automatically removing abandoned futureObjects
			if (futureObjectsMap.containsKey(ackNum)) {
				FutureObject futureObject = futureObjectsMap.remove(ackNum);

				if (futureObject instanceof MotorsCurrentSpeed) {
					fillMotorsCurrentSpeed((MotorsCurrentSpeed) futureObject,
							message);
				}
			}
		}

	}

	@Override
	public ExtensionRegistry getExtensionRegistry() {
		return extensionRegistry;
	}

	synchronized private int getNextSynNum() {
		return synNum++;
	}

	private void fillMotorsCurrentSpeed(MotorsCurrentSpeed mcs,
			DriverMsg message) {
		MotorsSpeed inputMcs = message.getExtension(RoboclawProto.currentSpeed);

		mcs.setFrontLeftSpeed(inputMcs.getFrontLeftSpeed());
		mcs.setFrontRightSpeed(inputMcs.getFrontRightSpeed());
		mcs.setRearLeftSpeed(inputMcs.getRearLeftSpeed());
		mcs.setRearRightSpeed(inputMcs.getRearRightSpeed());

		mcs.setAvailable();
	}

	private DriverMsg buildCurrentSpeedRequestMsg(int synNum) {

		DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
		driverMsgBuilder.setType(DriverMsg.MsgType.DATA);
		driverMsgBuilder.setExtension(RoboclawProto.currentSpeedRequest, true);

		driverMsgBuilder.setSynNum(synNum);

		return driverMsgBuilder.build();
	}

}
