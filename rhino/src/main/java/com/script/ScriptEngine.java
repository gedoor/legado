/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.io.Reader;

public interface ScriptEngine {
    public static final String FILENAME = "javax.script.filename";

    public Bindings createBindings();

    public Object eval(Reader var1) throws ScriptException;

    public Object eval(Reader var1, Bindings var2) throws ScriptException;

    public Object eval(Reader var1, ScriptContext var2) throws ScriptException;

    public Object eval(String var1) throws ScriptException;

    public Object eval(String var1, Bindings var2) throws ScriptException;

    public Object eval(String var1, ScriptContext var2) throws ScriptException;

    public Object get(String var1);

    public Bindings getBindings(int var1);

    public ScriptContext getContext();

    public void put(String var1, Object var2);

    public void setBindings(Bindings var1, int var2);

    public void setContext(ScriptContext var1);
}
