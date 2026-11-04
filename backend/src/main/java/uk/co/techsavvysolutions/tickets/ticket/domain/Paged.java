package uk.co.techsavvysolutions.tickets.ticket.domain;

import java.util.List;
import java.util.function.Function;

public record Paged<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        return (int) Math.ceil((double) totalItems / size);
    }

    public <R> Paged<R> map(Function<? super T, ? extends R> mapper) {
        return new Paged<>(items.stream().<R>map(mapper).toList(), page, size, totalItems);
    }
}
