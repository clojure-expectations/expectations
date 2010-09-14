import expectations.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(ExpectationsTestRunner.class)
public class ExampleTest implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        return "examples";
    }
}
