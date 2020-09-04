package groum;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import java.util.List;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import utils.FileIO;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import java.util.HashSet;
import java.util.Stack;
import java.util.HashMap;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ASTTypeBindingVisitor extends ASTVisitor
{
    private HashMap<String, String> localType;
    private HashMap<String, String> fieldType;
    private Stack<HashSet<String>> scopedTypes;
    
    public ASTTypeBindingVisitor(final HashMap<String, String> fieldType) {
        super();
        this.localType = new HashMap<String, String>();
        this.fieldType = new HashMap<String, String>();
        this.scopedTypes = new Stack<HashSet<String>>();
        this.fieldType = fieldType;
    }
    
    @Override
    public boolean visit(final SingleVariableDeclaration singVarDec) {
        final String variable = singVarDec.getName().toString();
        final String type = FileIO.getSimpleClassName(singVarDec.getType().toString());
        this.localType.put(variable, type);
        final HashSet<String> mine = this.scopedTypes.pop();
        final HashSet<String> parents = this.scopedTypes.pop();
        parents.add(variable);
        this.scopedTypes.push(parents);
        this.scopedTypes.push(mine);
        return true;
    }
    
    @Override
    public boolean visit(final VariableDeclarationExpression varDecExp) {
        final List<VariableDeclarationFragment> varDecFrag = (List<VariableDeclarationFragment>)varDecExp.fragments();
        final HashSet<String> mine = this.scopedTypes.pop();
        final HashSet<String> parents = this.scopedTypes.pop();
        for (final VariableDeclarationFragment fragment : varDecFrag) {
            final String variable = fragment.getName().toString();
            final String type = FileIO.getSimpleClassName(varDecExp.getType().toString());
            this.localType.put(variable, type);
            parents.add(variable);
        }
        this.scopedTypes.push(parents);
        this.scopedTypes.push(mine);
        return true;
    }
    
    @Override
    public boolean visit(final VariableDeclarationStatement varDecStat) {
        final List<VariableDeclarationFragment> varDecFrag = (List<VariableDeclarationFragment>)varDecStat.fragments();
        final HashSet<String> mine = this.scopedTypes.pop();
        final HashSet<String> parents = this.scopedTypes.pop();
        for (final VariableDeclarationFragment fragment : varDecFrag) {
            final String variable = fragment.getName().toString();
            final String type = FileIO.getSimpleClassName(varDecStat.getType().toString());
            this.localType.put(variable, type);
            parents.add(variable);
        }
        this.scopedTypes.push(parents);
        this.scopedTypes.push(mine);
        return true;
    }
    
    @Override
    public void preVisit(final ASTNode node) {
        this.scopedTypes.push(new HashSet<String>());
    }
    
    @Override
    public void postVisit(final ASTNode node) {
        final HashSet<String> types = this.scopedTypes.pop();
        if (types != null && !types.isEmpty()) {
            for (final String type : types) {
                this.localType.remove(type);
            }
        }
    }
    
    @Override
    public boolean visit(final MethodInvocation node) {
        String objectName = (node.getExpression() != null) ? node.getExpression().toString() : "this";
        if (objectName.equals("this")) {
            node.setProperty("TypeBinding", this.fieldType.get(objectName));
        }
        else if (this.localType.containsKey(objectName)) {
            node.setProperty("TypeBinding", this.localType.get(objectName));
        }
        else if (this.fieldType.containsKey(objectName)) {
            node.setProperty("TypeBinding", this.fieldType.get(objectName));
        }
        else if (node.getExpression().getNodeType() == 42 && Character.isUpperCase(objectName.charAt(0))) {
            node.setProperty("TypeBinding", objectName);
        }
        else {
            final String name = node.getName().toString();
            if (node.getExpression().getNodeType() == 11) {
                final CastExpression expr = (CastExpression)node.getExpression();
                objectName = FileIO.getSimpleClassName(expr.getType().toString());
                node.setProperty("TypeBinding", objectName);
            }
            else if (node.getExpression().getNodeType() == 32) {
                final MethodInvocation expr2 = (MethodInvocation)node.getExpression();
                objectName = expr2.getName().toString();
                node.setProperty("TypeBinding", objectName);
            }
            else if (node.getExpression().getNodeType() == 45) {
                node.setProperty("TypeBinding", "String");
            }
            else if (node.getExpression().getNodeType() == 36) {
                final ParenthesizedExpression parenthsis = (ParenthesizedExpression)node.getExpression();
                objectName = parenthsis.getExpression().toString();
                if (this.localType.containsKey(objectName)) {
                    node.setProperty("TypeBinding", this.localType.get(objectName));
                }
                else if (this.fieldType.containsKey(objectName)) {
                    node.setProperty("TypeBinding", this.fieldType.get(objectName));
                }
                else if (parenthsis.getExpression().getNodeType() == 11) {
                    final CastExpression expr3 = (CastExpression)parenthsis.getExpression();
                    objectName = FileIO.getSimpleClassName(expr3.getType().toString());
                    node.setProperty("TypeBinding", objectName);
                }
                else if (parenthsis.getExpression().getNodeType() == 14) {
                    final ClassInstanceCreation expr4 = (ClassInstanceCreation)parenthsis.getExpression();
                    objectName = FileIO.getSimpleClassName(expr4.getType().toString());
                    node.setProperty("TypeBinding", objectName);
                }
                else if (parenthsis.getExpression().getNodeType() == 32) {
                    final MethodInvocation expr5 = (MethodInvocation)parenthsis.getExpression();
                    objectName = expr5.getName().toString();
                    node.setProperty("TypeBinding", objectName);
                }
                else {
                    objectName = parenthsis.getExpression().toString();
                    if (name.equals("equals") || name.equals("toString") || name.equals("getName") || name.equals("write") || name.equals("writeln")) {
                        objectName = objectName.substring(objectName.lastIndexOf(46) + 1);
                        node.setProperty("TypeBinding", objectName);
                    }
                    else {
                        node.setProperty("TypeBinding", "#Uknown#");
                    }
                }
            }
            else if (node.getExpression().getNodeType() == 42 && Character.isUpperCase(objectName.charAt(0))) {
                node.setProperty("TypeBinding", objectName);
            }
            else if (name.equals("equals") || name.equals("toString") || name.equals("getName") || name.equals("write") || name.equals("writeln")) {
                objectName = objectName.substring(objectName.indexOf(46) + 1);
                node.setProperty("TypeBinding", objectName);
            }
            else {
                node.setProperty("TypeBinding", "#Uknown#");
            }
        }
        return true;
    }
    
    @Override
    public boolean visit(final SimpleName node) {
        final int pType = node.getParent().getNodeType();
        if (pType == ASTNode.QUALIFIED_NAME) {
            final QualifiedName parent = (QualifiedName)node.getParent();
            final String name = parent.getQualifier().toString();
            if (parent.getParent().getNodeType() != ASTNode.QUALIFIED_NAME && name.indexOf(46) == -1) {
                if (this.localType.containsKey(name)) {
                    node.setProperty("TypeBinding", String.valueOf(this.localType.get(name)) + "." + name);
                }
                else if (this.fieldType.containsKey(name)) {
                    node.setProperty("TypeBinding", String.valueOf(this.fieldType.get(name)) + "." + name);
                }
                else if (Character.isUpperCase(name.charAt(0))) {
                    node.setProperty("TypeBinding", String.valueOf(name) + "." + name);
                }
            }
        }
        else if (pType != 22 && pType != 7 && pType != 32) {
            final String name2 = node.toString();
            if (!this.localType.containsKey(name2) && this.fieldType.containsKey(name2)) {
                node.setProperty("TypeBinding", String.valueOf(this.fieldType.get("this")) + ".this");
            }
        }
        return true;
    }
    
    @Override
    public boolean visit(final FieldAccess node) {
        final String objectName = node.getExpression().toString();
        if (objectName.equals("this")) {
            node.setProperty("TypeBinding", this.fieldType.get("this"));
        }
        else if (this.localType.containsKey(objectName)) {
            node.setProperty("TypeBinding", this.localType.get(objectName));
        }
        else if (this.fieldType.containsKey(objectName)) {
            node.setProperty("TypeBinding", this.fieldType.get("this"));
        }
        else {
            String typeName = objectName;
            if (objectName.contains(".")) {
                final String rootName = objectName.substring(0, objectName.indexOf("."));
                final String remain = objectName.substring(objectName.indexOf("."));
                if (rootName.equals("this")) {
                    typeName = String.valueOf(this.fieldType.get("this")) + remain;
                }
                else if (this.localType.containsKey(rootName)) {
                    typeName = String.valueOf(this.localType.get(rootName)) + remain;
                }
                else if (this.fieldType.containsKey(rootName)) {
                    typeName = String.valueOf(this.fieldType.get("this")) + remain;
                }
            }
            node.setProperty("TypeBinding", typeName);
        }
        return true;
    }
    
    @Override
    public boolean visit(final ClassInstanceCreation node) {
        final String typeName = FileIO.getSimpleClassName(node.getType().toString());
        node.setProperty("TypeBinding", typeName);
        return true;
    }
    
    @Override
    public boolean visit(final CastExpression node) {
        final String type = FileIO.getSimpleClassName(node.getType().toString());
        node.setProperty("TypeBinding", type);
        return true;
    }
    
    @Override
    public boolean visit(final InstanceofExpression node) {
        final String type = FileIO.getSimpleClassName(node.getRightOperand().toString());
        node.setProperty("TypeBinding", type);
        return true;
    }
    
    @Override
    public boolean visit(final SuperMethodInvocation node) {
        node.setProperty("TypeBinding", this.fieldType.get("super"));
        return true;
    }
    
    @Override
    public boolean visit(final ConstructorInvocation node) {
        node.setProperty("TypeBinding", this.fieldType.get("this"));
        return true;
    }
    
    @Override
    public boolean visit(final SuperConstructorInvocation node) {
        node.setProperty("TypeBinding", this.fieldType.get("super"));
        return true;
    }
    
    @Override
    public boolean visit(final SuperFieldAccess node) {
        node.setProperty("TypeBinding", this.fieldType.get("super"));
        return true;
    }
    
    @Override
    public boolean visit(final Assignment node) {
        return true;
    }
}
