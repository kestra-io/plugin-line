package io.kestra.plugin.line;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.linecorp.bot.messaging.client.MessagingApiClientException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Unit tests for the two pure helpers introduced by the SDK migration. The broadcast round trip itself is covered
 * end to end by {@link LineExecutionTest}, which drives both endpoint shapes through the flow runner.
 */
class LineTemplateTest {
    @Test
    void apiEndpointKeepsABaseUrl() {
        assertThat(LineTemplate.apiEndpoint("https://api.line.me").toString(), is("https://api.line.me"));
    }

    @Test
    void apiEndpointTrimsALegacyBroadcastUrl() {
        assertThat(
            LineTemplate.apiEndpoint("https://api.line.me/v2/bot/message/broadcast").toString(),
            is("https://api.line.me")
        );
    }

    @Test
    void apiEndpointTrimsATrailingSlash() {
        assertThat(LineTemplate.apiEndpoint("http://localhost:59443/").toString(), is("http://localhost:59443"));
        assertThat(
            LineTemplate.apiEndpoint("http://localhost:59443/v2/bot/message/broadcast/").toString(),
            is("http://localhost:59443")
        );
    }

    @Test
    void asFailureReportsTheLineError() {
        var lineError = new MessagingApiClientException(
            response(400),
            "Invalid channel access token",
            List.of(),
            List.of()
        );

        Exception failure = LineTemplate.asFailure(new CompletionException(lineError));

        assertThat(failure, instanceOf(IllegalStateException.class));
        assertThat(failure.getMessage(), containsString("400"));
        assertThat(failure.getMessage(), containsString("Invalid channel access token"));
        assertThat(failure.getCause(), sameInstance(lineError));
    }

    @Test
    void asFailureUnwrapsAnyOtherCause() {
        var cause = new IOException("connection reset");

        assertThat(LineTemplate.asFailure(new CompletionException(cause)), sameInstance(cause));
    }

    private static Response response(int code) {
        return new Response.Builder()
            .request(new Request.Builder().url("https://api.line.me/v2/bot/message/broadcast").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Bad Request")
            .build();
    }
}
