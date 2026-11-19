package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Lazy
public final class OperationsCatalogVersionController {
    private final ObjectProvider<OperationsCatalogSaleCommand> commands;
    private final OperationsProblemWriter problems;
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public OperationsCatalogVersionController(ObjectProvider<OperationsCatalogSaleCommand> commands, OperationsProblemWriter problems) { this.commands = commands; this.problems = problems; }

    @PostMapping("/api/v1/operations/catalog/versions/{versionId}/publications")
    public void publishOperationsCatalogVersion(@PathVariable String versionId, @RequestBody byte[] body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        var caller = caller(request); var parsed = parse(versionId, body, true);
        success(command().publish(new OperationsCatalogSaleCommand.PublishCommand(parsed.versionId, parsed.active, parsed.revision, caller)), response);
    }

    @PostMapping("/api/v1/operations/catalog/versions/{versionId}/offsales")
    public void offsaleOperationsCatalogVersion(@PathVariable String versionId, @RequestBody byte[] body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        var caller = caller(request); var parsed = parse(versionId, body, false);
        success(command().offsale(new OperationsCatalogSaleCommand.OffsaleCommand(parsed.versionId, parsed.revision, caller)), response);
    }

    private OperationsCatalogSaleCommand.Caller caller(HttpServletRequest request) {
        if (!(request.getUserPrincipal() instanceof OperationsRequestPrincipal principal) || principal.caller() == null) throw new OperationsCatalogSaleCommand.Failure(OperationsCatalogSaleCommand.Code.UNAUTHENTICATED);
        return new OperationsCatalogSaleCommand.Caller(principal.caller().employeeId(), OperationsBoundaryFilter.trace(request));
    }
    private OperationsCatalogSaleCommand command() {
        try {
            var values = commands.stream().limit(2).toList();
            if (values.size() == 1) return values.getFirst();
        } catch (RuntimeException unavailable) {
            throw new OperationsCatalogSaleCommand.Failure(OperationsCatalogSaleCommand.Code.SERVICE_UNAVAILABLE);
        }
        throw new OperationsCatalogSaleCommand.Failure(OperationsCatalogSaleCommand.Code.SERVICE_UNAVAILABLE);
    }
    private Parsed parse(String rawVersionId, byte[] body, boolean publication) {
        try {
            UUID versionId = UUID.fromString(rawVersionId);
            if (!versionId.toString().equals(rawVersionId)) throw new IllegalArgumentException();
            if (body == null || body.length > 8192) throw new IllegalArgumentException();
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject()) throw new IllegalArgumentException();
            if (publication) {
                if (root.size() != 2 || !root.has("expectedActiveVersionId") || !root.has("expectedRevision")) throw new IllegalArgumentException();
                JsonNode active = root.get("expectedActiveVersionId"), revision = root.get("expectedRevision");
                UUID activeId = active.isNull() ? null : UUID.fromString(active.asString());
                if (activeId != null && !activeId.toString().equals(active.asString()) || !revision.isIntegralNumber()) throw new IllegalArgumentException();
                return new Parsed(versionId, activeId, positive(revision));
            }
            if (root.size() != 1 || !root.has("expectedRevision")) throw new IllegalArgumentException();
            return new Parsed(versionId, null, positive(root.get("expectedRevision")));
        } catch (Exception invalid) { throw new OperationsCatalogSaleCommand.Failure(OperationsCatalogSaleCommand.Code.VALIDATION_FAILED); }
    }
    private static int positive(JsonNode node) { if (!node.isIntegralNumber()) throw new IllegalArgumentException(); BigInteger value = node.bigIntegerValue(); if (value.signum() < 1 || value.bitLength() > 31) throw new IllegalArgumentException(); return value.intValueExact(); }
    private record Parsed(UUID versionId, UUID active, int revision) { }
    private void success(Object result, HttpServletResponse response) throws IOException { if (result == null) throw new OperationsCatalogSaleCommand.Failure(OperationsCatalogSaleCommand.Code.INTERNAL); byte[] body = mapper.writeValueAsBytes(result); problems.headers(response); response.setStatus(200); response.setContentType("application/json"); response.setContentLength(body.length); response.getOutputStream().write(body); }
    @ExceptionHandler(OperationsCatalogSaleCommand.Failure.class)
    public void known(OperationsCatalogSaleCommand.Failure failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, failure.code(), OperationsBoundaryFilter.trace(request)); }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, OperationsCatalogSaleCommand.Code.INTERNAL, OperationsBoundaryFilter.trace(request)); }
}
