package com.recommend.buct.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.recommend.buct.entity.Method;

public class FileUtil {
	public static String codeSnippetsPath = "F:\\APIRecommendation\\code-snippets";

	/**
	 * 遍历项目所在目录中的Java文件
	 * @param file 项目目录文件
	 * @param list 保存Java文件地址
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
	 * 提取项目中代码片段对应的类文件地址
	 * @param projectName	项目名称
	 * @param methodQualifiedName	方法全限定名
	 * @return
	 */
	public static String getCodeSnippetPath(String projectName, String methodQualifiedName) {
		String tmpMethodQualifiedName = methodQualifiedName.substring(0, methodQualifiedName.indexOf("("));
		String classQualifiedName = tmpMethodQualifiedName.substring(0, tmpMethodQualifiedName.lastIndexOf("/"));
		String classFilePath = classQualifiedName.replaceAll("/", "\\\\") + ".java";
		String projectPath = codeSnippetsPath + File.separator + projectName + File.separator + classFilePath;
		return projectPath;
	}

	public static String getCodeSnippets(Method m) {
		String methodName = m.getName();
		String methodQualifiedName = m.getQualifiedName();
		String[] params = methodQualifiedName.substring(methodQualifiedName.indexOf("(") + 1, methodQualifiedName.indexOf(")")).split(",");
		String projectPath = m.getCodeSnippets();
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
}
