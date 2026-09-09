package io.kestra.plugin.line;

import java.util.HashMap;
import java.util.Map;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller
public class FakeWebhookController {
    public static String data;
    public static Map<String, String> headers = new HashMap<>();

    /** The LINE SDK appends the operation path to the configured endpoint, so stub the real broadcast route. */
    @Post("/v2/bot/message/broadcast")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> broadcast(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;
        request.getHeaders().forEach((name, values) ->
        {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });

        return HttpResponse.ok("{}");
    }
}
