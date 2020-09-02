package utils;

public class Pair
{
    private Object obj1;
    private Object obj2;
    private double weight;
    
    public Pair(final Object obj1, final Object obj2) {
        super();
        this.weight = -1.0;
        this.obj1 = obj1;
        this.obj2 = obj2;
    }
    
    public Pair(final Object obj1, final Object obj2, final double weight) {
        super();
        this.weight = -1.0;
        this.obj1 = obj1;
        this.obj2 = obj2;
        this.weight = weight;
    }
    
    public double computeWeight(final Pair other) {
        return this.weight;
    }
    
    public int compareTo(final Pair other) {
        final double otherWeight = other.getWeight();
        if (this.weight > otherWeight) {
            return 1;
        }
        if (this.weight == otherWeight) {
            return 0;
        }
        return -1;
    }
    
    public Object getObj1() {
        return this.obj1;
    }
    
    public void setObj1(final Object obj1) {
        this.obj1 = obj1;
    }
    
    public Object getObj2() {
        return this.obj2;
    }
    
    public void setObj2(final Object obj2) {
        this.obj2 = obj2;
    }
    
    public double getWeight() {
        return this.weight;
    }
    
    public void setWeight(final double weight) {
        this.weight = weight;
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.weight);
    }
}
