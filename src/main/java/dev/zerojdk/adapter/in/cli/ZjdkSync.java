package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.event.CompositeConsoleEventHandler;
import dev.zerojdk.adapter.in.cli.event.JdkDownloadProgressPrinter;
import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.adapter.out.event.InMemoryDomainEventPublisher;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.model.context.LayoutContext;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.sync.ManifestSyncService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
@CommandLine.Command(
    header = "Download and prepare the JDK defined in the configuration",
    description = """
          %n  Ensures that the JDK version declared in the active Zero-JDK configuration
          is downloaded and installed for the current platform.
        
          By default, the command operates on the local .zjdk configuration in the
          current directory. Use '--global' to synchronize the global configuration.
        
          If the JDK version is already installed, no action is taken. If it is
          missing, the release is downloaded and extracted into the appropriate
          location.
        
          This command is typically used after cloning a project that includes a
          Zero-JDK configuration, to ensure the required JDK version is available
          on the system.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
public class ZjdkSync implements Runnable {
    private final PlatformDetection platformDetection;
    private final ManifestSyncService manifestSyncService;
    private final InMemoryDomainEventPublisher eventPublisher;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @CommandLine.Option(names = {"-g", "--global"}, description = "Sync globally")
    private boolean global;

    @Override
    public void run() {
        new CompositeConsoleEventHandler(
            new JdkDownloadProgressPrinter()).register(eventPublisher);

        Platform platform = platformDetection.detect();

        LayoutContext context = global
            ? LayoutContexts.global()
            : LayoutContexts.current();

        manifestSyncService.sync(platform, context);

        if (LayoutContexts.isGlobalContext(context)) {
            System.out.println(
                """
                Global configuration synchronized

                This acts as a global config across all directories unless overridden locally.
                Use --global to reflect this intention explicitly.
                """);
        } else {
            System.out.println("Local configuration synchronized.");
        }
    }
}
