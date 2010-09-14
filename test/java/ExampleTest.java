import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class ExampleTest implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        return "/home/jfields/dev/expectations/test/clojure";
    }
}
