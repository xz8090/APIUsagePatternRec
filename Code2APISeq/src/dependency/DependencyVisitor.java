package dependency;

import java.util.Iterator;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import change.CField;
import java.util.ArrayList;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import java.util.Collection;
import change.CClass;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import utils.FileIO;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import change.CMethod;
import java.util.HashSet;
import java.util.HashMap;
import org.eclipse.jdt.core.dom.ASTNode;
import change.ChangeEntity;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class DependencyVisitor extends ASTVisitor
{
   public static final String PROPERTY_SCOPE_VARS = "sv";
   public static final String PROPERTY_RETURN_TYPE = "return";
   public static final String PROPERTY_OBJ_TYPE = "TypeBinding";
   private ChangeEntity entity;
   private ASTNode root;
   private HashMap<String, String> allFieldTypes;
   private HashSet<String> types;
   private HashSet<String> fields;
   private HashSet<String> calleeNames;
   private HashSet<CMethod> callees;
   private String className;
   private String superClassName;
   private HashMap<String, String> localVariables;
   private HashMap<SimpleName, HashSet<SimpleName>> localVarLocs;
   private HashMap<SimpleName, HashSet<SimpleName>> scopedLocalVarLocs;
   private HashMap<String, SimpleName> mapName;

   public DependencyVisitor(final ChangeEntity entity, final ASTNode root, final String className,
	    final String superClassName, final HashMap<String, String> allFieldTypes) {
	 super();
	 this.types = new HashSet<String>();
	 this.fields = new HashSet<String>();
	 this.calleeNames = new HashSet<String>();
	 this.callees = new HashSet<CMethod>();
	 this.localVariables = new HashMap<String, String>();
	 this.localVarLocs = new HashMap<SimpleName, HashSet<SimpleName>>();
	 this.scopedLocalVarLocs = new HashMap<SimpleName, HashSet<SimpleName>>();
	 this.mapName = new HashMap<String, SimpleName>();
	 this.entity = entity;
	 this.root = root;
	 this.className = className;
	 this.superClassName = superClassName;
	 this.allFieldTypes = allFieldTypes;
   }

   public HashSet<String> getTypes()
   {
	 return this.types;
   }

   public HashSet<String> getFields()
   {
	 return this.fields;
   }

   public HashSet<CMethod> getCallees()
   {
	 return this.callees;
   }

   public HashSet<String> getCalleeNames()
   {
	 return this.calleeNames;
   }

   public HashMap<SimpleName, HashSet<SimpleName>> getLocalVarLocs()
   {
	 return this.localVarLocs;
   }

   @Override
   public boolean visit(final AnnotationTypeDeclaration node)
   {
	 //  System.out.println("visit Annotation type decl");
	 if (node.bodyDeclarations() != null)
	 {
	    for (int i = 0; i < node.bodyDeclarations().size(); ++i)
	    {
		  ((ASTNode) node.bodyDeclarations().get(i)).accept(this);
	    }
	 }
	// System.out.println("fnish visit Annotation type decl");
	 return false;
   }

   @Override
   public boolean visit(final AnnotationTypeMemberDeclaration node)
   {
	 return true;
   }

   @Override
   public boolean visit(final AnonymousClassDeclaration node)
   {
	  // System.out.println("visit Anonymous class decl");
	 if (node.bodyDeclarations() != null)
	 {
	    for (int i = 0; i < node.bodyDeclarations().size(); ++i)
	    {
		  ((ASTNode) node.bodyDeclarations().get(i)).accept(this);
	    }
	 }
	// System.out.println("finish visit Anonymous class decl");
	 return super.visit(node);
   }

   @Override
   public boolean visit(final ArrayAccess node)
   {
	 return true;
   }

   @Override
   public boolean visit(final ArrayCreation node)
   {
	  // System.out.println("visit Array clreation");
	 for (int i = 0; i < node.dimensions().size(); ++i)
	 {
	    ((ASTNode) node.dimensions().get(i)).accept(this);
	 }
	 if (node.getInitializer() != null)
	 {
	    node.getInitializer().accept(this);
	 }
	// System.out.println("finish visit Array clreation");
	 return false;
   }

   @Override
   public boolean visit(final ArrayInitializer node)
   {
	 return true;
   }

   @Override
   public boolean visit(final ArrayType node)
   {
	 return false;
   }

   @Override
   public boolean visit(final AssertStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final Assignment node)
   {
	 return true;
   }

   @Override
   public boolean visit(final Block node)
   {
	  // System.out.println("visit Block");
	 final HashSet<SimpleName> vars = new HashSet<SimpleName>();
	 for (int i = 0; i < node.statements().size(); ++i)
	 {
	    if (node.statements().get(i) instanceof VariableDeclarationStatement)
	    {
		  final VariableDeclarationStatement dec = (VariableDeclarationStatement) node
			   .statements().get(i);
		  final String type = FileIO.getSimpleClassName(dec.getType().toString());
		  this.types.add(type);
		  for (int j = 0; j < dec.fragments().size(); ++j)
		  {
			final VariableDeclarationFragment frag = (VariableDeclarationFragment) dec
				 .fragments().get(j);
			final String var = frag.getName().getIdentifier();
			vars.add(frag.getName());
			this.localVariables.put(var, type);
			final HashSet<SimpleName> names = new HashSet<SimpleName>();
			names.add(frag.getName());
			this.scopedLocalVarLocs.put(frag.getName(), names);
			this.mapName.put(var, frag.getName());
		  }
	    }
	 }
	 //System.out.println("finish visit block");
	 node.setProperty("sv", vars);
	 return true;
   }

   @Override
   public boolean visit(final BlockComment node)
   {
	 return false;
   }

   @Override
   public boolean visit(final BooleanLiteral node)
   {
	 return false;
   }

   @Override
   public boolean visit(final BreakStatement node)
   {
	 return false;
   }

   @Override
   public boolean visit(final CastExpression node)
   {
	 final String type = FileIO.getSimpleClassName(node.getType().toString());
	 node.setProperty("TypeBinding", type);
	 node.setProperty("return", type);
	 node.getExpression().accept(this);
	 return false;
   }

   @Override
   public boolean visit(final CatchClause node)
   {
	 node.getBody().accept(this);
	 return false;
   }

   @Override
   public boolean visit(final CharacterLiteral node)
   {
	 return false;
   }

   @Override
   public boolean visit(final ClassInstanceCreation node)
   {
	  // System.out.println("visit class inst creation");
	 final String type = FileIO.getSimpleClassName(node.getType().toString());
	 node.setProperty("return", type);
	 node.setProperty("TypeBinding", type);
	 if (node.getExpression() != null)
	 {
	    node.getExpression().accept(this);
	 }
	 for (int i = 0; i < node.arguments().size(); ++i)
	 {
	    ((ASTNode) node.arguments().get(i)).accept(this);
	 }
	 if (node.getAnonymousClassDeclaration() != null)
	 {
	    node.getAnonymousClassDeclaration().accept(this);
	 }
	// System.out.println("finish visit class inst creation");
	 return false;
   }

   @Override
   public boolean visit(final ConditionalExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final ConstructorInvocation node)
   {
	 //  System.out.println("visit ConstructorInvocation");
	 node.setProperty("TypeBinding", this.className);
	 node.setProperty("return", this.className);
	 final String methodName = String.valueOf(this.className) + "(" + node.arguments().size()
		  + ")";
	 final HashSet<CMethod> methods = this.entity.getCFile().getcSystem().getDataGraph()
		  .getInheritedMethods(this.className, methodName, new HashSet<CClass>());
	 if (methods.isEmpty())
	 {
	    this.calleeNames.add(methodName);
	 }
	 else
	 {
	    this.callees.addAll(methods);
	 }
	 for (int i = 0; i < node.arguments().size(); ++i)
	 {
	    ((ASTNode) node.arguments().get(i)).accept(this);
	 }
	// System.out.println("finish visit ConstructorInvocation");
	 return false;
   }

   @Override
   public boolean visit(final ContinueStatement node)
   {
	 return false;
   }

   @Override
   public boolean visit(final DoStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final EmptyStatement node)
   {
	 return false;
   }

   @Override
   public boolean visit(final EnhancedForStatement node)
   {
	  // System.out.println("visit EnhancedForStatement");
	 final SingleVariableDeclaration dec = node.getParameter();
	 final String var = dec.getName().getIdentifier();
	 final String type = FileIO.getSimpleClassName(dec.getType().toString());
	 this.localVariables.put(var, type);
	 this.types.add(type);
	 final HashSet<SimpleName> names = new HashSet<SimpleName>();
	 names.add(dec.getName());
	 this.scopedLocalVarLocs.put(dec.getName(), names);
	 this.mapName.put(var, dec.getName());
	// System.out.println("finish visit EnhancedForStatement");
	 return true;
	 
   }

   @Override
   public boolean visit(final EnumConstantDeclaration node)
   {
	 this.printError(node);
	 return false;
   }

   private void printError(final ASTNode node)
   {
	 System.err.println("Error: " + node.getClass().getSimpleName()
		  + " should not be in the body of method or field. In DependencyVisitor!");
   }

   @Override
   public boolean visit(final EnumDeclaration node)
   {
	 this.printError(node);
	 return false;
   }

   @Override
   public boolean visit(final ExpressionStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final FieldAccess node)
   {
	 return true;
   }

   @Override
   public boolean visit(final FieldDeclaration node)
   {
	  // System.out.println("visit FieldDeclaration"); 
	 for (int i = 0; i < node.fragments().size(); ++i)
	 {
	    ((ASTNode) node.fragments().get(i)).accept(this);
	 }
	// System.out.println("finish visit FieldDeclaration"); 
	 return false;
   }

   @Override
   public boolean visit(final ForStatement node)
   {
	  // System.out.println("visit for statement");
	 for (int i = 0; i < node.initializers().size()
		  && node.initializers().get(i) instanceof VariableDeclarationExpression; ++i)
	 {
	    final VariableDeclarationExpression decEx = (VariableDeclarationExpression) node
			.initializers().get(i);
	    for (int j = 0; j < decEx.fragments().size(); ++j)
	    {
		  final VariableDeclarationFragment decFrag = (VariableDeclarationFragment) decEx
			   .fragments().get(j);
		  final String var = decFrag.getName().getIdentifier();
		  final String type = FileIO.getSimpleClassName(decEx.getType().toString());
		  this.localVariables.put(var, type);
		  this.types.add(type);
		  final HashSet<SimpleName> names = new HashSet<SimpleName>();
		  names.add(decFrag.getName());
		  this.scopedLocalVarLocs.put(decFrag.getName(), names);
		  this.mapName.put(var, decFrag.getName());
	    }
	  //  System.out.println("finish visit for statement");
	 }
	 return true;
   }

   @Override
   public boolean visit(final IfStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final InfixExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final Initializer node)
   {
	 return true;
   }

   @Override
   public boolean visit(final InstanceofExpression node)
   {
	 final String type = FileIO.getSimpleClassName(node.getRightOperand().toString());
	 this.types.add(type);
	 node.setProperty("TypeBinding", type);
	 node.setProperty("return", type);
	 node.getLeftOperand().accept(this);
	 return false;
   }

   @Override
   public boolean visit(final Javadoc node)
   {
	 return false;
   }

   @Override
   public boolean visit(final LabeledStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final LineComment node)
   {
	 return false;
   }

   @Override
   public boolean visit(final MarkerAnnotation node)
   {
	 return false;
   }

   @Override
   public boolean visit(final MemberRef node)
   {
	 return false;
   }

   @Override
   public boolean visit(final MemberValuePair node)
   {
	 return true;
   }

   @Override
   public boolean visit(final MethodDeclaration node)
   {
	//   System.out.println("visit for method decl");
	 if (node.getReturnType2() != null)
	 {
	    this.types.add(FileIO.getSimpleClassName(node.getReturnType2().toString()));
	 }
	 for (int i = 0; i < node.parameters().size(); ++i)
	 {
	    final SingleVariableDeclaration dec = (SingleVariableDeclaration) node.parameters().get(i);
	    final String var = dec.getName().getIdentifier();
	    final String type = FileIO.getSimpleClassName(dec.getType().toString());
	    this.types.add(type);
	    this.localVariables.put(var, type);
	    final HashSet<SimpleName> names = new HashSet<SimpleName>();
	    names.add(dec.getName());
	    this.scopedLocalVarLocs.put(dec.getName(), names);
	    this.mapName.put(var, dec.getName());
	 }
	 if (node != this.root)
	 {
	    if (node.getParent().getNodeType() == 1)
	    {
		  if (node.getParent().getParent().getNodeType() == 14)
		  {
			final ClassInstanceCreation n = (ClassInstanceCreation) node.getParent().getParent();
			final String type2 = FileIO.getSimpleClassName(n.getType().toString());
			if (this.entity.getCFile().getcSystem().getDataGraph().getTypeNames()
				 .contains(type2))
			{
			   final String name = String.valueOf(node.getName().getIdentifier()) + "("
				    + node.parameters().size() + ")";
			   final HashSet<CMethod> methods = this.entity.getCFile().getcSystem()
				    .getDataGraph().getInheritedMethods(type2, name, new HashSet<CClass>());
			   this.callees.addAll(methods);
			}
		  }
	    }
	    else if (node.getParent().getNodeType() == 55)
	    {
		  final TypeDeclaration n2 = (TypeDeclaration) node.getParent();
		  final String type2 = FileIO.getSimpleClassName(n2.getName().getIdentifier());
		  if (this.entity.getCFile().getcSystem().getDataGraph().getTypeNames().contains(type2))
		  {
			final String name = String.valueOf(node.getName().getIdentifier()) + "("
				 + node.parameters().size() + ")";
			final HashSet<CMethod> methods = this.entity.getCFile().getcSystem().getDataGraph()
				 .getInheritedMethods(type2, name, new HashSet<CClass>());
			this.callees.addAll(methods);
		  }
	    }
	    else
	    {
		  System.err
			   .println("INFO: What else other than anonymous class could it be a method declaration within a method declaration!");
	    }
	 }
	 if (node.getBody() != null)
	 {
	    node.getBody().accept(this);
	 }
	// System.out.println("finish visit method decl");
	 return false;
   }

   @Override
   public boolean visit(final MethodInvocation node)
   {
	  // System.out.println("visit method invoc");
	 if (node.getExpression() != null)
	 {
	    node.getExpression().accept(this);
	 }
	 for (int i = 0; i < node.arguments().size(); ++i)
	 {
	    ((ASTNode) node.arguments().get(i)).accept(this);
	 }
	 //System.out.println("finish visit method invoc");
	 return false;
   }

   @Override
   public boolean visit(final MethodRef node)
   {
	 return false;
   }

   @Override
   public boolean visit(final MethodRefParameter node)
   {
	 return false;
   }

   @Override
   public boolean visit(final Modifier node)
   {
	 return false;
   }

   @Override
   public boolean visit(final NormalAnnotation node)
   {
	 return false;
   }

   @Override
   public boolean visit(final NullLiteral node)
   {
	 return false;
   }

   @Override
   public boolean visit(final NumberLiteral node)
   {
	 return false;
   }

   @Override
   public boolean visit(final PackageDeclaration node)
   {
	 return false;
   }

   @Override
   public boolean visit(final ParameterizedType node)
   {
	 return false;
   }

   @Override
   public boolean visit(final ParenthesizedExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final PostfixExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final PrefixExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final PrimitiveType node)
   {
	 return false;
   }

   @Override
   public boolean visit(final QualifiedType node)
   {
	 return false;
   }

   @Override
   public boolean visit(final QualifiedName node)
   {
	 return true;
   }

   @Override
   public boolean visit(final ReturnStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final SimpleName node)
   {
	 final String name = node.getIdentifier();
	 if (!node.isDeclaration() && !this.localVariables.containsKey(node.getIdentifier()))
	 {
	    if (this.allFieldTypes.containsKey(name))
	    {
		  this.fields.add(name);
		  node.setProperty("return", this.allFieldTypes.get(name));
	    }
	    else
	    {
		  node.setProperty("return", name);
	    }
	 }
	 else
	 {
	    node.setProperty("return", this.localVariables.get(name));
	    if (this.localVariables.containsKey(node.getIdentifier()))
	    {
		  this.scopedLocalVarLocs.get(this.mapName.get(name)).add(node);
	    }
	 }
	 return false;
   }

   @Override
   public boolean visit(final SimpleType node)
   {
	 node.getName().setProperty("return", node.getName().toString());
	 return false;
   }

   @Override
   public boolean visit(final SingleMemberAnnotation node)
   {
	 return false;
   }

   @Override
   public boolean visit(final SingleVariableDeclaration node)
   {
	 return true;
   }

   @Override
   public boolean visit(final StringLiteral node)
   {
	 return false;
   }

   @Override
   public boolean visit(final SuperConstructorInvocation node)
   {
	 if (this.superClassName != null)
	 {
	    node.setProperty("TypeBinding", this.superClassName);
	 }
	 final String methodName = String.valueOf(this.superClassName) + "(" + node.arguments().size()
		  + ")";
	 if (this.superClassName == null)
	 {
	    this.calleeNames.add(methodName);
	 }
	 else if (this.entity.getCFile().getcSystem().getDataGraph().getTypeNames()
		  .contains(this.superClassName))
	 {
	    final HashSet<CMethod> methods = this.entity.getCFile().getcSystem().getDataGraph()
			.getInheritedMethods(this.superClassName, methodName, new HashSet<CClass>());
	    if (methods.isEmpty())
	    {
		  this.calleeNames.add(methodName);
	    }
	    else
	    {
		  this.callees.addAll(methods);
	    }
	 }
	 else
	 {
	    this.calleeNames.add(methodName);
	 }
	 for (int i = 0; i < node.arguments().size(); ++i)
	 {
	    ((ASTNode) node.arguments().get(i)).accept(this);
	 }
	 return false;
   }

   @Override
   public boolean visit(final SuperFieldAccess node)
   {
	 final String name = node.getName().getIdentifier();
	 this.fields.add(name);
	 final ArrayList<CField> fs = new ArrayList<CField>(this.entity.getCFile().getcSystem()
		  .getDataGraph().getFields(this.superClassName, name, new HashSet<CClass>()));
	 if (fs.size() == 1)
	 {
	    node.setProperty("TypeBinding", fs.get(0).getCClass().getSimpleName());
	    node.setProperty("return", fs.get(0).getName());
	 }
	 else
	 {
	    node.setProperty("TypeBinding", this.superClassName);
	    node.setProperty("return", String.valueOf(this.superClassName) + "." + name);
	 }
	 return false;
   }

   @Override
   public boolean visit(final SuperMethodInvocation node)
   {
	 final String name = String.valueOf(node.getName().getIdentifier()) + "("
		  + node.arguments().size() + ")";
	 if (this.superClassName == null)
	 {
	    this.calleeNames.add(name);
	    node.setProperty("TypeBinding", "#Unknown#");
	    node.setProperty("return", name);
	 }
	 else
	 {
	    node.setProperty("TypeBinding", this.superClassName);
	    node.setProperty("return", String.valueOf(this.superClassName) + "." + name);
	    final HashSet<CMethod> methods = this.entity.getCFile().getcSystem().getDataGraph()
			.getInheritedMethods(this.superClassName, name, new HashSet<CClass>());
	    if (methods.isEmpty())
	    {
		  this.calleeNames.add(name);
	    }
	    else
	    {
		  this.callees.addAll(methods);
		  final String[] types = this.resolve(methods);
		  if (!types[0].isEmpty())
		  {
			node.setProperty("TypeBinding", types[0]);
			if (types[1] != null)
			{
			   node.setProperty("return", types[1]);
			}
		  }
	    }
	 }
	 for (int i = 0; i < node.arguments().size(); ++i)
	 {
	    ((ASTNode) node.arguments().get(i)).accept(this);
	 }
	 return false;
   }

   @Override
   public boolean visit(final SwitchCase node)
   {
	 return true;
   }

   @Override
   public boolean visit(final SwitchStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final SynchronizedStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final TagElement node)
   {
	 return false;
   }

   @Override
   public boolean visit(final TextElement node)
   {
	 return false;
   }

   @Override
   public boolean visit(final ThrowStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final ThisExpression node)
   {
	 node.setProperty("return", this.className);
	 return false;
   }

   @Override
   public boolean visit(final TryStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final TypeDeclaration node)
   {
	 return true;
   }

   @Override
   public boolean visit(final TypeDeclarationStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final TypeLiteral node)
   {
	 if (node.getType() != null)
	 {
	    this.types.add(FileIO.getSimpleClassName(node.getType().toString()));
	 }
	 return false;
   }

   @Override
   public boolean visit(final TypeParameter node)
   {
	 return true;
   }

   @Override
   public boolean visit(final VariableDeclarationExpression node)
   {
	 return true;
   }

   @Override
   public boolean visit(final VariableDeclarationFragment node)
   {
	 return true;
   }

   @Override
   public boolean visit(final VariableDeclarationStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final WhileStatement node)
   {
	 return true;
   }

   @Override
   public boolean visit(final WildcardType node)
   {
	 return false;
   }

   @Override
   public void endVisit(final Block node)
   {
	 final HashSet<SimpleName> vars = (HashSet<SimpleName>) node.getProperty("sv");
	 for (final SimpleName name : vars)
	 {
	    final String var = name.getIdentifier();
	    this.localVariables.remove(var);
	    this.localVarLocs.put(name, this.scopedLocalVarLocs.get(name));
	    this.scopedLocalVarLocs.remove(name);
	    this.mapName.remove(var);
	 }
	 node.setProperty("sv", null);
   }

   @Override
   public void endVisit(final EnhancedForStatement node)
   {
	 final SingleVariableDeclaration dec = node.getParameter();
	 final String var = dec.getName().getIdentifier();
	 this.localVariables.remove(var);
	 this.localVarLocs.put(dec.getName(),
		  new HashSet<SimpleName>(this.scopedLocalVarLocs.get(dec.getName())));
	 this.scopedLocalVarLocs.remove(dec.getName());
	 this.mapName.remove(var);
   }

   @Override
   public void endVisit(final ForStatement node)
   {
	 for (int i = 0; i < node.initializers().size()
		  && node.initializers().get(i) instanceof VariableDeclarationExpression; ++i)
	 {
	    final VariableDeclarationExpression decEx = (VariableDeclarationExpression) node
			.initializers().get(i);
	    for (int j = 0; j < decEx.fragments().size(); ++j)
	    {
		  final VariableDeclarationFragment decFrag = (VariableDeclarationFragment) decEx
			   .fragments().get(j);
		  final String var = decFrag.getName().getIdentifier();
		  this.localVariables.remove(var);
		  this.localVarLocs.put(decFrag.getName(), new HashSet<SimpleName>(
			   this.scopedLocalVarLocs.get(decFrag.getName())));
		  this.scopedLocalVarLocs.remove(decFrag.getName());
		  this.mapName.remove(var);
	    }
	 }
   }

   @Override
   public void endVisit(final MethodDeclaration node)
   {
	 for (int i = 0; i < node.parameters().size(); ++i)
	 {
	    final SingleVariableDeclaration dec = (SingleVariableDeclaration) node.parameters().get(i);
	    final String var = dec.getName().getIdentifier();
	    this.localVariables.remove(var);
	    this.localVarLocs.put(dec.getName(),
			new HashSet<SimpleName>(this.scopedLocalVarLocs.get(dec.getName())));
	    this.scopedLocalVarLocs.remove(dec.getName());
	    this.mapName.remove(var);
	 }
   }

   @Override
   public void endVisit(final ParenthesizedExpression node)
   {
	 if (node.getExpression().getProperty("return") == null)
	 {
	    node.setProperty("return", "#Uknown#");
	 }
	 else
	 {
	    node.setProperty("return", node.getExpression().getProperty("return"));
	 }
   }

   @Override
   public void endVisit(final FieldAccess node)
   {
	 final String name = node.getName().getIdentifier();
	 this.fields.add(name);
	 final String objName = (String) node.getExpression().getProperty("return");
	 final ArrayList<CField> fs = new ArrayList<CField>(this.entity.getCFile().getcSystem()
		  .getDataGraph().getFields(objName, name, new HashSet<CClass>()));
	 if (fs.size() == 1)
	 {
	    node.setProperty("return", fs.get(0).getType());
	    node.setProperty("TypeBinding", fs.get(0).getCClass().getSimpleName());
	 }
	 else if (node.getExpression().getNodeType() == 52)
	 {
	    node.setProperty("return", String.valueOf(this.className) + "." + name);
	    node.setProperty("TypeBinding", this.className);
	 }
	 else
	 {
	    node.setProperty("return", node.toString());
	    node.setProperty("TypeBinding", node.getExpression().toString());
	 }
   }

   @Override
   public void endVisit(final QualifiedName node)
   {
	 final String name = node.getName().getIdentifier();
	 final String objName = (String) node.getQualifier().getProperty("return");
	 final ArrayList<CField> fs = new ArrayList<CField>(this.entity.getCFile().getcSystem()
		  .getDataGraph().getFields(objName, name, new HashSet<CClass>()));
	 if (fs.size() == 1)
	 {
	    node.setProperty("return", fs.get(0).getType());
	    node.setProperty("TypeBinding", fs.get(0).getCClass().getSimpleName());
	 }
	 else
	 {
	    node.setProperty("return", node.toString());
	    node.setProperty("TypeBinding", node.getQualifier().toString());
	 }
   }

   @Override
   public void endVisit(final MethodInvocation node)
   {
	   if(node.toString().startsWith("attrs.setTitle("))
	   {
		   int a=0;
	   }
	 String type = null;
	 final String methodName = String.valueOf(node.getName().getIdentifier()) + "("
		  + node.arguments().size() + ")";
	 HashSet<CMethod> methods = new HashSet<CMethod>();
	 if (node.getExpression() != null && node.getExpression().getNodeType() != 52)
	 {
	    if (node.getExpression().getProperty("return") != null)
	    {
		  type = (String) node.getExpression().getProperty("return");
		  methods = this.entity.getCFile().getcSystem().getDataGraph()
			   .getInheritedMethods(type, methodName, new HashSet<CClass>());
	    }
	 }
	 else
	 {
	    type = this.className;
	    methods = this.entity.getCClass().getInheritedMethods(methodName, new HashSet<CClass>());
	 }
	 if (type == null)
	 {
	    this.calleeNames.add(methodName);
	 }
	 else if (methods.size() > 0)
	 {
	    final String[] types = this.resolve(methods);
	    if (!types[0].isEmpty())
	    {
		  node.setProperty("TypeBinding", types[0]);
		  if (types[1] != null)
		  {
			node.setProperty("return", types[1]);
		  }
	    }
	 }
	 else
	 {
	    this.calleeNames.add(methodName);
	    if (type.equals(this.className) && this.entity.getCClass().getExtendedClassName() != null)
	    {
		  node.setProperty("TypeBinding", this.entity.getCClass().getExtendedClassName());
	    }
	    node.setProperty("TypeBinding", type);
	 }
	 if (node.getProperty("return") == null)
	 {
	    node.setProperty("return", node.toString());
	 }
	 if (node.getProperty("TypeBinding") == null)
	 {
	    if (node.getExpression() == null)
	    {
		  node.setProperty("TypeBinding", "#Unknown#");
	    }
	    else
	    {
		  node.setProperty("TypeBinding", node.getExpression().toString());
	    }
	 }
   }

   private String[] resolve(final HashSet<CMethod> methods)
   {
	 String objType = "";
	 String returnType = "";
	 for (final CMethod m : methods)
	 {
	    if (objType.isEmpty())
	    {
		  objType = m.getCClass().getSimpleName();
	    }
	    else if (!objType.equals(m.getCClass().getSimpleName()))
	    {
		  return new String[] { "", "" };
	    }
	    if (returnType != null)
	    {
		  if (returnType.isEmpty())
		  {
			returnType = m.getReturnType();
		  }
		  else
		  {
			if (returnType.equals(m.getReturnType()))
			{
			   continue;
			}
			returnType = null;
		  }
	    }
	 }
	 return new String[] { objType, returnType };
   }
}
