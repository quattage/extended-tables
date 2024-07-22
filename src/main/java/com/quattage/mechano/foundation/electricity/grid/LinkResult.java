package com.quattage.mechano.foundation.electricity.grid;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/***
 * A LinkResult is a simple representation of the actions that should follow
 * when GridEdges are created between GridVertices. LinkResults also generate their
 * own translation keys for displaying.
 */
public enum LinkResult {

    INITIATED(true),
    SUCCESS(true),
    USER_CANCEL(false),
    ALREADY_EXISTS(false, false),       

    DESTINATION_UNSUPPORTED(false),       
    DESTINATION_FULL(false, false),  
    
    TOO_LONG(false),            
    OBSTRUCTED(false),               

    GENERIC(true);

    private final String key;
    private final Component message;
    private final boolean success;
    private final boolean fatal;

    private LinkResult(boolean success) {
        this.success = success;
        this.fatal = true;
        this.key = makeKey();
        this.message = Component.translatable(key);
    }

    private LinkResult(boolean success, boolean fatal) {
        this.success = success;
        this.fatal = fatal;
        this.key = makeKey();
        this.message = Component.translatable(key);
    }


    private String makeKey() {
        return "actionbar.mechano.connection." + this;
    }
    
    /**
     * Whether this condition represents a successful connection or not
     * @return True if this condition can proceed without potential errors.
     */
    public boolean isSuccessful() {
        return success;
    }

    /**
     * In the event of a failure case, isFatal can be used to check whether 
     * implementations proceed without completely cancelling the connection.
     * @return True if this is a fatal case, always false if this is a successful case.
     */
    public boolean isFatal() {
        if(success) return false;
        return fatal;
    }

    public Component getMessage() {
        return message;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns this NodeConnectResult's enum declaration as a String.
     */
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}