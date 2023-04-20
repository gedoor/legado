package com.script

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory

open class RhinoContextFactory : ContextFactory() {

    override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
        return when (featureIndex) {
            Context.FEATURE_ENABLE_JAVA_MAP_ACCESS -> true
            else -> super.hasFeature(cx, featureIndex)
        }
    }

}