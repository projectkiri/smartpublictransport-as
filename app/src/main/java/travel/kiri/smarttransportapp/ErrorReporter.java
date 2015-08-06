package travel.kiri.smarttransportapp;

/**
 * An interface for all activities that is able to report error.
 * @author pascal
 *
 */
public interface ErrorReporter {
	public void reportError(Object source, Throwable tr);
}
