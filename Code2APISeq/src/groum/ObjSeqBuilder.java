package groum;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.FileIO;

import java.util.Iterator;

import change.CFile;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.osgi.framework.InvalidSyntaxException;

import java.util.HashMap;

import change.CSystem;

import java.util.ArrayList;

public class ObjSeqBuilder
{
    private ArrayList<GROUMGraph> groums;
    private CSystem system;

    private int maxGroumSize;
    /**record parent-children relationships of ast nodes*/
    private HashMap<ASTNode, ArrayList<ASTNode>> mapTree;
    private String className;
    //private int fileID;
    private ArrayList<Integer> lines;
    /*** record data dependences*/
    private HashMap<Integer, HashSet<Integer>> dataDependencies;
    
    public ObjSeqBuilder() {
        super();
        this.groums = new ArrayList<GROUMGraph>();
        this.maxGroumSize = 0;
        this.dataDependencies = new HashMap<Integer, HashSet<Integer>>();
    }
    
    public void build(String code) throws InvalidSyntaxException {
    	//System.out.print("\tBuilding system dependences..."); 
    	this.system = new CSystem(code);
        this.system.buildDependencies();
        //System.out.print("\tBuilding Groums...");
        
        this.build(this.system.getFile());            
 
    }
    
    private void build(final CFile file) {
    	
        final String content = file.getContent();
       // this.fileID = GROUMNode.fileNames.size();
        //GROUMNode.fileNames.add(file.getPath());
        if (file.getCompileUnit() == null) {
            System.err.println("Error parsing " + file);
            return;
        }
        this.lines = new ArrayList<Integer>();
        int charCount = -1;
        do {
            ++charCount;
            this.lines.add(charCount);
            charCount = content.indexOf("\n", charCount);
        } while (charCount > -1);
        this.build(file.getCompileUnit());
    }
    
    public void build(final ASTNode ast) {
        final ASTNode root = ast.getRoot();
        final ASTFieldVisitor fieldVisitor = new ASTFieldVisitor();
        root.accept(fieldVisitor);//find field variables and their types
        this.mapTree = fieldVisitor.getMapTree();//maptree records parent-child relationships of ast nodes
        this.traverseMethodOnly(root);//build groum from root
    }
    
    private void traverseMethodOnly(final ASTNode root) {
    	
        if (root.getNodeType() == ASTNode.TYPE_DECLARATION) {
            final TypeDeclaration node = (TypeDeclaration)root;
            if (node.getParent().getNodeType() == ASTNode.COMPILATION_UNIT) {
                this.className = node.getName().toString();//find the class name of current ast node
            }
        }
        
        //方法声明
        if (root.getNodeType() == ASTNode.METHOD_DECLARATION) {
      	  //if the ast node is a method declaration, then build 
      	  //a groum for it and store it.
      	  
            final GROUMGraph aGraph = this.traverse(null, root);//build temporary groum, no data dependence.
            
            //aGraph.setFileID(this.fileID);
            final MethodDeclaration method = (MethodDeclaration)root;
            String groumName=String.valueOf(this.className) + "." + method.getName().getIdentifier()+"(";
            
            for(SingleVariableDeclaration para:(List<SingleVariableDeclaration>)method.parameters())
            	groumName+=para+",";
            if(groumName.endsWith(","))groumName=groumName.substring(0, groumName.length()-1);
            groumName+=")";
           
            //System.out.println(groumName);//eg. AnnClassifier.forward(List<Float> list)
            if(method.getJavadoc()!=null) {
            	aGraph.setComments(method.getJavadoc().toString());
            }
            aGraph.setName(String.valueOf(this.className) + "." + method.getName().getIdentifier());
            aGraph.addDataDependency();
//            aGraph.simplify();
            //build final groum. i.e., add data dependent edges (i.e., sharing variables) between nodes.
            aGraph.removeNonDependents();
//            aGraph.prune();
            aGraph.cleanUp();
            if (aGraph.getNodes().size() >= GROUMGraph.minSize) //check the groum size, if it is too small, ignore.
            {
            	aGraph.setId(this.groums.size() + 1);
            	this.groums.add(aGraph);//store the groum
            	if (this.maxGroumSize < aGraph.getNodes().size()) {
            		this.maxGroumSize = aGraph.getNodes().size();//record the max size of groums
            	}
            }
        }
        else {
            final ArrayList<ASTNode> children = this.mapTree.get(root);
            for (int i = children.size() - 1; i >= 0; --i) {
                final ASTNode child = children.get(i);
                this.traverseMethodOnly(child);
            }
        }
    }
    
    public ArrayList<GROUMGraph> getGroums() {
        return this.groums;
    }
    
    /***
     * Build a temporary groum for an AST node.
     * @param cfgNode: control flow graph node
     * @param root: root of current visiting AST 
     * @return
     */
    private GROUMGraph traverse(final GROUMNode cfgNode, final ASTNode root) {
        final GROUMGraph graph = new GROUMGraph();
        GROUMNode branch = null;
        final ArrayList<ASTNode> children = this.mapTree.get(root);//get child nodes of the root node
        GROUMNode aNode = null;
        switch (root.getNodeType()) {
            case ASTNode.CLASS_INSTANCE_CREATION: {//build groum for a class instance creation node
                final ClassInstanceCreation nNode = (ClassInstanceCreation)root;
                for (int i = 0; i < children.size(); ++i) {
                    final ASTNode child = children.get(i);
                    graph.mergeSeq(this.traverse(cfgNode, child));//sequentially concact the child nodes in the groum
                }
                //add parameters (dependent variables such as object name, parameter names) 
                final HashSet<ASTNode> paraExprs = this.getParameters(nNode.arguments());
                paraExprs.add(root);
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                String nObjectname;
                if (nNode.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
                    final VariableDeclarationFragment pNode = (VariableDeclarationFragment)nNode.getParent();
                    nObjectname = pNode.getName().toString();
                    parameters.add(GROUMNode.convertLabel(nObjectname));
                }
                else if (nNode.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
                    final Assignment pNode2 = (Assignment)nNode.getParent();
                    final Expression ex = pNode2.getLeftHandSide();
                    if (ex.getNodeType() == ASTNode.FIELD_ACCESS) {
                        FieldAccess fa = (FieldAccess)ex;
                        nObjectname = fa.getName().toString();
                    }
                    else {
                        nObjectname = ex.toString();
                    }
                    parameters.add(GROUMNode.convertLabel(ex.toString()));
                }
                else {
                    nObjectname = "anonymous";
                }
                /**build a object creation node for the current class declaration*/
                Object classname=nNode.getProperty("TypeBinding");
                if(classname==null)classname=nNode.getType();
                aNode = new GROUMNode("<init>", GROUMNode.TYPE_METHOD, classname.toString(), nObjectname, parameters);
                //aNode.setFileID(this.fileID);
                aNode.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                aNode.setEndLine(this.binSearch(root.getStartPosition() + root.getLength() - 1, this.lines));
                aNode.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(aNode);
                return graph;
            }
            case ASTNode.CONSTRUCTOR_INVOCATION: {//build a groum for a constructor invocation node
                for (int j = children.size() - 1; j >= 0; --j) {
                    final ASTNode child = children.get(j);
                    graph.mergeSeq(this.traverse(cfgNode, child));
                }
                final ConstructorInvocation ciNode = (ConstructorInvocation)root;
                final HashSet<ASTNode> paraExprs = this.getParameters(ciNode.arguments());
                paraExprs.add(root);
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                parameters.add(GROUMNode.convertLabel("this"));
                Object classname=ciNode.getProperty("TypeBinding");
 //##############################################################???????????????????????
                if(classname==null)classname="Unknown";//????????????????????
                aNode = new GROUMNode("<init>", GROUMNode.TYPE_METHOD, classname.toString(), "this", parameters);
                //aNode.setFileID(this.fileID);
                aNode.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                aNode.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                aNode.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(aNode);
                return graph;
            }
            case ASTNode.CAST_EXPRESSION: {//build a groum for a cast node
                for (int k = children.size() - 1; k >= 0; --k) {
                    final ASTNode child3 = children.get(k);
                    graph.mergeSeq(this.traverse(cfgNode, child3));
                }
                final HashSet<Integer> parameters = new HashSet<Integer>();
                final CastExpression cNode = (CastExpression)root;
                parameters.add(GROUMNode.convertLabel(cNode.getExpression().toString()));
                final ASTNode leftHandSide = this.getLeftHandSide(root);
                if (leftHandSide != null) {
                    parameters.add(GROUMNode.convertLabel(leftHandSide.toString()));
                }
                Object classname=root.getProperty("TypeBinding");
//##############################################################???????????????????????
                if(classname==null)classname="Unknown";//????????????????????
                aNode = new GROUMNode("cast", GROUMNode.TYPE_METHOD, classname.toString(), cNode.getExpression().toString(), parameters);
                //aNode.setFileID(this.fileID);
                aNode.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                aNode.setEndLine(this.binSearch(root.getStartPosition() + root.getLength() - 1, this.lines));
                aNode.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(aNode);
                return graph;
            }
            case ASTNode.INSTANCEOF_EXPRESSION: {//build a groum for a instanceof node
                for (int l = children.size() - 1; l >= 0; --l) {
                    final ASTNode child4 = children.get(l);
                    graph.mergeSeq(this.traverse(cfgNode, child4));
                }
                final HashSet<Integer> parameters = new HashSet<Integer>();
                final InstanceofExpression ieNode = (InstanceofExpression)root;
                parameters.add(GROUMNode.convertLabel(ieNode.getLeftOperand().toString()));
                final ASTNode leftHandSide = this.getLeftHandSide(root);
                if (leftHandSide != null) {
                    parameters.add(GROUMNode.convertLabel(leftHandSide.toString()));
                }
                Object classname=root.getProperty("TypeBinding");
//##############################################################???????????????????????
                if(classname==null)classname="Unknown";//????????????????????
                aNode = new GROUMNode("instance", GROUMNode.TYPE_METHOD, classname.toString(), ieNode.getLeftOperand().toString(), parameters);
                //aNode.setFileID(this.fileID);
                aNode.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                aNode.setEndLine(this.binSearch(root.getStartPosition() + root.getLength() - 1, this.lines));
                aNode.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(aNode);
                return graph;
            }
            //方法调用
            case ASTNode.METHOD_INVOCATION: {//build a groum for a method invocation node
                for (int m = children.size() - 1; m >= 0; --m) {
                    final ASTNode child5 = children.get(m);
                    graph.mergeSeq(this.traverse(cfgNode, child5));
                }
                final MethodInvocation mNode = (MethodInvocation)root;
                System.out.println(mNode.getName());
                if (mNode.getExpression() != null) {
                    final String ex2 = mNode.getExpression().toString();
                    if (ex2.contains("System.") || ex2.contains("java.")) {
                        return graph;
                    }
                }
                final HashSet<ASTNode> paraExprs = this.getParameters(mNode.arguments());
                paraExprs.add(root);
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                final ASTNode leftHandSide = this.getLeftHandSide(root);
                if (leftHandSide != null) {
                    parameters.add(GROUMNode.convertLabel(leftHandSide.toString()));
                }
                if (mNode.getExpression() != null) {
                    parameters.add(GROUMNode.convertLabel(mNode.getExpression().toString()));
                   if (children.get(0).getNodeType() == ASTNode.FIELD_ACCESS) {
                        final FieldAccess faNode = (FieldAccess) children.get(0);
                        Object classname= faNode.getProperty("TypeBinding");
 //########################################################################################?????
                        if(classname==null)classname="Unknown";//???????????????????????
                        aNode = new GROUMNode(mNode.getName().toString(), GROUMNode.TYPE_METHOD, classname.toString()
                    		, faNode.getName().toString(), parameters);
                    }
                    else {
                  	   Object classname= mNode.getProperty("TypeBinding");
//########################################################################################?????
                  	   if(classname==null)classname="Unknown";//???????????????????????
                        aNode = new GROUMNode(mNode.getName().toString(), GROUMNode.TYPE_METHOD, classname.toString(), 
                        		mNode.getExpression().toString(), parameters);
                    }
                }
                else {
                    parameters.add(GROUMNode.convertLabel("this"));
                    aNode = new GROUMNode(mNode.getName().toString(), GROUMNode.TYPE_METHOD, this.className, "this", parameters);
                }
                //aNode.setFileID(this.fileID);
                aNode.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                aNode.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                aNode.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(aNode);
                return graph;
            }
            case ASTNode.IF_STATEMENT: {//build a groum for a if node
                final IfStatement iNode = (IfStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(iNode.getExpression());
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("IF", GROUMNode.TYPE_CONTROL, label, "IF", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                if (iNode.getExpression() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, iNode.getExpression()));
                }
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, iNode.getThenStatement()), branch);
                if (iNode.getElseStatement() != null) {
                    graph.mergeABranch(this.traverse(branch, iNode.getElseStatement()), branch);
                    graph.getOuts().remove(branch);
                }
                return graph;
            }
            case ASTNode.CONDITIONAL_EXPRESSION: {//build a groum for a conditional node
                final ConditionalExpression ceNode = (ConditionalExpression)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(ceNode.getExpression());
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("IF", GROUMNode.TYPE_CONTROL, label, "IF", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                if (ceNode.getExpression() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, ceNode.getExpression()));
                }
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, ceNode.getThenExpression()), branch);
                if (ceNode.getElseExpression() != null) {
                    graph.mergeABranch(this.traverse(branch, ceNode.getElseExpression()), branch);
                    graph.getOuts().remove(branch);
                }
                return graph;
            }
            case ASTNode.FOR_STATEMENT: {//build a groum for a for node
                final ForStatement forNode = (ForStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(forNode.initializers());
                paraExprs.addAll(this.getParameters(forNode.getExpression()));
                paraExprs.addAll(this.getParameters(forNode.updaters()));
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("FOR", GROUMNode.TYPE_CONTROL, label, "FOR", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                for (int i2 = 0; i2 <= children.size() - 2; ++i2) {
                    final ASTNode child6 = children.get(i2);
                    graph.mergeSeq(this.traverse(cfgNode, child6));
                }
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, forNode.getBody()), branch);
                return graph;
            }
            case ASTNode.ENHANCED_FOR_STATEMENT: {//build a groum for a enhanced for node
                final EnhancedForStatement eforNode = (EnhancedForStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(eforNode.getParameter());
                paraExprs.addAll(this.getParameters(eforNode.getExpression()));
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("FOR", GROUMNode.TYPE_CONTROL, label, "FOR", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                if (eforNode.getExpression() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, eforNode.getExpression()));
                }
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, eforNode.getBody()), branch);
                return graph;
            }
            case ASTNode.WHILE_STATEMENT: {//
                final WhileStatement whileNode = (WhileStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(whileNode.getExpression());
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("WHILE", GROUMNode.TYPE_CONTROL, label, "WHILE", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                if (whileNode.getExpression() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, whileNode.getExpression()));
                }
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, whileNode.getBody()), branch);
                return graph;
            }
            case ASTNode.DO_STATEMENT: {
                final DoStatement doNode = (DoStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(doNode.getExpression());
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("DOWHILE", GROUMNode.TYPE_CONTROL, label, "DOWHILE", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(branch);
                graph.mergeABranch(this.traverse(branch, doNode.getBody()), branch);
                if (doNode.getExpression() != null) {
                    graph.mergeABranch(this.traverse(branch, doNode.getExpression()), branch);
                }
                return graph;
            }
            case ASTNode.SWITCH_STATEMENT: {//build groum for switch node
                final SwitchStatement sNode = (SwitchStatement)root;
                final String label = "CONTROL";
                final HashSet<ASTNode> paraExprs = this.getParameters(sNode.getExpression());
                final HashSet<Integer> parameters = this.getIds(paraExprs);
                this.getDependents(parameters);
                branch = new GROUMNode("SWITCH", GROUMNode.TYPE_CONTROL, label, "SWITCH", parameters);
                //branch.setFileID(this.fileID);
                branch.setStartLine(this.binSearch(root.getStartPosition(), this.lines));
                branch.setEndLine(this.binSearch(root.getStartPosition() + root.getLength(), this.lines));
                branch.setPid((cfgNode == null) ? "." : (String.valueOf(cfgNode.getPid()) + cfgNode.getId() + "."));
                graph.mergeSeq(this.traverse(cfgNode, sNode.getExpression()));
                graph.mergeSeq(branch);
                for (int i3 = 1; i3 < children.size(); ++i3) {
                    final ASTNode child7 = children.get(i3);
                    graph.mergeABranch(this.traverse(branch, child7), branch);
                }
                graph.getOuts().remove(branch);
                return graph;
            }
            case ASTNode.TRY_STATEMENT: {//build groum for try node
                final TryStatement tNode = (TryStatement)root;
                if (tNode.getBody() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, tNode.getBody()));
                }
                if (tNode.catchClauses() != null && !tNode.catchClauses().isEmpty()) {
                    final HashSet<GROUMNode> saveOuts = new HashSet<GROUMNode>();
                    saveOuts.addAll(graph.getOuts());
                    for (int i4 = 0; i4 < tNode.catchClauses().size(); ++i4) {
                        final CatchClause clause = (CatchClause) tNode.catchClauses().get(i4);
                        graph.mergeBranches(this.traverse(branch, clause), saveOuts);
                    }
                    graph.getOuts().removeAll(saveOuts);
                }
                if (tNode.getFinally() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, tNode.getFinally()));
                }
                return graph;
            }
            case ASTNode.INFIX_EXPRESSION: {//build groum for a infix node
                final HashSet<GROUMNode> saveOuts = new HashSet<GROUMNode>();
                saveOuts.addAll(graph.getOuts());
                for (int i4 = children.size() - 1; i4 >= 0; --i4) {
                    final ASTNode child8 = children.get(i4);
                    graph.mergeBranches(this.traverse(cfgNode, child8), saveOuts);
                }
                graph.getOuts().removeAll(saveOuts);
                return graph;
            }
            case ASTNode.ASSIGNMENT: {
                final Assignment asgnNode = (Assignment)root;
           
                GROUMGraph tobeAsgn=this.traverse(cfgNode, asgnNode.getRightHandSide());
                graph.mergeSeq(tobeAsgn);
                this.dataDependencies.put(this.getId(asgnNode.getLeftHandSide()), this.getIds(this.getParameters(asgnNode.getRightHandSide())));
                return graph;
            }
            case ASTNode.SINGLE_VARIABLE_DECLARATION: {
                final SingleVariableDeclaration svdNode = (SingleVariableDeclaration)root;
                if (svdNode.getInitializer() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, svdNode.getInitializer()));
                }
                this.dataDependencies.put(this.getId(svdNode.getName()), this.getIds(this.getParameters(svdNode.getInitializer())));
                return graph;
            }
            case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {// e.g., button=ButtionFactory.create();
                final VariableDeclarationFragment vdfNode = (VariableDeclarationFragment)root;
                if (vdfNode.getInitializer() != null) {
                    graph.mergeSeq(this.traverse(cfgNode, vdfNode.getInitializer()));
                }
                this.dataDependencies.put(this.getId(vdfNode.getName()), this.getIds(this.getParameters(vdfNode.getInitializer())));
                return graph;
            }
            default: {
                for (int i = 0; i < children.size(); ++i) {
                    final ASTNode child = children.get(i);
                    graph.mergeSeq(this.traverse(cfgNode, child));
                }
                return graph;
            }
        }
    }
    
    
    /***
     * binary search 
     * @param x - element to search for
     * @param al - a sorted array to search from
     * @return position of the element in the array
     */
    int binSearch(final int x, final ArrayList<Integer> al) {
        if (al.size() <= 1) {
            return al.size() - 1;
        }
        int low = 0;
        int high = al.size() - 1;
        while (low < high) {
            int mid = (low + high) / 2;
            if (al.get(mid) < x) {
                low = mid + 1;
            }
            else {
                if (al.get(mid) <= x) {
                    return mid;
                }
                high = mid - 1;
            }
        }
        if (al.get(low) > x) {
            return low - 1;
        }
        return low;
    }
    
    
    private HashSet<ASTNode> getParameters(final List<ASTNode> arguments) {
        final HashSet<ASTNode> parameters = new HashSet<ASTNode>();
        for (int i = 0; i < arguments.size(); ++i) {
            final ASTNode arg = (ASTNode) arguments.get(i);
            parameters.addAll(this.getParameters(arg));
        }
        return parameters;
    }
    
    private HashSet<ASTNode> getParameters(final ASTNode node) {
        final HashSet<ASTNode> parameters = new HashSet<ASTNode>();
        if (node != null) {
            if (node.getNodeType() == ASTNode.QUALIFIED_NAME) {
                parameters.add(node);
            }
            else if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {//get from method parameter
                final MethodInvocation expr = (MethodInvocation)node;
                parameters.add(node);
                parameters.addAll(this.getParameters(expr.getExpression()));
                parameters.addAll(this.getParameters(expr.arguments()));
            }
            else if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
                final ClassInstanceCreation expr = (ClassInstanceCreation)node;
                parameters.add(node);
                parameters.addAll(this.getParameters(expr.getExpression()));
                parameters.addAll(this.getParameters(expr.arguments()));
            }
            else if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
                parameters.add(node);
            }
            else {
                final ArrayList<ASTNode> children = this.mapTree.get(node);
                if (!children.isEmpty()) {
                    parameters.addAll(this.getParameters(children));
                }
            }
        }
        return parameters;
    }
    
    /**get label id*/
    private int getId(final Object obj) {
        return GROUMNode.convertLabel(obj.toString());
    }
    
    private HashSet<Integer> getIds(final Set<ASTNode> set) {
        final HashSet<Integer> ids = new HashSet<Integer>();
        final Iterator<ASTNode> iter = set.iterator();
        while (iter.hasNext()) {
            ids.add(this.getId(iter.next()));
        }
        return ids;
    }
    
    /**Augmenting a set of variables with their dependent variables??
     * @param dataItems: seed variables to be augmented
     * @return dataItems: an augmented set of variable ids that have data dependence. 
     */
    private void getDependents(final HashSet<Integer> dataItems) {
        final ArrayList<Integer> q = new ArrayList<Integer>(dataItems);
        while (!q.isEmpty()) {
            final int item = q.get(0);
            q.remove(0);
            if (this.dataDependencies.containsKey(item)) {
                final HashSet<Integer> dependents = new HashSet<Integer>(this.dataDependencies.get(item));
                dependents.removeAll(dataItems);
                q.addAll(dependents);
                dataItems.addAll(dependents);
            }
        }
    }
    
    private ASTNode getLeftHandSide(final ASTNode node) {
        for (ASTNode p = node.getParent(); p != null; p = p.getParent()) {
            if (p.getNodeType() == ASTNode.ASSIGNMENT) {
                return ((Assignment)p).getLeftHandSide();
            }
            if (p.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
                return ((VariableDeclarationFragment)p).getName();
            }
        }
        return null;
    }
    
    public CSystem getSystem()
    {
   	 return this.system;
    }
}



