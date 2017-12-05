package com.github.mlk.exceptions;

import static com.github.mlk.exceptions.Exceptions.ErrorResponse.Builder.anErrorResponse;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class Exceptions {

  private static final Logger log = LoggerFactory.getLogger(Exceptions.class);

  private static final String VAGUE_ERROR_MESSAGE = "Sorry, something failed.";

  public static class InternalServerError extends Error {}
  public static class BadRequest extends Error {}
  public static class Unauthorized extends Error {}
  public static class Forbidden extends Error {}

  public static class Error extends RuntimeException {

    private String code;
    private String description;

    public Error withCode(String code) {
      this.code = code;
      return this;
    }

    public Error withDescription(String description) {
      this.description = description;
      return this;
    }

    public String getCode() {
      return code;
    }

    public String getDescription() {
      return description;
    }
  }

  @ResponseStatus(UNAUTHORIZED)
  @ExceptionHandler(Unauthorized.class)
  @ResponseBody
  public ErrorResponse handleUnauthorized(HttpServletRequest request, Unauthorized exception) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withCode(exception.getCode())
        .withDescription(exception.getDescription())
        .build();
  }

  @ResponseStatus(FORBIDDEN)
  @ExceptionHandler(Forbidden.class)
  @ResponseBody
  public ErrorResponse handleForbidden(HttpServletRequest request, Forbidden exception) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withCode(exception.getMessage())
        .withDescription(exception.getDescription())
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(BadRequest.class)
  @ResponseBody
  public ErrorResponse handleBadRequest(HttpServletRequest request, BadRequest exception) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withCode(exception.getCode())
        .withDescription(exception.getDescription())
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(InternalServerError.class)
  @ResponseBody
  public ErrorResponse handleInternalServerError(HttpServletRequest request, InternalServerError exception) {
    log.error("Handled internal server error", exception);

    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withCode(exception.getCode())
        .withDescription(exception.getDescription())
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

    return anErrorResponse()
        .withUrl(httpServletRequest.getRequestURL().toString())
        .withDescription(VAGUE_ERROR_MESSAGE)
        .build();
  }

  @ResponseStatus(METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseBody
  public ErrorResponse handleMethodNotAllowed(HttpServletRequest request, Exception exception) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription(exception.getMessage())
        .build();
  }

  @ResponseStatus(UNSUPPORTED_MEDIA_TYPE)
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseBody
  public ErrorResponse handleMediaTypeNotSupported(HttpServletRequest request,
      Exception exception) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
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

    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription(description)
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ErrorResponse handleMessageNotReadable(HttpServletRequest request) {
    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription("Http message was not readable")
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseBody
  public ErrorResponse handleMethodTypeNotValid(HttpServletRequest request,
      MethodArgumentTypeMismatchException exception) {

    String description = String.format("Parameter value '%s' is not valid for request parameter '%s'",
        exception.getValue(), exception.getName());

    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription(description)
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(HttpServerErrorException.class)
  @ResponseBody
  public ErrorResponse handleHttpServerErrorException(HttpServletRequest httpServletRequest,
      HttpServerErrorException e) {
    log.error(
        format("Request failed with status: %s and response: %s", e.getStatusCode(),
            e.getResponseBodyAsString()), e);

    return anErrorResponse()
        .withUrl(httpServletRequest.getRequestURL().toString())
        .withDescription(VAGUE_ERROR_MESSAGE)
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(UnsatisfiedServletRequestParameterException.class)
  @ResponseBody
  public ErrorResponse handleUnsatisfiedParameter(HttpServletRequest request,
      UnsatisfiedServletRequestParameterException exception) {

    String unsatisfiedConditions = Stream.of(exception.getParamConditions())
        .collect(joining(","));

    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription(
            format("Parameter conditions not met for request: %s", unsatisfiedConditions))
        .build();
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  @ResponseBody
  public ErrorResponse catchAllHandler(HttpServletRequest request, Throwable ex) {
    log.error("Unexpected error handled", ex);

    return anErrorResponse()
        .withUrl(request.getRequestURL().toString())
        .withDescription(VAGUE_ERROR_MESSAGE)
        .build();
  }

  public static class ErrorResponse {

    private String url;
    private String code;
    private String description;

    public ErrorResponse(String url, String code, String description) {
      this.url = url;
      this.code = code;
      this.description = description;
    }

    public String getUrl() {
      return url;
    }

    public String getCode() {
      return code;
    }

    public String getDescription() {
      return description;
    }

    public static class Builder {

      private String url;
      private String code;
      private String description;

      public static Builder anErrorResponse() {
        return new Builder();
      }

      public Builder withUrl(String url) {
        this.url = url;
        return this;
      }

      public Builder withCode(String code) {
        this.code = code;
        return this;
      }

      public Builder withDescription(String description) {
        this.description = description;
        return this;
      }

      public ErrorResponse build() {
        return new ErrorResponse(url, code, description);
      }
    }
  }
}
