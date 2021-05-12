package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;

public interface TransitiveDepFilter {
    NestedSet<Artifact> filter(NestedSet<Artifact> artifacts);
}