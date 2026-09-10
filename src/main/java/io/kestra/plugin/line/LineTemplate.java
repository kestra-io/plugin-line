package io.kestra.plugin.line;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.linecorp.bot.client.base.exception.AbstractLineClientException;
import com.linecorp.bot.messaging.client.MessagingApiClientException;
import com.linecorp.bot.messaging.model.BroadcastRequest;
import com.linecorp.bot.messaging.model.TextMessage;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send a LINE broadcast message",
    description = "Send a broadcast message to all users who have added the LINE Official Account. Warning: limited to 60 requests per hour."
)
public abstract class LineTemplate extends AbstractLineConnection {
    private static final String DEFAULT_API_ENDPOINT = "https://api.line.me";

    /** Legacy `url` values pointed at the full operation path, the SDK only wants the API root. */
    private static final String BROADCAST_PATH = "/v2/bot/message/broadcast";

    @Schema(
        title = "LINE Messaging API endpoint",
        description = "Base URL of the LINE Messaging API. A full broadcast URL is also accepted and trimmed back to its base."
    )
    @Builder.Default
    @PluginProperty(group = "connection")
    protected Property<String> url = Property.ofValue(DEFAULT_API_ENDPOINT);

    @Schema(title = "Channel Access Token", description = "LINE Channel Access Token for authentication")
    @NotNull
    @PluginProperty(group = "main", secret = true)
    protected Property<String> channelAccessToken;

    @Schema(title = "Template to use", hidden = true)
    @PluginProperty(group = "advanced")
    protected Property<String> templateUri;

    @Schema(title = "Map of variables to use for the message template")
    @PluginProperty(group = "advanced")
    protected Property<Map<String, Object>> templateRenderMap;

    @Schema(title = "Message text body", description = "Direct message text (bypasses template)")
    @PluginProperty(group = "advanced")
    protected Property<String> textBody;

    @Schema(title = "Custom fields", description = "Custom fields to include in the notification")
    @PluginProperty(group = "advanced")
    protected Property<Map<String, Object>> customFields;

    @Schema(title = "Custom message", description = "Custom message to include in the notification")
    @PluginProperty(group = "advanced")
    protected Property<String> customMessage;

    @Schema(title = "Execution ID", description = "The execution ID")
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<String> executionId = Property.ofExpression("{{ execution.id }}");

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        final Logger logger = runContext.logger();

        final var rChannelAccessToken = runContext.render(this.channelAccessToken).as(String.class)
            .orElseThrow();
        final var rUrl = runContext.render(this.url).as(String.class)
            .orElse(DEFAULT_API_ENDPOINT);

        var messageText = getMessageText(runContext);
        if (messageText.isBlank()) {
            throw new IllegalArgumentException("Nothing to broadcast: set either `textBody` or `templateUri`.");
        }

        var client = this.messagingApiClient(runContext, rChannelAccessToken, apiEndpoint(rUrl));

        logger.debug("Broadcasting LINE message: {}", messageText);

        try {
            var result = client
                .broadcast(retryKey(runContext), new BroadcastRequest.Builder(List.of(new TextMessage(messageText))).build())
                .join();

            logger.info("LINE broadcast message sent successfully (requestId: {})", result.requestId());
        } catch (CompletionException e) {
            throw asFailure(e);
        }

        return null;
    }

    /** The SDK appends the operation path itself, so trim a legacy full broadcast URL back to the API root. */
    static URI apiEndpoint(String url) {
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        if (base.endsWith(BROADCAST_PATH)) {
            base = base.substring(0, base.length() - BROADCAST_PATH.length());
        }

        return URI.create(base);
    }

    /** Unwraps the {@link CompletionException} so the user sees LINE's own error, not a nested stack trace. */
    static Exception asFailure(CompletionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof MessagingApiClientException lineError) {
            return new IllegalStateException(
                "LINE broadcast failed with HTTP " + lineError.getCode() + ": " + lineError.getError(),
                lineError
            );
        }

        if (cause instanceof AbstractLineClientException lineError) {
            return new IllegalStateException(
                "LINE broadcast failed with HTTP " + lineError.getCode(),
                lineError
            );
        }

        return new IllegalStateException("LINE broadcast failed: " + cause.getMessage(), cause);
    }

    /**
     * LINE de-duplicates broadcasts sharing a retry key, so a re-run after a lost response cannot double-send to
     * every follower. Derived from the task run so retries of the same attempt reuse it.
     */
    private static UUID retryKey(RunContext runContext) {
        var taskrun = (Map<?, ?>) runContext.getVariables().get("taskrun");
        var id = taskrun == null ? null : taskrun.get("id");

        return id == null
            ? UUID.randomUUID()
            : UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String getMessageText(RunContext runContext) throws Exception {
        final var rTextBody = runContext.render(this.textBody).as(String.class);
        if (rTextBody.isPresent()) {
            return rTextBody.get();
        }

        final var rTemplateUri = runContext.render(this.templateUri).as(String.class);
        if (rTemplateUri.isPresent()) {
            String template = IOUtils.toString(
                Objects.requireNonNull(
                    this.getClass().getClassLoader().getResourceAsStream(rTemplateUri.get())
                ),
                StandardCharsets.UTF_8
            );

            Map<String, Object> templateVars = templateRenderMap != null
                ? runContext.render(templateRenderMap).asMap(String.class, Object.class)
                : Map.of();

            return runContext.render(template, templateVars);
        }

        return "";
    }
}
