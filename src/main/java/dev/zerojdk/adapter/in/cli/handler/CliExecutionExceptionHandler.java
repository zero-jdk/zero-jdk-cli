package dev.zerojdk.adapter.in.cli.handler;

import dev.zerojdk.adapter.out.config.ConfigFileAlreadyExistsException;
import dev.zerojdk.domain.port.out.layout.UnmanagedDirectoryException;
import dev.zerojdk.domain.service.config.UnsupportedIdentifierException;
import dev.zerojdk.domain.service.install.JdkDownloadFailedException;
import picocli.CommandLine;

public class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) {
        if (ex instanceof UnsupportedIdentifierException e) {
            System.err.printf("The defined version %s is not supported\n", e.getIdentifier());
        } else if (ex instanceof UnmanagedDirectoryException) {
            System.err.println("Not a zero-jdk managed directory (or any of the parent directories), and no global configuration exists");
        } else if (ex instanceof ConfigFileAlreadyExistsException) {
            System.err.println("Already initialized. Run 'set version' to change it or 'sync' to install");
        } else if (ex instanceof JdkDownloadFailedException e) {
            System.err.printf("There was an issue downloading the '%s'", e.getJdkVersion().getIdentifier());
        } else {
            System.err.println(ex.getMessage());
        }

        return 1;
    }
}
