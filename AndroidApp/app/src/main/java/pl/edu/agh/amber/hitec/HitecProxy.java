package pl.edu.agh.amber.hitec;

import com.google.protobuf.ExtensionRegistry;
import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.common.AmberProxy;
import pl.edu.agh.amber.common.proto.CommonProto.DriverHdr;
import pl.edu.agh.amber.common.proto.CommonProto.DriverMsg;
import pl.edu.agh.amber.hitec.proto.HitecProto;
import pl.edu.agh.amber.hitec.proto.HitecProto.SetAngle;
import pl.edu.agh.amber.hitec.proto.HitecProto.SetDifferentAngles;
import pl.edu.agh.amber.hitec.proto.HitecProto.SetSameAngle;
import pl.edu.agh.amber.hitec.proto.HitecProto.SetSpeed;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Proxy used to connect to use Maestro servo motors controller.
 *
 * @author Grzegorz Dziuban
 */
public class HitecProxy extends AmberProxy {

    private final static int DEVICE_TYPE = 7;

    private final ExtensionRegistry extensionRegistry;

    public HitecProxy(AmberClient amberClient, int deviceID) {
        super(DEVICE_TYPE, deviceID, amberClient, Logger.getLogger("HitecProxy"));

        logger.info("Starting and registering HitecProxy.");

        extensionRegistry = ExtensionRegistry.newInstance();
        HitecProto.registerAllExtensions(extensionRegistry);
    }

    @Override
    public void handleDataMsg(DriverHdr header, DriverMsg message) {
        logger.fine("No data messages need to be handled by HitecProxy.");
    }

    @Override
    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    public void setAngle(int address, int angle) {
        SetAngle.Builder commandBuilder = SetAngle.newBuilder();

        commandBuilder.setServoAddress(address);
        commandBuilder.setAngle(angle);

        DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
        driverMsgBuilder.setType(DriverMsg.MsgType.DATA);
        driverMsgBuilder.setExtension(HitecProto.setAngleCommand, commandBuilder.build());

        sendMesssage(driverMsgBuilder.build());
    }
    public void setSpeed(int address, int speed) {
        SetSpeed.Builder commandBuilder = SetSpeed.newBuilder();

        commandBuilder.setServoAddress(address);
        commandBuilder.setSpeed(speed);

        DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
        driverMsgBuilder.setType(DriverMsg.MsgType.DATA);
        driverMsgBuilder.setExtension(HitecProto.setSpeedCommand, commandBuilder.build());

        sendMesssage(driverMsgBuilder.build());
    }

    private void sendMesssage(DriverMsg message) {
        try {
            amberClient.sendMessage(buildHeader(), message);
        } catch (IOException e) {
            logger.severe("Message could not be sent from HitecProxy.");
        }
    }

    public void setSameAngle(int[] addresses, int angle) {
        SetSameAngle.Builder commandBuilder = SetSameAngle.newBuilder();

        for (int i = 0; i < addresses.length; i++) {
            commandBuilder.setServoAddresses(i, addresses[i]);
        }
        commandBuilder.setAngle(angle);

        DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
        driverMsgBuilder.setType(DriverMsg.MsgType.DATA);
        driverMsgBuilder.setExtension(HitecProto.setSameAngleCommand, commandBuilder.build());

        sendMesssage(driverMsgBuilder.build());
    }

    public void setDifferentAngles(int[] addresses, int[] angles) {
        SetDifferentAngles.Builder commandBuilder = SetDifferentAngles.newBuilder();

        if (addresses.length != angles.length) {
            logger.severe("Addresses and angles count must be equal for SetDifferentAngles command");
        }

        for (int i = 0; i < addresses.length; i++) {
            commandBuilder.setServoAddresses(i, addresses[i]);
            commandBuilder.setAngles(i, angles[i]);
        }

        DriverMsg.Builder driverMsgBuilder = DriverMsg.newBuilder();
        driverMsgBuilder.setType(DriverMsg.MsgType.DATA);
        driverMsgBuilder.setExtension(HitecProto.setDifferentAnglesCommand, commandBuilder.build());

        sendMesssage(driverMsgBuilder.build());
    }

}
