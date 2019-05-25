package com.jayway.jsonpath.internal.path;

public interface PathTokenAppender {
    PathTokenAppender appendPathToken(PathToken next);
}
