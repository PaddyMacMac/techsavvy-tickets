package uk.co.techsavvysolutions.tickets.web;

import uk.co.techsavvysolutions.tickets.shared.DomainException;
import uk.co.techsavvysolutions.tickets.identity.domain.NoCurrentUserException;
import uk.co.techsavvysolutions.tickets.ticket.domain.errors.InvalidStatusTransitionException;
import uk.co.techsavvysolutions.tickets.ticket.domain.errors.TicketNotFoundException;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** One place that turns failures into RFC 9457 "problem details" JSON the frontend can read. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException exception) {
        var status = switch (exception) {
            case TicketNotFoundException _ -> HttpStatus.NOT_FOUND;
            case InvalidStatusTransitionException _ -> HttpStatus.CONFLICT;
            case NoCurrentUserException _ -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some details need fixing.");
        problem.setTitle("Validation failed");
        problem.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                                (first, second) -> first,
                                LinkedHashMap::new)));
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }
}
