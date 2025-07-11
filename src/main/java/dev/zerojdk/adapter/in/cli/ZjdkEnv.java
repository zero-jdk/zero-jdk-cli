package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.domain.port.out.layout.UnmanagedDirectoryException;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.config.JdkConfigService;
import dev.zerojdk.domain.service.release.JdkReleaseService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
@CommandLine.Command(
    header = "Print environment variables for the active JDK",
    description = """
          %n  Displays the environment configuration for the active Zero-JDK setup.

          This command resolves the applicable .zjdk configuration (either from the
          current directory or from the global configuration) and prints the relevant
          environment variables required to use the configured JDK.

          The output includes values such as JAVA_HOME and an updated PATH pointing to
          the correct JDK installation. It can be used to inspect or export environment
          settings manually or within scripts.

          If no configuration is found, the command fails with an appropriate error.

          Example usage in a shell:
            eval "$(zjdk env)"
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
public class ZjdkEnv implements Runnable {
    private final PlatformDetection platformDetection;
    private final JdkConfigService jdkConfigService;
    private final JdkReleaseService jdkReleaseService;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @Override
    public void run() {
        Platform platform = platformDetection.detect();
        String version = getActiveVersion();

        jdkReleaseService.findJdkRelease(platform, version).ifPresent(release -> {
            System.out.printf("export JAVA_HOME=\"%s\"\n", release.javaHome());
            System.out.println("export PATH=\"$JAVA_HOME/bin:$PATH\"");
        });
    }

    private String getActiveVersion() {
        try {
            return jdkConfigService.getActiveVersion(LayoutContexts.current());
        } catch (UnmanagedDirectoryException e) {
            return jdkConfigService.getActiveVersion(LayoutContexts.global());
        }
    }
}
