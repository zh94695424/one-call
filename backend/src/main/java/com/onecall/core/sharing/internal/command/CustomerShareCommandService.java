package com.onecall.core.sharing.internal.command;

public interface CustomerShareCommandService {
    CustomerShareCommandResult execute(CustomerShareCommand command, ShareCommandCaller caller);
}
