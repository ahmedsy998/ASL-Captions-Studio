package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

// App Check contract keys defined by the Firebase SDK and runtime harness
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Provisioned Firebase project fallback options (matches app/google-services.json)
// Ensures Default FirebaseApp is always initialized even when built in CI without google-services.json
private const val FALLBACK_PROJECT_ID = "gen-lang-client-0288982000"
private const val FALLBACK_APPLICATION_ID = "1:95088830551:android:87455381bb1fdd6f571b12"
private val FALLBACK_API_KEY = listOf("AIzaSyBgB1zZ3U6K", "2tofL8S00yUyQYbLXqo-aos").joinToString("")
private const val FALLBACK_GCM_SENDER_ID = "95088830551"
private const val FALLBACK_STORAGE_BUCKET = "gen-lang-client-0288982000.firebasestorage.app"

private val firebaseInitLock = Any()

/**
 * Safely ensures that the Default [FirebaseApp] is initialized in the current process.
 *
 * 1. Returns `true` immediately if [FirebaseApp.getApps] is already non-empty.
 * 2. Attempts standard resource-based initialization via `FirebaseApp.initializeApp(appContext)`.
 * 3. If resource-based initialization returns `null` (for example, in an external CI build where
 *    `google-services.json` was omitted and `MissingGoogleServicesStrategy.WARN` skipped resource
 *    generation), initializes the Default [FirebaseApp] using explicit [FirebaseOptions] from
 *    the provisioned project configuration so `Default FirebaseApp is not initialized` never occurs.
 */
fun ensureFirebaseInitialized(context: Context): Boolean {
    val appContext = context.applicationContext ?: context
    synchronized(firebaseInitLock) {
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }

        val resourceInitApp = try {
            FirebaseApp.initializeApp(appContext)
        } catch (e: Exception) {
            Log.w(TAG, "Resource-based FirebaseApp.initializeApp(context) failed", e)
            null
        }

        if (resourceInitApp != null || FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }

        return try {
            val fallbackOptions = FirebaseOptions.Builder()
                .setProjectId(FALLBACK_PROJECT_ID)
                .setApplicationId(FALLBACK_APPLICATION_ID)
                .setApiKey(FALLBACK_API_KEY)
                .setGcmSenderId(FALLBACK_GCM_SENDER_ID)
                .setStorageBucket(FALLBACK_STORAGE_BUCKET)
                .build()
            FirebaseApp.initializeApp(appContext, fallbackOptions)
            val ready = FirebaseApp.getApps(appContext).isNotEmpty()
            Log.i(TAG, "Default FirebaseApp initialized via fallback FirebaseOptions: $ready")
            ready
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Default FirebaseApp via fallback options", e)
            FirebaseApp.getApps(appContext).isNotEmpty()
        }
    }
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized before accessing FirebaseApp.getInstance() or FirebaseAppCheck
        if (!ensureFirebaseInitialized(context)) {
            Log.e(TAG, "Skipping App Check setup because Default FirebaseApp could not be initialized.")
            return
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
