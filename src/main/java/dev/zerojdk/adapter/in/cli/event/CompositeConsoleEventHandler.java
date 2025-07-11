package dev.zerojdk.adapter.in.cli.event;

import dev.zerojdk.domain.port.out.event.DomainEventObserver;
import dev.zerojdk.domain.port.out.event.Observer;

import java.util.List;

public class CompositeConsoleEventHandler implements ConsoleEventHandler {
    private final List<ConsoleEventHandler> handlers;

    public CompositeConsoleEventHandler(ConsoleEventHandler... handlers) {
        this.handlers = List.of(handlers);
    }

    @Override
    public Observer register(DomainEventObserver observer) {
        List<Observer> observers = handlers.stream()
            .map(handler -> handler.register(observer))
            .toList();

        return () -> observers.forEach(Observer::close);
    }
}
