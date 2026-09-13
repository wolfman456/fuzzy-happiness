package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.support.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionMapsStatusAndMessage() {
        var response = handler.handleApi(ApiException.badRequest("nope"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("nope");
    }

    @Test
    void serverErrorApiExceptionStillMapsStatusAndMessage() {
        var response = handler.handleApi(ApiException.badGateway("rules data unavailable"));

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody().message()).isEqualTo("rules data unavailable");
    }

    @Test
    void validationErrorsAreJoined() {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "draft");
        binding.addError(new FieldError("draft", "name", "must not be blank"));
        binding.addError(new FieldError("draft", "age", "must be 13 or older"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message())
                .isEqualTo("name: must not be blank; age: must be 13 or older");
    }

    @Test
    void unreadableBodyReturnsBadRequest() {
        var response = handler.handleBadRequest(new HttpMessageNotReadableException("bad json", null));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Malformed request");
    }

    @Test
    void missingParameterReturnsBadRequest() throws Exception {
        var response = handler.handleBadRequest(new MissingServletRequestParameterException("q", "String"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Malformed request");
    }

    @Test
    void unexpectedExceptionReturns500() {
        var response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
    }
}