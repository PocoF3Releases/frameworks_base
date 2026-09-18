/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;

/** Additional reprocess inputs explicitly advertised by a Xiaomi camera provider. @hide */
final class XiaomiInputConfiguration {
    private static final String INPUTS = "xiaomi.scaler.availableStreamConfigurations";

    private XiaomiInputConfiguration() {}

    static boolean isSupported(CameraCharacteristics characteristics, InputConfiguration input) {
        if (characteristics == null || input == null || input.isMultiResolution()) return false;
        final StreamConfiguration[] streams;
        try {
            // CameraMetadataNative caches the resolved numeric tag in the Key. A
            // process-wide Key could reuse provider A's tag when inspecting B.
            // Give each lookup a fresh key so resolution uses this metadata's
            // vendor namespace, including after a provider restart/replacement.
            // This does not register tags or manufacture stream capabilities.
            streams = characteristics.get(new CameraCharacteristics.Key<>(
                    INPUTS, StreamConfiguration[].class));
        } catch (IllegalArgumentException | ClassCastException unsupported) {
            return false;
        }
        if (streams == null) return false;
        for (StreamConfiguration stream : streams) {
            if (stream == null || !stream.isInput() || stream.getWidth() != input.getWidth()
                    || stream.getHeight() != input.getHeight()) continue;
            try {
                // Vendor tuples use HAL formats (e.g. BLOB=0x21), not public JPEG=0x100.
                if (StreamConfigurationMap.imageFormatToPublic(stream.getFormat())
                        == input.getFormat()) return true;
            } catch (IllegalArgumentException unsupported) {
                // An invalid tuple must not admit arbitrary input formats or dimensions.
            }
        }
        return false;
    }
}
