package org.seimicrawler.xpath.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.exception.XpathParserException;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/13.
 */
public class Scope {
    private Elements context;
    private boolean isRecursion = false;
    private Scope parent;

    private Scope(Elements context) {
        super();
        this.context = new Elements();
        this.context.addAll(context);
    }

    private Scope(Element context) {
        super();
        this.context = new Elements();
        this.context.add(context);
    }

    public static Scope create(Elements elements) {
        return new Scope(elements);
    }

    public static Scope create(Element el) {
        return new Scope(el);
    }

    public static Scope create(Scope scope) {
        return new Scope(scope.context()).setParent(scope);
    }

    public Scope setParent(Scope scope) {
        this.parent = scope;
        return this;
    }

    public Scope getParent() {
        return parent;
    }

    public void setContext(Elements context) {
        this.context = context;
    }

    public boolean isRecursion() {
        return isRecursion;
    }

    void recursion() {
        this.isRecursion = true;
    }

    public void notRecursion() {
        this.isRecursion = false;
    }

    public Elements context() {
        return this.context;
    }

    public Element singleEl() {
        if (context.size() == 1) {
            return context.first();
        }
        throw new XpathParserException("current context is more than one el,total = " + context.size());
    }
}

