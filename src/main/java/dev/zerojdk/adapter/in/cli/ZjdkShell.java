package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.adapter.out.shell.scripts.BashScript;
import dev.zerojdk.adapter.out.shell.scripts.ZshScript;
import dev.zerojdk.domain.service.shell.ShellExtensionWriter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
    header = "Install or manage shell integration for environment setup",
    description = """
          %n  Provides commands to install shell integration scripts for Zero-JDK.
        
          Shell integration ensures that environment variables such as PATH and
          JAVA_HOME are automatically configured when entering a Zero-JDK managed
          directory.
        
          Use the 'install' subcommand to enable integration for Bash or Zsh shells.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    commandListHeading = "%nCommands:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
public class ZjdkShell {
    @CommandLine.Mixin
    private HelpOption helpOption;

    @CommandLine.Command(
        header = "Install shell integration scripts",
        description = """
              %n  Installs shell integration scripts that automatically configure the zjdk
              environment on terminal startup.
            
              These scripts update the PATH to reflect the correct JDK version as defined
              in the nearest .zjdk configuration, and clean up conflicting JDK paths.
            
              Use a dedicated subcommand to install support for a specific shell.
            """,
        descriptionHeading = "%nDescription:",
        optionListHeading = "Options:%n",
        commandListHeading = "%nCommands:%n",
        synopsisHeading = "%nSynopsis:%n  "
    )
    public static class Install {
        @CommandLine.Mixin
        private HelpOption helpOption;

        @RequiredArgsConstructor
        @CommandLine.Command(
            header = "Install Bash shell integration",
            description = """
                  %n  Installs the zjdk shell integration script for Bash.
                
                  After installation, a line is printed that should be added to the user's
                  ~/.bashrc file to activate the integration.
                
                  This enables automatic detection of Zero-JDK configurations when navigating
                  between directories.
                """,
            descriptionHeading = "%nDescription:",
            optionListHeading = "Options:%n",
            synopsisHeading = "%nSynopsis:%n  "
        )
        public static class Bash implements Runnable {
            private final ShellExtensionWriter shellExtensionWriter;

            @CommandLine.Mixin
            private HelpOption helpOption;

            @Override
            public void run() {
                Path path = shellExtensionWriter.write(new BashScript());

                System.out.printf("""
                    Add the following line to your ~/.bashrc file:
                        %s
                    
                    Restart your terminal or execute the above for the settings to take effect.
                    """, scriptLine(path));
            }
        }

        @RequiredArgsConstructor
        @CommandLine.Command(
            header = "Install Zsh shell integration",
            description = """
                  %n  Installs the zjdk shell integration script for Zsh.
                
                  After installation, a line is printed that should be added to the user's
                  ~/.zshrc file to activate the integration.
                
                  This enables automatic detection of Zero-JDK configurations when navigating
                  between directories.
                """,
            descriptionHeading = "%nDescription:",
            optionListHeading = "Options:%n",
            synopsisHeading = "%nSynopsis:%n  "
        )
        public static class Zsh implements Runnable {
            private final ShellExtensionWriter shellExtensionWriter;

            @CommandLine.Mixin
            private HelpOption helpOption;

            @Override
            public void run() {
                Path path = shellExtensionWriter.write(new ZshScript());

                System.out.printf("""
                    Add the following line to your ~/.zshrc file:
                        %s
                    
                    Restart your terminal or execute the above for the settings to take effect.
                    """, scriptLine(path));
            }
        }
    }

    private static String scriptLine(Path scriptPath) {
        Path home = Path.of(System.getProperty("user.home"));

        if (scriptPath.startsWith(home)) {
            scriptPath = home.relativize(scriptPath);
        }

        return """
            [ -f "$HOME/%s" ] && source "$HOME/%s"
            """.formatted(scriptPath, scriptPath);
    }
}
