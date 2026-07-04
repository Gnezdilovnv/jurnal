package com.example.reports.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar

object NotificationHelper {
    fun showSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(view, message, duration).show()
    }

    fun showSnackbar(view: View, message: String, actionText: String, action: (View) -> Unit) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setAction(actionText) { action(it) }
            .show()
    }
}
