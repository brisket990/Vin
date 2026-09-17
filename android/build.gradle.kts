// Top-level build file: plugin versions are declared here (with apply false)
// and applied per-module in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    // Reads app/google-services.json and wires up the Firebase project at
    // build time (push notifications -- see NOTIFICATIONS_SETUP.md).
    alias(libs.plugins.google.services) apply false
}
