package com.serhatyurdakul.todo.app.ui.helper

import android.app.Activity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.Const
import com.serhatyurdakul.todo.app.ui.main.MainActivity
import java.util.*

/*
* This class helps to perform firebase auth using gmail or 'email and password'
* It creates and starts the firebase auth UI
* It also returns the current logged in user anytime
*/
interface AuthHelper {

    // get current logged in user
    fun currentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    // config and start firebase auth UI
    fun signIn(activity: MainActivity) {
        if(!activity.isSignInPageVisible) {
            activity.isSignInPageVisible = true
            val providers = Arrays.asList(
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.EmailBuilder().build()
            )

            activity.startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setLogo(R.drawable.ic_logo)
                    .build(),
                Const.RequestCode.AUTH
            )
        }
    }
}