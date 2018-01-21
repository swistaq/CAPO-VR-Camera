import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hitec.HitecProxy;
import pl.edu.agh.amber.hokuyo.HokuyoProxy;
import pl.edu.agh.amber.hokuyo.Scan;

import java.io.IOException;

public class Convergence {

    //maksymalna i minimalna odległość skanu hokuyo w milimetrach
    private static final int HOKUYO_MAX = 4000;
    private static final int HOKUYO_MIN = 300;
    //liczba kroków pomiędzy równoległym i maksymalnie zbieżnym ustawieniem serwomotorów
    private static final int NUM_STEPS = 13;
    private static final int DIVISOR = (HOKUYO_MAX - HOKUYO_MIN) / NUM_STEPS;

    public static void main(String[] args) throws Exception{
        AmberClient client;
        try {
            client = new AmberClient("127.0.0.1", 26233);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        HokuyoProxy hokuyoProxy = new HokuyoProxy(client, 0);
        HitecProxy hitecProxy = new HitecProxy(client, 0);
        int leftZero = 87;
        int leftAddress = 5;
        int rightZero = 75;
        int rightAddress = 4;
        hitecProxy.setAngle(leftAddress, leftZero);
        hitecProxy.setAngle(rightAddress, rightZero);

        while (true) {
            Scan scan = hokuyoProxy.getSingleScan();
            float dist = getDist(scan);
//            System.out.println("Distance: "+dist);
            int deltaAngle = getDelta(dist);

            hitecProxy.setAngle(leftAddress, leftZero - deltaAngle);
            hitecProxy.setAngle(rightAddress,rightZero + deltaAngle);
            Thread.sleep(150);
        }

//        client.terminate();
    }

    // przeliczenie odległości na obrót kamery
    private static int getDelta(float dist) {
        if (dist < HOKUYO_MIN) return 13;
        if (dist > HOKUYO_MAX) return 0;
        return (int) (NUM_STEPS - ((dist - HOKUYO_MIN)/DIVISOR));
    }

    // obliczenie odległości jako średniej z dwóch wartości najbliżej środka obszaru skanowanego
    private static float getDist(Scan scan){
        float result = Float.MAX_VALUE;
        try {
            int size = scan.getPoints().size();
            result = (float) ((scan.getPoints().get(size / 2).getDistance() + scan.getPoints().get((size / 2)-1).getDistance())/2);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }
}
