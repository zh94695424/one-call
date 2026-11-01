package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.application.SharePageRequest;
import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import java.util.List;

public interface ShareQueryRepository {
    void requireActiveCreatorContext(TrustedShareCaller caller);
    List<ShareSummary> fetch(TrustedShareCaller caller, SharePageRequest request, int fetchSize);
}
