package org.exoplatform.push.service.fcm;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Error object for FCM response binding
 */
public class FcmError {
    @Key("code")
    private int code;

    @Key("message")
    private String message;

    @Key("status")
    private String status;

    @Key("details")
    private List<FcmDetail> details;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public List<FcmDetail> getDetails() {
        return details;
    }
}
