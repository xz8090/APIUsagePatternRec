package dependency;

import change.CField;
import java.util.Iterator;
import change.CMethod;
import java.util.Collection;
import change.CClass;
import java.util.HashMap;
import java.util.HashSet;

public class DataGraph
{
    private HashSet<String> typeNames;
    private HashMap<String, HashSet<CClass>> classesOfName;
    private HashMap<String, HashSet<String>> parentNamesOfTypeName;
    
    public DataGraph() {
        super();
        this.typeNames = new HashSet<String>();
        this.classesOfName = new HashMap<String, HashSet<CClass>>();
        this.parentNamesOfTypeName = new HashMap<String, HashSet<String>>();
    }
    
    public void addName(final String name) {
        this.typeNames.add(name);
    }
    
    public HashSet<String> getTypeNames() {
        return this.typeNames;
    }
    
    public void addPair(final String child, final String parent) {
        HashSet<String> names = this.parentNamesOfTypeName.get(child);
        if (names == null) {
            names = new HashSet<String>();
        }
        names.add(parent);
        this.parentNamesOfTypeName.put(child, names);
    }
    
    public void addClass(final CClass cc) {
        this.addName(cc.getSimpleName());
        HashSet<CClass> ccs = this.classesOfName.get(cc.getSimpleName());
        if (ccs == null) {
            ccs = new HashSet<CClass>();
        }
        ccs.add(cc);
        this.classesOfName.put(cc.getSimpleName(), ccs);
        HashSet<String> names = this.parentNamesOfTypeName.get(cc.getSimpleName());
        if (names == null) {
            names = new HashSet<String>();
        }
        names.addAll(cc.getSuperClassNames());
        this.parentNamesOfTypeName.put(cc.getSimpleName(), names);
    }
    
    public HashSet<CMethod> getInheritedMethods(final String className, final String methodName, final HashSet<CClass> visitedClasses) {
        final HashSet<CMethod> methods = new HashSet<CMethod>();
        if (this.classesOfName.containsKey(className)) {
            for (final CClass cc : this.classesOfName.get(className)) {
                if (!visitedClasses.contains(cc)) {
                    visitedClasses.add(cc);
                    final HashSet<CMethod> method = cc.getInheritedMethods(methodName, visitedClasses);
                    if (!method.isEmpty()) {
                        methods.addAll(method);
                    }
                    visitedClasses.remove(cc);
                }
            }
        }
        return methods;
    }
    
    public HashSet<CField> getFields(final String className, final String fieldName, final HashSet<CClass> visitedClasses) {
        final HashSet<CField> fs = new HashSet<CField>();
        if (this.classesOfName.containsKey(className)) {
            for (final CClass cc : this.classesOfName.get(className)) {
                if (!visitedClasses.contains(cc)) {
                    visitedClasses.add(cc);
                    final HashSet<CField> fields = cc.getFields(fieldName, visitedClasses);
                    fs.addAll(fields);
                    visitedClasses.remove(cc);
                }
            }
        }
        return fs;
    }
}
