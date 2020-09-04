package change;

import java.util.Iterator;
import java.util.Collection;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import dependency.DependencyVisitor;
import utils.FileIO;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import dependency.DataGraph;
import dependency.CallGraph;
import java.util.HashSet;
import org.eclipse.jdt.core.dom.Initializer;

public class CInitializer extends ChangeEntity
{
    private CClass cClass;
    private String name;
    private Initializer initializer;
    private CInitializer mappedInitializer;
    private HashSet<String> types;
    private HashSet<String> fields;
    
    public CInitializer(final CClass cClass, final int staticId, final Initializer initializer) {
        super();
        this.mappedInitializer = null;
        this.cClass = cClass;
        this.name = "init#" + staticId;
        this.initializer = initializer;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public CFile getCFile() {
        return this.cClass.getCFile();
    }
    
    public CClass getcClass() {
        return this.cClass;
    }
    
    public Initializer getInitializer() {
        return this.initializer;
    }
    
    public CInitializer getMappedInitializer() {
        return this.mappedInitializer;
    }
    
    public void setMappedInitializer(final CInitializer ci) {
        this.mappedInitializer = ci;
    }
    
    public HashSet<String> getTypes() {
        return this.types;
    }
    
    public HashSet<String> getFields() {
        return this.fields;
    }
    
    private CallGraph getCallGraph() {
        return this.cClass.getCFile().getcSystem().getCallGraph();
    }
    
    private DataGraph getDataGraph() {
        return this.getCFile().getcSystem().getDataGraph();
    }
    
    @Override
    public String getQualName() {
        return String.valueOf(this.cClass.getSimpleName()) + "." + this.name;
    }
    
    @Override
    public CClass getCClass() {
        return this.cClass;
    }
    
    public String getFullQualName() {
        return this.getQualName();
    }
    
    public void buildNames() {
    }
    
    public void buildDependencies() {
        String superClassName = null;
        if (this.cClass.getDeclaration() instanceof TypeDeclaration) {
            final TypeDeclaration superType = (TypeDeclaration)this.cClass.getDeclaration();
            if (superType.getSuperclassType() != null) {
                superClassName = FileIO.getSimpleClassName(superType.getSuperclassType().toString());
            }
        }
        final DependencyVisitor dv = new DependencyVisitor(this, this.initializer, this.cClass.getSimpleName(), superClassName, this.cClass.getFieldTypes());
        if (this.initializer != null) {
            this.initializer.accept(dv);
            for (final String callee : dv.getCalleeNames()) {
                this.getCallGraph().addCall(this, callee);
            }
            this.getCallGraph().addCall(this, dv.getCallees());
            (this.types = new HashSet<String>(dv.getTypes())).retainAll(this.getDataGraph().getTypeNames());
            this.fields = new HashSet<String>(dv.getFields());
        }
        else {
            this.types = new HashSet<String>();
            this.fields = new HashSet<String>();
        }
    }
    
    @Override
    public String toString() {
        return this.getQualName();
    }
}
