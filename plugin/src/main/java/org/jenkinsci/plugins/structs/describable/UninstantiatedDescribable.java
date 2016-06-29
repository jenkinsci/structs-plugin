package org.jenkinsci.plugins.structs.describable;

import hudson.model.Describable;
import org.jenkinsci.Symbol;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A reflection-like view of a particular {@link Describable} instance.
 *
 * @author Kohsuke Kawaguchi
 */
public class UninstantiatedDescribable {
    private String symbol;
    private String klass;
    private final Map<String,Object> arguments;

    public UninstantiatedDescribable(String symbol, String klass, Map<String, Object> arguments) {
        this.symbol = symbol;
        this.klass = klass;
        this.arguments = arguments;
    }

    public UninstantiatedDescribable(Map<String, Object> arguments) {
        this(null,null,arguments);
    }

    /**
     * If this nested describable has a suitable {@linkplain Symbol symbol name},
     * this method returns one.
     */
    public @Nullable String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * {@code $class} is an alternative means to specify the class in case there's no symbol.
     * Can be a short name if it's contextually unambiguous, or a FQCN.
     *
     * <p>
     * Either this or {@link #getSymbol()} has to return a non-null value.
     */
    public @Nullable String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    /**
     * All the nested arguments to this object.
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * For legacy use, we need to blow up this into a map form.
     * This requires recursively blowing up any nested {@link UninstantiatedDescribable}s.
     */
    /*package*/ Map<String,Object> toMap() {
        Map<String,Object> r = new TreeMap<String, Object>();
        for (Entry<String,Object> e : arguments.entrySet()) {
            Object v = e.getValue();
            // see DescribableParameter.uncoerce for possible variety
            v = toMap(v);
            if (v instanceof List) {
                ListIterator litr = ((List) v).listIterator();
                while (litr.hasNext()) {
                    litr.set(toMap(litr.next()));
                }
            }
            r.put(e.getKey(),v);
        }
        if (klass !=null)
            r.put(DescribableModel.CLAZZ, klass);
        if (symbol!=null)
            r.put(DescribableModel.SYMBOL,symbol);
        return r;
    }

    private static Object toMap(Object v) {
        if (v instanceof UninstantiatedDescribable) {
            UninstantiatedDescribable ud = (UninstantiatedDescribable) v;
            return ud.toMap();
        } else {
            return v;
        }
    }

    public Object instantiate() throws Exception {
        return instantiate(Object.class);
    }

    /**
     * Instantiates an actual {@link Describable} object from the specified arguments.
     *
     * @param base
     *      The expected type of the instance. The interpretation of the symbol and $class
     *      depends on this parameter.
     */
    public <T> T instantiate(Class<T> base) throws Exception {
        Class<?> c = DescribableModel.resolveClass(base, klass, symbol);
        return base.cast(new DescribableModel(c).instantiate(arguments));
    }

    public static UninstantiatedDescribable from(Object o) {
        return DescribableModel.uninstantiate2_(o);
    }

    // equals & hashCode needed for DescribableModel.uninstantiate2
    // to find values that can be omitted because it's the default value
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UninstantiatedDescribable that = (UninstantiatedDescribable) o;

        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        if (klass != null ? !klass.equals(that.klass) : that.klass != null) return false;
        return arguments.equals(that.arguments);

    }

    @Override
    public int hashCode() {
        int result = symbol != null ? symbol.hashCode() : 0;
        result = 31 * result + (klass != null ? klass.hashCode() : 0);
        result = 31 * result + arguments.hashCode();
        return result;
    }

    /**
     * Debug assistance. The output might change.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (symbol!=null)
            b.append('@').append(symbol);
        if (klass!=null)
            b.append('$').append(klass);
        b.append('(');
        boolean first = true;
        for (Entry<String,Object> e : arguments.entrySet()) {
            if (first)  first = false;
            else        b.append(',');
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        b.append(')');
        return b.toString();
   }
}
