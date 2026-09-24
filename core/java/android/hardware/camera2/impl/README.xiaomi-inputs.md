# Optional Xiaomi reprocess inputs

`config_cameraXiaomiVendorInputConfigurations` defaults to false. A product
with a compatible provider may overlay it to true to accept extra input
configurations from `xiaomi.scaler.availableStreamConfigurations`.

Each tuple is HAL format, width, height, direction. Only positive dimensions,
exact direction 1, recognized public formats and matching requested inputs are
accepted. Standard advertised inputs remain authoritative. Multi-resolution
and maximum-resolution requests do not use this compatibility path. Tags are
resolved per characteristics object; a missing tag never adds capabilities.

The native session client-name path in frameworks/av retains the existing
`camera.package_name` build setting. Alioth keeps `com.android.camera`.
A separate default-off opt-in enables the Xiaomi vendor-tag fallback:

```make
$(call soong_config_set_bool,camera,xiaomi_session_client_name,true)
```

This permits lookup of `com.xiaomi.sessionparams.clientName` when the original
configured lookup fails. The metadata value is still the bound owning client
package. Products without this flag do not use the Xiaomi fallback. Do not
replace the existing package setting with a vendor tag to enable this path.
