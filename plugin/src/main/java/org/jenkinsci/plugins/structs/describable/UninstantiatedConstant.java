/*
 * The MIT License
 *
 * Copyright (c) 2016 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.structs.describable;

import java.io.Serializable;

import org.jenkinsci.plugins.structs.SymbolLookup;

import groovy.lang.MissingPropertyException;

/**
 * Wraps a constant value to evaluate later
 *
 * @since TODO
 */
public class UninstantiatedConstant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * ctor
     *
     * @param name name of the constant value
     */
    public UninstantiatedConstant(String name) {
        this.name = name;
    }

    /**
     * @return name of the constant value
     */
    public String getName() {
        return name;
    }

    /**
     * Return a constant value determined in the current context
     *
     * @param base class determined in the current context
     * @param <T> class determined in the current context
     * @return a constant value
     * @throws MissingPropertyException when no appropreate constant value if found
     */
    public <T> T instantiate(Class<T> base) throws MissingPropertyException {
        T candidate = SymbolLookup.get().findConst(base, getName());
        if (candidate != null) {
            return candidate;
        }

        if (base.isEnum()) {
            for (T enumValue: base.getEnumConstants()) {
                if (((Enum<?>)enumValue).name().equals(getName())) {
                    return enumValue;
                }
            }
        }

        throw new MissingPropertyException(String.format(
            "No such property: %s",
            getName()
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UninstantiatedConstant that = (UninstantiatedConstant) o;

        if ((name != null) ? !name.equals(that.name) : (that.name != null)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (name != null) ? name.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("UninstantiatedConstant(%s)", getName());
   }
}
