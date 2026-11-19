package com.onecall.core.operations_iam.internal.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public final class CatalogVersionRequestBinderTests {
    private final UUID version = UUID.fromString("00000000-0000-0000-0000-000000000038");
    private final OperationsCatalogSaleCommand command = mock(OperationsCatalogSaleCommand.class);
    private final OperationsCatalogVersionController controller;
    private final Method parser;

    CatalogVersionRequestBinderTests() throws Exception {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("command", command);
        controller = new OperationsCatalogVersionController(beans.getBeanProvider(OperationsCatalogSaleCommand.class), mock(OperationsProblemWriter.class));
        parser = OperationsCatalogVersionController.class.getDeclaredMethod("parse", String.class, byte[].class, boolean.class);
        parser.setAccessible(true);
    }

    private Object parse(String id, byte[] body, boolean publication) {
        try { return parser.invoke(controller, id, body, publication); }
        catch (InvocationTargetException failure) { throw (RuntimeException) failure.getCause(); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private Object parse(String body, boolean publication) {
        return parse(version.toString(), body.getBytes(StandardCharsets.UTF_8), publication);
    }

    private void invalid(String body, boolean publication) {
        assertThatThrownBy(() -> parse(body, publication)).isInstanceOfSatisfying(Failure.class,
                failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
        verifyNoInteractions(command);
    }

    @Test void publicationRequiresExactlyTwoFields() {
        assertThat(parse("{\"expectedActiveVersionId\":null,\"expectedRevision\":1}", true)).isNotNull();
        for (String body : List.of("{}", "[]", "null", "1", "{\"expectedRevision\":1}", "{\"expectedActiveVersionId\":null}")) invalid(body, true);
    }

    @Test void publicationAcceptsExplicitNullActive() throws Exception {
        Object parsed = parse("{\"expectedActiveVersionId\":null,\"expectedRevision\":2147483647}", true);
        Method active = parsed.getClass().getDeclaredMethod("active");
        active.setAccessible(true);
        assertThat(active.invoke(parsed)).isNull();
        assertThat(parse("{\"expectedActiveVersionId\":\"" + version + "\",\"expectedRevision\":1}", true)).isNotNull();
        for (String value : List.of("false", "1", "{}", "[]", "\"\"")) invalid("{\"expectedActiveVersionId\":" + value + ",\"expectedRevision\":1}", true);
    }

    @Test void offsaleRequiresExactlyOneField() {
        assertThat(parse("{\"expectedRevision\":1}", false)).isNotNull();
        for (String body : List.of("{}", "[]", "null", "{\"expectedActiveVersionId\":null,\"expectedRevision\":1}")) invalid(body, false);
    }

    @Test void rejectsUnknownFields() {
        invalid("{\"expectedRevision\":1,\"unknown\":true}", false);
        invalid("{\"expectedActiveVersionId\":null,\"expectedRevision\":1,\"unknown\":true}", true);
    }

    @Test void rejectsDuplicateFields() {
        invalid("{\"expectedRevision\":1,\"expectedRevision\":1}", false);
        invalid("{\"expectedActiveVersionId\":null,\"expectedActiveVersionId\":null,\"expectedRevision\":1}", true);
    }

    @Test void rejectsTrailingTokens() {
        for (String trailing : List.of("{}", "null", "1", "[]", "garbage")) {
            invalid("{\"expectedRevision\":1}" + trailing, false);
            invalid("{\"expectedActiveVersionId\":null,\"expectedRevision\":1}" + trailing, true);
        }
    }

    @Test void rejectsQuotedFractionalOverflowAndZeroRevision() {
        for (String value : List.of("\"1\"", "1.0", "1e0", "2147483648", "999999999999999999999999999", "0", "-1", "null", "true", "{}", "[]")) {
            invalid("{\"expectedRevision\":" + value + "}", false);
            invalid("{\"expectedActiveVersionId\":null,\"expectedRevision\":" + value + "}", true);
        }
    }

    @Test void rejectsNonCanonicalUuid() {
        for (String id : List.of("", "BAD", "00000000-0000-0000-0000-00000000003A", "{" + version + "}", " " + version, version + "/", "0-0-0-0-38", version + ";x", version + "%2f")) {
            assertThatThrownBy(() -> parse(id, "{\"expectedRevision\":1}".getBytes(StandardCharsets.UTF_8), false))
                    .isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
        }
    }

    @Test void rejectsInvalidUtf8AndOversizedBody() {
        for (byte[] body : List.of(new byte[] {(byte) 0xc3, 0x28}, (" ".repeat(8193) + "{}").getBytes(StandardCharsets.UTF_8))) {
            assertThatThrownBy(() -> parse(version.toString(), body, false)).isInstanceOfSatisfying(Failure.class,
                    failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
        }
    }

    @Test void rejectsReasonAndIdempotencyFields() {
        for (String field : List.of("reason", "reasonCode", "actor", "role", "snapshot", "idempotencyKey", "Idempotency-Key")) {
            invalid("{\"expectedRevision\":1,\"" + field + "\":\"PRIVATE\"}", false);
            invalid("{\"expectedActiveVersionId\":null,\"expectedRevision\":1,\"" + field + "\":\"PRIVATE\"}", true);
        }
    }
}
