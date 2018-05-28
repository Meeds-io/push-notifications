package org.exoplatform.push.service.fcm;

import com.google.api.client.util.Key;

/**
 * Response object for FCM response binding
 */
public class FcmResponse {
    @Key("error")
    private FcmError error;

    public FcmError getError() {
        return error;
    }
}
