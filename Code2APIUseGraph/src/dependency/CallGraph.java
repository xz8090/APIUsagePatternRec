package dependency;

import java.util.Collection;
import java.util.Iterator;
import change.CMethod;
import change.ChangeEntity;
import java.util.HashMap;
import java.util.HashSet;

public class CallGraph
{
    private static final HashSet<String> commonNames;
    private HashMap<String, HashSet<ChangeEntity>> callersOfMethodName;
    private HashMap<CMethod, HashSet<ChangeEntity>> callersOfMethod;
    
    static {
        (commonNames = new HashSet<String>()).add("toString(0)");
        CallGraph.commonNames.add("clone(0)");
        CallGraph.commonNames.add("equals(1)");
        CallGraph.commonNames.add("finalize(0)");
        CallGraph.commonNames.add("hashCode(0)");
    }
    
    public CallGraph() {
        super();
        this.callersOfMethodName = new HashMap<String, HashSet<ChangeEntity>>();
        this.callersOfMethod = new HashMap<CMethod, HashSet<ChangeEntity>>();
    }
    
    public void addMethod(final CMethod cMethod) {
        if (!this.isCommonName(cMethod.getName()) && !this.callersOfMethodName.containsKey(cMethod.getName())) {
            this.callersOfMethodName.put(cMethod.getName(), new HashSet<ChangeEntity>());
        }
        if (!this.isCommonName(cMethod.getName()) && !this.callersOfMethod.containsKey(cMethod)) {
            this.callersOfMethod.put(cMethod, new HashSet<ChangeEntity>());
        }
    }
    
    public HashMap<String, HashSet<ChangeEntity>> getCallersOfMethodName() {
        return this.callersOfMethodName;
    }
    
    public HashMap<CMethod, HashSet<ChangeEntity>> getCallersOfMethod() {
        return this.callersOfMethod;
    }
    
    private boolean isCommonName(final String name) {
        return CallGraph.commonNames.contains(name);
    }
    
    public void addCall(final ChangeEntity caller, final String callee) {
        if (this.callersOfMethodName.containsKey(callee)) {
            final HashSet<ChangeEntity> callers = this.callersOfMethodName.get(callee);
            callers.add(caller);
            this.callersOfMethodName.put(callee, callers);
        }
    }
    
    public void addCall(final ChangeEntity caller, final HashSet<CMethod> callees) {
        for (final CMethod callee : callees) {
            final HashSet<ChangeEntity> callers = this.callersOfMethod.get(callee);
            if (callers != null) {
                callers.add(caller);
                this.callersOfMethod.put(callee, callers);
            }
        }
    }
    
    public boolean hasCallDependency(final ChangeEntity ce1, final ChangeEntity ce2) {
        if (!this.callersOfMethodName.containsKey(ce1.getName()) || !this.callersOfMethodName.containsKey(ce2.getName())) {
            return false;
        }
        if (this.callersOfMethodName.get(ce1.getName()).contains(ce2) || this.callersOfMethodName.get(ce2.getName()).contains(ce1)) {
            return true;
        }
        final HashSet<ChangeEntity> names = new HashSet<ChangeEntity>(this.callersOfMethodName.get(ce1.getName()));
        names.retainAll(this.callersOfMethodName.get(ce2.getName()));
        if (!names.isEmpty()) {
            return true;
        }
        if (ce1 instanceof CMethod && ce2 instanceof CMethod) {
            final HashSet<CMethod> ies1 = ((CMethod)ce1).getInheritedMethods();
            final HashSet<CMethod> ies2 = ((CMethod)ce2).getInheritedMethods();
            final HashSet<ChangeEntity> callers1 = new HashSet<ChangeEntity>();
            final HashSet<ChangeEntity> callers2 = new HashSet<ChangeEntity>();
            for (final CMethod ie1 : ies1) {
                if (this.callersOfMethod.get(ie1).contains(ce2)) {
                    return true;
                }
                callers1.addAll(callersOfMethod.get(ie1));
            }
            for (final CMethod ie2 : ies2) {
                if (this.callersOfMethod.get(ie2).contains(ce1)) {
                    return true;
                }
                callers2.addAll(callersOfMethod.get(ie2));
            }
        }
        else if (ce1 instanceof CMethod) {
            final HashSet<CMethod> ies1 = ((CMethod)ce1).getInheritedMethods();
            for (final CMethod ie3 : ies1) {
                if (this.callersOfMethod.get(ie3).contains(ce2)) {
                    return true;
                }
            }
        }
        else if (ce2 instanceof CMethod) {
            final HashSet<CMethod> ies3 = ((CMethod)ce2).getInheritedMethods();
            for (final CMethod ie4 : ies3) {
                if (this.callersOfMethod.get(ie4).contains(ce1)) {
                    return true;
                }
            }
        }
        else {
            System.err.println("ERROR: cannot be here in checking call dependency!");
            if (this.callersOfMethod.get(ce1).contains(ce2) || this.callersOfMethod.get(ce2).contains(ce1)) {
                return true;
            }
            final HashSet<ChangeEntity> callers3 = new HashSet<ChangeEntity>(this.callersOfMethod.get(ce1));
            callers3.retainAll(this.callersOfMethod.get(ce2));
            if (!callers3.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public void refine() {
        for (final String name : this.callersOfMethodName.keySet()) {
            if (!this.callersOfMethodName.get(name).isEmpty()) {
                for (final CMethod cm : this.callersOfMethod.keySet()) {
                    if (cm.getName().equals(name)) {
                        this.callersOfMethodName.get(name).addAll(callersOfMethod.get(cm));
                    }
                }
            }
        }
    }
}
