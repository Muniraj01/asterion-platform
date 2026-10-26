
package com.asterion.ledger.infrastructure.messaging;

public class PermanentLedgerEventException extends RuntimeException {

    public PermanentLedgerEventException(String message) {
        super(message);
    }

    public PermanentLedgerEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
