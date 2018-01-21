package pl.edu.agh.amber.common;

/**
 * Cyclic data handler interface. Used by user to implements own cyclic data
 * handler.
 * 
 * @author Michał Konarski <konarski@student.agh.edu.pl>
 * 
 * 
 * @param <T>
 *            Data type that is delivered cyclicly.
 */
public interface CyclicDataListener<T> {

	/**
	 * Handle cyclic data.
	 * 
	 * @param data
	 *            data to handle.
	 */
	void handle(T data);

}
