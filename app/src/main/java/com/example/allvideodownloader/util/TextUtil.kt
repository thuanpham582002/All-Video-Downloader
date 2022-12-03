package com.example.allvideodownloader.util

import android.widget.Toast
import androidx.core.text.isDigitsOnly
import com.example.allvideodownloader.App.Companion.applicationScope
import com.example.allvideodownloader.App.Companion.context
import com.example.allvideodownloader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

object TextUtil {

    fun String.isNumberInRange(start: Int, end: Int): Boolean {
        return this.isNotEmpty() && this.isDigitsOnly() && this.length < 10 && this.toInt() >= start && this.toInt() <= end
    }

    fun matchUrlFromClipboard(s: String): String {
        matchUrlFromString(s).run {
            if (isEmpty())
                makeToast(R.string.paste_fail_msg)
            else
                makeToast(R.string.paste_msg)
            return this
        }
    }

    fun matchUrlFromSharedText(s: String): String {
        matchUrlFromString(s).run {
            if (isEmpty())
                makeToast(R.string.share_fail_msg)
            else
                makeToast(R.string.share_success_msg)
            return this
        }
    }

    private fun matchUrlFromString(s: String): String {
        val builder = StringBuilder()
        val pattern =
            Pattern.compile("(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?")
        with(pattern.matcher(s)) {
            if (PreferenceUtil.getValue(PreferenceUtil.CUSTOM_COMMAND))
                while (find()) {
                    if (builder.isNotEmpty())
                        builder.append("\n")
                    builder.append(group())
                }
            else if (find())
                builder.append(group())
        }
        return builder.toString()
    }

    fun String?.toHttpsUrl(): String =
        this?.run {
            if (matches(Regex("^(http:).*"))) replaceFirst("http", "https") else this
        } ?: ""


    fun makeToast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun makeToastSuspend(text: String) {
        applicationScope.launch(Dispatchers.Main) {
            makeToast(text)
        }
    }

    fun makeToast(stringId: Int) {
        Toast.makeText(context, context.getString(stringId), Toast.LENGTH_SHORT).show()
    }
}