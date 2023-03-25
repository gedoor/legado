/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.io.Reader;

@SuppressWarnings("UnnecessaryModifier")
public interface Compilable {
    public CompiledScript compile(Reader var1) throws ScriptException;

    public CompiledScript compile(String var1) throws ScriptException;
}
