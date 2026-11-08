package com.onecall.archtests.evidence;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.support.MediaGatewayTestProviders.*;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource.*;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ControlledMediaByteSourceTests {
    private ResolvedMediaObject media(UUID id) { return new ResolvedMediaObject(id, digest(PAYLOAD), PAYLOAD.length, "audio/wav", 10); }

    @Test void byteIntervalsAndOpenRequestsRejectUnboundedOrNullScope() {
        for (long[] bounds : List.of(new long[]{-1, 1}, new long[]{0, 0}, new long[]{2, 1}))
            assertThatThrownBy(() -> new ByteInterval(bounds[0], bounds[1])).hasMessage("Invalid byte interval").hasNoCause();
        var media = media(UUID.randomUUID());
        assertThatThrownBy(() -> new OpenRequest(null, new ByteInterval(0, 1))).hasMessage("Invalid byte request");
        assertThatThrownBy(() -> new OpenRequest(media, null)).hasMessage("Invalid byte request");
        assertThatThrownBy(() -> new OpenRequest(media, new ByteInterval(0, 17))).hasMessage("Invalid byte request");
        assertThatThrownBy(() -> new Result.Opened(null)).hasMessage("Invalid byte session");
        assertThat(new ByteInterval(0, Long.MAX_VALUE).length()).isEqualTo(Long.MAX_VALUE);
        assertThat(Result.class.isSealed()).isTrue();
    }
    @Test void deterministicSourceBindsObjectHashSizeAndExactInterval() throws Exception {
        UUID id = UUID.randomUUID();
        var source = new Source(id, PAYLOAD);
        for (var wrong : List.of(media(UUID.randomUUID()), new ResolvedMediaObject(id, "a".repeat(64), 16, "audio/wav", 10),
                new ResolvedMediaObject(id, digest(PAYLOAD), 17, "audio/wav", 10)))
            assertThat(source.open(new OpenRequest(wrong, new ByteInterval(2, 6)))).isInstanceOf(Result.Unavailable.class);
        assertThat(source.reads).hasValue(0);
        for (int chunk : List.of(1, 3, 17)) {
            source.chunk = chunk;
            try (var session = ((Result.Opened) source.open(new OpenRequest(media(id), new ByteInterval(2, 6)))).session()) {
                var output = new ByteArrayOutputStream(); byte[] buffer = new byte[7];
                int count;
                while ((count = session.read(buffer, 1, 6)) != -1) output.write(buffer, 1, count);
                assertThat(output.toByteArray()).isEqualTo("2345".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                assertThat(session.read(buffer, 0, 1)).isEqualTo(-1);
            }
        }
        assertThat(source.positions).allMatch(position -> position >= 2 && position <= 6);
        assertThat(source.closes).hasValue(3);
    }
    @Test void sessionsCloseAbortAndRejectReadsAfterTerminalState() throws Exception {
        for (boolean abortFirst : List.of(true, false)) {
            UUID id = UUID.randomUUID(); var source = new Source(id, PAYLOAD);
            var session = ((Result.Opened) source.open(new OpenRequest(media(id), new ByteInterval(0, 16)))).session();
            if (abortFirst) { session.abort(); session.abort(); }
            session.close(); session.close(); session.abort();
            assertThat(source.aborts).hasValue(abortFirst ? 1 : 0);
            assertThat(source.closes).hasValue(1);
            assertThatThrownBy(() -> session.read(new byte[1], 0, 1)).isInstanceOf(IOException.class).hasMessage("Media session closed").hasNoCause();
        }
        UUID id = UUID.randomUUID(); var source = new Source(id, PAYLOAD); source.fault = Fault.OPEN;
        assertThatThrownBy(() -> source.open(new OpenRequest(media(id), new ByteInterval(0, 16)))).isInstanceOf(IOException.class);
        assertThat(source.partialReleases).isEqualTo(1);
        assertThat(source.last).isNull();
        for (Fault fault : List.of(Fault.CLOSE, Fault.CLEANUP)) {
            var failing = new Source(id, PAYLOAD); failing.fault = fault;
            var session = ((Result.Opened) failing.open(new OpenRequest(media(id), new ByteInterval(0, 16)))).session();
            if (fault == Fault.CLEANUP) assertThatThrownBy(session::abort).isInstanceOf(IOException.class);
            assertThatThrownBy(session::close).isInstanceOf(IOException.class);
            session.close(); session.abort();
            assertThat(failing.closes).hasValue(1);
            assertThatThrownBy(() -> session.read(new byte[1], 0, 1)).hasMessage("Media session closed");
        }
    }
    @Test void byteRequestsSessionsAndFailuresHaveRedactedRepresentations() throws Exception {
        UUID id = UUID.randomUUID(); var source = new Source(id, PAYLOAD);
        var request = new OpenRequest(media(id), new ByteInterval(0, 16));
        var result = (Result.Opened) source.open(request);
        var mapper = JsonMapper.builder().build();
        for (Object value : List.of(request, request.interval(), result, result.session())) {
            assertThat(value.toString()).contains("REDACTED").doesNotContain(id.toString(), digest(PAYLOAD), PRIVATE_CANARY, "012345");
            assertThat(mapper.writeValueAsString(value)).contains("REDACTED").doesNotContain(id.toString(), digest(PAYLOAD), "sizeBytes");
        }
        result.session().close();
        assertThat(OpenRequest.class.getRecordComponents()).extracting(java.lang.reflect.RecordComponent::getName).containsExactly("media", "interval");
    }
}
