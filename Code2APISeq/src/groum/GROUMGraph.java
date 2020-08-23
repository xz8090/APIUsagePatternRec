package groum;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.io.Serializable;


public class GROUMGraph implements Serializable
{
    public static final int minSize = 1;//3
    public static final int maxSize = 15;
    private static final long serialVersionUID = 1L;
    public static int nextId=1;
    private int id;
    private int fileID;
    private String name;
    private String comments;
    private String rawcode;
    protected HashSet<GROUMNode> nodes;
    /***  nodes that are tails of the groum and are about to be merged with other groums ; used for merge*/
    protected HashSet<GROUMNode> outs;
    /***  nodes that are heads of the groum and about to be merged with other groums ; useful for merge */
    protected HashSet<GROUMNode> ins;
    
    public GROUMGraph() {
        super();
        this.fileID = -1;
        this.nodes = new HashSet<GROUMNode>();
        this.outs = new HashSet<GROUMNode>();
        this.ins = new HashSet<GROUMNode>();
        this.id = GROUMGraph.nextId++;
    }
    
    public void addDataNode(final GROUMNode node)
    {
   	 if(!this.nodes.contains(node))this.nodes.add(node);
    }
    
    public void addNode(final GROUMNode node) {
        if (!this.nodes.contains(node)) {
            this.outs.add(node);
            this.nodes.add(node);
            this.ins.add(node);
        }
    }
    
    public void removeNode(final GROUMNode node) {
        if (this.nodes.contains(node)) {
            this.nodes.remove(node);
            this.ins.remove(node);
            this.outs.remove(node);
        }
    }
    
    
    public GROUMEdge createNewEdge(final GROUMNode node, final GROUMNode anoNode,String label) {
        if (!node.getOutNodes().contains(anoNode)) {
            return new GROUMEdge(node, anoNode,label);
        }
        return null;
    }
    
    /***
     * Sequential merge with another groum (=>).
     * X => Y is also a groum containing all nodes and edges of X and Y . 
     * However, there will be an edge from each sink node (i.e. node having no outgoing edge)
     * of X to each source node (i.e. node having no incoming edge) of Y. 
     * Those edges represent the temporal usage order, 
     * i.e. all nodes of X are used before all nodes of Y .
     * @param target
     */
    public void mergeSeq(final GROUMGraph target) {
        if (target.getNodes().size() == 0) {
            return;
        }
        if (this.nodes.size() == 0) {
            this.nodes.addAll(target.getNodes());
            this.ins.addAll(target.getIns());
            this.outs.addAll(target.getOuts());
            return;
        }
        this.nodes.addAll(target.getNodes());
        for (GROUMNode aNode : this.outs) {
            for (GROUMNode anoNode : target.ins) {
                this.createNewEdge(aNode, anoNode,"seq");
            }
        }
        if (target.getOuts().size() != 0) {
            this.outs.removeAll(this.outs);
            this.outs.addAll(target.getOuts());
        }
    }

    
    /**
     * Parallel merge with another groum.
     * @param target - another groum to be merged, here it is a branch content such as a body of while.
     * @param branch - branch node such as IF,while
     */
    public void mergeABranch( GROUMGraph target, GROUMNode branch) {
        if (target.getNodes().size() == 0) {
            return;
        }
        this.nodes.addAll(target.getNodes());
        for (GROUMNode aNode : target.ins) {
            this.createNewEdge(branch, aNode,"seq");
        }
        this.outs.addAll(target.getOuts());
    }
    
    /** sequentially merge current graph with a branch node
     */
    public void mergeSeq(GROUMNode branch) {
        this.addNode(branch);
        this.ins.remove(branch);
        for (final GROUMNode aNode : this.outs) {
            if (!aNode.equals(branch)) {
                this.createNewEdge(aNode, branch,"seq");
            }
        }
        if (this.ins.size() == 0) {
            this.ins.add(branch);
        }
        this.outs.removeAll(this.outs);
        this.outs.add(branch);
    }
    
    public void mergeBranches(final GROUMGraph target, final HashSet<GROUMNode> saveOuts) {
        if (target.getNodes().size() == 0) {
            return;
        }
        if (saveOuts.size() == 0) {
            this.nodes.addAll(target.getNodes());
            this.ins.addAll(target.getIns());
            this.outs.addAll(target.getOuts());
            return;
        }
        this.nodes.addAll(target.getNodes());
        for (GROUMNode aNode : saveOuts) {
            for (GROUMNode anoNode : target.ins) {
                this.createNewEdge(aNode, anoNode,"seq");
            }
        }
        this.outs.addAll(target.getOuts());
    }
    
    /***
     * Remove unnecessary nodes and edges
     */
    public void prune()
    {
   	 pruneDumbEdges();
   	 pruneIsolatedNodes();
    }
    
    /**
     *remove edges that are linked to outer nodes
     */
    public void pruneDumbEdges()
    {
   	 for (GROUMNode node : new HashSet<GROUMNode>(this.nodes)) {
   		   List<GROUMEdge> edgesToBeRemoved=new ArrayList<GROUMEdge>();
				for(GROUMEdge outedge:node.getOutEdges())//remove sink edges that are linked to outer nodes
				{
					if(!nodes.contains(outedge.getDest())){
						  edgesToBeRemoved.add(outedge);
					}
				}
				for(GROUMEdge inedge:node.getInEdges())//remove source edges that are linked to outer nodes
				{
					if(!nodes.contains(inedge.getSrc())){
						edgesToBeRemoved.add(inedge);
					}
				}
				for(GROUMEdge edge:edgesToBeRemoved)edge.delete();
   	 }

    }
    
    /***
     * remove isolated or duplicated nodes
     */
    public void pruneIsolatedNodes() {
   	 Set<GROUMNode> nodes_toremove=new HashSet<GROUMNode>();
        for (GROUMNode node : new HashSet<GROUMNode>(this.nodes)) {
            if(node.getInEdges().isEmpty()&&node.getOutEdges().isEmpty())//remove an isolated node
            {
            	nodes_toremove.add(node);
            	//this.nodes.remove(node);
            	continue;
            }
        }
        for(GROUMNode node:nodes_toremove)this.nodes.remove(node);
    }
    
    /***
     * remove isolated or duplicated nodes
     */
    public void simplify() {
        Set<GROUMEdge> edges_toremove=new HashSet<GROUMEdge>();
        Set<GROUMNode> nodes_toremove=new HashSet<GROUMNode>();
        for(GROUMNode node:new HashSet<GROUMNode>(this.nodes))
        {
      	  if (node.getType() != GROUMNode.TYPE_METHOD) {//do nothing with control nodes
              continue;
      	  }
            /**remove duplicate node*/
      	  if(node.getOutEdges().size()==1) {
    		  	 GROUMEdge edge=node.getOutEdges().iterator().next();
    		  	 GROUMNode next = edge.getDest();
             if (node.getObjectNameId() == next.getObjectNameId() && node.getMethod() == next.getMethod()) {
                  //if a node is identical to its sink node, remove it.
            	 for (GROUMEdge inEdge : new HashSet<GROUMEdge>(node.getInEdges())) {
                        inEdge.setDest(next);
                 }
                 if (node.getParameters() != null && !node.getParameters().isEmpty()) {
                        if (next.getParameters() == null) {
                            next.setParameters(new HashSet<Integer>());
                        }
                        next.getParameters().addAll(node.getParameters());
                 }
                 next.setSingletonType(GROUMNode.TYPE_MULTIPLE);
                  // edge.delete();
                 edges_toremove.add(edge);
                 nodes_toremove.add(node);
                  // this.nodes.remove(node);
              }
           }
        }
        for(GROUMNode node2:nodes_toremove)this.nodes.remove(node2);
        for(GROUMEdge edge:edges_toremove)edge.delete();
    }
     
    
    public void addDataDependency() {
        for (final GROUMNode node : this.nodes) {
            this.addDataDependency(node);
        }
    }
    

    
    /***
     * Find data dependency of a node with other linked nodes and add edge between them.
     * Traverse over all the linked nodes and find if any node has the same variables of the current node, 
     * if so, then add a "data" edge with them.
     * @param node
     */
    private void addDataDependency(final GROUMNode node) {
  
        final Vector<GROUMNode> queue = new Vector<GROUMNode>();
        queue.addAll(node.getOutNodes());
        final HashSet<GROUMNode> checkedNodes = new HashSet<GROUMNode>();
        final HashMap<Integer, HashMap<Integer, Integer>> checkedLabels = new HashMap<Integer, HashMap<Integer, Integer>>();
        final HashSet<Integer> checkedObjects = new HashSet<Integer>();
        checkedNodes.add(node);
        HashMap<Integer, Integer> depths = new HashMap<Integer, Integer>();
        depths.put(this.getDepth(node.getPid()), 1);
        int label = (node.getObjectNameId() << 16) + node.getMethodID();//label of current node
        checkedLabels.put(label, depths);
        checkedObjects.add(node.getObjectNameId());//object name of current node
        while (!queue.isEmpty()) {
            final GROUMNode aNode = queue.firstElement();
            queue.remove(aNode);
            for (final GROUMNode anoNode : aNode.getOutNodes()) {
                if (!checkedNodes.contains(anoNode)) {
                    queue.add(anoNode);
                    checkedNodes.add(anoNode);
                    label = (anoNode.getObjectNameId() << 16) + anoNode.getMethodID();//label or another node
                    final int depth = this.getDepth(anoNode.getPid());
                    depths = checkedLabels.get(label);
                    if ((depths != null && depths.containsKey(depth) && depths.get(depth) >= 2)
                  		  || (!node.getPid().startsWith(anoNode.getPid()) && !anoNode.getPid().startsWith(node.getPid())) 
                  		  || !node.hasDataDependency(anoNode)) {
                        continue;//have no dependence,continue
                    }
                    int c = 0;
                    if (depths == null) {
                        depths = new HashMap<Integer, Integer>();
                    }
                    else if (depths.containsKey(depth)) {
                        c = depths.get(depth);
                    }
                    ++c;
                    depths.put(depth, c);
                    checkedLabels.put(label, depths);
                    this.createNewEdge(node, anoNode,"data");//???
                }
            }
        }
    }
    
    public void removeNonDependents() {
        for (final GROUMNode node : this.nodes) {
            final HashSet<GROUMEdge> tmp = new HashSet<GROUMEdge>(node.getOutEdges());
            for (final GROUMEdge outEdge : tmp) {
 //##############################################################????????????????????
   //??????need to carefully check dependence. sometimes two control nodes do not have data dependence but is disconnected.
   //??????the problem is: when disconnect node A with B which is not data dependent, we need to consider reconnect A and B's sinks
            	GROUMNode next=outEdge.getDest(); 
            	if (!node.hasDataDependency(next)) {
                    outEdge.delete();
   /*                 for(GROUMEdge nextnext:next.getOutEdges())//??? be careful about this code. 
                  	  this.createNewEdge(node, nextnext.getDest(), nextnext.getLabel());*/
                }
            }
        }
    }
    
    public void cleanUp() {
        for (final GROUMNode node : this.nodes) {
            node.setPid(null);
            node.setParameters(null);
        }
    }
    
    private int getDepth(final String pid) {
        int depth = 0;
        for (int i = 0; i < pid.length(); ++i) {
            if (pid.charAt(i) == '.') {
                ++depth;
            }
        }
        return depth - 1;
    }
    
    
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, Integer> countLabel = new HashMap<String, Integer>();
        sb.append("Method: " + this.name + " File: " + GROUMNode.fileNames.get(this.fileID) + "\r\n");
        sb.append("List of nodes:" + this.nodes.size() + "\r\n");
        for (final GROUMNode node : this.nodes) {
            sb.append("node " + node.getId() + " with label:" + GROUMNode.labelOfID.get(node.getClassNameId()) + "." + GROUMNode.labelOfID.get(node.getObjectNameId()) + "." + node.getMethod() + " - " + node.getMethodID() + " - " + node.getLabel() + "\r\n");
            if (countLabel.get(node.getMethod()) != null) {
                final int count = countLabel.get(node.getMethod()) + 1;
                countLabel.put(node.getMethod(), count);
            }
            else {
                countLabel.put(node.getMethod(), 1);
            }
        }
        int numEdges = 0;
        sb.append("List of edges:\r\n");
        for (final GROUMNode node2 : this.nodes) {
            for (final GROUMEdge edge : node2.getOutEdges()) {
                final GROUMNode anoNode = edge.getDest();
                if (!anoNode.getInEdges().contains(edge)) {
                    System.err.println("ERRORERRORERRORERRORERRORERROR");
                    System.err.println(String.valueOf(node2.getId()) + "-" + anoNode.getId());
                }
                sb.append("node " + node2.getId() + " --> node" + anoNode.getId() + "\r\n");
                ++numEdges;
            }
        }
        sb.append("Total " + numEdges + " edges" + "\r\n");
        numEdges = 0;
        for (final GROUMNode node2 : this.nodes) {
            for (final GROUMEdge edge : node2.getInEdges()) {
                final GROUMNode anoNode = edge.getSrc();
                if (!anoNode.getOutEdges().contains(edge)) {
                    System.err.println("ERRORERRORERRORERRORERRORERROR");
                    System.err.println(String.valueOf(node2.getId()) + "-" + anoNode.getId());
                }
                sb.append("node " + node2.getId() + " <-- node" + anoNode.getId() + "\r\n");
                ++numEdges;
            }
        }
        sb.append("Total " + numEdges + " edges" + "\r\n");
        return sb.toString();
    }
    
    public int getId() {
       return this.id;
   }
   
   public void setId(final int index) {
       this.id = index;
   }
   
   public int getFileID() {
       return this.fileID;
   }
   
   public void setFileID(final int fileID) {
       this.fileID = fileID;
   }
   
   public String getName() {
       return this.name;
   }
   
   public void setName(final String name) {
       this.name = name;
   }
   
   public String getComments() {
	return comments;
}

public String getRawcode() {
	return rawcode;
}

public void setRawcode(String rawcode) {
	this.rawcode = rawcode;
}

public void setComments(String comments) {
	this.comments = comments;
}

public HashSet<GROUMNode> getNodes() {
       return this.nodes;
   }
   
   public HashSet<GROUMEdge> getEdges()
   {
   	HashSet<GROUMEdge> edges=new HashSet<GROUMEdge>();
   	for(GROUMNode node:nodes)edges.addAll(node.getInEdges());
   	return edges;
   }
   
   public HashSet<GROUMNode> getOuts() {
       return this.outs;
   }
   
   public HashSet<GROUMNode> getIns() {
       return this.ins;
   }
}
