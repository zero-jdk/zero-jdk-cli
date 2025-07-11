package dev.zerojdk.adapter.in.cli.renderer;

import dev.zerojdk.domain.model.JdkVersion;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class JdkVersionRenderer {
    public void render(List<JdkVersion> versions, int indent, Comparator<JdkVersion> ordering) {
        String whitespace = " ".repeat(indent);

        mergeVariants(versions).stream()
            .sorted(ordering)
            .forEach(v -> printJdkVersion(whitespace, v));
    }

    public void render(String ws, JdkVersion v) {
        System.out.printf("%sVersion:       %s (%d - %s)%n", ws, v.getDistributionVersion(), v.getMajorVersion(), v.getJavaVersion());
        System.out.printf("%sIdentifier(s): %s%n", ws, v.getIdentifier());
        System.out.printf("%sSupport:       %s%n", ws, v.getSupport() == JdkVersion.Support.LTS ? "LTS" : "Non-LTS");
        System.out.printf("%sLink:          %s%n", ws, v.getLink());
    }

    private void printJdkVersion(String ws, JdkVersion v) {
        render(ws, v);
        System.out.println();
    }

    private List<JdkVersion> mergeVariants(List<JdkVersion> versions) {
        return versions.stream()
            .collect(Collectors.groupingBy(JdkVersion::getDistributionVersion))
            .values().stream()
            .map(this::mergeGroup)
            .toList();
    }

    private JdkVersion mergeGroup(List<JdkVersion> group) {
        JdkVersion first = group.getFirst();
        JdkVersion result = new JdkVersion();

        result.setDistribution(first.getDistribution());
        result.setDistributionVersion(first.getDistributionVersion());
        result.setJavaVersion(first.getJavaVersion());
        result.setMajorVersion(first.getMajorVersion());
        result.setPlatform(first.getPlatform());
        result.setSupport(first.getSupport());
        result.setLink(first.getLink());
        result.setIdentifier(group.stream()
            .map(JdkVersion::getIdentifier)
            .collect(Collectors.joining(" ")));

        return result;
    }
}
