/*
 * Decompiled with CFR 0.152.
 */
package com.script;

public abstract class CompiledScript {
    public abstract Object eval(ScriptContext var1) throws ScriptException;

    public abstract ScriptEngine getEngine();

    public Object eval(Bindings bindings) throws ScriptException {
        ScriptContext ctxt = this.getEngine().getContext();
        if (bindings != null) {
            SimpleScriptContext tempctxt = new SimpleScriptContext();
            tempctxt.setBindings(bindings, 100);
            tempctxt.setBindings(ctxt.getBindings(200), 200);
            tempctxt.setWriter(ctxt.getWriter());
            tempctxt.setReader(ctxt.getReader());
            tempctxt.setErrorWriter(ctxt.getErrorWriter());
            ctxt = tempctxt;
        }
        return this.eval(ctxt);
    }

    public Object eval() throws ScriptException {
        return this.eval(this.getEngine().getContext());
    }
}
