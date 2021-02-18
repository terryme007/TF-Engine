import com.springcloud.dubbo_provider.InitTest;
import org.junit.Test;

public class CodeTest {

    @Test
    public void initTest() throws IllegalAccessException, InstantiationException {
        InitTest initTest=new InitTest();
        System.out.println(initTest.getA());
        System.out.println(initTest.getB());
    }
}
