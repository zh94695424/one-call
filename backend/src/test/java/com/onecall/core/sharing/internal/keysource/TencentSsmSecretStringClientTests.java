package com.onecall.core.sharing.internal.keysource;

import static com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider.Settings;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.provider.CvmRoleCredential;
import com.tencentcloudapi.ssm.v20190923.SsmClient;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueRequest;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class TencentSsmSecretStringClientTests {
    private CapturedOutput output;
    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void explicitNameAndVersionReachOneSdkRead() throws Exception {
        var settings = Settings.create("test-region", "synthetic opaque /名称", "version /确切", null, null);
        SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenReturn(response(settings, valid()));
        AtomicInteger factories = new AtomicInteger();
        var client = new TencentSsmSecretStringClient(settings, configured -> { assertThat(configured).isSameAs(settings); factories.incrementAndGet(); return sdk; });
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings, client);
        verifyNoInteractions(sdk); assertThat(factories).hasValue(0);
        for (int count = 1; count <= 3; count++) {
            available(provider);
            verify(sdk, times(count)).GetSecretValue(any(GetSecretValueRequest.class));
        }
        var requests = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(sdk, times(3)).GetSecretValue(requests.capture());
        for (var request : requests.getAllValues()) {
            assertThat(request.getSecretName().equals(settings.secretName)).isTrue();
            assertThat(request.getVersionId().equals(settings.versionId)).isTrue();
            assertThat(request.getEncryptionPublicKey()).isNull();
            assertThat(request.getEncryptionAlgorithm()).isNull();
        }
        assertThat(factories).hasValue(1); verifyNoMoreInteractions(sdk);
    }

    @Test void roleOnlyFactoryIsLazyAndHasNoStaticFallback() throws Exception {
        try (var roles = mockConstruction(CvmRoleCredential.class);
                var clients = mockConstruction(SsmClient.class, (sdk, construction) -> {
                    assertThat(construction.arguments()).hasSize(3);
                    assertThat(construction.arguments().getFirst()).isInstanceOf(CvmRoleCredential.class);
                    when(sdk.GetSecretValue(any())).thenReturn(response(settings(), valid()));
                })) {
            var client = new TencentSsmSecretStringClient(settings());
            var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), client);
            assertThat(roles.constructed()).isEmpty(); assertThat(clients.constructed()).isEmpty();
            available(provider); available(provider);
            assertThat(roles.constructed()).hasSize(1); assertThat(clients.constructed()).hasSize(1);
            verifyNoInteractions(roles.constructed().getFirst());
            var invalid = new TencentSsmSecretStringClient(null);
            assertThat(invalid.read()).isNull(); assertThat(roles.constructed()).hasSize(1);
        }
        String source = source();
        assertThat(source).contains("new CvmRoleCredential()", "private synchronized SsmClient client()")
                .doesNotContain("DefaultCredentialsProvider", "EnvironmentVariable", "ProfileCredentials", "OIDC", "new Credential(",
                        "getenv", "getSnapshot", "metadata.tencentyun", "SecretId", "SecretKey", "getToken");
    }

    @Test void clientRegionHttpsTimeoutAndRetrySettingsAreExact() throws Exception {
        for (String[] bounds : List.of(new String[]{null, null}, new String[]{"1", "1"}, new String[]{"5", "10"})) {
            Settings settings = Settings.create("test-region", "synthetic-name", "synthetic-version", bounds[0], bounds[1]);
            List<ClientProfile> profiles = new ArrayList<>();
            try (var roles = mockConstruction(CvmRoleCredential.class);
                    var clients = mockConstruction(SsmClient.class, (sdk, construction) -> {
                        assertThat(construction.arguments().get(1)).isEqualTo("test-region");
                        profiles.add((ClientProfile) construction.arguments().get(2));
                        when(sdk.GetSecretValue(any())).thenReturn(response(settings, valid()));
                    })) {
                available(new TencentSsmMediaPlaybackSigningKeyProvider(settings, new TencentSsmSecretStringClient(settings)));
                assertThat(profiles).hasSize(1);
                ClientProfile profile = profiles.getFirst(); HttpProfile http = profile.getHttpProfile();
                assertThat(profile.getSignMethod()).isEqualTo(ClientProfile.SIGN_TC3_256);
                assertThat(profile.isDebug()).isFalse(); assertThat(profile.isUnsignedPayload()).isFalse();
                assertThat(profile.getBackupEndpoint()).isNull(); assertThat(profile.getLanguage()).isNull();
                assertThat(http.getReqMethod()).isEqualTo(HttpProfile.REQ_POST);
                assertThat(http.getProtocol()).isEqualTo("https://");
                assertThat(http.getEndpoint()).isEqualTo("ssm.tencentcloudapi.com");
                assertThat(http.getConnTimeout()).isEqualTo(bounds[0] == null ? 2 : Integer.parseInt(bounds[0]));
                assertThat(http.getReadTimeout()).isEqualTo(bounds[1] == null ? 3 : Integer.parseInt(bounds[1]));
                assertThat(http.getWriteTimeout()).isEqualTo(http.getConnTimeout());
                assertThat(http.getProxyHost()).isNull(); assertThat(http.getProxyUsername()).isNull(); assertThat(http.getProxyPassword()).isNull();
                assertThat(http.getSslSocketFactory()).isNull(); assertThat(http.getX509TrustManager()).isNull();
                assertThat(http.getHostnameVerifier()).isNull(); assertThat(http.getHttpClient()).isNull();
                assertThat(http.getRootDomain()).isEqualTo("tencentcloudapi.com"); assertThat(http.getApigwEndpoint()).isNull();
                verifyNoInteractions(roles.constructed().getFirst());
                verify(clients.constructed().getFirst()).GetSecretValue(any()); verifyNoMoreInteractions(clients.constructed().getFirst());
            }
        }
        assertThat(source()).doesNotContain(".retry(", "callTimeout", "retryOnConnectionFailure", "okhttp3", "TrustAll", "setProxy");
    }

    @Test void nullMissingBinaryAndMismatchedResponsesFailClosed() throws Exception {
        var responses = new ArrayList<GetSecretValueResponse>(); responses.add(null);
        for (String identity : Arrays.asList(null, "", "mismatch")) {
            var name = response(settings(), valid()); name.setSecretName(identity); responses.add(name);
            var version = response(settings(), valid()); version.setVersionId(identity); responses.add(version);
        }
        for (String secret : Arrays.asList(null, "", " ", "\r\n\t")) responses.add(response(settings(), secret));
        for (String binary : List.of(" ", "binary-canary")) for (String secret : Arrays.asList(null, "", valid())) {
            var response = response(settings(), secret); response.setSecretBinary(binary); responses.add(response);
        }
        for (var response : responses) {
            SsmClient sdk = mock(SsmClient.class); when(sdk.GetSecretValue(any())).thenReturn(response);
            unavailable(new TencentSsmMediaPlaybackSigningKeyProvider(settings(), new TencentSsmSecretStringClient(settings(), configured -> sdk)));
            verify(sdk).GetSecretValue(any()); verifyNoMoreInteractions(sdk);
        }
        for (String binary : Arrays.asList(null, "")) {
            SsmClient sdk = mock(SsmClient.class); var response = response(settings(), valid()); response.setSecretBinary(binary);
            when(sdk.GetSecretValue(any())).thenReturn(response);
            available(new TencentSsmMediaPlaybackSigningKeyProvider(settings(), new TencentSsmSecretStringClient(settings(), configured -> sdk)));
        }
    }

    @Test void authCamAndResourceStateFailuresAreUnavailable() throws Exception {
        failures(List.of("AuthFailure", "AuthFailure.SignatureFailure", "UnauthorizedOperation", "ResourceNotFound", "FailedOperation.SecretDisabled", "FailedOperation.SecretDestroyed"));
        AtomicInteger attempts = new AtomicInteger(); SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenReturn(response(settings(), valid()));
        var client = new TencentSsmSecretStringClient(settings(), configured -> {
            if (attempts.getAndIncrement() == 0) throw new IllegalStateException(new IOException(UUID.randomUUID().toString()));
            return sdk;
        });
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), client);
        unavailable(provider); available(provider); assertThat(attempts).hasValue(2); verify(sdk).GetSecretValue(any());
    }

    @Test void throttleUnknownAndServerFailuresAreUnavailable() throws Exception {
        failures(List.of("RequestLimitExceeded", "InternalError", "Unknown-" + UUID.randomUUID(), "ServiceUnavailable"));
        SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenThrow(new IllegalStateException(new IOException(UUID.randomUUID().toString())))
                .thenReturn(response(settings(), valid()));
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), new TencentSsmSecretStringClient(settings(), configured -> sdk));
        unavailable(provider); available(provider); verify(sdk, times(2)).GetSecretValue(any()); verifyNoMoreInteractions(sdk);
    }

    @Test void networkTlsAndTimeoutFailuresAreUnavailable() throws Exception {
        String canary = UUID.randomUUID().toString();
        for (Exception cause : List.of(new UnknownHostException(canary), new ConnectException(canary), new SocketException(canary),
                new SSLException(canary), new SocketTimeoutException(canary))) {
            SsmClient sdk = mock(SsmClient.class);
            when(sdk.GetSecretValue(any())).thenThrow(new TencentCloudSDKException(canary, cause));
            unavailable(new TencentSsmMediaPlaybackSigningKeyProvider(settings(), new TencentSsmSecretStringClient(settings(), configured -> sdk)));
            verify(sdk).GetSecretValue(any()); verifyNoMoreInteractions(sdk);
        }
        assertThat(output.getAll().contains(canary)).isFalse();
    }

    @Test void clientFailuresAndStringFormsNeverLeak() throws Exception {
        String canary = "synthetic-" + UUID.randomUUID();
        Settings settings = Settings.create("test-region", canary, canary, null, null);
        SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenThrow(new TencentCloudSDKException(canary, canary, canary))
                .thenThrow(new TencentCloudSDKException(canary, new IOException(canary)))
                .thenReturn(response(settings, valid()));
        var client = new TencentSsmSecretStringClient(settings, configured -> sdk);
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings, client);
        var mapper = JsonMapper.builder().build();
        String rendered = client + mapper.writeValueAsString(client) + settings + mapper.writeValueAsString(settings);
        for (int count = 0; count < 3; count++) {
            var result = provider.currentKeys();
            assertThat(result instanceof Result.Available).isEqualTo(count == 2);
            rendered += result + mapper.writeValueAsString(result);
        }
        rendered += output.getAll();
        assertThat(rendered.contains(canary) || rendered.contains(valid())).isFalse();
        assertThat(client.toString()).isEqualTo("TencentSsmSecretStringClient[REDACTED]");
    }

    private void failures(List<String> codes) throws Exception {
        String canary = UUID.randomUUID().toString();
        for (String code : codes) {
            SsmClient sdk = mock(SsmClient.class);
            when(sdk.GetSecretValue(any())).thenThrow(new TencentCloudSDKException(canary, canary, code));
            unavailable(new TencentSsmMediaPlaybackSigningKeyProvider(settings(), new TencentSsmSecretStringClient(settings(), configured -> sdk)));
            verify(sdk).GetSecretValue(any()); verifyNoMoreInteractions(sdk);
        }
        assertThat(output.getAll().contains(canary)).isFalse();
    }
    static GetSecretValueResponse response(Settings settings, String secret) {
        var response = new GetSecretValueResponse(); response.setSecretName(settings.secretName); response.setVersionId(settings.versionId);
        response.setSecretString(secret); response.setRequestId(UUID.randomUUID().toString()); return response;
    }
    private static String source() throws IOException {
        return Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClient.java"));
    }
}
