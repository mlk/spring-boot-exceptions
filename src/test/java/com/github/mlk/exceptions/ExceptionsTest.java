package com.github.mlk.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.mlk.exceptions.Exceptions.BadRequest;
import com.github.mlk.exceptions.Exceptions.InternalServerError;
import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionsTest {

  @Mock
  private Operation operation;

  private MockMvc mockMvc;

  private final String jsonContent = "{\"value\": \"data\"}";

  @Before
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(
        new ExceptionsTestController(operation))
        .setControllerAdvice(new Exceptions())
        .build();
  }

  @Test
  public void throwsInternalServerError() throws Exception {
    doThrow(new InternalServerError("Chuck Norris instantiates abstract classes"))
        .when(operation).action();

    mockMvc.perform(post("/test")
        .content(jsonContent)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("status", is(500)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("SERVER_ERROR")))
        .andExpect(jsonPath("description", is("Chuck Norris instantiates abstract classes")));
  }

  @Test
  public void throwsUnauthorized() throws Exception {
    doThrow(new Unauthorized("I don't know the password"))
            .when(operation).action();

    mockMvc.perform(post("/test")
            .content(jsonContent)
            .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("status", is(401)))
            .andExpect(jsonPath("url", is("http://localhost/test")))
            .andExpect(jsonPath("message", is("CLIENT_ERROR")))
            .andExpect(jsonPath("description", is("I don't know the password")));
  }

  @Test
  public void throwsBadRequest() throws Exception {
    doThrow(new BadRequest("Bad client")).when(operation).action();

    mockMvc.perform(post("/test")
        .content(jsonContent)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("status", is(400)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("CLIENT_ERROR")))
        .andExpect(jsonPath("description", is("Bad client")));
  }

  @Test
  public void throwsInternalServerErrorForUnhandledHttpClientErrorException() throws Exception {
    doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Something gone wrong",
        "{\"error\":\"wasBad\"}".getBytes(),
        Charsets.UTF_8)).when(operation).action();

    mockMvc.perform(post("/test")
        .content(jsonContent)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("status", is(500)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("SERVER_ERROR")))
        .andExpect(jsonPath("description", is("Sorry, something failed.")));
  }

  @Test
  public void throwsInternalServerErrorForUnhandledExceptions() throws Exception {
    doThrow(new RestClientException("Sensitive information")).when(operation)
        .action();

    mockMvc.perform(post("/test")
        .content(jsonContent)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("status", is(500)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("SERVER_ERROR")))
        .andExpect(jsonPath("description", is("Sorry, something failed.")));
  }

  @Test
  public void handlesMethodNotAllowed() throws Exception {
    mockMvc.perform(put("/test"))
        .andDo(print())
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("status", is(405)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("CLIENT_ERROR")))
        .andExpect(jsonPath("description", is("Request method 'PUT' not supported")));
  }

  @Test
  public void handlesParameterConditionNotMet() throws Exception {
    mockMvc.perform(get("/test"))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("status", is(400)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("CLIENT_ERROR")))
        .andExpect(jsonPath("description", is("Parameter conditions not met for request: param")));
  }

  @Test
  public void handlesMediaTypeNotSupported() throws Exception {
    mockMvc.perform(post("/test")
        .contentType(MediaType.APPLICATION_ATOM_XML)
        .content(jsonContent))
        .andDo(print())
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("status", is(415)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("CLIENT_ERROR")))
        .andExpect(
            jsonPath("description", is("Content type 'application/atom+xml' not supported")));
  }

  @Test
  public void handlesMethodArgumentTypeNotValid() throws Exception {
    mockMvc.perform(get("/test/enum")
        .param("enum", "NOT_AN_ENUM")
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("status", is(400)))
        .andExpect(jsonPath("url", is("http://localhost/test/enum")))
        .andExpect(jsonPath("message", is("CLIENT_ERROR")))
        .andExpect(jsonPath("description",
            is("Parameter value 'NOT_AN_ENUM' is not valid for request parameter 'enum'")));
  }

  @Test
  public void throwsInternalServerErrorForUnhandledHttpServerErrorException() throws Exception {
    doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something gone wrong",
        "{\"error\":\"server error\"}".getBytes(), Charsets.UTF_8)).when(operation).action();

    mockMvc.perform(post("/test")
        .content(jsonContent)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("status", is(500)))
        .andExpect(jsonPath("url", is("http://localhost/test")))
        .andExpect(jsonPath("message", is("SERVER_ERROR")))
        .andExpect(jsonPath("description", is("Sorry, something failed.")));
  }

  @RestController
  @RequestMapping("test")
  @Profile("never-load-me")
  public static class ExceptionsTestController {

    private final Operation operation;

    public ExceptionsTestController(Operation operation) {
      this.operation = operation;
    }

    @PostMapping
    public void post(@RequestBody Data data) {
      operation.action();
    }

    @GetMapping(params = "param")
    public Data get(String param) {
      return new Data();
    }

    @GetMapping(path = "enum", params = "enum")
    public Data getWithEnum(@RequestParam(name = "enum") Enum ignored) {
      return new Data();
    }
  }

  public interface Operation {

    Data action();
  }

  enum Enum {
    VALUE
  }

  static class Data {

  }
}
