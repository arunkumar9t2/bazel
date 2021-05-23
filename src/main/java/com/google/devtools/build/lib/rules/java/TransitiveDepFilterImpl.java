package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.packages.AttributeMap;
import com.google.devtools.build.lib.packages.Type;

import java.util.List;
import java.util.function.Predicate;
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
        final AttributeMap attributes = ruleContext.attributes();
        if (attributes.has("tags")) {
            final List<String> tags = attributes.get("tags", Type.STRING_LIST);
            final String self = getSelfTag(tags);
            if (self != null) {
                final List<String> directTags = directTags(tags);
                final NestedSetBuilder<Artifact> filtered = new NestedSetBuilder(input.getOrder());
                final List<Artifact> filteredArtifacts = input.toList()
                        .stream()
                        .filter(artifact -> {
                            final boolean isAndroidSdk = artifact.getExecPathString().endsWith("android-ijar.jar");
                            final String targetLabel = artifact.getOwner().toString();
                            final boolean isValidTarget = !targetLabel.startsWith("//") || targetLabel.contains(self);
                            boolean isDirectDep = directTags.stream().anyMatch(targetLabel::startsWith);
                            final boolean isAllowed = isAndroidSdk || isValidTarget || isDirectDep;
                            if (!isAllowed) {
                                // System.err.println(self + ": Removed:" + artifact.getExecPathString());
                            }
                            return isAllowed;
                        }).collect(Collectors.toList());
                return filtered.addAll(filteredArtifacts).build();
            }
        }
        return input;
    }

    public Predicate<? super Artifact> filterTransitiveArtifacts() {
        return (Predicate<Artifact>) artifact -> {
            final AttributeMap attributes = ruleContext.attributes();
            if (attributes.has("tags")) {
                final List<String> tags = attributes.get("tags", Type.STRING_LIST);
                final String self = getSelfTag(tags);
                if (self != null) {
                    final List<String> directTags = directTags(tags);
                    final String targetLabel = artifact.getOwner().toString();
                    final boolean isValidTarget = !targetLabel.startsWith("//") || targetLabel.contains(self);
                    boolean isDirectDep = directTags.stream().anyMatch(targetLabel::startsWith);
                    boolean isAllowed = isDirectDep || isValidTarget;
                    if (!isAllowed) {
                        // System.err.println(self + ": Removed:" + artifact.getExecPathString());
                    }
                    return isAllowed;
                }
            }
            return true;
        };
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