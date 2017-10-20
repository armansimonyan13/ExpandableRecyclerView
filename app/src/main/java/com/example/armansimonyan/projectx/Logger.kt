package com.example.armansimonyan.projectx

import android.util.Log

/**
 * Created by armansimonyan on 10/20/17.
 */

object Logger {

	fun log(string: String, vararg args: Any) {
		Log.d(Constants.TAG, String.format(string, args))
	}

}
