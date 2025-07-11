package dev.zerojdk.adapter.in.cli.event;

import dev.zerojdk.domain.port.out.event.DomainEventObserver;
import dev.zerojdk.domain.port.out.event.Observer;

public interface ConsoleEventHandler {
    Observer register(DomainEventObserver publisher);
}
