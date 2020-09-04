package groum;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import utils.FileIO;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ASTFieldVisitor extends ASTVisitor
{
    private HashMap<String, String> fieldType;
    private ArrayList<String> fields;
    String className;
    private HashMap<ASTNode, ArrayList<ASTNode>> mapTree;
    private HashMap<String, String> imports;
    
    public ASTFieldVisitor() {
        super();
        this.fieldType = new HashMap<String, String>();
        this.fields = new ArrayList<String>();
        this.className = "";
        this.mapTree = new HashMap<ASTNode, ArrayList<ASTNode>>();
        this.imports = new HashMap<String, String>();
    }
    
    public HashMap<String, String> getFieldType() {
        return this.fieldType;
    }
    
    public ArrayList<String> getFields() {
        return this.fields;
    }
    
    public String getClassName() {
        return this.className;
    }
    
    public HashMap<ASTNode, ArrayList<ASTNode>> getMapTree() {
        return this.mapTree;
    }
    
    public HashMap<String, String> getImports() {
        return this.imports;
    }
    
    /**
     * recode parent-children relations of AST nodes
     */
    @Override
    public void preVisit(final ASTNode node) {
        this.mapTree.put(node, new ArrayList<ASTNode>());
        if (node.getParent() != null) {
            final ArrayList<ASTNode> children = this.mapTree.get(node.getParent());
            children.add(node);
            this.mapTree.put(node.getParent(), children);
        }
    }
    
    /**
     * record import info
     */
    @Override
    public boolean visit(final ImportDeclaration node) {
        if (!node.toString().contains("*")) {
            this.imports.put(FileIO.getSimpleClassName(node.getName().getFullyQualifiedName()), node.getName().getFullyQualifiedName());
        }
        return true;
    }
    
    /**
     * record variables and their types
     */
    @Override
    public boolean visit(final TypeDeclaration node) {
        if (node.getParent().getNodeType() == ASTNode.COMPILATION_UNIT) {
            this.className = node.getName().toString();
            this.fieldType.put("this", this.className);
            this.fields.add("this");
            if (node.getSuperclassType() != null) {
                final String superClass = FileIO.getSimpleClassName(node.getSuperclassType().toString());
                this.fieldType.put("super", superClass);
                this.fields.add("super");
            }
        }
        return true;
    }
    
    /**
     * record field variables and their types
     */
    @Override
    public boolean visit(final FieldDeclaration fieldDec) {
        final List<VariableDeclarationFragment> varDecFrag = (List<VariableDeclarationFragment>)fieldDec.fragments();
        for (final VariableDeclarationFragment fragment : varDecFrag) {
            final String variable = fragment.getName().toString();
            final String type = FileIO.getSimpleClassName(fieldDec.getType().toString());
            this.fieldType.put(variable, type);
            this.fields.add(variable);
        }
        return true;
    }
}
