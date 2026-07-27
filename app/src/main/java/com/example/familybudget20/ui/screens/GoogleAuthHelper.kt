package com.example.familybudget20.ui.screens

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Web client ID généré par Firebase quand le provider Google est activé
// (Authentication > Sign-in method > Google). Récupérable ensuite dans
// google-services.json sous oauth_client (client_type: 3).
private const val GOOGLE_WEB_CLIENT_ID =
    "637545960686-3k83k68egifnrvutdji7s7ugui7658l6.apps.googleusercontent.com"

data class GoogleIdentity(
    val idToken: String,
    val givenName: String?,
    val displayName: String?
)

// Ouvre le sélecteur de compte Google natif et retourne l'identité choisie.
// Lève GetCredentialException (annulation, aucun compte, etc.) ou
// IllegalStateException si la réponse n'a pas le format attendu.
suspend fun requestGoogleIdentity(context: Context): GoogleIdentity {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(GOOGLE_WEB_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credentialManager = CredentialManager.create(context)
    val response = credentialManager.getCredential(
        context = context as Activity,
        request = request
    )

    val credential = response.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleIdentity(
            idToken = googleCredential.idToken,
            givenName = googleCredential.givenName,
            displayName = googleCredential.displayName
        )
    } else {
        throw IllegalStateException("Format de compte Google inattendu")
    }
}
