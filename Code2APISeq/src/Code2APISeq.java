import groum.GROUMGraph;
import groum.GROUMNode;
import groum.ObjSeqBuilder;
import utils.GenerateDot;
import utils.GeneratePng;
import utils.PythonInvoke;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.output.TeeOutputStream;
import org.osgi.framework.InvalidSyntaxException;

import entity.Graph;


/***
 * Collect Code Groum and Comments from a set of java projects.
 * This class is processed before GithubData.java
 * @author v-xiaodg
 *
 */
public class Code2APISeq {
	/***/	
	private static Map<String,Set<String>> APIs=prepAPIList();
	private static ArrayList<GROUMGraph> arrayList;	
	private static ArrayList<String> allDiffAPIS = new ArrayList<>();
	private static Map<String, String> methodNameAPIS = new HashMap<String,String>();
	
	public static String txt2String(File file){
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                result.append(System.lineSeparator()+s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return result.toString();
    }
	//保存控制台输出
	public static void saveLog() {
		long t=System.currentTimeMillis();
		try {
			String f = "logs/" + t + ".log";
		    FileOutputStream fos = new FileOutputStream(f);//f:生成的文件路径
		    //we will want to print in standard "System.out" and in "file"
		    TeeOutputStream myOut=new TeeOutputStream(System.out, fos);
		    PrintStream ps = new PrintStream(myOut, true); //true - auto-flush after println
		    System.setOut(ps);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	public static void project2graph() {
		saveLog();
		//System.out.println(code);
		//String code=args[0];
		//String apiseq=code2apiseq(code);
		
		String fileUrl = "C:\\Users\\Administrator\\Downloads\\ANN-master";
		folder2apiseq(fileUrl);
		
		int vcount = allDiffAPIS.size();
		Graph graph = new Graph(vcount);
		graph.setVertexList(allDiffAPIS);
		//获取Map中的所有key
        Set<String> keySet = methodNameAPIS.keySet();
        //遍历存放所有key的Set集合
        Iterator<String> it =keySet.iterator();
        while(it.hasNext()){                        //利用了Iterator迭代器
            //得到每一个key
            String key = it.next();
            //通过key获取对应的value
            String value = methodNameAPIS.get(key);
            String[] apiMethods = value.split(";");
            System.out.println("key="+key+";value="+value);
            for (int i=0;i<apiMethods.length;i++) {
            	String api1 = apiMethods[i];
            	for (int j=i+1;j<apiMethods.length;j++) {
                	String api2 = apiMethods[j];
                	if(allDiffAPIS.contains(api1) && allDiffAPIS.contains(api2)) {
                		int index1 = allDiffAPIS.indexOf(api1);
                		int index2 = allDiffAPIS.indexOf(api2);
                		graph.insertEdges(index1, index2, 1);
                		
                	}
    			}
			}
        }
		System.out.println("api count:"+vcount);
		for (int i=0;i<allDiffAPIS.size();i++) {
			String string = allDiffAPIS.get(i);
			System.out.print((i+1)+"." +string+" ");
		}
		System.out.println();
		String arrayStr = graph.showGraph();//类似[1,1],[2,2],[3,3]的格式
		//调用Python程序生成networkx图
		try {
			PythonInvoke.invoke(arrayStr, graph.getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		//生成dot程序
		String path = GenerateDot.graph2dot(graph);
		System.out.println("dot path:"+path);
		//dot绘制图
		String pngpath = "pngs/" + graph.getName() + ".png";
		try {
			GeneratePng.generateImg(path,pngpath);
			System.out.println("png path:"+pngpath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	public static void main(String[] args) throws Exception
	{
		project2graph();
		
	}
	
	public static String code2apiseq(String snippet) {
		String apiseq="";
		
		//String code="class AB {\n public static void main() {\n "+snippet+ "}\n}";	
		String code=snippet;
		ObjSeqBuilder groumbuilder= new ObjSeqBuilder();
		try {
			groumbuilder.build(code);
		} catch (InvalidSyntaxException e) {
			return "InvalidSyntax";			
		}
		//System.out.println(groumbuilder);
		if(groumbuilder.getGroums().isEmpty())return "Empty";
		GROUMGraph groum=groumbuilder.getGroums().get(0);
		//System.out.println(groum);

		String ouseq= Groum2Sentence(groum);	
		//System.out.println(ouseq);
		apiseq=processAPISeq(ouseq);			
		
		return apiseq;
	}
	
	public static void javafile2apiseq(String code) {
			
		ObjSeqBuilder groumbuilder= new ObjSeqBuilder();
		try {
			groumbuilder.build(code);
		} catch (InvalidSyntaxException e) {
			System.out.println("InvalidSyntax");
		}
		//System.out.println(groumbuilder);
		if(groumbuilder.getGroums().isEmpty()) System.out.println("Empty");
		//获取一个java文件的所有方法声明
		for(int i=0;i<groumbuilder.getGroums().size();i++) {
			String apiseq="";
			GROUMGraph groum=groumbuilder.getGroums().get(i);
			String ouseq= Groum2Sentence(groum);	
			//System.out.println(ouseq);
			apiseq=processAPISeq(ouseq);
			String methodName = groum.getName();
			methodNameAPIS.put(methodName, apiseq);
			
		}		
		
	}
	
	public static void folder2apiseq(String path) {
		File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                    	String folderName = file2.getName();
                        System.out.println("文件夹:" + folderName);
                        if(!folderName.startsWith(".") && !folderName.endsWith("Test") && !folderName.endsWith("test"))
                        	folder2apiseq(file2.getAbsolutePath());
                    } else {
                    	String fileName = file2.getName();
                        System.out.println("文件名:" + fileName);
                        if(fileName.endsWith(".java") && !fileName.endsWith("Test.java") && !fileName.endsWith("test.java")) {
                        	String filePath = file2.getAbsolutePath();
                        	File javafile = new File(filePath);
                    		String code=txt2String(javafile);
                    		//解析每个.java的代码文件，解析结果保存在全局methodNameAPIS中
                    		javafile2apiseq(code);
                        }
                        	
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
	}

	
	
	private static String Groum2Sentence(GROUMGraph groum)
	{
		String sentence="";
		TreeMap<Integer,List<GROUMNode>> nodelines=new TreeMap<Integer,List<GROUMNode>>();
		HashSet<GROUMNode> nodes=groum.getNodes();
		List<GROUMNode> orderedNodes=new ArrayList<GROUMNode>();

		for (final GROUMNode node : nodes) {
			int startLineNum=node.getStartLine();
			List<GROUMNode> nds=nodelines.get(startLineNum);
			if(nds==null)nds=new ArrayList<GROUMNode>();
			nds.add(node);
			nodelines.put(startLineNum, nds);
		}
		
		Entry<Integer,List<GROUMNode>> linenodes=null;
		while((linenodes= nodelines.pollFirstEntry())!=null)
		{
			List<GROUMNode> nds=linenodes.getValue();
			if(nds.size()==1)orderedNodes.add(nds.get(0));
			else{//order them according to their edge order in the groum.
				GROUMNode[] nodearray= new GROUMNode[nds.size()];
				for(int i=0;i<nds.size();i++)nodearray[i]=nds.get(i);
				for(int i=0;i<nodearray.length-1;i++)
				{
					for(int j=i+1;j<nodearray.length;j++)
					{
						//if(nodearray[j].getOutNodes().contains(nodearray[i])||nodearray[i].getInNodes().contains(nodearray[j]))
						if(nodearray[i].getId()>nodearray[j].getId())
						{	
							GROUMNode tmp=nodearray[i];
							nodearray[i]=nodearray[j];
							nodearray[j]=tmp;
						}	
					}
				}
				for(GROUMNode n:nodearray)
					orderedNodes.add(n);
			}			
		}		
		for(GROUMNode n:orderedNodes)
		{
			if(n.getClassName()!=null)
			if(!n.getClassName().endsWith("Exception")&&!n.getClassName().endsWith("Unknown"))
			sentence+=n.getLabel()+ " ";
		}
		return sentence.trim();
	}
	

	
	public static Map<String,Set<String>> prepAPIList() 
	{
		Map<String,Set<String>> apis=new HashMap<String,Set<String>>();
		
		for(String line:JDKAPIs.javaapis)
		{
			String[] classmethods=line.split("\t");
			for(String api:classmethods){
				String classname=api.split("\\.")[0];
				String methodname=api.split("\\.")[1];
				if(apis.get(classname)==null)apis.put(classname, new HashSet<String>());
				else
				{
					Set<String> methods=apis.get(classname);
					methods.add(methodname);
					apis.put(classname, methods);
				}
			}
		}
		return apis;
	}

	private static String processAPISeq(String ouseq)
	{
		List<String> apiseqlist=new ArrayList<String>();
		double num_validAPIs=0.0;
		double num_validOUSeqs=0.0;
		ouseq=ouseq.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\\(\\s*\\)","");
		String [] seqs= ouseq.split("\\s+");
		
		for(int i=0;i<seqs.length;i++)
		{
			String objcall=seqs[i];//String.trim  BufferedReader.<init>  List<String>.add
			String lastobjcall=(i==0?"":seqs[i-1]);
			if(objcall.trim().equals(lastobjcall.trim()))continue;
			if(objcall.equals("System.out.println"))
			{
				/*apiseqlist.add("PRINT");//system.out.println
				num_validOUSeqs++;
				num_validAPIs++;*///ignore print because it could be used for debugging
				continue;
			}
			if(objcall.contains("Log")||objcall.matches(".+\\.log$")||objcall.contains("Debug"))continue;//ignore log/debug call
			String[] objcallelements=objcall.split("\\.");
			if(objcallelements.length!=2)continue;
			String classname=objcallelements[0];//e.g., SortedMap<String,List<Document>>
			if(classname.equals("CONTROL"))continue;//ignore control node
			num_validOUSeqs++;
			classname=classname.replaceAll("<", " < ").replaceAll(">", " > ").replaceAll(",", " , ").replaceAll("\\s\\s+", " ");
			String [] classnameelements=classname.split("\\s+");//SortedMap < String , List < Document > >
			//只收集JDK中的API
			//if(!APIs.containsKey(classnameelements[0].replaceAll("\\[\\]", "")))continue;
			if(!APIs.containsKey(classnameelements[0].replaceAll("\\[\\]", "")))continue;
			classname="";
			for(String classnameelement:classnameelements)
			{
				if(classnameelement.matches("[<>,]"))classname+=classnameelement;
				else if(APIs.containsKey(classnameelement.replaceAll("\\[\\]", "")))classname+=classnameelement;
				else continue;
			}
			//这个地方正则表达式不完善
			classname=classname.replaceAll("\\[", " [").replaceAll("<\\s*>", "");//remove < >
			classname=classname.replaceAll("<\\s+,", "<");//<,String> => <String>
			classname=classname.replaceAll(",\\s+>", ">");//<String,> => <String>			
			String methodname=objcallelements[1];
			methodname=methodname.replaceAll("\\([^\\(\\)]*\\)", "");//remove parameters
			
			String classnameMethod = classname+"."+methodname;
			classnameMethod = classnameMethod.replaceAll(" ", "");
			//由于不关心调用序列，只关心是否出现过该API方法，因此只添加一次API
			if(!apiseqlist.contains(classnameMethod)) {
				apiseqlist.add(classnameMethod);//每一个方法声明中的不同API方法
				if(!allDiffAPIS.contains(classnameMethod)) {
					allDiffAPIS.add(classnameMethod);//每一个项目中的不同API方法
				}
				
			}
				
			
		}
		String apiseqs="";
		for(int i=0;i<apiseqlist.size();i++)
		{
			String lastapi=(i==0?"":apiseqlist.get(i-1));
			if(apiseqlist.get(i).trim().equals(lastapi.trim()))continue;//filter duplicate objcall
			apiseqs+=apiseqlist.get(i)+";";
			num_validAPIs++;
		}
		//暂时不考虑
		//if(num_validAPIs<2||num_validAPIs<0.6*num_validOUSeqs) return ""; //if most of the object usages are not api calls 
														  //then it could be too specific application, 
														  //do not include it in the training set
		apiseqs=apiseqs.replaceAll("\\s\\s+", " ").replaceAll("<\\s+>", "").trim();
		return apiseqs;
	}
	
	

	
}