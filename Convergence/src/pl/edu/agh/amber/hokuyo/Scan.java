package pl.edu.agh.amber.hokuyo;

import pl.edu.agh.amber.common.FutureObject;

import java.util.LinkedList;
import java.util.List;

public class Scan extends FutureObject {
    private List<MapPoint> points;

    public void setPoints(List<Double> angles, List<Integer> distances) {
        points = new LinkedList<MapPoint>();
        for (int i = 0; i < angles.size() && i < distances.size(); i++) {
            double angle = angles.get(i);
            int distance = distances.get(i);
            points.add(new MapPoint(distance, angle, 0));
        }
    }

    public List<MapPoint> getPoints() throws Exception {
        if (!isAvailable()) {
            waitAvailable();
        }
        return points;
    }
    
    public List<MapPoint> getPoints(long timeout) throws Exception {
        if (!isAvailable()) {
            waitAvailable(timeout);
        }
        return points;
    }
}
