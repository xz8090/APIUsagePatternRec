package com.recommend.buct.windows;

import com.intellij.util.ui.DialogUtil;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

/**
 * 修改组件
 */
public class MyToolModule  {


}

/**
 * 修改JTable中单元格的文本自动换行，及背景颜色，单元格最小高度等
 */
class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {
    public static int[] h = new int[5];
    WordWrapCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value.toString());
        setFont(new Font("Times New Roman",Font.BOLD,16));
        setBackground(Color.lightGray);
        setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
        if (table.getRowHeight(row) != getPreferredSize().height) {
            h[row] = getPreferredSize().height<50?50:getPreferredSize().height;
            table.setRowHeight(row, h[row]);
        }
        return this;
    }
}

class WordWrapCellEditor extends AbstractCellEditor implements TableCellEditor {
    private JPanel panel;
    private JScrollPane scrollpane;
    private JTextArea jTextArea;
    private String val;

    public WordWrapCellEditor(){
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        jTextArea = new JTextArea();
        jTextArea.setFont(new Font("Times New Roman",Font.BOLD,16));
        jTextArea.setLineWrap(true);
        jTextArea.setEditable(true);
        jTextArea.addFocusListener(new FocusListener(){
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println("代码片段聚焦");
            }
            @Override
            public void focusLost(FocusEvent e) {
                //失去焦点执行的代码
                jTextArea.setText(jTextArea.getText());
            }
        });
        scrollpane=new JScrollPane();//创建滚动条面板
        scrollpane.setViewportView(jTextArea);
        panel.add(scrollpane, BorderLayout.CENTER);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        System.out.println("编辑");
        panel.setBackground(Color.lightGray);
        panel.setSize(table.getColumnModel().getColumn(column).getWidth(), WordWrapCellRenderer.h[row]);
        if (table.getRowHeight(row) != WordWrapCellRenderer.h[row]) {
            table.setRowHeight(row, WordWrapCellRenderer.h[row]);
        }
        val = String.valueOf(value);
        jTextArea.setText(value == null ? "" : val);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return val;
    }
}

/**
 * 修改JTable中单元格的文本为按钮，按钮名称为原文本
 */
class MyButtonRenderer implements TableCellRenderer {
    private JPanel panel;
    private JButton button;
    private String val;

    public MyButtonRenderer() {
        initButton();
        initPanel();
        panel.add(button, BorderLayout.CENTER);
    }

    private void initButton() {
        button = new JButton();
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        val = String.valueOf(value);
        button.setText(value == null ? "" : val);
        return panel;
    }

}

/**
 * 修改JTable中单元格的文本为按钮后的点击事件
 */
class MyButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private JPanel panel;
    private JButton button;
    private String val;

    public MyButtonEditor() {
        initButton();
        initPanel();
        panel.add(this.button, BorderLayout.CENTER);
    }

    private void initButton() {
        button = new JButton();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int res = JOptionPane.showConfirmDialog(null,
                        "是否导入该代码片段并替换活动方法?", "提示",
                        JOptionPane.YES_NO_OPTION);
                if(res ==  JOptionPane.YES_OPTION){
                    System.out.println("yes");
                }
                //stopped!!!!
                fireEditingStopped();
            }
        });

    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        val = String.valueOf(value);
        button.setText(value == null ? "" : val);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return val;
    }

}


