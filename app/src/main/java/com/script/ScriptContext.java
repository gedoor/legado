/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

public interface ScriptContext {
    public static final int ENGINE_SCOPE = 100;
    public static final int GLOBAL_SCOPE = 200;

    public Object getAttribute(String var1);

    public Object getAttribute(String var1, int var2);

    public int getAttributesScope(String var1);

    public Bindings getBindings(int var1);

    public Writer getErrorWriter();

    public Reader getReader();

    public List<Integer> getScopes();

    public Writer getWriter();

    public Object removeAttribute(String var1, int var2);

    public void setAttribute(String var1, Object var2, int var3);

    public void setBindings(Bindings var1, int var2);

    public void setErrorWriter(Writer var1);

    public void setReader(Reader var1);

    public void setWriter(Writer var1);
}
