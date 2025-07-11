package dev.zerojdk.adapter.in.cli.renderer;

import dev.zerojdk.domain.model.release.JdkRelease;

import java.util.Comparator;
import java.util.List;

public class JdkReleaseRenderer {
    private final JdkVersionRenderer  jdkVersionRenderer = new  JdkVersionRenderer();

    public void render(List<JdkRelease> versions, int indent, Comparator<JdkRelease> ordering) {
        String whitespace = " ".repeat(indent);

        versions.stream()
            .sorted(ordering)
            .forEach(v -> render(whitespace, v));
    }

    public void render(String ws, JdkRelease r) {
        jdkVersionRenderer.render(ws, r.jdkVersion());

        System.out.printf("%sJava Home:     %s%n", ws, r.javaHome());
        System.out.println();
    }
}
