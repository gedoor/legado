/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleScriptContext
        implements ScriptContext {
    private static List<Integer> scopes = new ArrayList<Integer>(2);
    protected Bindings engineScope = new SimpleBindings();
    protected Writer errorWriter = new PrintWriter(System.err, true);
    protected Bindings globalScope = null;
    protected Reader reader = new InputStreamReader(System.in);
    protected Writer writer = new PrintWriter(System.out, true);

    @Override
    public void setBindings(Bindings bindings, int scope) {
        switch (scope) {
            case 100: {
                if (bindings == null) {
                    throw new NullPointerException("Engine scope cannot be null.");
                }
                this.engineScope = bindings;
                return;
            }
            case 200: {
                this.globalScope = bindings;
                return;
            }
        }
        throw new IllegalArgumentException("Invalid scope value.");
    }

    @Override
    public Object getAttribute(String name) {
        if (this.engineScope.containsKey(name)) {
            return this.getAttribute(name, 100);
        }
        if (this.globalScope == null || !this.globalScope.containsKey(name)) {
            return null;
        }
        return this.getAttribute(name, 200);
    }

    @Override
    public Object getAttribute(String name, int scope) {
        switch (scope) {
            case 100: {
                return this.engineScope.get(name);
            }
            case 200: {
                if (this.globalScope != null) {
                    return this.globalScope.get(name);
                }
                return null;
            }
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        switch (scope) {
            case 100: {
                if (this.getBindings(100) != null) {
                    return this.getBindings(100).remove(name);
                }
                return null;
            }
            case 200: {
                if (this.getBindings(200) != null) {
                    return this.getBindings(200).remove(name);
                }
                return null;
            }
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        switch (scope) {
            case 100: {
                this.engineScope.put(name, value);
                return;
            }
            case 200: {
                if (this.globalScope != null) {
                    this.globalScope.put(name, value);
                    return;
                }
                return;
            }
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    @Override
    public Writer getWriter() {
        return this.writer;
    }

    @Override
    public Reader getReader() {
        return this.reader;
    }

    @Override
    public void setReader(Reader reader2) {
        this.reader = reader2;
    }

    @Override
    public void setWriter(Writer writer2) {
        this.writer = writer2;
    }

    @Override
    public Writer getErrorWriter() {
        return this.errorWriter;
    }

    @Override
    public void setErrorWriter(Writer writer2) {
        this.errorWriter = writer2;
    }

    @Override
    public int getAttributesScope(String name) {
        if (this.engineScope.containsKey(name)) {
            return 100;
        }
        if (this.globalScope == null || !this.globalScope.containsKey(name)) {
            return -1;
        }
        return 200;
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == 100) {
            return this.engineScope;
        }
        if (scope == 200) {
            return this.globalScope;
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    @Override
    public List<Integer> getScopes() {
        return scopes;
    }

    static {
        scopes.add(100);
        scopes.add(200);
        scopes = Collections.unmodifiableList(scopes);
    }
}
