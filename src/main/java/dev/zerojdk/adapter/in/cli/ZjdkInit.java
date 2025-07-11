package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.event.CompositeConsoleEventHandler;
import dev.zerojdk.adapter.in.cli.event.JdkDownloadProgressPrinter;
import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.adapter.out.config.ConfigFileAlreadyExistsException;
import dev.zerojdk.adapter.out.event.InMemoryDomainEventPublisher;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.model.context.LayoutContext;
import dev.zerojdk.domain.model.context.LocalLayoutContext;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.config.JdkConfigService;
import dev.zerojdk.domain.service.sync.ManifestSyncService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;

@RequiredArgsConstructor
@CommandLine.Command(
    header = "Initialize a project or global Zero-JDK config",
    description = """
        %n  Initializes a .zjdk directory in the current working directory, marking it
          as a Zero-JDK managed project. This configuration defines the required JDK
          version independently of any system-wide setup.
        
          By default, the configuration is created in the current directory. If the
          '--global' option is specified, or if the command is executed from the user's
          home directory, a global configuration is created instead.
        
          The '--version' option allows explicitly setting the desired JDK version.
          If no version is provided, the latest long-term support (LTS) release of
          Temurin from the local catalog is used.
        
          The list of available versions can be retrieved using the 'list available'
          command. To update the catalog used during initialization, the user can run
          'zjdk update'.
        
          If a configuration already exists in the target location, the command fails
          without making changes.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  ",
    sortOptions = false
)
public class ZjdkInit implements Runnable {
    private final PlatformDetection platformDetection;
    private final JdkConfigService jdkConfigService;
    private final ManifestSyncService manifestSyncService;
    private final InMemoryDomainEventPublisher eventPublisher;

    @CommandLine.Option(names = {"--version"}, description = "Initialize with this JDK version")
    private String version;

    @CommandLine.Option(names = {"-g", "--global"}, description = "Initialize globally")
    private boolean global;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @Override
    public void run() {
        new CompositeConsoleEventHandler(
            new JdkDownloadProgressPrinter()).register(eventPublisher);

        Platform platform = platformDetection.detect();

        LayoutContext context = global
            ? LayoutContexts.global()
            : LayoutContexts.current();

        try {
            jdkConfigService.createConfiguration(platform, version, context);

            if (LayoutContexts.isGlobalContext(context)) {
                System.out.println(
                    """
                    Global configuration initialized at $HOME/.zjdk
    
                    This acts as a global config across all directories unless overridden locally.
                    Use --global to reflect this intention explicitly.
                    """);
            } else {
                System.out.println("""
                    Local configuration initialized
                    """);
            }
        } catch (ConfigFileAlreadyExistsException e) {
            System.err.println("Already initialized. Run 'set version' to modify.");
            return;
        }

        manifestSyncService.sync(platform, context);
    }
}
