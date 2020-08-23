package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class PythonInvoke {

	public static String invoke(String arrayStr,String name) throws Exception {
		
		String pathNow = System.getProperty("user.dir");
		String pngPath = pathNow + "\\pngs\\" + name +".png";
		String pystr = array2pystr(arrayStr,pngPath);
		String pyPath = pathNow + "\\py\\pyfiles\\" + name +".py";
		String flag = pystr2py(pystr,pyPath);
		String line = "";
		if(flag=="success") {
			String []para=new String[] {"python",pyPath};
			Process result = Runtime.getRuntime().exec(para);
			BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream(),"GBK"));
			while((line=reader.readLine())!=null) {
				System.out.println(line);
			}
			reader.close();
		}
		return line;
	}
	public static String array2pystr(String arrayStr,String pngPath) {
		pngPath = pngPath.replaceAll("\\\\", "/");
		System.out.println(pngPath);
		String pystr = "import utils\r\n" + 
				"\r\n" + 
				"if __name__=='__main__':\r\n" + 
				"    matrix = ["+arrayStr+"]\r\n" + 
						"    utils.adMmatrix2Img(matrix,'"+ pngPath +"')\r\n" + 
						"    print('finish')";
		return pystr;
	}
	public static String pystr2py(String pyStr,String pyPath) {
		String flag = "0";
		try {
			File file = new File(pyPath);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fileWriter);
			bw.write(pyStr);
			bw.close();
			flag = "success";
	    } catch (IOException e) {
	        e.printStackTrace();
	        flag = "error";
	    }
		return flag;
	}
	public static void main(String[] args) {
		
	}
}
