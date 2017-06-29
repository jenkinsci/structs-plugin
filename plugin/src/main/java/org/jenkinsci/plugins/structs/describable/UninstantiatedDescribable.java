package org.jenkinsci.plugins.structs.describable;

import hudson.model.Describable;
import org.jenkinsci.Symbol;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.CheckForNull;

/**
 * A reflection-like view of a particular {@link Describable} instance.
 *
 * @author Kohsuke Kawaguchi
 */
public class UninstantiatedDescribable implements Serializable {
    private String symbol;
    private String klass;
    private final Map<String,?> arguments;
    private DescribableModel model;

    public UninstantiatedDescribable(String symbol, String klass, Map<String, ?> arguments) {
        this.symbol = symbol;
        this.klass = klass;
        this.arguments = arguments;
    }

    public UninstantiatedDescribable(Map<String, ?> arguments) {
        this(null,null,arguments);
    }

    /**
     * If this nested describable has a suitable {@linkplain Symbol symbol name},
     * this method returns one.
     *
     * <p>
     * Either this or {@link #getKlass()} has to return a non-null value.
     */
    public @Nullable String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * {@value DescribableModel#CLAZZ} is an alternative means to specify the class in case there's no symbol.
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
     * Returns the model associated with this object.
     *
     * If this object was created from a model (via {@link #from(Object)}) this method
     * returns that model.
     */
    public @CheckForNull DescribableModel getModel() {
        return model;
    }

    public void setModel(DescribableModel model) {
        this.model = model;
    }

    /**
     * All the nested arguments to this object.
     */
    public Map<String, ?> getArguments() {
        return arguments;
    }

    /**
     * Returns true if and only if the arguments is one and that is the only required parameter
     * from the model.
     *
     * <p>
     * This usually signals a short-hand syntax to write down the instantiation syntax.
     */
    public boolean hasSoleRequiredArgument() {
        if (arguments.size()!=1)    return false;
        if (model==null)    return false;

        DescribableParameter p = model.getSoleRequiredParameter();
        if (p==null)        return false;
        return arguments.containsKey(p.getName());
    }

    /**
     * For legacy use, we need to blow up this into a map form.
     * This requires recursively blowing up any nested {@link UninstantiatedDescribable}s.
     */
    public Map<String,Object> toMap() {
        Map<String,Object> r = toShallowMap();
        for (Entry<String,?> e : arguments.entrySet()) {
            Object v = e.getValue();
            // see DescribableParameter.uncoerce for possible variety
            v = toMap(v);
            if (v instanceof List) {
                List l = new ArrayList();
                for (Object o : (List) v) {
                    l.add(toMap(o));
                }
                v = l;
            }
            r.put(e.getKey(),v);
        }
        return r;
    }

    /**
     * Converts this {@link UninstantiatedDescribable} to a literal map expression without recursively doing so for children.
     */
    public Map<String,Object> toShallowMap() {
        Map<String,Object> r = new TreeMap<String, Object>(arguments);
        if (klass !=null)
            r.put(DescribableModel.CLAZZ, klass);
// there's no use writing both $class and $symbol. $symbol is little more readable, but given that this is already
// a fallback behaviour, let's not complicate things by adding yet another way to instantiate a Describable
//        if (symbol!=null)
//            r.put(DescribableModel.SYMBOL,symbol);
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

    /**
     * Instantiates an actual {@link Describable} through {@linkplain #getModel() the model},
     * unless {@link #klass} or {@link #symbol} will be set to specify a specific type, in which
     * case that takes a precedence.
     */
    public Object instantiate() throws Exception {
        DescribableModel m = getModel();
        return instantiate(m!=null ? m.getType() : Object.class);
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
        for (Entry<String,?> e : arguments.entrySet()) {
            if (first)  first = false;
            else        b.append(',');
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        b.append(')');
        return b.toString();
   }

    private static final long serialVersionUID = 1L;

    /**
     * As a short-hand, if a {@link DescribableModel} has only one required parameter,
     * {@link #instantiate(Class)} accepts a single-item map whose key is this magic token.
     *
     * <p>
     * To avoid clients from needing to special-case this key, {@link #from(Object)} does not
     * produce {@link #arguments} that contains this magic token. Clients who want
     * to take advantages of this should look at {@link DescribableModel#hasSingleRequiredParameter()}
     */
    public static final String ANONYMOUS_KEY = "<anonymous>";
}
