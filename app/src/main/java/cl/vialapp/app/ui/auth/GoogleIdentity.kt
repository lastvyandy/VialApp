package cl.vialapp.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal class GoogleSignInCancelled : Exception()
internal class GoogleSignInUnavailable(cause: Throwable) : Exception("Google sign-in unavailable", cause)

internal object GoogleIdentity{
    suspend fun getToken(context:Context,clientId:String):String{
        var c=context
        while(c is ContextWrapper&&c !is Activity)c=c.baseContext
        val activity=c as? Activity ?: error("Activity required")
        val option=GetSignInWithGoogleOption.Builder(clientId).build()
        val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response=try {
            CredentialManager.create(activity).getCredential(context=activity, request=request)
        } catch (e: GetCredentialCancellationException) {
            throw GoogleSignInCancelled()
        } catch (e: GetCredentialException) {
            throw GoogleSignInUnavailable(e)
        }
        val credential=response.credential
        check(credential is CustomCredential&&credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
