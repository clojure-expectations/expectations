import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class FailureTest implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        return "test/clojure/failure";
    }
}
