package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.event.CompositeConsoleEventHandler;
import dev.zerojdk.adapter.in.cli.event.JdkDownloadProgressPrinter;
import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.model.context.LayoutContexts;
import dev.zerojdk.adapter.out.event.InMemoryDomainEventPublisher;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.model.context.LayoutContext;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.config.JdkConfigService;
import dev.zerojdk.domain.service.sync.ManifestSyncService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@CommandLine.Command(
    header = "Update JDK-related settings",
    description = """
          %n  Provides commands for updating parts of the Zero-JDK configuration.
        
          Use the 'version' subcommand to change the configured JDK version
          in either the local or global .zjdk configuration.
        """,
    descriptionHeading = "%nDESCRIPTION:",
    optionListHeading = "OPTIONS:%n",
    commandListHeading = "%nCOMMANDS:%n",
    synopsisHeading = "%nSYNOPSIS:%n  ",
    synopsisSubcommandLabel = "<COMMAND>"
)
public class ZjdkSet {
    @CommandLine.Mixin
    private HelpOption helpOption;

    @RequiredArgsConstructor
    @CommandLine.Command(
        header = "Set the JDK version",
        description = """
              %n  Updates the JDK version defined in the active Zero-JDK configuration.
            
              By default, the version is written to the local configuration file in the
              current directory. Use the '--global' flag to apply the change to the global
              configuration instead.
            
              The specified version must exist in the catalog. To view available
              versions, use 'zjdk list available'. To refresh the catalog, use 
              'zjdk update'.
            
              After updating the configuration, the corresponding JDK release is
              automatically downloaded and installed if not already present.
            """,
        descriptionHeading = "%nDescription:",
        optionListHeading = "Options:%n",
        commandListHeading = "%nCommands:%n",
        synopsisHeading = "%nSynopsis:%n  ",
        customSynopsis = "zjdk set version [-gh] <version>",
        sortOptions = false
    )
    public static class Version implements Runnable {
        private final PlatformDetection platformDetection;
        private final JdkConfigService jdkConfigService;
        private final ManifestSyncService manifestSyncService;
        private final InMemoryDomainEventPublisher eventPublisher;

        @CommandLine.Spec
        private CommandLine.Model.CommandSpec spec;

        @CommandLine.Option(names = {"-g", "--global"}, description = "Set globally")
        private boolean global;

        @CommandLine.Parameters(index = "0", hidden = true)
        private String version;

        @CommandLine.Mixin
        private HelpOption helpOption;

        @Override
        public void run() {
            if (version == null) {
                throw new CommandLine.ParameterException(spec.commandLine(), "Missing version");
            }

            new CompositeConsoleEventHandler(
                new JdkDownloadProgressPrinter()).register(eventPublisher);

            Platform platform = platformDetection.detect();

            LayoutContext context = global
                ? LayoutContexts.global()
                : LayoutContexts.current();

            jdkConfigService.updateConfiguration(platform, version, context);

            if (LayoutContexts.isGlobalContext(context)) {
                System.out.printf(
                    """
                    Global version set to '%s'
    
                    This acts as a global config across all directories unless overridden locally.
                    Use --global to reflect this intention explicitly.
                    %n""", version);
            } else {
                System.out.printf("""
                    Local version set to '%s'
                    %n""", version);
            }

            manifestSyncService.sync(platform, context);
        }
    }
}
