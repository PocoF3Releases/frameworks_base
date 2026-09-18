/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media.audiofx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Optional compatibility client for an installed OZO capture effect.
 *
 * <p>This class does not implement the native processor, install a tuning file,
 * or enable capture processing automatically. Clients must supply a recording
 * session and handle {@link #create} returning null. It is independent of the
 * MIUI MediaRecorder/Stagefright OZO extensions.
 *
 * @hide
 */
public class OzoAudioEffect extends AudioEffect {
    private static final UUID OZO_EFFECT_TYPE =
            UUID.fromString("56d6b082-1a83-455a-84a8-9db3a35cf532");
    private static final UUID OZO_EFFECT_UUID =
            UUID.fromString("7e384a3b-7850-4a64-a097-884250d8a737");

    private static final int PARAM_DEVICE = 111111;
    private static final int PARAM_WNR_LEVEL = 111119;
    private static final int PARAM_AUDIO_LEVEL = 111121;
    private static final int PARAM_MICBLOCKING_MODE = 111122;
    private static final int PARAM_MICBLOCKING_LEVEL = 111123;
    private static final int PARAM_GENERIC = 111124;
    private static final int MAX_MIC_EVENTS = 8;

    /** @hide */
    public interface MicBlockingCallback {
        void onMicBlocking(int micIndex, int level);
    }

    /** MIUI client parameter spellings; not a declaration of native support. @hide */
    public static final class OzoParameters {
        public static final String DISABLED = "off";
        public static final String ENABLED = "on";
        public static final String FEAT_CUSTOM = "custom";
        public static final String FEAT_FOCUS = "focus";
        public static final String FEAT_FOCUSAZIMUTH = "focus-azimuth";
        public static final String FEAT_FOCUSELEVATION = "focus-elevation";
        public static final String FEAT_FOCUSHEIGHT = "focus-height";
        public static final String FEAT_FOCUSWIDTH = "focus-width";
        public static final String FEAT_NOISESUPPRESSION = "ns";
        public static final String FEAT_WINDSCREEN = "wnr";
        public static final String FEAT_ZOOM = "zoom";
        public static final String NS_SMART = "smart";

        private OzoParameters() {}
    }

    private OzoAudioEffect(int audioSession) {
        super(OZO_EFFECT_TYPE, OZO_EFFECT_UUID, 0, audioSession);
    }

    /** Returns null for an unavailable backend or an invalid recording session. */
    public static OzoAudioEffect create(int audioSession) {
        // Never create a capture effect on the global output mix.
        if (audioSession <= 0) return null;
        try {
            Descriptor[] effects = queryEffects();
            if (effects != null) {
                for (Descriptor effect : effects) {
                    if (effect != null && OZO_EFFECT_UUID.equals(effect.uuid)
                            && OZO_EFFECT_TYPE.equals(effect.type)) {
                        // Enumeration can race service death or effect removal.
                        return new OzoAudioEffect(audioSession);
                    }
                }
            }
        } catch (RuntimeException unavailable) {
            // Preserve the optional factory contract, including a dead audio service.
        }
        return null;
    }

    public int setDevice(String id) {
        if (id == null || id.isEmpty() || id.indexOf('\0') >= 0) return ERROR_BAD_VALUE;
        byte[] value = id.getBytes(StandardCharsets.UTF_8);
        if (value.length > 36) return ERROR_BAD_VALUE;
        return setParameter(PARAM_DEVICE, value);
    }

    public String getDevice() {
        byte[] value = new byte[36];
        int size = getParameter(PARAM_DEVICE, value);
        if (size <= 0 || size > value.length) return null;
        int end = 0;
        while (end < size && value[end] != 0) end++;
        return new String(value, 0, end, StandardCharsets.UTF_8);
    }

    public int enableWnr() { return setOzoParameter("wnr", "on"); }
    public int disableWnr() { return setOzoParameter("wnr", "off"); }

    public int getWnrLevel() {
        byte[] value = new byte[1];
        return getParameter(PARAM_WNR_LEVEL, value) == value.length
                ? value[0] : ERROR_BAD_VALUE;
    }

    public int[] getAudioLevel() {
        byte[] value = new byte[8];
        if (getParameter(PARAM_AUDIO_LEVEL, value) != value.length) {
            return new int[] {-1, -1};
        }
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.nativeOrder());
        return new int[] {buffer.getInt(), buffer.getInt()};
    }

    public int enableNs(boolean smart) { return setOzoParameter("ns", smart ? "smart" : "on"); }
    public int disableNs() { return setOzoParameter("ns", "off"); }
    public int enableMicBlocking() {
        return setParameter(PARAM_MICBLOCKING_MODE, "on".getBytes(StandardCharsets.UTF_8));
    }

    public void getMicBlocking(MicBlockingCallback callback) {
        if (callback == null) return;
        byte[] value = new byte[4 + MAX_MIC_EVENTS * 8];
        int size = getParameter(PARAM_MICBLOCKING_LEVEL, value);
        if (size < 4 || size > value.length) return;
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.nativeOrder());
        int count = buffer.getInt();
        // Reject truncated/invalid replies before delivering any partial notification.
        // Compare before multiplying: a vendor count must not overflow a size check.
        if (count < 0 || count > MAX_MIC_EVENTS || count > (size - 4) / 8) return;
        for (int i = 0; i < count; i++) {
            callback.onMicBlocking(buffer.getInt(), buffer.getInt());
        }
    }

    public int enableFocus() { return setOzoParameter("focus", "on"); }
    public int disableFocus() { return setOzoParameter("focus", "off"); }
    public int setFocusGain(double gain) { return setFiniteParameter("zoom", gain); }
    public int setFocusAzimuth(double azimuth) {
        return setFiniteParameter("focus-azimuth", azimuth);
    }
    public int setFocusElevation(double elevation) {
        return setFiniteParameter("focus-elevation", elevation);
    }
    public int setFocusWidth(double width) { return setFiniteParameter("focus-width", width); }
    public int setFocusHeight(double height) { return setFiniteParameter("focus-height", height); }

    private int setFiniteParameter(String key, double value) {
        return Double.isFinite(value) ? setOzoParameter(key, Double.toString(value)) : ERROR_BAD_VALUE;
    }

    public int setOzoParameter(String key, String value) {
        if (key == null || key.isEmpty() || key.indexOf('=') >= 0 || key.indexOf('\0') >= 0
                || value == null || value.indexOf('\0') >= 0) return ERROR_BAD_VALUE;
        // The vendor ABI is UTF-8 text, not a binary float or a list of int parameters.
        return setParameter(PARAM_GENERIC, (key + "=" + value).getBytes(StandardCharsets.UTF_8));
    }
}
