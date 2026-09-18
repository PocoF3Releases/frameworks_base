/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;

/** Additional reprocess inputs explicitly advertised by a Xiaomi camera provider. @hide */
final class XiaomiInputConfiguration {
    private static final String INPUTS = "xiaomi.scaler.availableStreamConfigurations";

    private XiaomiInputConfiguration() {}

    static boolean isSupported(CameraCharacteristics characteristics, InputConfiguration input) {
        if (characteristics == null || input == null || input.isMultiResolution()) return false;
        final int[] streams;
        try {
            // CameraMetadataNative caches the resolved numeric tag in the Key. A
            // process-wide Key could reuse provider A's tag when inspecting B.
            // Give each lookup a fresh key so resolution uses this metadata's
            // vendor namespace, including after a provider restart/replacement.
            // This does not register tags or manufacture stream capabilities.
            streams = characteristics.get(new CameraCharacteristics.Key<>(
                    INPUTS, int[].class));
        } catch (IllegalArgumentException | ClassCastException | UnsupportedOperationException unsupported) {
            return false;
        }
        // Decode the raw four-int tuples ourselves: the generic stream marshaler
        // treats every nonzero direction as input, including malformed values.
        if (streams == null || streams.length % 4 != 0) return false;
        for (int i = 0; i < streams.length; i += 4) {
            if (streams[i + 3] != 1 || streams[i + 1] <= 0 || streams[i + 2] <= 0
                    || streams[i + 1] != input.getWidth()
                    || streams[i + 2] != input.getHeight()) continue;
            try {
                // Vendor tuples use HAL formats (e.g. BLOB=0x21), not public JPEG=0x100.
                if (StreamConfigurationMap.imageFormatToPublic(streams[i])
                        == input.getFormat()) return true;
            } catch (IllegalArgumentException unsupported) {
                // An invalid tuple must not admit arbitrary input formats or dimensions.
            }
        }
        return false;
    }
}
