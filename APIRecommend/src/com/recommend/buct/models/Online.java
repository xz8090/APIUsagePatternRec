package com.recommend.buct.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.recommend.buct.utils.ComputeUtil;
import com.recommend.buct.utils.FileUtil;

public class Online {

	public static void main(String[] args) {
		Online onlineModel = new Online();
		Context c = AHCG.getContext();// 编程上下文
		// 计算活动项目特征
		onlineModel.projectFeatures(c, AHCG.getProjectDatas());
		// 计算离线项目特征
		Offline.projectFeatures(AHCG.getProjectDatas());
		// 计算项目相似性
		Map<Long, Double> results = onlineModel.recall(AHCG.getContext(), AHCG.getProjectDatas());
		int N = 3;// 取相似的前3个
		int i = 0;
		for (Long key : results.keySet()) {
			if (i < N)
				System.out.println(key + "," + results.get(key) + "," + AHCG.findProjectById(key).getName());
			i++;
		}
		// 获取3个最相似项目中的方法
		List<Method> methods = AHCG.findMethodsFromSimilarityProject(results, N);
		// 计算活动方法特征
		long activityMid = c.getActiveMethod();
		Method activityMethod = c.getMethods().get(activityMid);
		onlineModel.methodFeatures(activityMethod);
		// 计算候选方法特征
		// Offline.methodFeatures(methods);
		// 计算方法相似性
		results = onlineModel.recall(activityMethod, methods);
		N = 20;// 取相似的前20个
		i = 0;
		for (Long key : results.keySet()) {
			if (i < N)
				System.out.println(
						key + "," + results.get(key) + "," + AHCG.findMethodById(key, methods).getQualifiedName());
			i++;
		}

	}

	public Online() {
		//AHCG.loadMetaDatasets();
		//AHCG.buildAHCG();
		System.out.println("在线模型初始化："+AHCG.pid + "," + AHCG.mid + "," + AHCG.iid);
		//Context c = onlineAnalysis();
		//AHCG.setContext(c);
	}

	/**
	 * 在线解析客户端项目源码
	 */
	public Context onlineAnalysis() {
		// 用本地测试项目来模拟,最相似项目编号40，最相似方法编号62253
		String testProjects = "F:\\APIRecommendation\\TestProjects";
		String activeProject = "041bbb2ae01cdab1dc5db14563fced96cc868f9c";
		String activeMethod = "org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)";
		String[] standardAPIs = {
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/setTopic(java.lang.String)",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#java/lang/String/equals(java.lang.Object)",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#java/lang/Integer/valueOf(java.lang.String)",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/MqttWillAndTestament()",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#java/lang/Math/min(int,int)",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#java/lang/String/getBytes()",
				"org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/fromString(java.lang.String)#org/eclipse/smarthome/io/transport/mqtt/MqttWillAndTestament/setQos(int)" };
		Context c = new Context();
		try {
			File pfile = new File(testProjects + File.separator + activeProject + ".txt");
			c = new Context(AHCG.pid, activeProject, pfile.getAbsolutePath());
			BufferedReader reader = new BufferedReader(new FileReader(pfile));
			String line = null;
			List<String> methodList = new ArrayList<>();// 已提取的方法
			while ((line = reader.readLine()) != null) {
				String[] tmp = line.split("#");
				String methodQualifiedName = tmp[0];
				String APIQualifiedName = tmp[1];

				Method tmpMethod = new Method();
				if (methodList.contains(methodQualifiedName)) {
					tmpMethod = c.getMethodByName(methodQualifiedName);
				} else {
					// 记录需要预测的方法编号
					if (methodQualifiedName.equals(activeMethod))
						c.setActiveMethod(AHCG.mid);
					// 存储方法信息并编号
					String tmpMethodQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.indexOf("("));
					String methodName = tmpMethodQualifiedName.substring(tmpMethodQualifiedName.lastIndexOf("/") + 1);
					String codeSnippetPath = FileUtil.getCodeSnippetPath(activeProject, methodQualifiedName);
					tmpMethod = new Method(AHCG.mid, methodName, methodQualifiedName, codeSnippetPath);
					AHCG.mid++;
					methodList.add(methodQualifiedName);
					c.addMethod(tmpMethod);
				}

				API tmpAPI = new API();
				if (AHCG.APIMap.keySet().contains(APIQualifiedName)) {
					tmpAPI = AHCG.APIMap.get(APIQualifiedName);
				} else {
					String tmpAPIQualifiedName = APIQualifiedName.substring(0, APIQualifiedName.indexOf("("));
					String APIName = tmpAPIQualifiedName.substring(tmpAPIQualifiedName.lastIndexOf("/") + 1);
					tmpAPI = new API(AHCG.iid, APIName, APIQualifiedName);
					AHCG.APIMap.put(APIQualifiedName, tmpAPI);
					AHCG.iid++;
				}
				tmpMethod.addAPI(tmpAPI);
				c.addAPI(tmpAPI);
			}
			reader.close();
			AHCG.pid++;
		} catch (IOException e) {
		}
		return c;
	}

	/**
	 * 计算活动项目特征
	 *
	 * @param c            表示活动项目上下文信息
	 * @param projectDatas 表示语料库项目信息
	 */
	public void projectFeatures(Context c, List<Project> projectDatas) {
		Map<Long, API> apis = c.getAPIs(); // 获取当前项目的API节点
		Map<Long, Integer> APICount = c.getAPICount(); // 获取每个API出现的次数
		// 统计活动项目中所有API出现的次数
		long TSUM = 0;
		for (Long key : APICount.keySet()) {
			int Ti = APICount.get(key);
			TSUM += Ti;
		}

		for (Long iid : apis.keySet()) { // 计算每个API节点的tfidf特征
			int count = APICount.get(iid);
			double tf = count * 1.0 / TSUM; // 计算TF特征
			// 统计多少个项目包含编号为iid的API
			int freq = 0;
			for (Project p : projectDatas) {
				if (p.getAPICount().containsKey(iid))
					freq++;
			}
			double tfidf = ComputeUtil.computeTFIDF(tf, projectDatas.size(), freq);
			c.setFeature(iid, tfidf);
		}
	}

	/**
	 * 项目召回算法，返回项目集合
	 * @param c	编程现场项目
	 * @param projectDatas	语料库项目数据集
	 * @return	Map集合，key表示项目编号，value表示与活动项目的相似性值
	 */
	public Map<Long, Double> recall(Context c, List<Project> projectDatas) {
		Map<Long, Double> features1 = c.getFeatures();
		Map<Long, Double> results = new HashMap<>();
		for (Project p : projectDatas) {
			Map<Long, Double> features2 = p.getFeatures();
			double result = ComputeUtil.computeProjectSimilarity(features1, features2);
			if (result > 0)
				results.put(p.getId(), result);
		}
		results = ComputeUtil.sortMapByValue(results);
		return results;
	}

	/**
	 * 计算活动方法特征
	 *
	 * @param m	活动方法
	 */
	public void methodFeatures(Method m) {
		float[] f = new float[AHCG.APIMap.size()];
		for (Long iid : m.getAPIs().keySet()) {
			f[iid.intValue()] = m.getAPICount().get(iid);
		}
		m.setVec(f);
	}

	/**
	 * 方法召回算法，从候选方法集合中找到相似方法并返回方法集合
	 * @param activityMethod	活动方法
	 * @param methodDatas	相似的候选方法
	 * @return	Map集合，key表示方法编号，value表示与活动方法的相似性值
	 */
	public Map<Long, Double> recall(Method activityMethod, List<Method> methodDatas) {
		Map<Long, Double> results = new HashMap<>();
		// 加载离线模型，进行线上召回
		// 这里先用Jaccard相似性作为线上召回
		for (Method method : methodDatas) {
			double result = ComputeUtil.computeMethodSimilarity(activityMethod, method);
			results.put(method.getId(), result);
		}
		results = ComputeUtil.sortMapByValue(results);
		return results;
	}

	/**
	 * 方法召回算法，从相似项目集合中找到相似性大于0的方法并返回方法集合
	 * @param activityMethod	活动方法
	 * @param similarityProjects	相似的项目集合，key表示项目id，value表示相似值
	 * @return	Map集合，key表示方法编号，value表示与活动方法的相似性值
	 */
	public Map<String, Double> recall(Method activityMethod, Map<Long, Double> similarityProjects) {
		Map<String, Double> results = new HashMap<>();
		for (Long pid : similarityProjects.keySet()) {
			Project p = AHCG.findProjectById(pid);
			for (Long mid : p.getMethods().keySet()) {
				Method method = p.getMethods().get(mid);
				double result = ComputeUtil.computeMethodSimilarity(activityMethod, method);
				if (result > 0) {
					results.put(pid + "#" + method.getId(), result);
				}
			}
		}
		results = ComputeUtil.sortStringMapByValue(results);
		return results;
	}


	/**
	 * API召回算法，返回API集合
	 * @param activityMethod 活动方法
	 * @param topNSim 相似的方法集合，key表示项目id#方法id，value表示相似值
	 * @param projectResults 相似的项目集合，key表示项目id，value表示相似值
	 * @return Map集合，key表示API编号，value表示与上下文相关性
	 */
	public Map<Long, Double> recall(Method activityMethod,Map<String, Double> topNSim,Map<Long,Double> projectResults) {
		// 待预测的目标API
		Set<Long> aimAPIs = new HashSet<>();
		// 相似方法和活动方法中已存在的API
		Set<Long> exiAPIs = new HashSet<>();
		for (Long iidTmp : activityMethod.getAPIs().keySet())
			exiAPIs.add(iidTmp);
		// 活动方法中没有的API都是待预测的目标API
		for (String key : topNSim.keySet()) {
			String parts[] = key.split("#");
			Long pidtmp = Long.parseLong(parts[0]);
			Long midtmp = Long.parseLong(parts[1]);
			Project projectTmp = AHCG.findProjectById(pidtmp);
			Method methodTmp = projectTmp.getMethods().get(midtmp);
			for (Long iidTmp : methodTmp.getAPIs().keySet()) {
				exiAPIs.add(iidTmp);
				if (!activityMethod.getAPIs().keySet().contains(iidTmp))
					aimAPIs.add(iidTmp);
			}
		}
		System.out.println("目标API数量：" + aimAPIs.size());
		System.out.println("已存在的API数量" + exiAPIs.size());
		Map<Long, Double> APIResults = new HashMap<>();
		for (Long iidtmp : aimAPIs) {
			System.out.println("目标API编号："+iidtmp);
			double totalSim = 0, rating = 0;
			for (String key : topNSim.keySet()) {
				String line = key.trim();
				String parts[] = line.split("#");
				Long pidtmp = Long.parseLong(parts[0]);
				Long midtmp = Long.parseLong(parts[1]);
				double avgMDRating = activityMethod.getAPIs().size() * 1.0 / exiAPIs.size();
				double projectSim = projectResults.get(pidtmp);
				Project projectTmp = AHCG.findProjectById(pidtmp);
				Method methodTmp = projectTmp.getMethods().get(midtmp);
				double val = projectSim
						* (methodTmp.getAPIs().containsKey(iidtmp) ? methodTmp.getAPICount().get(iidtmp) : 0);
				double methodSim = topNSim.get(key);
				totalSim += methodSim;
				rating += (val - avgMDRating) * methodSim;
			}
			if (totalSim != 0)
				rating /= totalSim;
			double activeMDrating = 0.8;
			rating += activeMDrating;
			APIResults.put(iidtmp, rating);
		}
		APIResults = ComputeUtil.sortMapByValue(APIResults);
		return APIResults;
	}

	/**
	 * 取活动项目的推荐相似项目的前N个结果
	 * @param projectResults 已排序的候选项目相似结果，key为项目编号，value为相似结果
	 * @param N top-N
	 * @return 前N个项目
	 */
	public static List<Project> recommendationProject(Map<Long,Double> projectResults,int N){
		int count = 0;
		List<Project> topNProject = new ArrayList<>();
		for (Long pidtmp : projectResults.keySet()) {
			Project projecttmp = AHCG.findProjectById(pidtmp);
			topNProject.add(projecttmp);
			count++;
			if (count > N)
				break;
		}
		return topNProject;
	}

	/**
	 * 取活动方法的相似方法的前N个结果
	 * @param methodResults 已排序的候选方法相似结果，key为项目编号#方法，value为相似结果
	 * @param N top-N
	 * @return 前N个方法
	 */
	public Map<String,Double> recommendationMethod(Map<String,Double> methodResults,int N){
		int count = 0;
		Map<String,Double> topNMethod = new HashMap<>();
		for (String pidAndMid : methodResults.keySet()) {
			topNMethod.put(pidAndMid,methodResults.get(pidAndMid));
			count++;
			if (count > N)
				break;
		}
		return topNMethod;
	}

	/**
	 * 对光标位置预测API的前N个结果
	 * @param APIResults 已排序的候选API预测结果，key为api编号，value为预测结果
	 * @param N top-N
	 * @return 前N个API对象及预测值。key是API编号，value是预测值。
	 */
	public Map<String, Double> recommendationAPI(Map<Long, Double> APIResults,int N){
		int count = 0;
		Map<String, Double> topNAPI = new HashMap<>();
		for (Long iidtmp : APIResults.keySet()) {
			String tmpAPIQualifiedName = AHCG.findAPIById(iidtmp).getQualifiedName();
			if(tmpAPIQualifiedName!=null) {
				topNAPI.put(tmpAPIQualifiedName, APIResults.get(iidtmp));
				count++;
				if (count >= N)
					break;
			}
		}
		//topNAPI = ComputeUtil.sortStringMapByValue(topNAPI);
		return topNAPI;
	}

	/**
	 * 根据已选API推荐API使用模式
	 * @param c	编程上下文
	 * @param topNProject	相似的前N个项目
	 * @return	返回项目编号#方法编号和相关性值
	 */
	public static Map<String, Double> recommendation(Context c,List<Project> topNProject) {
		Map<String, Double> results = new HashMap<>();
		for (Project p : topNProject) {
			Long pid = p.getId();
			for (Long mid : p.getMethods().keySet()) {
				Method method = p.getMethods().get(mid);
				double result = ComputeUtil.computeMethodSimilarity(c, method);
				if (result > 0) {
					results.put(pid + "#" + method.getId(), result);
				}
			}
		}
		results = ComputeUtil.sortStringMapByValue(results);
		return results;
	}

}
