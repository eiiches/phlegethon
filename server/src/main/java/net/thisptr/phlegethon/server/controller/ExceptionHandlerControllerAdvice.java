package net.thisptr.phlegethon.server.controller;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.thisptr.phlegethon.service.NamespaceNotFoundException;
import net.thisptr.phlegethon.service.RecordingNotFoundException;
import net.thisptr.phlegethon.service.RecordingTooLargeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionHandlerControllerAdvice {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorResponse {
        @JsonProperty("message")
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    @ResponseBody
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler({NamespaceNotFoundException.class, RecordingNotFoundException.class})
    public ErrorResponse handleNamespaceNotFoundException(Exception e) {
        return new ErrorResponse(e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({RecordingTooLargeException.class})
    public ErrorResponse handleTooLargeException(Exception e) {
        return new ErrorResponse(e.getMessage());
    }
}
