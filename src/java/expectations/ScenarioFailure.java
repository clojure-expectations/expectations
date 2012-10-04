package expectations;

public class ScenarioFailure extends RuntimeException {
    public ScenarioFailure(String message) {
        super(message);
    }
}


