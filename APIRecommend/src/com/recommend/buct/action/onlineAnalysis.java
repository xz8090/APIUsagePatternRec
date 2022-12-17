package com.recommend.buct.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.util.*;
import com.intellij.util.Consumer;
import com.recommend.buct.entity.API;
import com.recommend.buct.entity.Context;
import com.recommend.buct.entity.Method;
import com.recommend.buct.models.AHCG;
import com.recommend.buct.models.Online;
import com.recommend.buct.utils.FileUtil;
import com.recommend.buct.utils.onlineTools;
import com.recommend.buct.windows.MyToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class onlineAnalysis extends AnAction {
    public static boolean ifLoadMetaDatas = false;
    public static Map<String,Long> APINameToId;

    @Override
    public void actionPerformed(AnActionEvent e) {
        //获取当前在操作的工程上下文
        Project project = e.getData(PlatformDataKeys.PROJECT);
        String proName = project.getName();
        System.out.println("活动项目名："+proName);
        //获取当前操作的类文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        //判断是否加载了本地元数据
        if(ifLoadMetaDatas) {
            System.out.println(String.format("元数据项目数：%d，方法数：%d，API数：%d",AHCG.pid,AHCG.mid,AHCG.iid));
            //获得当前对象的文件目录
            PsiDirectory activityDirectory = psiFile.getParent();
            PsiDirectory projectDirectory = onlineTools.getProjectDirectory(activityDirectory, proName);

            //定义活动上下文
            Context c = new Context(AHCG.pid, proName, projectDirectory.getVirtualFile().getPath());
            AHCG.pid++;

            //获取项目中所有PsiFile
            List<PsiFile> psiFiles = new ArrayList<>();
            onlineTools.getAllSubPsiFiles(projectDirectory, psiFiles);
            //获取编辑位置信息
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (editor == null || psiFile == null) {
                return;
            }
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            if (element != null) {
                //当前光标所在方法
                PsiMethod acitivityMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
                if (acitivityMethod != null) {
                    c.setActiveMethod(AHCG.mid);
                    //获得方法全限定名
                    String qualifiedNameOfMethod = onlineTools.getAPIQualifiedName(acitivityMethod);
                    String codeSnippetPath = FileUtil.getCodeSnippetPath(proName, qualifiedNameOfMethod);
                    Method activityMethod = new Method(AHCG.mid, acitivityMethod.getName(), qualifiedNameOfMethod, codeSnippetPath);
                    AHCG.mid++;
                    c.addMethod(activityMethod);
                    System.out.println("活动方法:" + qualifiedNameOfMethod);

                    //获得方法内的API调用
                    PsiElement[] psiElements = acitivityMethod.getBody().getStatements();
                    Set<String> apiSet = new HashSet<>();
                    for (PsiElement elementOfCodeBlock : psiElements) {
                        onlineTools.findMethodCallExpression(elementOfCodeBlock, apiSet);
                    }
                    System.out.println("活动方法API列表:");
                    for (String APIQualifiedName : apiSet) {
                        System.out.println(APIQualifiedName);
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
                        activityMethod.addAPI(tmpAPI);
                        c.addAPI(tmpAPI);
                    }

                    //光标所在类
                    PsiClass activityClass = acitivityMethod.getContainingClass();
                    for (PsiFile targetPsiFile : psiFiles) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(targetPsiFile.getVirtualFile());
                        PsiClass[] javaFileClasses = psiJavaFile.getClasses();
                        for (PsiClass javaFileClass : javaFileClasses) {
                            System.out.println("类名：" + javaFileClass.getName() + ";全限定名：" + javaFileClass.getQualifiedName());
                            PsiMethod[] psiMethods = javaFileClass.getMethods();
                            System.out.println("活动方法所在类的其他方法:");
                            for (int i = 0; i < psiMethods.length; i++) {
                                //活动方法不需要提取了
                                if (!psiMethods[i].getContainingClass().getQualifiedName().equals(activityClass.getQualifiedName()) || !psiMethods[i].getName().equals(acitivityMethod.getName())) {
                                    PsiCodeBlock psiCodeBlock = psiMethods[i].getBody();
                                    psiElements = psiCodeBlock.getStatements();
                                    apiSet = new HashSet<>();
                                    System.out.println("方法名:" + psiMethods[i].getName());
                                    String methodQualifiedName = onlineTools.getAPIQualifiedName(psiMethods[i]);
                                    codeSnippetPath = FileUtil.getCodeSnippetPath(proName, methodQualifiedName);
                                    Method tmpMethod = new Method(AHCG.mid, psiMethods[i].getName(), methodQualifiedName, codeSnippetPath);
                                    AHCG.mid++;
                                    c.addMethod(tmpMethod);
                                    System.out.println("方法全限定名:" + methodQualifiedName);

                                    for (PsiElement elementOfCodeBlock : psiElements) {
                                        onlineTools.findMethodCallExpression(elementOfCodeBlock, apiSet);
                                    }
                                    System.out.println("方法API列表:");
                                    for (String APIQualifiedName : apiSet) {
                                        System.out.println(APIQualifiedName);
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
                                        activityMethod.addAPI(tmpAPI);
                                        c.addAPI(tmpAPI);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("光标区域不在方法体！");
                }
            }
            //保存上下文信息
            AHCG.setContext(c);
            System.out.println("活动项目和方法："+c.getId()+","+c.getActiveMethod());
            System.out.println(String.format("活动项目：%s;活动方法：%s;正在预测API",proName,AHCG.findMethodById(c.getId(),c.getActiveMethod()).getQualifiedName()));
            List<String> APIRecList = new ArrayList<>();
            APINameToId = new HashMap<>();
            AHCG.MMR(0.9,APINameToId,APIRecList,10);
            //从相似的前100个项目中推荐API使用模型
            List<com.recommend.buct.entity.Project> topNProject = Online.recommendationProject(AHCG.projectResults, 100);

            JBPopupFactory instance = JBPopupFactory.getInstance();// 创建实例

            instance.createPopupChooserBuilder(APIRecList)
                    .setTitle("API推荐列表")// 设置标题
                    .setFont(new Font("Times New Roman",Font.BOLD,18))
                    .setItemSelectedCallback(new Consumer<String>() {// 添加监听事件
                        @Override
                        public void consume(String s) {
                            if (s != null) {
                                String recAPIName = s.split(" ")[0];
                                Long iidtmp = APINameToId.get(recAPIName);
                                System.out.println(recAPIName+";"+iidtmp);
                                c.setPredictAPI(iidtmp);
                                Map<String, Double> recommendationResults = Online.recommendation(c, topNProject);
                                int k=0;
                                int N = 5;
                                Object[][] rowData = new Object[N][];
                                for (String key : recommendationResults.keySet()) {
                                    if(k<N) {
                                        Long ppid = Long.parseLong(key.split("#")[0]);
                                        Long mmid = Long.parseLong(key.split("#")[1]);
                                        com.recommend.buct.entity.Project p = AHCG.findProjectById(ppid);
                                        Method m = AHCG.findMethodById(ppid, mmid);
                                        //System.out.println(key+","+recommendationResults.get(key)+","+p.getName()+","+m.getQualifiedName());
                                        String codes = AHCG.getCodeSnippets(p.getName(),m.getQualifiedName());
                                        //System.out.println(codes);
                                        rowData[k] = new Object[4];//结果表有4列
                                        rowData[k][0] = k+1;
                                        rowData[k][1] = codes.isEmpty()?"Code snippet missing...":codes;
                                        rowData[k][2] = recommendationResults.get(key);
                                        rowData[k][3] = "导入";
                                    }else break;
                                    k++;
                                }
                                MyToolWindowFactory.apiRecTools.updateTables(rowData);
                            }
                        }
                    }).createPopup().showInBestPositionFor(e.getDataContext());
        }else{
            JOptionPane.showMessageDialog(null, "请先加载元数据！","错误提示",JOptionPane.ERROR_MESSAGE);
        }
    }


}
