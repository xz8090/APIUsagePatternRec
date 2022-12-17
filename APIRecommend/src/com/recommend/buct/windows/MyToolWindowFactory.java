package com.recommend.buct.windows;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.recommend.buct.utils.onlineTools;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyToolWindowFactory implements ToolWindowFactory {
    public static APIRecTools apiRecTools;
    public static List<PsiJavaFile> psiJavaFiles;
    public static String proName;
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if(apiRecTools==null) apiRecTools = new APIRecTools(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(apiRecTools.getContentPanel(), "API使用模式列表", false);
        toolWindow.getContentManager().addContent(content);
        proName = project.getName();
        if(psiJavaFiles==null) psiJavaFiles = new ArrayList<>();
        VirtualFile proVirtualFile = project.getProjectFile().getParent().getParent();
        onlineTools.readVirtualFile(proVirtualFile,psiJavaFiles,project);
        System.out.println("项目上下文读取成功！");
    }
}
