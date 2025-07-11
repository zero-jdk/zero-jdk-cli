package dev.zerojdk.adapter.in.cli.event;

import dev.zerojdk.domain.port.out.event.DomainEventObserver;
import dev.zerojdk.domain.port.out.event.Observer;
import dev.zerojdk.domain.model.release.events.download.JdkDownloadProgress;
import dev.zerojdk.domain.model.release.events.download.JdkDownloadStarted;

import java.util.ArrayList;
import java.util.List;

public class JdkDownloadProgressPrinter implements ConsoleEventHandler {
    @Override
    public Observer register(DomainEventObserver observer) {
        final String CSI = "\u001B[";

        List<Observer> observers = new ArrayList<>();

        observers.add(
            observer.register(JdkDownloadStarted.class, e -> {
                    System.out.printf("Downloading: %s... ", e.version().getIdentifier());
                    System.out.print(CSI + "s");
                    System.out.flush();
                }
            )
        );

        observers.add(
            observer.register(JdkDownloadProgress.class, e -> {
                System.out.print(CSI + "u");
                System.out.printf("%3d%%", e.bytesRead() * 100 / e.totalBytes());
                System.out.flush();

                if (e.bytesRead() >= e.totalBytes()) {
                    System.out.println();
                }
            })
        );

        return () -> observers.forEach(Observer::close);
    }
}