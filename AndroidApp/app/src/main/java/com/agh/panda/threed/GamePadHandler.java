package com.agh.panda.threed;

import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;

import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.roboclaw.RoboclawProxy;

/**
 * Created by swistaq on 28.08.2017.
 */

public class GamePadHandler {
    private final RoboclawProxy roboclawProxy;

    public GamePadHandler(AmberClient client, Context context) {
        roboclawProxy = new RoboclawProxy(client, 0);
    }

    public void handleMotionEvent(MotionEvent ev) {
        // Check that the ev came from a game controller
        if ((ev.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                ev.getAction() == MotionEvent.ACTION_MOVE) {
            // Process the current movement sample in the batch (position -1)
            processJoystickInput(ev, -1);
        }
    }

    private void processJoystickInput(MotionEvent event,
                                      int historyPos) {

        InputDevice mInputDevice = event.getDevice();

        // Calculate the horizontal distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat axis, or the right control stick.
        float x = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X, historyPos);
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_X, historyPos);
        }
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_Z, historyPos);
        }

        // Calculate the vertical distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat switch, or the right control stick.
        float y = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y, historyPos);
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_Y, historyPos);
        }
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_RZ, historyPos);
        }
        //AXIS_RX - L trigger
        //AXIS_RY - R trigger

        float GAS = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RY, historyPos);
        float BRAKE = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RX, historyPos);

        System.out.println(GamePadHandler.class.getSimpleName() + " Joystick pos X: " + x + " Y: " + y + " GAS: " + GAS + " BRAKE: " + BRAKE);
        try {
            int speedMulti = 150;
            int left = (int) (speedMulti * (-y) + speedMulti * x);
            int right = (int) (speedMulti * (-y) - speedMulti * x);
            //flip dla cofania
            if(y>0.1){
                int tmp = left;
                left = right;
                right = tmp;
            }
            roboclawProxy.sendMotorsCommand(left, right, left, right);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        // Update the ship object based on the new x and y value

    }

    private static float getCenteredAxis(MotionEvent event,
                                         InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis) :
                            event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
}
