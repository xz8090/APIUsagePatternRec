package change;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.JavaCore;

import utils.FileIO;

import java.io.File;
import java.util.HashSet;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class CFile extends ChangeEntity
{
   private CSystem cSystem;
   //private String path;
   //private String simpleName;
   private String content;
   private CFile mappedFile;
   private CompilationUnit compileUnit;
   private String packageName = "";
   private HashMap<String, CClass> imports;
   private HashMap<String, String> importedClasses;
	private HashSet<String> importedPackages;
   private HashSet<CClass> classes;

   public CFile(final CSystem cSystem, final String source) {
	 super();
	 this.classes = new HashSet<CClass>();
	 this.cSystem = cSystem;
	 this.content=source;
	 //this.path = file.getAbsolutePath();
	 //this.simpleName = FileIO.getSimpleFileName(this.path);
	 final Map<String,String> options = JavaCore.getOptions();
	 options.put("org.eclipse.jdt.core.compiler.compliance", "1.8");
	 options.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", "1.8");
	 options.put("org.eclipse.jdt.core.compiler.source", "1.8");
	 final ASTParser parser = ASTParser.newParser(AST.JLS8);
	 parser.setSource(source.toCharArray());
	 parser.setCompilerOptions(options);
	 final ASTNode ast =parser.createAST(null);
	 final CompilationUnit cu = (CompilationUnit) ast;
	 if (cu.types() == null || cu.types().isEmpty())//discard source files that has no type declarations
	 {
	    //System.out.println("\t\tDiscarded " + file.getAbsolutePath());
	 }
	 else
	 {
	    this.compileUnit = cu;
	    this.packageName = cu.getPackage().getName().getFullyQualifiedName();
		this.importedClasses = new HashMap<String, String>();
		this.importedPackages = new HashSet<String>();
		if (cu.imports() != null) {
			for (int i = 0; i < cu.imports().size(); i++) {
				ImportDeclaration id = (ImportDeclaration) cu.imports().get(i);
				//System.out.println("getFullyQualifiedName:"+id.getName());
				if (id.isStatic()) {
					if (id.getName() instanceof QualifiedName) {
    					QualifiedName name = (QualifiedName) id.getName();
    					if (id.isOnDemand()) {
    						this.importedClasses.put(name.getName().getIdentifier(), name.getFullyQualifiedName());
        				}
        				else if (id.getName().isQualifiedName()) {
        					//FIXME
        					//this.importedClasses.put(name.getName().getIdentifier(), name.getQualifier().getFullyQualifiedName());
        					this.importedClasses.put(name.getName().getIdentifier(), name.getFullyQualifiedName());
        				}
					}
				}
				else if (id.isOnDemand()) {
					this.importedPackages.add(id.getName().getFullyQualifiedName());
				}
				else if (id.getName().isQualifiedName()) {
					QualifiedName name = (QualifiedName) id.getName();
					//FIXME
					//this.importedClasses.put(name.getName().getIdentifier(), name.getQualifier().getFullyQualifiedName());
					this.importedClasses.put(name.getName().getIdentifier(), name.getFullyQualifiedName());
				}
			}
		}
	    for (int index = 0; index < cu.types().size(); ++index)
	    {
		  final AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) cu.types().get(
			   index);
		  switch (declaration.getNodeType()) {
			case ASTNode.TYPE_DECLARATION:
			{
			   final TypeDeclaration type = (TypeDeclaration) declaration;
			   this.classes.add(new CClass(this, type, null));
			   break;
			}
			case ASTNode.ENUM_DECLARATION:
			{
			   final EnumDeclaration type2 = (EnumDeclaration) declaration;
			   this.classes.add(new CClass(this, type2, null));
			   break;
			}
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
			{
			   final AnnotationTypeDeclaration type3 = (AnnotationTypeDeclaration) declaration;
			   this.classes.add(new CClass(this, type3, null));
			   break;
			}
			default:
			{
			   System.out.println("Info: Some other type declaration not implemented. "
				    + declaration.getClass().getSimpleName());
			   break;
			}
		  }
	    }
	 }
   }

   public CompilationUnit getCompileUnit()
   {
	 return this.compileUnit;
   }

   public CSystem getcSystem()
   {
	 return this.cSystem;
   }

  

  

   public HashSet<CClass> getClasses()
   {
	 return this.classes;
   }

   public CFile getMappedFile()
   {
	 return this.mappedFile;
   }

   public void setMappedFile(final CFile mappedFile)
   {
	 this.mappedFile = mappedFile;
   }

   @Override
   public CFile getCFile()
   {
	 return this;
   }

   @Override
   public CClass getCClass()
   {
	 return null;
   }

   public void buildNames()
   {
	 for (final CClass cc : this.classes)
	 {
	    cc.buildNames();
	 }
   }

   public void buildDependencies()
   {
	 for (final CClass cc : this.classes)
	 {
	    cc.buildDependencies();
	 }
   }
   
   public String getContent()
   {
	   return this.content;
   }

@Override
public String getName() {
	// TODO Auto-generated method stub
	return "";
}

@Override
public String getQualName() {
	// TODO Auto-generated method stub
	return "";
}

 
}
