package com.onecall.core.sharing.internal.keysource;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider.Settings;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.provider.CvmRoleCredential;
import com.tencentcloudapi.ssm.v20190923.SsmClient;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueRequest;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueResponse;

final class TencentSsmSecretStringClient implements TencentSsmMediaPlaybackSigningKeyProvider.Reader {
    private final Settings settings;
    private final ClientFactory factory;
    private SsmClient client;

    TencentSsmSecretStringClient(Settings settings) { this(settings, TencentSsmSecretStringClient::productionClient); }

    TencentSsmSecretStringClient(Settings settings, ClientFactory factory) {
        this.settings = settings;
        this.factory = factory;
    }

    @Override public String read() {
        try {
            if (settings == null) return null;
            GetSecretValueRequest request = new GetSecretValueRequest();
            request.setSecretName(settings.secretName);
            request.setVersionId(settings.versionId);
            GetSecretValueResponse response = client().GetSecretValue(request);
            if (response == null || !settings.secretName.equals(response.getSecretName())
                    || !settings.versionId.equals(response.getVersionId())) return null;
            String binary = response.getSecretBinary();
            String secret = response.getSecretString();
            if (binary != null && !binary.isEmpty() || secret == null || secret.isBlank()) return null;
            return secret;
        } catch (TencentCloudSDKException | RuntimeException failure) { return null; }
    }

    private synchronized SsmClient client() {
        if (client == null) client = factory.create(settings);
        return client;
    }

    private static SsmClient productionClient(Settings settings) {
        HttpProfile http = new HttpProfile();
        http.setReqMethod(HttpProfile.REQ_POST);
        http.setProtocol(HttpProfile.REQ_HTTPS);
        http.setEndpoint("ssm.tencentcloudapi.com");
        http.setConnTimeout(settings.connectSeconds);
        http.setReadTimeout(settings.readSeconds);
        http.setWriteTimeout(settings.connectSeconds);
        ClientProfile profile = new ClientProfile(ClientProfile.SIGN_TC3_256, http);
        profile.setDebug(false);
        profile.setUnsignedPayload(false);
        return new SsmClient(new CvmRoleCredential(), settings.region, profile);
    }

    @FunctionalInterface
    interface ClientFactory { SsmClient create(Settings settings); }

    @JsonValue public String redacted() { return "TencentSsmSecretStringClient[REDACTED]"; }
    @Override public String toString() { return redacted(); }
}
