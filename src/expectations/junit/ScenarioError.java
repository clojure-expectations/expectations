package expectations.junit;

import java.util.Map;

public class ScenarioError extends AssertionError {
    public final String name;
    public final Map uniqueId;

    public ScenarioError(String name, Map uniqueId, String msg) {
        super(msg);
        this.name = name;
        this.uniqueId = uniqueId;
    }
}
