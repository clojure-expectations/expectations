package expectations.junit;

import clojure.lang.RT;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

public class ExpectationsTestRunner extends Runner {
    private final Runner clojureRunner;

    public ExpectationsTestRunner(Class<TestSource> testSourceClass) throws Exception {
        TestSource source = testSourceClass.newInstance();
        RT.loadResourceScript("expectations/junit/runner.clj");
        clojureRunner = (Runner) RT.var("expectations.junit.runner", "create-runner").invoke(source);
    }

    @Override
    public Description getDescription() {
        return clojureRunner.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        clojureRunner.run(notifier);
    }

    public interface TestSource {
        String testPath();
    }
}
