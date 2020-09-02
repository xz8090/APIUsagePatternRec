package groum;

public class GROUMEdge
{
	/**# of all edges during one building of all groums*/
    public static int numOfEdges=0;
    
    /**a unique identifier number of the edge during one building of groums, as the order that it is created */
    private int id;
    
    private GROUMNode src;
    private GROUMNode dest;
    
    /**label of the edge, can be seq (sequential edge) or data (data dependence edge)*/
    private String label;
   
    
    public GROUMEdge(final GROUMNode src, final GROUMNode dest) {
        super();
        this.label = "seq";
        this.id = ++GROUMEdge.numOfEdges;
        this.src = src;
        this.dest = dest;
        src.addOutEdge(this);
        dest.addInEdge(this);
    }
    
    public GROUMEdge(final GROUMNode src, final GROUMNode dest, final String label) {
        this(src, dest);
        this.label = label;
    }
    
    public GROUMNode getSrc() {
        return this.src;
    }
    
    public void setSrc(final GROUMNode node) {
        if (this.dest.getInNodes().contains(node)) {
            this.delete();
        }
        else {
            (this.src = node).addOutEdge(this);
        }
    }
    
    public GROUMNode getDest() {
        return this.dest;
    }
    
    public void setDest(final GROUMNode node) {
        if (this.src.getOutNodes().contains(node)) {
            this.delete();
        }
        else {
            (this.dest = node).addInEdge(this);
        }
    }
    
    public int getId() {
        return this.id;
    }
    
    public void setId(final int id) {
        this.id = id;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public void setLabel(final String label) {
        this.label = label;
    }
    
    public void delete() {
        this.src.getOutEdges().remove(this);
        this.dest.getInEdges().remove(this);
    }
    
    public String toString()
    {
   	 return src.getLabel()+"#"+src.getId()+"-"+dest.getLabel()+"#"+dest.getId();
    }
}
