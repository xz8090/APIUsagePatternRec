package com.recommend.buct.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.recommend.buct.entity.API;
import com.recommend.buct.entity.Method;
import com.recommend.buct.models.AHCG;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class onlineTools {

    /**
     * 根据活动方法所在目录不断查找上一级目录
     * @param activityDirectory 活动方法的类文件所在目录
     * @param projectName   活动项目名称
     * @return  活动项目所在目录
     */
    public static PsiDirectory getProjectDirectory(PsiDirectory activityDirectory, String projectName) {
        if(activityDirectory.getName().equals(projectName)) return activityDirectory;
        //System.out.println(activityDirectory.getName()+";"+activityDirectory.getVirtualFile().getPath());
        return getProjectDirectory(activityDirectory.getParent(),projectName);
    }

    /**
     * 遍历目标目录下的所有java类文件，保存到PsiFile文件列表中
     * @param projectDirectory  目标目录
     * @param psiFiles  文件列表
     */
    public static void getAllSubPsiFiles(PsiDirectory projectDirectory, List<PsiFile> psiFiles){
        //先对目录下所有文件进行扫描
        for(PsiFile psiFileTmp:projectDirectory.getFiles()){
            //获取当前类目录的所有java文件
            if(psiFileTmp.getName().endsWith(".java")){
                psiFiles.add(psiFileTmp);
            }
        }
        //再对目录下的子目录进行扫描
        for(PsiDirectory subDirectory:projectDirectory.getSubdirectories()){
            getAllSubPsiFiles(subDirectory,psiFiles);
        }
    }

    /**
     * 获取方法的全限定名
     * @param psiMethod
     * @return
     */
    public static String getAPIQualifiedName(PsiMethod psiMethod){
        //获得API类名
        String strOfClass = psiMethod.getContainingClass().getQualifiedName();
        strOfClass = strOfClass.substring(strOfClass.indexOf(":")+1,strOfClass.length());
        strOfClass = strOfClass.replaceAll("\\.","/");
        //获得API全限定名（类名+API名称）
        String qualifiedNameOfMethod = strOfClass+"/"+psiMethod.getName()+"(";
        //获得API调用的参数
        PsiParameter[] params = psiMethod.getParameterList().getParameters();
        for (int i=0;i<params.length;i++){
            PsiParameter psiParameter = params[i];
            if(i!=params.length-1) qualifiedNameOfMethod = qualifiedNameOfMethod + psiParameter.getType().getCanonicalText()+",";
            else qualifiedNameOfMethod = qualifiedNameOfMethod + psiParameter.getType().getCanonicalText();
        }
        qualifiedNameOfMethod = qualifiedNameOfMethod + ")";
        return qualifiedNameOfMethod.replaceAll("\\.\\.\\.", "[]");
    }

    /**
     * 获取每一条语句中的API调用，保存在apiSet集合中
     * @param psiElement
     * @param apiSet
     */
    public static void findMethodCallExpression(PsiElement psiElement, Set<String> apiSet){
        if(psiElement instanceof PsiMethodCallExpression){
            PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) psiElement;
            //System.out.println("method:"+methodCallExpression.getText());
            PsiReferenceExpression referenceExpression = methodCallExpression.getMethodExpression();
            if(referenceExpression.getReference() != null){
                PsiElement elementOfMethod = referenceExpression.getReference().resolve();
                if(elementOfMethod instanceof PsiMethod){
                    String qualifiedNameOfAPI = getAPIQualifiedName((PsiMethod) elementOfMethod);
                    //System.out.println(qualifiedNameOfAPI);
                    apiSet.add(qualifiedNameOfAPI);
                }
            }
        }
        PsiElement[] elements = psiElement.getChildren();
        if(elements != null){
            for(PsiElement childElement: elements){
                findMethodCallExpression(childElement,apiSet);
            }
        }
    }

    /**
     * 遍历项目所在目录中的Java文件
     * @param file 项目目录文件
     * @param list 保存PsiJavaFile文件
     * @param project 活动项目
     */
    public static void readVirtualFile(VirtualFile file, List<PsiJavaFile> list, Project project) {
        if (file == null)
            return;
        VirtualFile[] files = file.getChildren();
        for (VirtualFile f : files) {
            if (f.isDirectory()) {
                readVirtualFile(f,list,project);
            } else {
                if (f.getName().endsWith(".java")) {
                    PsiJavaFile psiJavaFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(f);
                    list.add(psiJavaFile);
                }
            }
        }
    }

    /**
     * 解析活动项目并保存数据到本地
     * @param list  活动项目中的类文件
     * @param localPath 本地磁盘地址
     */
    public static String analysisProjectToSaveLocal(List<PsiJavaFile> list, String localPath, String proName) throws IOException {
        String projectDataPath = localPath+File.separator+proName+".txt";
        File file = new File(projectDataPath);
        FileWriter fw = new FileWriter(file);
        for(PsiJavaFile psiJavaFile:list){
            PsiClass[] javaFileClasses = psiJavaFile.getClasses();
            for (PsiClass javaFileClass : javaFileClasses) {
                PsiMethod[] psiMethods = javaFileClass.getMethods();
                for (int i = 0; i < psiMethods.length; i++) {
                    PsiCodeBlock psiCodeBlock = psiMethods[i].getBody();
                    PsiElement[] psiElements = psiCodeBlock.getStatements();
                    Set<String> apiSet = new HashSet<>();
                    String methodQualifiedName = getAPIQualifiedName(psiMethods[i]);
                    for (PsiElement elementOfCodeBlock : psiElements) {
                        onlineTools.findMethodCallExpression(elementOfCodeBlock, apiSet);
                    }
                    for (String APIQualifiedName : apiSet) {
                        fw.write(methodQualifiedName+"#"+APIQualifiedName);
                        fw.write("\r\n");
                    }
                }
            }
        }
        fw.close();
        return projectDataPath;
    }

    /**
     * 调用本地CMD命令，执行命令
     */
    public static void execCommandAndGetOutput(String cmd) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            int exitValue = process.waitFor(); // 退出值 0 为正常，其他为异常
            String getStreamStr = getStreamStr(process.getInputStream());
            System.out.println("exitValue: " + exitValue);
            System.out.println(getStreamStr);
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得控制台输出结果
     * @param is
     * @return
     * @throws IOException
     */
    public static String getStreamStr(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        br.close();
        return sb.toString();
    }

    public static void executeCMD(int type){
        Thread t = new Thread(new Runnable() {
            public void run() {
                String cmd = "";
                switch (type){
                    case 1:
                        System.out.println("执行CAPIRD模型启动命令");
                        cmd = "cmd /c cd /d F:\\APIRecommendation\\trainApplication\\CAPIRD && python main.py";
                        break;
                    case 2:
                        System.out.println("执行GLAPI模型启动命令");
                        cmd = "cmd /c cd /d F:\\APIRecommendation\\trainApplication\\GLAPI && python GLAPI.py";
                        break;
                    case 3:
                        System.out.println("执行GLAPI模型特征提取命令");
                        cmd = "cmd /c cd /d F:\\APIRecommendation\\trainApplication\\GLAPI && python feature_extraction.py";
                        break;
                }
                execCommandAndGetOutput(cmd);
            }
        });
		t.start();
    }

    public static void main(String[] args){
        executeCMD(2);
        executeCMD(3);
        executeCMD(1);
    }
}
