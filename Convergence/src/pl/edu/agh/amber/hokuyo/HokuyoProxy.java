package pl.edu.agh.amber.hokuyo;

import com.google.protobuf.ExtensionRegistry;
import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.common.AmberProxy;
import pl.edu.agh.amber.common.CyclicDataListener;
import pl.edu.agh.amber.common.FutureObject;
import pl.edu.agh.amber.common.proto.CommonProto;
import pl.edu.agh.amber.hokuyo.proto.HokuyoProto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Proxy used to connect to Hokuyo sensor.
 *
 * @author Pawel Suder <pawel@suder.info>
 */
public class HokuyoProxy extends AmberProxy {

    /* Magic-constant */
    private final static int DEVICE_TYPE = 4;

    private Map<Integer, FutureObject> futureObjectsMap = new ConcurrentHashMap<Integer, FutureObject>();
    private CyclicDataListener<Scan> scanListener;
    private final ReentrantLock listenerLock = new ReentrantLock();

    private int synNum = 100;
    private final ExtensionRegistry extensionRegistry;

    /**
     * Generic proxy constructor. Must be invoked from subclasses.
     *
     * @param amberClient {@link AmberClient} instance
     * @param deviceID    ID given to particular Roboclaw instance (most times this
     *                    should be 0)
     */
    public HokuyoProxy(AmberClient amberClient, int deviceID) {
        super(DEVICE_TYPE, deviceID, amberClient, Logger.getLogger("HokuyoProxy"));

        logger.info("Starting and registering HokuyoProxy.");

        extensionRegistry = ExtensionRegistry.newInstance();
        HokuyoProto.registerAllExtensions(extensionRegistry);
    }

    public void registerMultiScanListener(CyclicDataListener<Scan> listener) throws IOException {
        logger.fine(String.format("Registering ScanListener"));

        CommonProto.DriverMsg driverMsg = buildSubscribeActionMsg();

        synchronized (listenerLock) {
            scanListener = listener;
        }

        amberClient.sendMessage(buildHeader(), driverMsg);
    }

    public void subscribe(CyclicDataListener<Scan> listener) throws IOException {
        CommonProto.DriverMsg driverMsg = buildSubscribeActionMsg();

        synchronized (listenerLock) {
            scanListener = listener;
        }

        amberClient.sendMessage(buildHeader(), driverMsg);
    }

    private CommonProto.DriverMsg buildSubscribeActionMsg() {
        CommonProto.DriverMsg.Builder driverMsgBuilder = CommonProto.DriverMsg.newBuilder();
        driverMsgBuilder.setType(CommonProto.DriverMsg.MsgType.SUBSCRIBE);
        return driverMsgBuilder.build();
    }

    public void unsubscribe() throws IOException {
        CommonProto.DriverMsg driverMsg = buildUnsubscribeActionMsg();

        synchronized (listenerLock) {
            scanListener = null;
        }

        amberClient.sendMessage(buildHeader(), driverMsg);
    }

    private CommonProto.DriverMsg buildUnsubscribeActionMsg() {
        CommonProto.DriverMsg.Builder driverMsgBuilder = CommonProto.DriverMsg.newBuilder();
        driverMsgBuilder.setType(CommonProto.DriverMsg.MsgType.UNSUBSCRIBE);
        return driverMsgBuilder.build();
    }

    @Override
    public void handleDataMsg(CommonProto.DriverHdr header, CommonProto.DriverMsg message) {
        logger.fine("Handling data message");

        if (!message.hasAckNum() || message.getAckNum() == 0) {

            Scan scan = new Scan();
            fillScan(scan, message);
            scan.setAvailable();

            synchronized (listenerLock) {
                if (scanListener != null) {
                    scanListener.handle(scan);
                }
            }

        } else {
            int ackNum = message.getAckNum();

            // TODO: automatically removing abandoned futureObjects
            if (futureObjectsMap.containsKey(ackNum)) {
                FutureObject futureObject = futureObjectsMap.remove(ackNum);

                if (futureObject != null) {
                    if (futureObject instanceof Scan) {
                        fillScan((Scan) futureObject, message);
                    }
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

    public Scan getSingleScan() throws IOException {
        logger.fine("Get single scan.");

        int synNum = getNextSynNum();

        CommonProto.DriverMsg msg = buildGetSingleScanRequestMsg(synNum);

        Scan singleScan = new Scan();
        futureObjectsMap.put(synNum, singleScan);

        amberClient.sendMessage(buildHeader(), msg);

        return singleScan;
    }

    private CommonProto.DriverMsg buildGetSingleScanRequestMsg(int synNum) {
        CommonProto.DriverMsg.Builder driverMsgBuilder = CommonProto.DriverMsg.newBuilder();

        driverMsgBuilder.setType(CommonProto.DriverMsg.MsgType.DATA);
        driverMsgBuilder.setExtension(HokuyoProto.getSingleScan, true);

        driverMsgBuilder.setSynNum(synNum);

        return driverMsgBuilder.build();
    }

    public void fillScan(Scan singleScan, CommonProto.DriverMsg message) {
        HokuyoProto.Scan scan = message.getExtension(HokuyoProto.scan);

        singleScan.setPoints(scan.getAnglesList(), scan.getDistancesList());

        singleScan.setAvailable();
    }
}
