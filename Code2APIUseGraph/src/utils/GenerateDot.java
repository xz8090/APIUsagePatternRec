package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import entity.Graph;

public class GenerateDot {

	//存放dot文件位置
	private static String dotUrl = "dots/";
	
	public static String str2dot(String path,String content) {
		String flag = "0";
		try {
			File file = new File(path);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fileWriter);
			bw.write(content);
			bw.close();
			flag = "success";
	    } catch (IOException e) {
	        e.printStackTrace();
	        flag = "error";
	    }
		return flag;
	}
	public static String graph2dot(Graph graph) {
		String path = dotUrl + graph.getName() + ".dot";
		ArrayList<String> VertexList = graph.getVertexList();
		String content = "graph {\r\n" + "    { rank=same;";
		for (String string : VertexList) {
			content = content + "\"" + string + "\";";
		}
		content = content + "}\r\n";
		int[][] edges = graph.getEdges();
		for(int i=0;i<VertexList.size();i++) {
			for(int j=i+1;j<VertexList.size();j++) {
				int weight = edges[i][j];
				if(weight>0) {
					String v1 = VertexList.get(i);
					String v2 = VertexList.get(j);	
					content = content + "\"" + v1 + "\" -- \"" + v2 + "\" \r\n";
				}
				
			}
		}
		content = content + "}";
		String flag = str2dot(path,content);
		if(flag=="success") {
			System.out.println("保存成功！路径："+path);
		}
		return path;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int n = 5;
		Graph graph = new Graph(5);
		String[] vertexValue = { "A", "B", "C", "D", "E" };
		// 建立各顶点所对应的值
		for (String vertex : vertexValue) {
			graph.insertVertex(vertex);
		}
		// 构造边的关系
		graph.insertEdges(0, 0, 1);
		graph.insertEdges(0, 2, 1);
		graph.insertEdges(1, 2, 1);
		graph.insertEdges(1, 3, 1);
		graph.insertEdges(1, 4, 1);
		String path = graph2dot(graph);
		System.out.println(path);
	}

}
