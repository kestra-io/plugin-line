package io.kestra.plugin.line;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.linecorp.bot.client.base.http.HttpInterceptor;
import com.linecorp.bot.messaging.client.MessagingApiClient;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractLineConnection extends Task implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Options",
        description = "The options to set to customize the HTTP client"
    )
    @PluginProperty(dynamic = true, group = "advanced")
    protected RequestOptions options;

    /** The LINE SDK defaults to 10s. Kept at the read-idle ceiling Kestra's HTTP client applied before the migration. */
    static final Duration DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5);

    /** Built per run rather than cached, so a channel access token is never held in a long-lived map. */
    protected MessagingApiClient messagingApiClient(
        RunContext runContext,
        String channelAccessToken,
        URI apiEndpoint) throws IllegalVariableEvaluationException {
        var builder = MessagingApiClient.builder(channelAccessToken)
            .apiEndPoint(apiEndpoint)
            .readTimeout(DEFAULT_READ_TIMEOUT);

        if (this.options == null) {
            return builder.build();
        }

        var rConnectTimeout = runContext.render(this.options.getConnectTimeout()).as(Duration.class);
        if (rConnectTimeout.isPresent()) {
            builder.connectTimeout(rConnectTimeout.get());
        }

        var rReadTimeout = runContext.render(this.options.getReadTimeout()).as(Duration.class);
        if (rReadTimeout.isPresent()) {
            builder.readTimeout(rReadTimeout.get());
        }

        if (this.options.getHeaders() != null) {
            Map<String, String> rHeaders = runContext.render(this.options.getHeaders())
                .asMap(String.class, String.class);

            if (rHeaders != null && !rHeaders.isEmpty()) {
                builder.addInterceptor(headerInterceptor(rHeaders));
            }
        }

        return builder.build();
    }

    private static HttpInterceptor headerInterceptor(Map<String, String> headers) {
        return chain ->
        {
            var request = chain.request().newBuilder();
            headers.forEach(request::addHeader);

            return chain.proceed(request.build());
        };
    }

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(title = "The time allowed to establish a connection to the server before failing")
        @PluginProperty(group = "execution")
        private final Property<Duration> connectTimeout;

        @Schema(title = "The maximum time allowed for reading data from the server before failing")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readTimeout = Property.ofValue(DEFAULT_READ_TIMEOUT);

        @Schema(
            title = "The time allowed for a read connection to remain idle before closing it",
            description = "No longer applied. The LINE SDK's HTTP client has no separate read-idle timeout; use `readTimeout` instead.",
            deprecated = true
        )
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Schema(
            title = "The time an idle connection can remain in the client's connection pool before being closed",
            description = "No longer applied. Connection pooling is managed by the LINE SDK.",
            deprecated = true
        )
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Schema(
            title = "The maximum content length of the response",
            description = "No longer applied. LINE broadcast responses are a fixed small JSON payload.",
            deprecated = true
        )
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(
            title = "The default charset for the request",
            description = "No longer applied. The LINE Messaging API is UTF-8 only.",
            deprecated = true
        )
        @Builder.Default
        @PluginProperty(group = "advanced")
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);

        @Schema(
            title = "HTTP headers",
            description = "HTTP headers to include in the request"
        )
        @PluginProperty(group = "advanced")
        public Property<Map<String, String>> headers;
    }
}
