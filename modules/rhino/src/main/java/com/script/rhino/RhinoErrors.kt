package com.script.rhino

class RhinoInterruptError(override val cause: Throwable) : Error()

class RhinoRecursionError(): Error("Maximum recursion depth exceeded.")
