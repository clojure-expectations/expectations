package expectations.junit;

public class ScenarioError extends AssertionError {
    public final String name;

    public ScenarioError(String name, String msg) {
        super(msg);
        this.name = name;
    }
}
