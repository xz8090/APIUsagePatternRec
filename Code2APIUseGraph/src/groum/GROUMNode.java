package groum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.Serializable;

public class GROUMNode implements Serializable
{
    private static final long serialVersionUID = 4L;
    public static final int TYPE_FIELD = 0;
    public static final int TYPE_METHOD = 1;
    public static final int TYPE_CONTROL = 2;
    public static final int TYPE_DATA = 3;
    public static final int TYPE_OTHER = 11111;
    public static final int TYPE_SINGLE = 1;// a node is single
    public static final int TYPE_MULTIPLE = 2;// a node is repeated
    public static final int TYPE_LOOP = 3;//
    public static int numOfNodes=0;
    
    /**A unique identifier number of a node during one building of groums*/
    private int id;
    
    /**Description of the node, with a "Class.method()" or "IF/WHILE" format*/
    private String label;
    
    /**order of method name in the global label repo*/
    private int methodId;
    /**order of object name in the global label repo*/
    private int objectNameId;
    /**order of class name in the global label repo*/
    private int classNameId;

    private int fileID;
    
    /**IDs of all parameter identifiers (e.g., object names)*/
    private HashSet<Integer> parameters;
    /**Node type : control node (loop,control) or action node(field? method?)*/
    private int type;
    /**How many times the node represent? a single one or multiple nodes?*/
    private int singletonType;
    private int startLine;
    private int endLine;
    private String pid;
    private GROUMGraph graph;
    /**edges link in the node*/
    private HashSet<GROUMEdge> inEdges;
    /**edges out from the node*/
    private HashSet<GROUMEdge> outEdges;
    /**<label,ID> map, given a label, find its id in the whole label repo*/
    public static HashMap<String, Integer> idOfLabel = new HashMap<String, Integer>();
    public static HashMap<Integer, String> labelOfID= new HashMap<Integer, String>();
    /**list of all file names that build the groums*/
    public static ArrayList<String> fileNames= new ArrayList<String>();
    
    public GROUMNode(final String label) {
        super();
        this.type = TYPE_OTHER;
        this.singletonType = TYPE_SINGLE;
        this.inEdges = new HashSet<GROUMEdge>();
        this.outEdges = new HashSet<GROUMEdge>();
        this.id = ++GROUMNode.numOfNodes;
        this.label = label;
    }
    
    public GROUMNode(final String methodName, final int type, final String className, final String objectName) {
        super();
        this.type = TYPE_OTHER;
        this.singletonType = TYPE_SINGLE;
        this.inEdges = new HashSet<GROUMEdge>();
        this.outEdges = new HashSet<GROUMEdge>();
        this.id = ++GROUMNode.numOfNodes;
        this.methodId = convertLabel(methodName);
        this.type = type;
        this.classNameId = convertLabel(className);
        this.objectNameId = convertLabel(objectName);
        this.setLabel();
    }
    
    public GROUMNode(final String methodName, final int type, final String className, final String objectName, final HashSet<Integer> parameters) {
        super();
        this.type = TYPE_OTHER;
        this.singletonType = TYPE_SINGLE;
        this.inEdges = new HashSet<GROUMEdge>();
        this.outEdges = new HashSet<GROUMEdge>();
        this.id = ++GROUMNode.numOfNodes;
        this.methodId = convertLabel(methodName);
        this.type = type;
        this.classNameId = convertLabel(className);
        this.objectNameId = convertLabel(objectName);
        this.parameters = new HashSet<Integer>(parameters);
        this.setLabel();
    }
    
    /***
     * Get the index of the label in the global label repo.
     * @param label
     * @return
     */
    public static int convertLabel(final String label) {
        if (GROUMNode.idOfLabel.get(label) == null) {
            final int index = GROUMNode.idOfLabel.size() + 1;
            GROUMNode.idOfLabel.put(label, index);
            GROUMNode.labelOfID.put(index, label);
            return index;
        }
        return GROUMNode.idOfLabel.get(label);
    }
    
    public int getId() {
        return this.id;
    }
    
    /**get method label(name)*/
    public String getMethod() {
        return GROUMNode.labelOfID.get(this.methodId);
    }
    
    /**get the order of the method name in the global label repo*/
    public int getMethodID() {
        return this.methodId;
    }
    
    /**get node label*/
    public String getLabel() {
        return this.label;
    }
    
    /**set node label as class name. method name, note that the node label will not be indexed in the global label repo.*/
    public void setLabel() {
        this.label = String.valueOf(this.getClassName()) + "." + this.getMethod();
    }
    
    /***
     * chech whether this node has data dependency with another node (i.e., another node's object name is one of this node's parameters
     * or this node's object name is one of another node's parameters)
     * @param node: another node
     * @return
     */
    public boolean hasDataDependency(final GROUMNode node) {
        if ((this.parameters != null && !this.parameters.isEmpty() && this.parameters.contains(node.getObjectNameId())) 
      		  || (node.getParameters() != null && !node.getParameters().isEmpty() && node.getParameters().contains(this.objectNameId))) 
        {
            return true;
        }
        final HashSet<Integer> pars = new HashSet<Integer>();
        pars.addAll(parameters);
        pars.retainAll(node.getParameters());
        return !pars.isEmpty();
    }
    
    public int getType() {
        return this.type;
    }
    
    public void setParameters(final HashSet<Integer> parameters) {
        this.parameters = parameters;
    }
    
    public HashSet<Integer> getParameters() {
        return this.parameters;
    }
    
    public int getClassNameId() {
        return this.classNameId;
    }
    
    public String getClassName() {
        return GROUMNode.labelOfID.get(this.classNameId);
    }
    
    public int getObjectNameId() {
        return this.objectNameId;
    }
    
    public String getObjectName() {
        return GROUMNode.labelOfID.get(this.objectNameId);
    }
    
    public int getSingletonType() {
        return this.singletonType;
    }
    
    public void setSingletonType(final int singletonType) {
        this.singletonType = singletonType;
    }
    
    public int getFileID() {
        return this.fileID;
    }
    
    public void setFileID(final int fileID) {
        this.fileID = fileID;
    }
    
    public int getStartLine() {
        return this.startLine;
    }
    
    public void setStartLine(final int startLine) {
        this.startLine = startLine;
    }
    
    public int getEndLine() {
        return this.endLine;
    }
    
    public void setEndLine(final int endLine) {
        this.endLine = endLine;
    }
    
    public GROUMGraph getGraph() {
        return this.graph;
    }
    
    public void setGraph(final GROUMGraph graph) {
        this.graph = graph;
    }
    
    public String getPid() {
        return this.pid;
    }
    
    public void setPid(final String pid) {
        this.pid = pid;
    }
    
    public HashSet<GROUMEdge> getInEdges() {
        return this.inEdges;
    }
    
    public HashSet<GROUMEdge> getOutEdges() {
        return this.outEdges;
    }
    
    public HashSet<GROUMNode> getInNodes() {
        final HashSet<GROUMNode> nodes = new HashSet<GROUMNode>();
        for (final GROUMEdge e : this.inEdges) {
            nodes.add(e.getSrc());
        }
        return nodes;
    }
    
    public HashSet<GROUMNode> getOutNodes() {
        final HashSet<GROUMNode> nodes = new HashSet<GROUMNode>();
        for (final GROUMEdge e : this.outEdges) {
            nodes.add(e.getDest());
        }
        return nodes;
    }
    
    public void addInEdge(final GROUMEdge edge) {
        this.inEdges.add(edge);
    }
    
    public void addOutEdge(final GROUMEdge edge) {
        this.outEdges.add(edge);
    }
    
    public void getNeighbors(final HashMap<String, HashSet<GROUMNode>> neighbors, final HashMap<String, HashSet<GROUMNode>> nextNodes) {
        for (final GROUMEdge e : this.inEdges) {
            final GROUMNode n = e.getSrc();
            final String label = n.getLabel();
            HashSet<GROUMNode> ns = neighbors.get(label);
            if (ns == null) {
                ns = new HashSet<GROUMNode>();
            }
            ns.add(n);
            neighbors.put(label, ns);
            if ((this.type != 2 || n.getType() != 2) && !this.label.equals(n.getLabel())) {
                HashSet<GROUMNode> nexts = nextNodes.get(label);
                if (nexts == null) {
                    nexts = new HashSet<GROUMNode>();
                }
                nexts.add(n);
                nextNodes.put(label, nexts);
            }
        }
        for (final GROUMEdge e : this.outEdges) {
            final GROUMNode n = e.getDest();
            final String label = n.getLabel();
            HashSet<GROUMNode> ns = neighbors.get(label);
            if (ns == null) {
                ns = new HashSet<GROUMNode>();
            }
            ns.add(n);
            neighbors.put(label, ns);
            if ((this.type != 2 || n.getType() != 2) && !this.label.equals(n.getLabel())) {
                HashSet<GROUMNode> nexts = nextNodes.get(label);
                if (nexts == null) {
                    nexts = new HashSet<GROUMNode>();
                }
                nexts.add(n);
                nextNodes.put(label, nexts);
            }
        }
    }
    
    public void delete() {
        for (final GROUMEdge e : new HashSet<GROUMEdge>(this.inEdges)) {
            e.delete();
        }
        for (final GROUMEdge e : new HashSet<GROUMEdge>(this.outEdges)) {
            e.delete();
        }
        this.graph.removeNode(this);
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.label) + "#"+this.id+"@" + this.startLine;
    }
}
