package uk.co.techsavvysolutions.tickets.domain.ticket;

public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size must be >= 1");
        }
    }
}
