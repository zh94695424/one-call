package com.onecall.core.operations_iam.internal.web;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

import com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Lazy
public final class OperationsCatalogDraftController {
    private final ObjectProvider<OperationsCatalogDraftCommand> commands;
    private final OperationsProblemWriter problems;
    private final CatalogDraftRequestBinder binder = new CatalogDraftRequestBinder();
    private final JsonMapper mapper = JsonMapper.builder().build();
    public OperationsCatalogDraftController(ObjectProvider<OperationsCatalogDraftCommand> commands, OperationsProblemWriter problems) { this.commands = commands; this.problems = problems; }

    @PostMapping("/api/v1/operations/catalog/versions")
    public void createOperationsCatalogVersionDraft(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Caller caller = caller(request);
        var parsed = binder.create(body(request));
        var result = command().create(new CreateCommand(parsed.productId(), parsed.versionNo(), parsed.commercial(), parsed.benefitLines(), parsed.channels(), caller));
        if (result == null) throw new Failure(Code.INTERNAL);
        success(result.draft(), result.created() ? 201 : 200, response);
    }
    @PutMapping("/api/v1/operations/catalog/versions/{versionId}/draft")
    public void replaceOperationsCatalogVersionDraft(@PathVariable String versionId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Caller caller = caller(request);
        var parsed = binder.replace(versionId, body(request));
        success(command().replace(new ReplaceCommand(parsed.versionId(), parsed.expectedRevision(), parsed.commercial(), caller)), 200, response);
    }
    private byte[] body(HttpServletRequest request) {
        try { return request.getInputStream().readNBytes(8193); }
        catch (IOException invalid) { throw new Failure(Code.VALIDATION_FAILED); }
    }
    private Caller caller(HttpServletRequest request) {
        if (!(request.getUserPrincipal() instanceof OperationsRequestPrincipal principal) || principal.caller() == null) throw new Failure(Code.UNAUTHENTICATED);
        return new Caller(principal.caller().employeeId(), OperationsBoundaryFilter.trace(request));
    }
    private OperationsCatalogDraftCommand command() {
        try { var values = commands.stream().limit(2).toList(); if (values.size() == 1) return values.getFirst(); }
        catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
    private void success(CatalogVersionDraft draft, int status, HttpServletResponse response) throws IOException {
        if (draft == null) throw new Failure(Code.INTERNAL);
        var result = new LinkedHashMap<String, Object>();
        result.put("catalogVersionId", draft.catalogVersionId()); result.put("productId", draft.productId()); result.put("versionNo", draft.versionNo());
        result.put("saleStatus", draft.saleStatus()); result.put("offerType", draft.offerType()); result.put("audience", draft.audience());
        result.put("priceCents", draft.priceCents()); result.put("currency", draft.currency()); result.put("billingCycle", draft.billingCycle());
        result.put("installments", draft.installments()); result.put("grantIntervalMonths", draft.grantIntervalMonths());
        result.put("benefitLines", draft.benefitLines()); result.put("channels", draft.channels()); result.put("revision", draft.revision());
        result.put("createdAt", draft.createdAt().toString()); result.put("publishedAt", null); result.put("snapshotSha256", draft.snapshotSha256());
        byte[] body = mapper.writeValueAsBytes(result);
        problems.headers(response); response.setStatus(status); response.setContentType("application/json"); response.setContentLength(body.length); response.getOutputStream().write(body);
    }
    @ExceptionHandler(Failure.class)
    public void known(Failure failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, failure.code(), OperationsBoundaryFilter.trace(request)); }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, Code.INTERNAL, OperationsBoundaryFilter.trace(request)); }
}
