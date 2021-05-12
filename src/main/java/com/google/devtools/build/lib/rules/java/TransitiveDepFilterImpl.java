package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.packages.AttributeMap;
import com.google.devtools.build.lib.packages.Type;

import java.util.List;
import java.util.stream.Collectors;

public class TransitiveDepFilterImpl implements TransitiveDepFilter {
    private final RuleContext ruleContext;

    public TransitiveDepFilterImpl(RuleContext context) {
        ruleContext = context;
    }

    @Override
    public NestedSet<Artifact> filter(NestedSet<Artifact> artifacts) {
        return filterTransDeps(artifacts);
    }

    private NestedSet<Artifact> filterTransDeps(NestedSet<Artifact> input) {
        AttributeMap attributes = ruleContext.attributes();
        if (attributes.has("tags")) {
            List<String> tags = attributes.get("tags", Type.STRING_LIST);
            String self = getSelfTag(tags);
            if (self != null) {
                // filter here
                tags = directTags(tags);
                NestedSetBuilder<Artifact> filtered = new NestedSetBuilder(input.getOrder());
                for (Artifact item : input.toList()) {
                    String owner = item.getOwner().toString();
                    if (!owner.startsWith("//") || owner.contains(self)) {
                        filtered.add(item);
                        continue;
                    }
                    for (String tag : tags) {
                        if (owner.startsWith(tag)) {
                            filtered.add(item);
                        } else {
                        }
                    }
                }
                return filtered.build();
            }
        }
        return input;
    }

    private List<String> directTags(List<String> tags) {
        return tags.stream()
                .filter(s -> s.startsWith("@direct//"))
                .map(s -> s.substring(7) + ":")
                .collect(Collectors.toList());
    }

    private String getSelfTag(List<String> tags) {
        for (String tag : tags) {
            if (tag.startsWith("@self//")) return tag.substring(6) + ":";
        }
        return null;
    }
}