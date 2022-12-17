package com.recommend.buct.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
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

public class Offline {
	public static String localCodesPath = "F:\\APIRecommendation\\projects";
	public static String localAPIsPath = "E:\\2019200815\\experiment\\Focus\\dataset\\SH_L";
	public static String newAPIsPath = "F:\\APIRecommendation\\projects-APIs";
	public static String codeSnippetsPath = "F:\\APIRecommendation\\code-snippets";

	public static void main(String[] args) {
		Offline offline = new Offline();
		System.out.println(System.currentTimeMillis());
		AHCG.getProjectDatas();
		System.out.println(System.currentTimeMillis());
	}

	public Offline() {
		AHCG.loadMetaDatasets();
		AHCG.buildAHCG();
		System.out.println(AHCG.pid + "," + AHCG.mid + "," + AHCG.iid);
		projectFeatures(AHCG.getProjectDatas());
	}

	/**
	 * 静态解析项目源码
	 */
	public void staticAnalysis(String projectPath) {
		List<String> classFilePathList = new ArrayList<>();
		// 遍历项目源码库，找到所有项目的类文件
		FileUtil.readfile(new File(projectPath), classFilePathList);
		// 解析类文件
		for (String classFilePath : classFilePathList) {
			try {
				// 读取类文件内容
				FileInputStream fin = new FileInputStream(classFilePath);
				InputStreamReader reader = new InputStreamReader(fin);
				BufferedReader buffReader = new BufferedReader(reader);
				String strTmp = "";
				StringBuilder sb = new StringBuilder();
				while ((strTmp = buffReader.readLine()) != null) {
					sb.append(strTmp);
					sb.append("\r\n");
				}
				String content = sb.toString();
				// 创建AST解析器
				ASTParser parsert = ASTParser.newParser(AST.JLS3);
				// 设置静态解析源
				parsert.setSource(content.toCharArray());
				CompilationUnit result = (CompilationUnit) parsert.createAST(null);
				List types = result.types();
				TypeDeclaration typeDec = (TypeDeclaration) types.get(0);
				// 获取方法声明
				MethodDeclaration methodDec[] = typeDec.getMethods();
				for (MethodDeclaration method : methodDec) {
					// 获取方法参数
					List<SingleVariableDeclaration> paras = method.parameters();

				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 计算离线项目特征
	 *
	 * @param projectDatas 表示离线项目数据集
	 */
	public static void projectFeatures(List<Project> projectDatas) {
		// 先统计每个API的tf特征
		Map<Long, Integer> tfMap = new HashMap<>();
		for (Project p : projectDatas) { // 暂不加活动上下文
			// 统计多少个项目包含编号为iid的API
			for (Long iid : p.getAPIs().keySet()) {
				if (tfMap.containsKey(iid))
					tfMap.put(iid, tfMap.get(iid) + 1);
				else
					tfMap.put(iid, 1);
			}
		}
		for (Project p : projectDatas) {
			Map<Long, API> apis = p.getAPIs();
			Map<Long, Integer> APICount = p.getAPICount(); // 获取每个API出现的次数
			// 统计活动项目中所有API出现的次数
			long TSUM = 0;
			for (Long key : APICount.keySet()) {
				int Ti = APICount.get(key);
				TSUM += Ti;
			}
			for (Long iid : apis.keySet()) { // 计算每个API节点的tfidf特征
				int count = APICount.get(iid);
				double tf = count * 1.0 / TSUM; // 计算TF特征
				double tfidf = ComputeUtil.computeTFIDF(tf, projectDatas.size(), tfMap.get(iid));
				p.setFeature(iid, tfidf);
			}
		}

	}

	/**
	 * 计算离线方法特征
	 *
	 * @param projectDatas 表示离线项目数据集
	 */
	public static void methodFeatures(List<Method> methodDatas) {
		// 获取项目中所有方法
		for (Method m : methodDatas) {
			float[] f = new float[AHCG.APIMap.size()];
			for (Long iid : m.getAPIs().keySet()) {
				f[iid.intValue()] = m.getAPICount().get(iid);
			}
			m.setVec(f);
		}
	}

	/**
	 * 数据预处理
	 */
	public void preprocessing() {

	}

	/**
	 * 使用CAPIRD模型训练，返回权重参数lambda
	 */
	public void trainOfCAPIRD() {

	}

	/**
	 * 使用GLAPI模型训练，返回API的特征表示
	 */
	public void trainOfGLAPI() {

	}

	/**
	 * 将代码片段存放在对应的项目下
	 * list 项目名称列表
	 * codeSnippetsPath	存放类文件的地址，目录结构按照项目>包名>类名的层次结构存放
	 */
	public static void opt2() {
		List<String> list = getLoadedProjects(localCodesPath);
		for (String projectName : list) {
			// 获取项目元数据中存在的class文件，如根据com/maxmind/geoip/DatabaseInfo/getDate()名称获取com\maxmind\geoip\DatabaseInfo.java文件
			Set<String> sets = getClassFiles(projectName);
			List<String> classFilePathList = new ArrayList<>();
			// 遍历项目源码库，找到所有项目的类文件
			readfile(new File(localCodesPath + File.separator + projectName), classFilePathList);
			System.out.println(classFilePathList.size());
			// 将源码库中存在元数据中的类文件复制到新文件位置，并按照项目层次结构保存
			for (String str : sets) {
				int n = 0;
				for (String classFilePath : classFilePathList) {
					if (classFilePath.contains(str)) {
						System.out.println("包含：" + str);
						String newFilePath = codeSnippetsPath + File.separator + projectName + File.separator + str;
						copyFileUsingFileChannels(classFilePath, newFilePath);
						break;
					} else {
						n++;
					}
				}
				if (n == classFilePathList.size())
					System.out.println("不包含：" + str);
			}
		}

	}

	/**
	 * 递归收集项目目录下所有.java的静态文件的地址，存放在参数list中
	 * @param file	项目目录对应的文件对象
	 * @param list	.java的文件绝对地址
	 */
	public static void readfile(File file, List<String> list) {
		if (file == null)
			return;
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isFile()) {
				if (f.getName().endsWith(".java")) {
					list.add(f.getAbsolutePath());
				}
			} else if (f.isDirectory()) {
				readfile(f, list);
			}
		}
	}

	/**
	 * 提取项目中的类文件地址
	 * @param projectName	项目名称
	 * @return	项目解析结果中涉及的类文件地址集合
	 */
	public static Set<String> getClassFiles(String projectName) {
		String projectAPIsPath = newAPIsPath + File.separator + projectName + ".txt";
		Set<String> sets = new HashSet<>();
		try {
			FileInputStream fin = new FileInputStream(projectAPIsPath);
			InputStreamReader reader = new InputStreamReader(fin);
			BufferedReader buffReader = new BufferedReader(reader);
			String strTmp = "";
			while ((strTmp = buffReader.readLine()) != null) {
				String methodQualifiedName = strTmp.split("#")[0];
				String classQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.lastIndexOf("/"));
				String classFilePath = classQualifiedName.replaceAll("/", "\\\\") + ".java";
				sets.add(classFilePath);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sets;
	}

	/**
	 * 提取已有项目对应的API结构
	 * localCodesPath	存在代码的项目所在目录
	 * localAPIsPath	项目解析后的目录
	 * newAPIsPath	需要转移解析结果的新目录
	 */
	public static void opt1() {
		String localCodesPath = "F:\\projects";
		String localAPIsPath = "E:\\2019200815\\experiment\\Focus\\dataset\\SH_L";
		String newAPIsPath = "F:\\projects-APIs";
		// 获取项目源码库的项目名称
		List<String> list = getLoadedProjects(localCodesPath);
		System.out.println(list.size());
		// 获取已提取API的项目元数据，key为项目名称，value为项目代码
		Map<String, String> maps = getLoadedAPIs(localAPIsPath);
		System.out.println(maps.size());
		int n = 0;
		// 过滤元数据不存在源代码的项目，将存在源代码的元数据转移到新目录
		for (String name : list) {
			if (maps.keySet().contains(name)) {
				String oldFilePath = maps.get(name);
				String newFilePath = newAPIsPath + File.separator + name + ".txt";
				copyFileUsingFileChannels(oldFilePath, newFilePath);
				n++;
			}
		}
		System.out.println(n);
	}

	/**
	 * 获取需要加载的项目
	 * @param localPath	项目目录
	 * @return	需加载的项目列表
	 */
	public static List<String> getLoadedProjects(String localPath) {
		List<String> list = new ArrayList<String>();
		File file = new File(localPath);
		File[] filesName = file.listFiles();
		for (File file2 : filesName) {
			if (file2.isDirectory()) {
				list.add(file2.getName());

			}
		}
		return list;
	}

	/**
	 * 获得项目地址索引
	 * @param localPath	项目目录
	 * @return	map，key为项目名，value为本地存放地址
	 */
	public static Map<String, String> getLoadedAPIs(String localPath) {
		Map<String, String> maps = new HashMap<>();
		File file = new File(localPath);
		File[] filesName = file.listFiles();
		for (File file2 : filesName) {
			if (!file2.isDirectory()) {
				if (file2.getName().length() > 8) {
					String name = file2.getName();
					maps.put(name.replaceAll(".txt", ""), file2.getAbsolutePath());
				}

			}
		}
		return maps;
	}

	/**
	 * 复制文件
	 * @param oldFilePath	旧文件地址
	 * @param newFilePath	新文件地址
	 */
	public static void copyFileUsingFileChannels(String oldFilePath, String newFilePath) {
		try {
			File source = new File(oldFilePath);
			File dest = new File(newFilePath);
			File dirs = new File(dest.getParent());
			if (!dirs.exists())
				dirs.mkdirs();
			if (!dest.exists()) {
				FileChannel inputChannel = null;
				FileChannel outputChannel = null;
				inputChannel = new FileInputStream(source).getChannel();
				outputChannel = new FileOutputStream(dest).getChannel();
				outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
				inputChannel.close();
				outputChannel.close();
			} else {
				System.out.println("文件已存在！");
			}
		} catch (Exception e) {
		}
	}

}
