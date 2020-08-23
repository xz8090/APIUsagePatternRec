package test;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PyTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String pathNow = System.getProperty("user.dir");
		String []para=new String[] {"python",pathNow+"\\py\\pyfiles\\test.py"};
		Process result = Runtime.getRuntime().exec(para);
		BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream(),"GBK"));
		String line;
		while((line=reader.readLine())!=null) {
			System.out.println(line);
		}
		reader.close();
	}
	
}
