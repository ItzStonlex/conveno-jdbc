package net.conveno.jdbc.response;

import lombok.NonNull;

import java.util.ArrayList;

public class ConvenoTransactionResponse extends ArrayList<ConvenoResponse> {

    public final void addAll(@NonNull ConvenoTransactionResponse transactionResponse) {
        super.addAll(transactionResponse);
    }
}
