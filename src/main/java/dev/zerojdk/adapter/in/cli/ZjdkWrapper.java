package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.wrapper.WrapperInstaller;
import dev.zerojdk.infrastructure.VersionProvider;
import lombok.RequiredArgsConstructor;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ProcessProperties;
import picocli.CommandLine;

import java.nio.file.Path;

@RequiredArgsConstructor
@CommandLine.Command(
    header = "Generate a wrapper script to bootstrap zjdk automatically",
    description = """
          %n  Creates a wrapper script that bootstraps zjdk automatically when a project
          is executed on a system where zjdk is not yet installed.
        
          This script can be checked into version control alongside the project to
          ensure portability and consistent behavior across environments.
        
          The wrapper downloads and initializes zjdk as needed, using the project's
          .zjdk configuration.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
public class ZjdkWrapper implements Runnable {
    private final PlatformDetection platformDetection;
    private final WrapperInstaller wrapperInstaller;
    private final VersionProvider versionProvider;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @Override
    public void run() {
         if (!ImageInfo.inImageRuntimeCode()) {
             // TODO: proper exception
            throw new RuntimeException("Runtime error. Not using Graal?");
         }

        wrapperInstaller.install(
            platformDetection.detect(),
            versionProvider.getVersion(),
            Path.of(ProcessProperties.getExecutableName()),
            LayoutContexts.current());
    }
}
