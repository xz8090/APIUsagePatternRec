package change;

import java.util.Iterator;
import java.io.File;
import java.util.Stack;
import dependency.CallGraph;
import dependency.DataGraph;
import java.util.HashMap;

public class CSystem
{
   // private String path;
    private CFile file;
    private DataGraph dataGraph;
    private CallGraph callGraph;
    
    public CSystem(final String code) {
        super();
        this.dataGraph = new DataGraph();
        this.callGraph = new CallGraph();
  
        this.buildStructure(code);
    }
    

    
    public CFile getFile() {
        return this.file;
    }
    
    public DataGraph getDataGraph() {
        return this.dataGraph;
    }
    
    public CallGraph getCallGraph() {
        return this.callGraph;
    }
    
    private void buildStructure(String code) {
       try{
             final CFile cf = new CFile(this, code);
             this.file= cf;
       }catch(IllegalArgumentException e)
       {
            System.err.println(e.getStackTrace());                	
       }            
        
    }
    
    public void buildDependencies() {
        this.buildNames();
        
        	//System.out.println("Building dependence for file: "+cf.getName());
         this.file.buildDependencies();
        
    }
    
    private void buildNames() {
        
           this.file.buildNames();
        
    }
}
