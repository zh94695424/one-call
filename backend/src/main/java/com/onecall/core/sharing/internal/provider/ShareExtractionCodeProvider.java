package com.onecall.core.sharing.internal.provider;

import java.util.UUID;

public interface ShareExtractionCodeProvider {
    ProviderResult<ExtractionCodeMaterial> generate(UUID grantId, int nextCodeGeneration);
}
