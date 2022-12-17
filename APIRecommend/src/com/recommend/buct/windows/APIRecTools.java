package com.recommend.buct.windows;

import com.intellij.openapi.wm.ToolWindow;
import com.recommend.buct.models.AHCG;
import com.recommend.buct.utils.onlineTools;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

public class APIRecTools {
    private JPanel myPanel;
    private JButton loadMetaDatasBtn;
    private JButton offlineLearnBtn;
    private JButton featureExtractBtn;
    private JButton cancelBtn;
    private JTable APIUsagesTab;
    private JScrollPane tabScrollPane;
    private JComboBox modelType;

    private boolean loadMetaDatasBtnIsClick = false;
    private boolean contextAnalysisBtnIsClick = false;
    private int selectType = 1;

    public APIRecTools(ToolWindow toolWindow){
        init(toolWindow);
    }

    /**
     * 组件初始化，如点击事件
     * @param toolWindow
     */
    private void init(ToolWindow toolWindow) {
        cancelBtn.addActionListener(e -> toolWindow.hide(null));
        //加载元数据按钮事件
        loadMetaDatasBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!loadMetaDatasBtnIsClick) {
                    System.out.println("正在加载元素据");
                    AHCG.loadMetaDatasets();
                    AHCG.buildAHCG();
                    loadMetaDatasBtnIsClick = true;
                }else{
                    JOptionPane.showMessageDialog(null, "元数据已加载！","提示",JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        //模型选择下拉框事件
        modelType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                switch (event.getStateChange()) {
                    case ItemEvent.SELECTED:
                        System.out.println("选中" + event.getItem());
                        if(event.getItem().toString().contains("CAPIRD")){
                            selectType = 1;
                        }else if(event.getItem().toString().contains("GLAPI")){
                            selectType = 2;
                        }else{
                            selectType = 1;
                        }
                        break;
                    case ItemEvent.DESELECTED:
                        System.out.println("取消选中" + event.getItem());
                        break;
                }
            }

        });
        //离线学习按钮事件
        offlineLearnBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    System.out.println("离线解析...");
                    String projectDataPath = onlineTools.analysisProjectToSaveLocal(MyToolWindowFactory.psiJavaFiles,AHCG.offlineApplication,MyToolWindowFactory.proName);
                    contextAnalysisBtnIsClick = true;
                    JOptionPane.showMessageDialog(null, "代码解析成功，解析结果保存在:"+projectDataPath,"成功",JOptionPane.INFORMATION_MESSAGE);
                    // 两种启动方式
                    // 1.通过http请求方式触发相关服务启动
                    // 2.通过CMD命令执行相关服务
                    if(contextAnalysisBtnIsClick){
                        onlineTools.executeCMD(selectType);
                        JOptionPane.showMessageDialog(null, "正在启动后台训练模型！","提示",JOptionPane.INFORMATION_MESSAGE);
                    }else{
                        JOptionPane.showMessageDialog(null, "请先进行上下文解析！","提示",JOptionPane.INFORMATION_MESSAGE);
                    }

                } catch (IOException ioException) {
                    JOptionPane.showMessageDialog(null, "代码解析或保存时发生异常！","异常",JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        //特征提取按钮事件
        featureExtractBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onlineTools.executeCMD(3);
                JOptionPane.showMessageDialog(null, "正在提取特征，将保存在目录：F:\\APIRecommendation\\trainApplication\\GLAPI\\data","提示",JOptionPane.INFORMATION_MESSAGE);
            }
        });

    }

    public void updateTables(Object[][] rowData){
        String[] columnNames = {"N", "代码片段", "相似值", "操作"};
        if(rowData!=null) {
            TableModel dataModel = new DefaultTableModel(rowData, columnNames) {
                //只允许第2列和第4列可编辑
                public boolean isCellEditable(int row, int column) {
                    return column == 1 || column == 3;
                }
            };
            APIUsagesTab = new JTable(dataModel);
            APIUsagesTab.getColumnModel().getColumn(0).setMinWidth(25);
            APIUsagesTab.getColumnModel().getColumn(0).setMaxWidth(25);
            TableColumn column = APIUsagesTab.getColumnModel().getColumn(1);
            column.setCellRenderer(new WordWrapCellRenderer());
            column.setCellEditor(new WordWrapCellEditor());
            //dataTable.getColumnModel().getColumn(1).setCellRenderer(new WordWrapCellRenderer());
            APIUsagesTab.getColumnModel().getColumn(2).setMinWidth(50);
            APIUsagesTab.getColumnModel().getColumn(2).setMaxWidth(50);
            APIUsagesTab.getColumnModel().getColumn(3).setMinWidth(40);
            APIUsagesTab.getColumnModel().getColumn(3).setMaxWidth(40);
            column = APIUsagesTab.getColumnModel().getColumn(3);
            column.setCellRenderer(new MyButtonRenderer());
            column.setCellEditor(new MyButtonEditor());
            tabScrollPane.setViewportView(APIUsagesTab);
        }
    }

    public JPanel getContentPanel() {
        return myPanel;
    }
}

