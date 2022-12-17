package com.recommend.buct.models;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.recommend.buct.action.onlineAnalysis;
import com.recommend.buct.utils.ComputeUtil;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.recommend.buct.entity.API;
import com.recommend.buct.entity.Context;
import com.recommend.buct.entity.Method;
import com.recommend.buct.entity.Project;
import com.recommend.buct.utils.FileUtil;
import com.recommend.buct.utils.XMLUtil;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AHCG {
	public static String projectsPath = "F:\\APIRecommendation\\projects-APIs";
	public static String metaDatasPath = "F:\\APIRecommendation\\AHCG";
	public static String codeSnippetsPath = "F:\\APIRecommendation\\code-snippets";
	public static String offlineApplication = "F:\\APIRecommendation\\trainApplication\\CAPIRD\\data\\TEST";

	//项目、方法、API编号
	public static long pid = 0;
	public static long mid = 0;
	public static long iid = 0;
	//项目中API可能有相同的，放在外面统计和提取全局的API信息.key是API全限定名，value是API
	public static Map<String,API> APIMap = new HashMap<>();
	//相似项目集合
	public static Map<Long,Double> projectResults;

	private static List<Project> projectDatas;
	private static Context context;


	public static void main(String[] args) {
		context();
		//System.out.println(getCodeSnippets("041bbb2ae01cdab1dc5db14563fced96cc868f9c","org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)"));
	}

	/**
	 * 提取离线解析的项目元数据集
	 */
	public static void loadMetaDatasets() {
		try {
			//先获取已有的元数据
			File dirs = new File(metaDatasPath);
			File[] projectFiles= dirs.listFiles();
			List<String> existDatas = new ArrayList<>();
			for(File pfile:projectFiles) existDatas.add(pfile.getName().replaceAll(".xml", ""));

			//加载源数据
			dirs = new File(projectsPath);
			projectFiles= dirs.listFiles();
			System.out.println("project count:"+projectFiles.length);

			for(File pfile:projectFiles) {
				String projectName = pfile.getName().replaceAll(".txt", "");
				if(!existDatas.contains(projectName)) {
					//创建项目文件信息
					Project p = new Project(pid,projectName,pfile.getAbsolutePath());
					//读取类文件信息，每一行数据格式【方法全限定名#API全限定名】
					BufferedReader reader = new BufferedReader(new FileReader(pfile));
					String line = null;
					//项目中每个方法都不相同，每个项目中进行统计和提取局部的方法信息
					List<String> methodList = new ArrayList<>();//已提取的方法
					while ((line = reader.readLine()) != null) {
						String[] tmp = line.split("#");
						String methodQualifiedName = tmp[0];
						String APIQualifiedName = tmp[1];

						//如果该方法已经提取了，说明该行数据仍属于该方法，只需要在之前的方法中找到之前已提取的方法节点
						//否则重新编号并添加到该项目中以及已提取方法列表中
						Method targetMethod = new Method();
						if(methodList.contains(methodQualifiedName)) {
							targetMethod = p.getMethodByName(methodQualifiedName);
						}else {
							String tmpMethodQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.indexOf("("));
							String methodName = tmpMethodQualifiedName.substring(tmpMethodQualifiedName.lastIndexOf("/") + 1);
							String codeSnippetPath = FileUtil.getCodeSnippetPath(projectName, methodQualifiedName);
							targetMethod = new Method(mid, methodName, methodQualifiedName, codeSnippetPath);
							mid++;
							methodList.add(methodQualifiedName);
							p.addMethod(targetMethod);
						}

						//如果该API存在，则从全局中获取该API编号
						//否则将该API添加到全局已提取的API列表中
						API targetAPI = new API();
						if(APIMap.keySet().contains(APIQualifiedName)) {
							targetAPI = APIMap.get(APIQualifiedName);
						}else {
							String tmpAPIQualifiedName = APIQualifiedName.substring(0, APIQualifiedName.indexOf("("));
							String APIName = tmpAPIQualifiedName.substring(tmpAPIQualifiedName.lastIndexOf("/") + 1);
							targetAPI = new API(iid,APIName,APIQualifiedName);
							APIMap.put(APIQualifiedName, targetAPI);
							iid++;
						}

						//将该API节点添加到方法节点下和项目节点下
						targetMethod.addAPI(targetAPI);
						p.addAPI(targetAPI);
					}//while read end
					reader.close();
					XMLUtil.convertToXml(p,metaDatasPath+File.separator+p.getName()+".xml");
				}
				pid++;
				System.out.println("project analyze finish:"+pid);
			}

		} catch (IOException e) {

		}
	}

	/**
	 * 构建AHCG模型
	 */
	public static final int MIN_PROGRESS = 0;
	public static int MAX_PROGRESS = 100;
	public static int currentProgress = MIN_PROGRESS;

	public static void buildAHCG() {
		if(projectDatas==null) projectDatas = new ArrayList<>();
		File dirs = new File(metaDatasPath);
		File[] projectFiles= dirs.listFiles();
		MAX_PROGRESS = projectFiles.length;
		System.out.println("project count:"+projectFiles.length);
		/*for (File pfile : projectFiles) {
			Project project = (Project) XMLUtil.convertXmlFileToObject(Project.class, pfile.getAbsolutePath());
			projectDatas.add(project);
			for(Long key:project.getMethods().keySet()) {
				if(key>mid) mid = key;
			}
			for(Long key:project.getAPIs().keySet()) {
				if(key>iid) iid = key;
				API targetAPI = project.getAPIs().get(key);
				APIMap.put(targetAPI.getQualifiedName(), targetAPI);
			}
			currentProgress++;
			System.out.println("load project:"+currentProgress);
		}*/
		//设置进度条
		JFrame jf = new JFrame("元数据加载进度");
		jf.setAlwaysOnTop(true);
		jf.setResizable(false);
		jf.setSize(300, 80);
		jf.setLocationRelativeTo(null);
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		// 创建一个进度条
		final JProgressBar progressBar = new JProgressBar();
		// 设置进度的 最小值 和 最大值
		progressBar.setMinimum(MIN_PROGRESS);
		progressBar.setMaximum(MAX_PROGRESS);
		// 设置当前进度值
		progressBar.setValue(currentProgress);
		// 绘制百分比文本（进度条中间显示的百分数）
		progressBar.setStringPainted(true);
		// 添加进度改变通知
		progressBar.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				System.out.println("当前进度值: " + progressBar.getValue() + "; " +
						"进度百分比: " + progressBar.getPercentComplete());
			}
		});
		// 添加到内容面板
		panel.add(progressBar);
		jf.setContentPane(panel);
		jf.setVisible(true);
		Thread t = new Thread(new Runnable() {
			public void run() {
				for (File pfile : projectFiles) {
					Project project = (Project) XMLUtil.convertXmlFileToObject(Project.class, pfile.getAbsolutePath());
					projectDatas.add(project);
					for(Long key:project.getMethods().keySet()) {
						if(key>mid) mid = key;
					}
					for(Long key:project.getAPIs().keySet()) {
						if(key>iid) iid = key;
						API targetAPI = project.getAPIs().get(key);
						APIMap.put(targetAPI.getQualifiedName(), targetAPI);
					}
					//进度条前进
					currentProgress++;
					progressBar.setValue(currentProgress);
				}
				if(currentProgress>=MAX_PROGRESS) {
					jf.setVisible(false);
					//载入元数据后才能开放在线解析入口
					onlineAnalysis.ifLoadMetaDatas = true;
				}
			}
		});
		t.start();

		//mid和iid索引从最大的编号后一位开始
		mid++;
		iid++;

	}

	public static Context getContext() {
		return context;
	}

	public static void setContext(Context c) {
		context = c;
	}

	public static List<Project> getProjectDatas() {
		return projectDatas;
	}

	/**
	 * 根据项目编号找元数据集中的项目
	 * @param pid
	 * @return
	 */
	public static Project findProjectById(long pid) {
		for(Project p:projectDatas) {
			if(p.getId()==pid) return p;
		}
		return null;
	}

	/**
	 * 从相似项目中获取方法
	 * @param results	相似项目结果集
	 * @param N	取Top-N相似项目中的方法
	 * @return
	 */
	public static List<Method> findMethodsFromSimilarityProject(Map<Long,Double> results,int N){
		int i = 0;
		List<Method> similarMethods = new ArrayList<>();
		for (Long pid : results.keySet()) {
			if(i<N) {
				Project p = findProjectById(pid);
				if(p!=null) {
					Map<Long,Method> methods = p.getMethods();
					for (Long mid : methods.keySet()) {
						similarMethods.add(methods.get(mid));
					}
				}
			}else break;
			i++;
		}
		return similarMethods;
	}

	public static Method findMethodById(long mid,List<Method> methods) {
		for(Method m:methods) {
			if(m.getId()==mid) return m;
		}
		return null;
	}

	public static Method findMethodById(long pid,long mid) {
		Project p = findProjectById(pid);
		Map<Long, Method> methods = null;
		if(p==null) methods = getContext().getMethods();
		else methods = p.getMethods();
		for(Long mmid:methods.keySet()) {
			if(mmid==mid) return methods.get(mmid);
		}
		return null;
	}

	public static API findAPIById(long iid) {
		for(String key:APIMap.keySet()) {
			API api = APIMap.get(key);
			if(api.getId()==iid) return api;
		}
		return null;
	}

	/**
	 * 获取项目源码库中的代码片段
	 *
	 * @param projectName         项目名称
	 * @param methodQualifiedName 方法的全限定名
	 * @return	代码片段字符串
	 */
	public static String getCodeSnippets(String projectName, String methodQualifiedName) {
		String classQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.lastIndexOf("/"));
		String classFilePath = classQualifiedName.replaceAll("/", "\\\\") + ".java";
		String tmpMethodQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.indexOf("("));
		String methodName = tmpMethodQualifiedName.substring(tmpMethodQualifiedName.lastIndexOf("/") + 1);
		String[] params = methodQualifiedName
				.substring(methodQualifiedName.indexOf("(") + 1, methodQualifiedName.indexOf(")")).split(",");
		String projectPath = codeSnippetsPath + File.separator + projectName + File.separator + classFilePath;
		String codeSnippets = "";

		try {
			FileInputStream fin = new FileInputStream(projectPath);
			InputStreamReader reader = new InputStreamReader(fin);
			BufferedReader buffReader = new BufferedReader(reader);
			String strTmp = "";
			StringBuilder sb = new StringBuilder();
			while ((strTmp = buffReader.readLine()) != null) {
				sb.append(strTmp);
				sb.append("\r\n");
			}
			String content = sb.toString();
			// 创建解析器
			ASTParser parsert = ASTParser.newParser(AST.JLS3);
			// 设定解析器的源代码字符
			parsert.setSource(content.toCharArray());
			// 使用解析器进行解析并返回AST上下文结果(CompilationUnit为根节点)
			CompilationUnit result = (CompilationUnit) parsert.createAST(null);
			// 获取类型
			List types = result.types();
			// 取得类型声明
			TypeDeclaration typeDec = (TypeDeclaration) types.get(0);
			// 取得函数(Method)声明列表
			MethodDeclaration methodDec[] = typeDec.getMethods();
			for (MethodDeclaration method : methodDec) {
				if (method.getName().toString().equals(methodName)) {
					List<SingleVariableDeclaration> paras = method.parameters();
					int count = 0;
					for (int i = 0; i < params.length && paras.size() == params.length; i++) {
						if (!params[i].isEmpty()) {
							// 参数匹配
							if (params[i].contains(paras.get(i).getType().toString())) {
								count++;
							}
						}
					}
					if (count == paras.size()) {
						codeSnippets = method.toString();
						break;
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return codeSnippets;
	}

	/**
	 * 相关性度量,返回API集合。key是API全限定名，value是相关性值
	 */
	public static Map<String,Double> context() {
		Online onlineModel = new Online();
		Context c = AHCG.getContext();//获取编程上下文信息
		//计算活动项目特征
		onlineModel.projectFeatures(c,AHCG.getProjectDatas());
		//计算离线项目特征
		Offline.projectFeatures(AHCG.getProjectDatas());
		//计算项目相似性，key是项目id，value是相似性值
		projectResults = onlineModel.recall(AHCG.getContext(), AHCG.getProjectDatas());
		//计算活动方法特征
		long activityMid = c.getActiveMethod();
		Method activityMethod = c.getMethods().get(activityMid);
		onlineModel.methodFeatures(activityMethod);
		//计算候选方法特征
		//Offline.methodFeatures(methods);
		//计算方法相似性,key是项目id#方法id，value是相似性值
		Map<String,Double> methodResults = onlineModel.recall(activityMethod,projectResults);
		//取最相似的前10个方法
		Map<String,Double> topNMethod = onlineModel.recommendationMethod(methodResults, 10);

		//计算API相关性
		Map<Long, Double> APIResults = onlineModel.recall(activityMethod, topNMethod, projectResults);
		//取最相关的前20个API
		Map<String, Double> topNAPI = onlineModel.recommendationAPI(APIResults, 20);
		return topNAPI;
	}

	/**
	 * 关联性度量,返回API集合。key是API全限定名，value是关联性值
	 */
	public static Map<Long,Double> correlation(Long targetIid) {
		Map<Long,Integer> APICorrelationFrequent = new HashMap<>();
		//考虑前三个相似项目中的API关联性
		int count = 0;
		int N = 3;
		for(Long ppid:projectResults.keySet()){
			Project targetProject = findProjectById(ppid);
			Map<Long, Method> targetMethodMaps = targetProject.getMethods();
			for(Long mmid:targetMethodMaps.keySet()){
				Method targetMethod = targetMethodMaps.get(mmid);
				Set<Long> otherAPIs = targetMethod.getAPIs().keySet();
				if(otherAPIs.contains(targetIid)){
					for(Long otherIid:otherAPIs){
						if(otherIid!=targetIid) APICorrelationFrequent.put(otherIid,APICorrelationFrequent.getOrDefault(otherIid,0)+1);
					}
				}
			}
			count++;
			if (count > N)
				break;
		}
		Map<Long,Double> normalizationAPICorFrequent = ComputeUtil.normalizationAPICorFrequent(APICorrelationFrequent);
		return normalizationAPICorFrequent;
	}

	public static void MMR(double lambda,Map<String,Long> APINameToId,List<String> APIRecList,int N){
		Map<String,Double> topNAPI = context();
		Map<String,Double> finalTopNAPI = new HashMap<>();
		//tmpAPIQualifiedName表示已选API
		for(String tmpAPIQualifiedName:topNAPI.keySet()){
			Double contextPredictValue = topNAPI.get(tmpAPIQualifiedName);
			API predictAPI = APIMap.get(tmpAPIQualifiedName);
			double maxCorrelation = 0.0;
			Map<Long,Double> correlationResults = correlation(predictAPI.getId());
			for(Long iidtmp:correlationResults.keySet()){
				double d = correlationResults.get(iidtmp);
				if(d>maxCorrelation) maxCorrelation = d;
			}
			Double predictValue = lambda*contextPredictValue+(1-lambda)*maxCorrelation;
			if(tmpAPIQualifiedName.length()>47) tmpAPIQualifiedName = tmpAPIQualifiedName.substring(0,47)+"... ";
			else {
				StringBuilder sb = new StringBuilder(tmpAPIQualifiedName);
				for (int i = tmpAPIQualifiedName.length(); i < 50; i++) {
					sb.append(" ");
				}
				tmpAPIQualifiedName = sb.toString();
			}
			APINameToId.put(tmpAPIQualifiedName.split(" ")[0],predictAPI.getId());
			finalTopNAPI.put(tmpAPIQualifiedName,predictValue);
		}
		finalTopNAPI = ComputeUtil.sortStringMapByValue(finalTopNAPI);
		int count = 0;
		for(String APIQualifiedName:finalTopNAPI.keySet()){
			double predictValue = finalTopNAPI.get(APIQualifiedName);
			String recResult = String.format("%-55s %6.2f%%",APIQualifiedName,predictValue*100);
			APIRecList.add(recResult);
			count++;
			if (count > N)
				break;
		}
	}
}
