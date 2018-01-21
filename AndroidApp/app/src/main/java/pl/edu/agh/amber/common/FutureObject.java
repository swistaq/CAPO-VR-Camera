package pl.edu.agh.amber.common;

/**
 * Class implementing future object pattern used to wait for data from devices.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 * 
 */
public class FutureObject {

	protected boolean available = false;

	protected Exception exception = null;

	public boolean isAvailable() throws Exception {
		if (exception != null) {
			throw exception;
		}
		return available;
	}

	/**
	 * Blocks until data is available.
	 * 
	 * @throws Exception
	 *			 any exception
	 */
	public synchronized void waitAvailable() throws Exception {
		try {
			while (!isAvailable()) {
				wait();
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	/**
	 * Blocks until data is available or to timeout.
	 *
	 * @param timeout in milliseconds
	 * @throws Exception
	 */
	public synchronized void waitAvailable(long timeout) throws Exception {
		if (!isAvailable()) {
			wait(timeout);
		}
	}

	/**
	 * Sets the object is available and notifies all waiting clients.
	 */
	public synchronized void setAvailable() {
		available = true;
		notifyAll();
	}

	/**
	 * Sets exception and notifies all waiting clients.
	 * 
	 * @param e
	 *			Exception to throw.
	 */
	public synchronized void setException(Exception e) {
		available = true;
		exception = e;
		notifyAll();
	}

}
