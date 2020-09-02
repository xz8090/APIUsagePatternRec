package entity;

import java.util.ArrayList;
import java.util.Arrays;

public class Graph {
	//项目名
	private String name = System.currentTimeMillis()+"";
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// 存放每个结点内的数据
	private ArrayList<String> vertexList;
	
	public ArrayList<String> getVertexList() {
		return vertexList;
	}
	
	public void setVertexList(ArrayList<String> vertexList) {
		this.vertexList = vertexList;
	}

	// 存放图对应的邻接矩阵
	private int[][] edges;
	
	public int[][] getEdges() {
		return edges;
	}

	// 存放边的数目
	private int numOfEdges;

	public static void main(String[] args) {
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
		graph.showGraph();

	}

	/**
	 * 
	 * @param 顶点个数
	 */
	public Graph(int n) {
		edges = new int[n][n];
		vertexList = new ArrayList<String>(n);
		for(int i=0;i<n;i++) {
			int[] arr = new int[n];
			for(int j=0;j<n;j++) {
				arr[j] = 0;
			}
			edges[i] = arr;
		}
	}

	/**
	 * 
	 * @Title: insertVertex
	 * @Description: 向顶点中加入该顶点的数据
	 * @param @param vertex 要插入的数据
	 * @return void 返回类型
	 */
	public void insertVertex(String vertex) {
		vertexList.add(vertex);
	}

	/**
	 * 
	 * @Title: insertEdges
	 * @Description: 将邻接矩阵各个结点之间的关系建立起来，1代表相连，0代表不相连
	 * @param @param v1 代表下标为v1的顶点
	 * @param @param v2 代表下标为v2的顶点
	 * @param @param weight 权值，不是0就是1
	 * @return void 返回类型
	 */
	public void insertEdges(int v1, int v2, int weight) {
		//加上原先边的权重
		edges[v1][v2] = edges[v1][v2] + weight;
		edges[v2][v1] = edges[v2][v1] + weight;
		
		numOfEdges++;
	}

	// 返回结点数
	public int getNumOfVertex() {
		return vertexList.size();
	}

	// 返回边数
	public int getNumOfEdges() {
		return numOfEdges;
	}

	// 返回i对应的数据
	public String getValueByyIndex(int i) {
		return vertexList.get(i);
	}

	// 返回v1和v2的权值
	public int getWeight(int v1, int v2) {
		return edges[v1][v2];
	}

	// 显示图对应的矩阵
	public String showGraph() {
		String arrayStr = "";
		for (int[] link : edges) {
			String s = Arrays.toString(link);
			System.out.println(s);
			arrayStr = arrayStr + s + ',';
		}
		if(arrayStr.isEmpty()) return "none";
		else return arrayStr.substring(0, arrayStr.length()-1);
	}
}

