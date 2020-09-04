package change;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import utils.FileIO;

import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

public class CClass extends ChangeEntity
{
    public static final double thresholdSimilarity = 0.75;
    private CFile cFile;
    private int modifiers;
    private String annotation;
    private String simpleName;
    private AbstractTypeDeclaration declaration;
    private String extendedClassName;
    private HashSet<String> superClassNames;
    private CClass outterClass;
    private ArrayList<CClass> innerClasses;
    private ArrayList<CField> fields;
    private ArrayList<CMethod> methods;
    private ArrayList<CInitializer> initializers;
    
    public CClass(final CFile cFile, final TypeDeclaration type, final CClass outterClass) {
        super();
        this.annotation = "";
        this.extendedClassName = null;
        this.superClassNames = new HashSet<String>();
        this.outterClass = null;
        this.innerClasses = new ArrayList<CClass>();
        this.fields = new ArrayList<CField>();
        this.methods = new ArrayList<CMethod>();
        this.initializers = new ArrayList<CInitializer>();
        this.cFile = cFile;
        this.outterClass = outterClass;
        this.modifiers = type.getModifiers();
        this.simpleName = type.getName().getIdentifier();
        this.declaration = type;
        if (type.getSuperclassType() != null) {
            final String name = FileIO.getSimpleClassName(type.getSuperclassType().toString());
            this.extendedClassName = name;
            this.superClassNames.add(name);
        }
        if (type.superInterfaceTypes() != null) {
            for (int i = 0; i < type.superInterfaceTypes().size(); ++i) {
                final String name2 = FileIO.getSimpleClassName(type.superInterfaceTypes().get(i).toString());
                this.superClassNames.add(name2);
            }
        }
        if (type.getFields() != null) {
            FieldDeclaration[] fields;
            for (int length = (fields = type.getFields()).length, l = 0; l < length; ++l) {
                final FieldDeclaration field = fields[l];
                for (int j = 0; j < field.fragments().size(); ++j) {
                    final VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(j);
                    final String fieldType = FileIO.getSimpleClassName(field.getType().toString());
                    this.fields.add(new CField(this, field, fieldType, fragment));
                }
            }
        }
        if (type.getMethods() != null) {
            MethodDeclaration[] methods;
            for (int length2 = (methods = type.getMethods()).length, n = 0; n < length2; ++n) {
                final MethodDeclaration method = methods[n];
                this.methods.add(new CMethod(this, method));
            }
        }
        if (type.getTypes() != null && type.getTypes().length > 0) {
            this.innerClasses = new ArrayList<CClass>();
            TypeDeclaration[] types;
            for (int length3 = (types = type.getTypes()).length, n2 = 0; n2 < length3; ++n2) {
                final TypeDeclaration innerType = types[n2];
                this.innerClasses.add(new CClass(cFile, innerType, this));
            }
        }
        if (type.bodyDeclarations() != null) {
            int staticId = 0;
            for (int k = 0; k < type.bodyDeclarations().size(); ++k) {
                if (type.bodyDeclarations().get(k) instanceof Initializer) {
                    this.initializers.add(new CInitializer(this, staticId, (Initializer) type.bodyDeclarations().get(k)));
                    ++staticId;
                }
            }
        }
    }
    
    public CClass(final CFile cFile, final EnumDeclaration type, final CClass outterClass) {
        super();
        this.annotation = "";
        this.extendedClassName = null;
        this.superClassNames = new HashSet<String>();
        this.outterClass = null;
        this.innerClasses = new ArrayList<CClass>();
        this.fields = new ArrayList<CField>();
        this.methods = new ArrayList<CMethod>();
        this.initializers = new ArrayList<CInitializer>();
        this.cFile = cFile;
        this.outterClass = outterClass;
        this.modifiers = type.getModifiers();
        this.simpleName = type.getName().getIdentifier();
        this.declaration = type;
        if (type.superInterfaceTypes() != null) {
            for (int i = 0; i < type.superInterfaceTypes().size(); ++i) {
                final String name = type.superInterfaceTypes().get(i).toString();
                this.superClassNames.add(FileIO.getSimpleClassName(name));
            }
        }
        for (int j = 0; j < type.bodyDeclarations().size(); ++j) {
            final ASTNode dec = (ASTNode) type.bodyDeclarations().get(j);
            final int nodeType = dec.getNodeType();
            if (nodeType == ASTNode.FIELD_DECLARATION) {
                final FieldDeclaration field = (FieldDeclaration)dec;
                for (int k = 0; k < field.fragments().size(); ++k) {
                    final VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(k);
                    final String fieldType = FileIO.getSimpleClassName(field.getType().toString());
                    this.fields.add(new CField(this, field, fieldType, fragment));
                }
            }
            else if (nodeType == ASTNode.METHOD_DECLARATION) {
                this.methods.add(new CMethod(this, (MethodDeclaration)dec));
            }
            else {
                System.out.println("Info: not supported " + dec.getClass().getSimpleName() + " in enum declaration!!!");
            }
        }
    }
    
    public CClass(final CFile cFile, final AnnotationTypeDeclaration type, final CClass outterClass) {
        super();
        this.annotation = "";
        this.extendedClassName = null;
        this.superClassNames = new HashSet<String>();
        this.outterClass = null;
        this.innerClasses = new ArrayList<CClass>();
        this.fields = new ArrayList<CField>();
        this.methods = new ArrayList<CMethod>();
        this.initializers = new ArrayList<CInitializer>();
        this.cFile = cFile;
        this.outterClass = outterClass;
        this.modifiers = type.getModifiers();
        this.simpleName = type.getName().getIdentifier();
        this.declaration = type;
        for (int j = 0; j < type.bodyDeclarations().size(); ++j) {
            final ASTNode dec = (ASTNode) type.bodyDeclarations().get(j);
            final int nodeType = dec.getNodeType();
            if (nodeType == ASTNode.FIELD_DECLARATION) {
                final FieldDeclaration field = (FieldDeclaration)dec;
                for (int i = 0; i < field.fragments().size(); ++i) {
                    final VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(i);
                    final String fieldType = FileIO.getSimpleClassName(field.getType().toString());
                    this.fields.add(new CField(this, field, fieldType, fragment));
                }
            }
            else if (nodeType == ASTNode.METHOD_DECLARATION) {
                this.methods.add(new CMethod(this, (MethodDeclaration)dec));
            }
            else {
                System.out.println("Info: not supported " + dec.getClass().getSimpleName() + " in enum declaration!!!  " +cFile.getQualName());
            }
        }
    }
    
    @Override
    public CFile getCFile() {
        return this.cFile;
    }
    
    @Override
    public CClass getCClass() {
        return this;
    }
    
    public int getModifiers() {
        return this.modifiers;
    }
    
    public String getAnnotation() {
        return this.annotation;
    }
    
    public String getSimpleName() {
        return this.simpleName;
    }
    
    @Override
    public String getName() {
        return this.simpleName;
    }
    
    @Override
    public String getQualName() {
        return String.valueOf(this.cFile.getQualName()) + "." + this.simpleName;
    }
    
    public String getFullQualName() {
        return  this.simpleName;
    }
    
    public AbstractTypeDeclaration getDeclaration() {
        return this.declaration;
    }
    
    public HashSet<String> getSuperClassNames() {
        return this.superClassNames;
    }
    
    public CClass getOutterClass() {
        return this.outterClass;
    }
    
    public ArrayList<CClass> getInnerClasses(final boolean recursive) {
        final ArrayList<CClass> classes = new ArrayList<CClass>(this.innerClasses);
        if (recursive) {
            for (final CClass icc : this.innerClasses) {
                classes.addAll(icc.getInnerClasses(true));
            }
        }
        return classes;
    }
    
    public ArrayList<CField> getFields() {
        return this.fields;
    }
    
    public ArrayList<CMethod> getMethods() {
        return this.methods;
    }
    
    public ArrayList<CInitializer> getInitializers() {
        return this.initializers;
    }
    
    public HashMap<String, String> getFieldTypes() {
        final HashMap<String, String> fieldTypes = this.getFieldTypes(this.fields);
        if (this.outterClass != null) {
            fieldTypes.putAll(this.outterClass.getFieldTypes());
        }
        return fieldTypes;
    }
    
    private HashMap<String, String> getFieldTypes(final ArrayList<CField> fields) {
        final HashMap<String, String> fieldTypes = new HashMap<String, String>();
        for (final CField cf : fields) {
            fieldTypes.put(cf.getName(), cf.getType());
        }
        return fieldTypes;
    }
    
    public void buildNames() {
        this.cFile.getcSystem().getDataGraph().addClass(this);
        for (final CField cf : this.fields) {
            cf.buildNames();
        }
        for (final CMethod cm : this.methods) {
            cm.buildNames();
        }
        for (final CInitializer ci : this.initializers) {
            ci.buildNames();
        }
        for (final CClass cc : this.innerClasses) {
            cc.buildNames();
        }
    }
    
    public void buildDependencies() {
        for (final CField cf : this.fields) {
        	//System.out.println("Field: "+cf.toString());
            cf.buildDependencies();
        }
        for (final CMethod cm : this.methods) {
        	//System.out.println("Method: "+cm.toString());
            cm.buildDependencies();
        }
        for (final CInitializer ci : this.initializers) {
        	//System.out.println("Initializer: "+ci.toString());
            ci.buildDependencies();
        }
        for (final CClass cc : this.innerClasses) {
        	//System.out.println("Class: "+cc.toString());
            cc.buildDependencies();
        }
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
    

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((simpleName == null) ? 0 : simpleName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CClass other = (CClass) obj;
		if (simpleName == null) {
			if (other.simpleName != null)
				return false;
		} else if (!simpleName.equals(other.simpleName))
			return false;
		return true;
	}

	private HashSet<CMethod> getMethodByName(final String methodName) {
        final HashSet<CMethod> methods = new HashSet<CMethod>();
        for (final CMethod cm : this.methods) {
            if (cm.getName().equals(methodName)) {
                methods.add(cm);
            }
        }
        return methods;
    }
    
    public HashSet<CMethod> getInheritedMethods(final String methodName, final HashSet<CClass> visitedClasses) {
        final HashSet<CMethod> methods = this.getMethodByName(methodName);
        visitedClasses.add(this);
        final HashSet<CMethod> pMethods = new HashSet<CMethod>();
        if (this.extendedClassName != null && this.cFile.getcSystem().getDataGraph().getTypeNames().contains(this.extendedClassName)) {
            final String simpleMethodName = methodName.substring(0, methodName.indexOf(40));
            if (simpleMethodName.equals(this.simpleName)) {
                pMethods.addAll(this.cFile.getcSystem().getDataGraph().getInheritedMethods(this.extendedClassName, String.valueOf(this.extendedClassName) + methodName.substring(methodName.indexOf(40)), visitedClasses));
            }
            pMethods.addAll(this.cFile.getcSystem().getDataGraph().getInheritedMethods(this.extendedClassName, methodName, visitedClasses));
        }
        if (!pMethods.isEmpty()) {
            return pMethods;
        }
        if (!methods.isEmpty()) {
            return methods;
        }
        if (this.outterClass != null) {
            return this.outterClass.getInheritedMethods(methodName, visitedClasses);
        }
        return new HashSet<CMethod>();
    }
    
    public HashSet<CField> getFields(final String name, final HashSet<CClass> visitedClasses) {
        visitedClasses.add(this);
        final HashSet<CField> fs = new HashSet<CField>();
        for (final CField cf : this.fields) {
            if (cf.getName().equals(name)) {
                fs.add(cf);
                return fs;
            }
        }
        if (!this.superClassNames.isEmpty()) {
            for (final String scn : this.superClassNames) {
                if (this.cFile.getcSystem().getDataGraph().getTypeNames().contains(scn)) {
                    fs.addAll(this.cFile.getcSystem().getDataGraph().getFields(scn, name, visitedClasses));
                }
            }
        }
        return fs;
    }
    
    public String getExtendedClassName() {
        if (this.extendedClassName != null) {
            return this.extendedClassName;
        }
        if (this.outterClass != null) {
            return this.outterClass.getExtendedClassName();
        }
        return null;
    }
}
