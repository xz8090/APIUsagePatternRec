package change;

import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import org.eclipse.jdt.core.dom.ASTVisitor;
import dependency.DependencyVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import dependency.DataGraph;
import dependency.CallGraph;
import utils.FileIO;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class CMethod extends ChangeEntity
{
    private static final String separatorParameter = "#";
    private CClass cClass;
    private int modifiers;
    private String annotation;
    private String name;
    private String simpleName;
    private int numOfParameters;
    private String returnType;
    private MethodDeclaration declaration;
    private CMethod mappedMethod;
    private String parameterTypes;
    private HashSet<String> types;
    private HashSet<String> fields;
    private HashMap<SimpleName, HashSet<SimpleName>> localVarLocs;
    
    public CMethod(final CClass cClass, final MethodDeclaration method) {
        super();
        this.annotation = "";
        this.mappedMethod = null;
        this.cClass = cClass;
        this.modifiers = method.getModifiers();
        int i = 0;
        while (i < method.modifiers().size()) {
            final ASTNode modifier = (ASTNode) method.modifiers().get(i);
            if (modifier.getNodeType() == ASTNode.MARKER_ANNOTATION) {
                this.annotation = ((MarkerAnnotation)modifier).getTypeName().toString();
                if (!this.annotation.toLowerCase().contains("null")) {
                    this.annotation = "";
                    break;
                }
                break;
            }
            else {
                ++i;
            }
        }
        this.simpleName = method.getName().getIdentifier();
        this.numOfParameters = method.parameters().size();
        this.name = String.valueOf(this.simpleName) + "(" + this.numOfParameters + ")";
        this.declaration = method;
        this.parameterTypes = "#";
        for (i = 0; i < method.parameters().size(); ++i) {
            final SingleVariableDeclaration dec = (SingleVariableDeclaration) method.parameters().get(i);
            String paraType = FileIO.getSimpleClassName(dec.getType().toString());
            String temp = dec.toString();
            for (int l = temp.length(); temp.endsWith("[]"); temp = temp.substring(0, l - 2), l -= 2) {
                paraType = String.valueOf(paraType) + "[]";
            }
            this.parameterTypes = String.valueOf(this.parameterTypes) + paraType + "#";
        }
        String returnType;
        if (method.getReturnType2() != null) {
            returnType = method.getReturnType2().toString();
            returnType = FileIO.getSimpleClassName(returnType);
        }
        else {
            returnType = "void";
        }
        this.returnType = returnType;
    }
    
    public CClass getcClass() {
        return this.cClass;
    }
    
    public int getModifiers() {
        return this.modifiers;
    }
    
    public String getAnnotation() {
        return this.annotation;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    public String getSimpleName() {
        return this.simpleName;
    }
    
    @Override
    public String getQualName() {
        return String.valueOf(this.cClass.getSimpleName()) + "." + this.name;
    }
    
    public int getNumOfParameters() {
        return this.numOfParameters;
    }
    
    public String getParameterTypes() {
        return this.parameterTypes;
    }
    
    public String getReturnType() {
        return this.returnType;
    }
    
    private String getFullName() {
        return String.valueOf(this.simpleName) + this.parameterTypes;
    }
    
    public String getFullQualName() {
        return String.valueOf(this.cClass.getFullQualName()) + "." + this.getFullName();
    }
    
    public MethodDeclaration getDeclaration() {
        return this.declaration;
    }
    
    public CMethod getMappedMethod() {
        return this.mappedMethod;
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
        this.getCallGraph().addMethod(this);
    }
    
    public void buildDependencies() {
        String superClassName = null;
        if (this.cClass.getDeclaration() instanceof TypeDeclaration) {
            final TypeDeclaration superType = (TypeDeclaration)this.cClass.getDeclaration();
            if (superType.getSuperclassType() != null) {
                superClassName = FileIO.getSimpleClassName(superType.getSuperclassType().toString());
            }
        }

        if(this.name.startsWith("handleShow"))
        {
        	int a=0;
        }
        
        final DependencyVisitor dv = new DependencyVisitor(this, this.declaration, this.cClass.getSimpleName(), superClassName, this.cClass.getFieldTypes());
      
        this.declaration.accept(dv);
        
        dv.getCalleeNames().remove(this.name);
        for (final String callee : dv.getCalleeNames()) {
        	
            this.getCallGraph().addCall(this, callee);
            //System.out.println("adding callee: "+callee);
        }
        //System.out.println("call graph finished");
        dv.getCallees().removeAll(this.getInheritedMethods());
        this.getCallGraph().addCall(this, dv.getCallees());
        (this.types = new HashSet<String>(dv.getTypes())).retainAll(this.getDataGraph().getTypeNames());
        this.fields = new HashSet<String>(dv.getFields());
        this.localVarLocs = new HashMap<SimpleName, HashSet<SimpleName>>(dv.getLocalVarLocs());
    }
    
    public HashSet<CMethod> getInheritedMethods() {
        HashSet<CMethod> methods = new HashSet<CMethod>();
        if (!this.cClass.getSuperClassNames().isEmpty()) {
            methods = this.cClass.getInheritedMethods(this.name, new HashSet<CClass>());
        }
        if (methods.isEmpty()) {
            methods.add(this);
        }
        return methods;
    }
    
    @Override
    public String toString() {
        return this.getQualName();
    }
}
