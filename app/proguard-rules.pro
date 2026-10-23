# kotlinx.serialization, Hilt and Navigation ship their own consumer rules.

# Retrofit reads response types from method signatures. A DTO that is only a type argument
# (HelixPage<FollowerDto>, where only `total` is read) would otherwise be removed and seen as Object:
# "Unable to create converter for HelixPage<Object>" on the channel page in release builds.
-keepattributes Signature
-keep,allowobfuscation class app.twicast.tv.data.remote.** { *; }
