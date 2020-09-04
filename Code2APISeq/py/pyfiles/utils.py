import networkx as nx
import matplotlib.pyplot as plt
def adMmatrix2Img(matrix,filePath):
    plt.figure(num=None, figsize=(30, 30), dpi=80) 
    G=nx.Graph()
    n = len(matrix)
    point = []
    for i in range(n):
        point.append(i)
    G.add_nodes_from(point)
    edglist=[]
    for i in range(n):
        for k in range(i+1,n):
            if matrix[i][k] > 0:
                edglist.append((i,k,matrix[i][k]))
    G.add_weighted_edges_from(edglist)
    position = nx.circular_layout(G)
    nx.draw_networkx_nodes(G,position, nodelist=point, node_color="y")
    nx.draw_networkx_edges(G,position,width=[float(d['weight']) for (u,v,d) in G.edges(data=True)])
    nx.draw_networkx_labels(G,position)
    plt.savefig(filePath)