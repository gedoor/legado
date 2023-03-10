/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.util.Map;

public interface Bindings
        extends Map<String, Object> {
    @Override
    public boolean containsKey(Object var1);

    @Override
    public Object get(Object var1);

    @Override
    public Object put(String var1, Object var2);

    @Override
    public void putAll(Map<? extends String, ? extends Object> var1);

    @Override
    public Object remove(Object var1);
}
