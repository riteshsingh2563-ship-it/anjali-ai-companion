package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
    val persistenceKey = FirebaseApp.getInstance().persistenceKey
    val prefs = context.getSharedPreferences(
      "$DEBUG_STORE_PREFIX$persistenceKey",
      Context.MODE_PRIVATE
    )

    if (!debugToken.isNullOrEmpty()) {
      // Verified debug token passed via Intent (AI Studio preview runner or test harness)
      prefs.edit()
        .putString(DEBUG_SECRET_KEY, debugToken)
        .commit()
      FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
      )
      Log.d(TAG, "App Check debug secret configured successfully from launch Intent.")
    } else if (prefs.contains(DEBUG_SECRET_KEY)) {
      // Previously stored debug token from AI Studio preview runner
      FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
      )
      Log.d(TAG, "App Check debug secret restored from SharedPreferences.")
    } else {
      // Real physical device or shared APK without a pre-registered debug token.
      // Installing DebugAppCheckProviderFactory here would generate an unregistered random UUID
      // that Google's App Check server rejects with HTTP 403. Use reCAPTCHA Enterprise instead.
      val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
      if (resId != 0) {
        val siteKey = context.getString(resId)
        if (siteKey.isNotBlank()) {
          FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
          )
          Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for physical device.")
        } else {
          Log.w(TAG, "recaptcha_site_key resource string is blank.")
        }
      } else {
        Log.w(TAG, "recaptcha_site_key string resource not found in package.")
      }
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}
