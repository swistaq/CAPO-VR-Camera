package pl.edu.agh.amber.roboclaw;

import pl.edu.agh.amber.common.FutureObject;

/**
 * Structure that holds robot's motors current speed values.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 *
 */
public class MotorsCurrentSpeed extends FutureObject {

	private int frontLeftSpeed;
	private int frontRightSpeed;
	private int rearLeftSpeed;
	private int rearRightSpeed;
	
	public MotorsCurrentSpeed() {
		
	}

	public MotorsCurrentSpeed(int frontLeftSpeed, int frontRightSpeed,
			int rearLeftSpeed, int rearRightSpeed) {
		this.frontLeftSpeed = frontLeftSpeed;
		this.frontRightSpeed = frontRightSpeed;
		this.rearLeftSpeed = rearLeftSpeed;
		this.rearRightSpeed = rearRightSpeed;
	}

	public int getFrontLeftSpeed() throws Exception {
		if (!available) {
			waitAvailable();
		}

		return frontLeftSpeed;
	}

	public void setFrontLeftSpeed(int frontLeftSpeed) {
		this.frontLeftSpeed = frontLeftSpeed;
	}

	public int getFrontRightSpeed() throws Exception {
		if (!available) {
			waitAvailable();
		}

		return frontRightSpeed;
	}

	public void setFrontRightSpeed(int frontRightSpeed) {
		this.frontRightSpeed = frontRightSpeed;
	}

	public int getRearLeftSpeed() throws Exception {
		if (!available) {
			waitAvailable();
		}

		return rearLeftSpeed;
	}

	public void setRearLeftSpeed(int rearLeftSpeed) {
		this.rearLeftSpeed = rearLeftSpeed;
	}

	public int getRearRightSpeed() throws Exception {
		if (!available) {
			waitAvailable();
		}

		return rearRightSpeed;
	}

	public void setRearRightSpeed(int rearRightSpeed) {
		this.rearRightSpeed = rearRightSpeed;
	}

}
