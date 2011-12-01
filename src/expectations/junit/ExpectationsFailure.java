package expectations.junit;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;

public class ExpectationsFailure extends Failure {

    public ExpectationsFailure(Description description, String message) {
        super(description, new NullException(message));
    }

    private static class NullException extends Throwable {
        private final String message;

        public NullException(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public void printStackTrace(PrintWriter writer) {
            writer.println(message);
        }

        @Override
        public String toString() {
            return message;
        }
    }
}