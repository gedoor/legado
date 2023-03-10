/*
 * Decompiled with CFR 0.152.
 */
package com.script;

import java.io.Reader;

public abstract class AbstractScriptEngine
        implements ScriptEngine {
    protected ScriptContext context = new SimpleScriptContext();

    public AbstractScriptEngine() {
    }

    public AbstractScriptEngine(Bindings n) {
        this();
        if (n == null) {
            throw new NullPointerException("n is null");
        }
        this.context.setBindings(n, 100);
    }

    @Override
    public void setContext(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null context");
        }
        this.context = ctxt;
    }

    @Override
    public ScriptContext getContext() {
        return this.context;
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == 200) {
            return this.context.getBindings(200);
        }
        if (scope == 100) {
            return this.context.getBindings(100);
        }
        throw new IllegalArgumentException("Invalid scope value.");
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == 200) {
            this.context.setBindings(bindings, 200);
        } else if (scope == 100) {
            this.context.setBindings(bindings, 100);
        } else {
            throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    @Override
    public void put(String key, Object value) {
        Bindings nn = this.getBindings(100);
        if (nn != null) {
            nn.put(key, value);
        }
    }

    @Override
    public Object get(String key) {
        Bindings nn = this.getBindings(100);
        if (nn != null) {
            return nn.get(key);
        }
        return null;
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return this.eval(reader, this.getScriptContext(bindings));
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        return this.eval(script, this.getScriptContext(bindings));
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return this.eval(reader, this.context);
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return this.eval(script, this.context);
    }

    protected ScriptContext getScriptContext(Bindings nn) {
        SimpleScriptContext ctxt = new SimpleScriptContext();
        Bindings gs = this.getBindings(200);
        if (gs != null) {
            ctxt.setBindings(gs, 200);
        }
        if (nn != null) {
            ctxt.setBindings(nn, 100);
            ctxt.setReader(this.context.getReader());
            ctxt.setWriter(this.context.getWriter());
            ctxt.setErrorWriter(this.context.getErrorWriter());
            return ctxt;
        }
        throw new NullPointerException("Engine scope Bindings may not be null.");
    }
}
