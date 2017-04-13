package com.github.mlk.exceptions;

import static com.github.mlk.exceptions.Exceptions.ErrorResponse.Builder.anError;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class Exceptions {

  private static final Logger log = LoggerFactory.getLogger(Exceptions.class);

  private static final String CLIENT_ERROR = "CLIENT_ERROR";
  private static final String SERVER_ERROR = "SERVER_ERROR";

  public static class InternalServerError extends RuntimeException {

    public InternalServerError(String description) {
      super(description);
    }
  }

  public static class BadRequest extends RuntimeException {

    public BadRequest(String description) {
      super(description);
    }
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(BadRequest.class)
  @ResponseBody
  public ErrorResponse handleBadRequest(HttpServletRequest request, Exception exception) {
    return anError()
        .withStatus(BAD_REQUEST.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(CLIENT_ERROR)
        .withDescription(exception.getMessage())
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(InternalServerError.class)
  @ResponseBody
  public ErrorResponse handleInternalServerError(HttpServletRequest request, Exception exception) {
    log.error("Handled internal server error", exception);

    return anError()
        .withStatus(INTERNAL_SERVER_ERROR.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(SERVER_ERROR)
        .withDescription(exception.getMessage())
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(HttpClientErrorException.class)
  @ResponseBody
  public ErrorResponse handleHttpClientErrorException(HttpServletRequest httpServletRequest,
      HttpClientErrorException e) {
    log.error(
        format("Downstream call failed with status: %s and response: %s", e.getStatusCode(),
            e.getResponseBodyAsString()), e);

    return anError()
        .withStatus(INTERNAL_SERVER_ERROR.value())
        .withUrl(httpServletRequest.getRequestURL().toString())
        .withMessage(SERVER_ERROR)
        .withDescription("Downstream call failed")
        .build();
  }

  @ResponseStatus(METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseBody
  public ErrorResponse handleMethodNotAllowed(HttpServletRequest request, Exception exception) {
    return anError()
        .withStatus(METHOD_NOT_ALLOWED.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(CLIENT_ERROR)
        .withDescription(exception.getMessage())
        .build();
  }

  @ResponseStatus(UNSUPPORTED_MEDIA_TYPE)
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseBody
  public ErrorResponse handleMediaTypeNotSupported(HttpServletRequest request,
      Exception exception) {
    return anError()
        .withStatus(UNSUPPORTED_MEDIA_TYPE.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(CLIENT_ERROR)
        .withDescription(exception.getMessage())
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseBody
  public ErrorResponse handleMethodArgumentNotValid(HttpServletRequest request,
      MethodArgumentNotValidException exception) {

    String description = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> format("%s %s", error.getField(), error.getDefaultMessage())).collect(
            Collectors.joining(", "));

    return anError()
        .withStatus(BAD_REQUEST.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(CLIENT_ERROR)
        .withDescription(description)
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(UnsatisfiedServletRequestParameterException.class)
  @ResponseBody
  public ErrorResponse handleUnsatisfiedParameter(HttpServletRequest request,
      UnsatisfiedServletRequestParameterException exception) {

    String unsatisfiedConditions = Stream.of(exception.getParamConditions())
        .collect(joining(","));

    return anError()
        .withStatus(BAD_REQUEST.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(CLIENT_ERROR)
        .withDescription(
            format("Parameter conditions not met for request: %s", unsatisfiedConditions))
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  @ResponseBody
  public ErrorResponse catchAllHandler(HttpServletRequest request, Throwable ex) {
    log.error("Unexpected error handled", ex);

    return anError()
        .withStatus(INTERNAL_SERVER_ERROR.value())
        .withUrl(request.getRequestURL().toString())
        .withMessage(SERVER_ERROR)
        .withDescription("Sorry, something failed.")
        .build();
  }

  public static class ErrorResponse {

    private int status;
    private String url;
    private String message;
    private String description;

    public ErrorResponse(int status, String url, String message, String description) {
      this.status = status;
      this.url = url;
      this.message = message;
      this.description = description;
    }

    public int getStatus() {
      return status;
    }

    public String getUrl() {
      return url;
    }

    public String getMessage() {
      return message;
    }

    public String getDescription() {
      return description;
    }

    public static class Builder {

      private int status;
      private String url;
      private String message;
      private String description;

      public static Builder anError() {
        return new Builder();
      }

      public Builder withStatus(int status) {
        this.status = status;
        return this;
      }

      public Builder withUrl(String url) {
        this.url = url;
        return this;
      }

      public Builder withMessage(String message) {
        this.message = message;
        return this;
      }

      public Builder withDescription(String description) {
        this.description = description;
        return this;
      }

      public ErrorResponse build() {
        return new ErrorResponse(status, url, message, description);
      }
    }
  }
}
