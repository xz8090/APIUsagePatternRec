package change;

import java.util.Iterator;
import java.util.Collection;
import org.eclipse.jdt.core.dom.ASTVisitor;
import dependency.DependencyVisitor;
import utils.FileIO;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import dependency.DataGraph;
import dependency.CallGraph;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import java.util.HashSet;

public class CField extends ChangeEntity
{
    private CClass cClass;
    private int modifiers;
    private HashSet<String> sModifiers;
    private String annotation;
    private String name;
    private String type;
    private Expression initializer;
    private HashSet<String> types;
    private HashSet<String> fields;
    
    public CField(final CClass cClass, final FieldDeclaration field, final String type, final VariableDeclarationFragment fragment) {
        super();
        this.sModifiers = new HashSet<String>();
        this.annotation = "";
        this.cClass = cClass;
        this.modifiers = field.getModifiers();
        for (int i = 0; i < field.modifiers().size(); ++i) {
            final ASTNode modifier =  (ASTNode) field.modifiers().get(i);
            if (modifier.getNodeType() == 78) {
                this.annotation = ((MarkerAnnotation)modifier).getTypeName().toString();
                if (!this.annotation.toLowerCase().contains("null")) {
                    this.annotation = "";
                }
            }
            else {
                this.sModifiers.add(modifier.toString());
            }
        }
        this.name = fragment.getName().getIdentifier();
        this.type = type;
        this.initializer = fragment.getInitializer();
    }
    
    public CClass getcClass() {
        return this.cClass;
    }
    
    public int getModifiers() {
        return this.modifiers;
    }
    
    public HashSet<String> getSModifiers() {
        return this.sModifiers;
    }
    
    public String getAnnotation() {
        return this.annotation;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getQualName() {
        return String.valueOf(this.cClass.getSimpleName()) + "." + this.name;
    }
    
    public String getFullQualName() {
        return String.valueOf(this.getcClass().getFullQualName()) + "." + this.name;
    }
    
    public String getType() {
        return this.type;
    }
    
    public Expression getInitializer() {
        return this.initializer;
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
        return this.cClass.getCFile().getcSystem().getDataGraph();
    }
    
    @Override
    public CFile getCFile() {
        return this.cClass.getCFile();
    }
    
    @Override
    public CClass getCClass() {
        return this.cClass;
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
        this.fields.add(this.name);
    }
    
    @Override
    public String toString() {
        return this.getQualName();
    }
}
