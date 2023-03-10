/*
 * Decompiled with CFR 0.152.
 */
package com.script;

public interface Invocable {
    public <T> T getInterface(Class<T> var1);

    public <T> T getInterface(Object var1, Class<T> var2);

    public Object invokeFunction(String var1, Object ... var2) throws ScriptException, NoSuchMethodException;

    public Object invokeMethod(Object var1, String var2, Object ... var3) throws ScriptException, NoSuchMethodException;
}
