package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.adapter.in.cli.renderer.JdkVersionRenderer;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.domain.port.out.layout.UnmanagedDirectoryException;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.catalog.CatalogService;
import dev.zerojdk.domain.service.config.JdkConfigService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
@CommandLine.Command(
    header = "Print information about the currently active JDK",
    description = """
          %n  Displays metadata about the JDK version declared in the active Zero-JDK
          configuration.
        
          This command resolves the configuration from the current directory or, if
          not found, from the global configuration. If the declared JDK version is
          found in the local catalog and installed, relevant metadata such as the
          version number, identifiers, support type (e.g. LTS), and vendor link is
          displayed.
        
          If the JDK is not installed or the configured version is unknown to the
          catalog, no information is shown.
        
          This command is useful for inspecting what version is currently configured
          and verifying its catalog metadata.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
public class ZjdkInfo implements Runnable {
    private final PlatformDetection platformDetection;
    private final JdkConfigService jdkConfigService;
    private final CatalogService catalogService;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @Override
    public void run() {
        Platform platform = platformDetection.detect();
        String version = getActiveVersion();

        JdkVersionRenderer jdkVersionRenderer = new JdkVersionRenderer();

        catalogService.findByIdentifier(platform, version)
            .ifPresent(release -> jdkVersionRenderer.render("", release));
    }

    private String getActiveVersion() {
        try {
            return jdkConfigService.getActiveVersion(LayoutContexts.current());
        } catch (UnmanagedDirectoryException e) {
            return jdkConfigService.getActiveVersion(LayoutContexts.global());
        }
    }
}
