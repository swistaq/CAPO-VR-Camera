package com.agh.panda.filters;

import java.util.Arrays;

public class GyroFilter {
    private final Double GAIN = 3.945400498e+09;

    private final double xv[] = new double[11];
    private final double yv[] = new double[11];

    public GyroFilter() {
        Arrays.fill(xv, 0.0);
        Arrays.fill(yv, 0.0);
    }

    public Float filterValue(Float freshValue) {
        xv[0] = xv[1];
        xv[1] = xv[2];
        xv[2] = xv[3];
        xv[3] = xv[4];
        xv[4] = xv[5];
        xv[5] = xv[6];
        xv[6] = xv[7];
        xv[7] = xv[8];
        xv[8] = xv[9];
        xv[9] = xv[10];
        xv[10] = freshValue / GAIN;
        yv[0] = yv[1];
        yv[1] = yv[2];
        yv[2] = yv[3];
        yv[3] = yv[4];
        yv[4] = yv[5];
        yv[5] = yv[6];
        yv[6] = yv[7];
        yv[7] = yv[8];
        yv[8] = yv[9];
        yv[9] = yv[10];
        yv[10] = (xv[0] + xv[10]) + 10 * (xv[1] + xv[9]) + 45 * (xv[2] + xv[8])
                + 120 * (xv[3] + xv[7]) + 210 * (xv[4] + xv[6]) + 252 * xv[5]
                + (-0.2207395696 * yv[0]) + (2.5398193660 * yv[1])
                + (-13.1785686730 * yv[2]) + (40.6121844050 * yv[3])
                + (-82.3226284420 * yv[4]) + (114.7024954800 * yv[5])
                + (-111.2646525400 * yv[6]) + (74.2040555110 * yv[7])
                + (-32.5660116440 * yv[8]) + (8.4940458520 * yv[9]);

        return (float) yv[10];
    }
}
