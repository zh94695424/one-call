package com.onecall.archtests.evidence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.internal.mediaread.JooqEvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class EvidenceMediaObjectReaderTests {
    private static final UUID EVIDENCE = UUID.randomUUID();
    private static final UUID MEDIA = UUID.randomUUID();
    private static final UUID RECORDING = UUID.randomUUID();
    private static final String[] NAMES = {"er_id", "source_type", "source_ref_id", "er_status", "evidence_id",
            "media_object_id", "artifact_role", "media_id", "sha256", "size_bytes", "mime_type", "recording_id",
            "recording_media_id", "recording_status", "validated_duration"};

    private static Object[] valid() {
        return new Object[] {EVIDENCE, "CALL_RECORDING", RECORDING, "SOLIDIFIED", EVIDENCE, MEDIA, "ORIGINAL", MEDIA,
                "a".repeat(64), 123L, "audio/wav", RECORDING, MEDIA, "VALIDATED", 10};
    }

    private static TransactionTemplate directTransactions() {
        var template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        return template;
    }

    private static EvidenceMediaObjectReader reader(Object[]... values) {
        return new JooqEvidenceMediaObjectReader(DSL.using(new MockConnection(context -> {
            assertThat(context.bindings()).containsExactly(EVIDENCE);
            assertThat(context.sql()).contains("LEFT JOIN evidence.evidence_artifact a ON a.evidence_id = er.id AND a.artifact_role = 'ORIGINAL'",
                    "LEFT JOIN evidence.recording r ON er.source_type = 'CALL_RECORDING'", "WHERE er.id = ?")
                    .doesNotContain("SELECT *", "LIMIT", "bucket", "object_key", "object_version_id");
            assertThat(context.sql().substring(7, context.sql().indexOf(" FROM ")).split(",")).hasSize(15);
            Field<?>[] fields = Arrays.stream(NAMES).map(name -> DSL.field(DSL.name(name), Object.class)).toArray(Field<?>[]::new);
            DSLContext database = DSL.using(SQLDialect.POSTGRES);
            var result = database.newResult(fields);
            for (Object[] value : values) {
                Record record = database.newRecord(fields);
                record.fromArray(value);
                result.add(record);
            }
            return new MockResult[] {new MockResult(values.length, result)};
        }), SQLDialect.POSTGRES), directTransactions());
    }

    @Test
    void validRowsResolveSafelyAndZeroOrMultipleRowsHaveDistinctResults() {
        Result result = reader(valid()).resolve(EVIDENCE);
        assertThat(result).isEqualTo(new Result.Found(new ResolvedMediaObject(MEDIA, "a".repeat(64), 123, "audio/wav", 10)));
        Object[] optionalDuration = valid();
        optionalDuration[14] = null;
        assertThat(reader(optionalDuration).resolve(EVIDENCE)).isInstanceOf(Result.Found.class);
        assertThat(reader().resolve(EVIDENCE)).isEqualTo(new Result.Missing());
        assertThat(reader(valid(), valid()).resolve(EVIDENCE)).isEqualTo(new Result.Unavailable());
    }

    @Test
    void incompleteInconsistentAndInvalidExistingFactsNeverBecomeMissing() {
        for (int index = 0; index < 14; index++) {
            Object[] values = valid();
            values[index] = null;
            assertThat(reader(values).resolve(EVIDENCE)).as("null field %s", NAMES[index]).isEqualTo(new Result.Unavailable());
        }
        Object[][] corruptions = {{0, UUID.randomUUID()}, {1, "OTHER"}, {2, UUID.randomUUID()}, {3, "PENDING"},
                {4, UUID.randomUUID()}, {5, UUID.randomUUID()}, {6, "DERIVED"}, {7, UUID.randomUUID()},
                {8, "A".repeat(64)}, {8, "short"}, {9, 0L}, {9, -1L}, {10, " "}, {11, UUID.randomUUID()},
                {12, UUID.randomUUID()}, {13, "UPLOADED"}, {14, 0}, {14, -1}};
        for (Object[] corruption : corruptions) {
            Object[] values = valid();
            values[(Integer) corruption[0]] = corruption[1];
            assertThat(reader(values).resolve(EVIDENCE)).as("corrupt field %s", corruption[0]).isEqualTo(new Result.Unavailable());
        }
    }

    @Test
    void nullInputPrecedesDatabaseAccessAndSqlAndTransactionFailuresAreOpaque() {
        var database = mock(DSLContext.class);
        var transactions = directTransactions();
        var reader = new JooqEvidenceMediaObjectReader(database, transactions);
        assertThatNullPointerException().isThrownBy(() -> reader.resolve(null));
        verifyNoInteractions(database, transactions);
        var failing = DSL.using(new MockConnection(context -> { throw new SQLException("synthetic-secret", "42501"); }), SQLDialect.POSTGRES);
        assertThat(new JooqEvidenceMediaObjectReader(failing, directTransactions()).resolve(EVIDENCE)).isEqualTo(new Result.Unavailable());
        doThrow(new IllegalStateException("synthetic-secret")).when(transactions).execute(any());
        assertThat(reader.resolve(EVIDENCE)).isEqualTo(new Result.Unavailable());
        verifyNoInteractions(database);
    }

    @Test
    void publicApiSerializationValidationAndStringFormsExposeOnlySafeMetadata() throws Exception {
        var media = new ResolvedMediaObject(MEDIA, "a".repeat(64), 123, "audio/wav", null);
        var mapper = JsonMapper.builder().build();
        var json = mapper.readTree(mapper.writeValueAsString(media));
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("mediaObjectId", "sha256", "sizeBytes", "mimeType", "durationSeconds");
        assertThat(json.get("mediaObjectId").asText()).isEqualTo(MEDIA.toString());
        assertThat(json.get("durationSeconds").isNull()).isTrue();
        assertThat(EvidenceMediaObjectReader.Result.class.isSealed()).isTrue();
        assertThat(Arrays.stream(EvidenceMediaObjectReader.Result.class.getPermittedSubclasses()).map(Class::getSimpleName))
                .containsExactlyInAnyOrder("Found", "Missing", "Unavailable");
        assertThat(EvidenceMediaObjectReader.class.getDeclaredMethods()).hasSize(1);
        for (Object value : List.of(media, new Result.Found(media), new Result.Missing(), new Result.Unavailable(), reader(valid()))) {
            assertThat(value.toString()).doesNotContain(MEDIA.toString(), "a".repeat(64), "audio/wav", "bucket", "object_key", "password");
        }
        assertThatNullPointerException().isThrownBy(() -> new Result.Found(null));
        assertThatNullPointerException().isThrownBy(() -> new ResolvedMediaObject(null, "a".repeat(64), 1, "audio/wav", null));
        assertThatNullPointerException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, null, 1, "audio/wav", null));
        assertThatNullPointerException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, "a".repeat(64), 1, null, null));
        for (String hash : List.of("", "A".repeat(64), "g".repeat(64), "a".repeat(63))) {
            assertThatIllegalArgumentException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, hash, 1, "audio/wav", null));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, "a".repeat(64), 0, "audio/wav", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, "a".repeat(64), 1, " ", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new ResolvedMediaObject(MEDIA, "a".repeat(64), 1, "audio/wav", 0));
    }
}
