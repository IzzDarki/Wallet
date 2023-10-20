package com.izzdarki.wallet.logic.updates

import android.content.Context
import android.graphics.text.LineBreaker
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.textview.MaterialTextView
import com.izzdarki.wallet.utils.Utility.setPaddingBottom
import izzdarki.wallet.R

fun Context.showUpdateAlert(versionName: String, content: List<Pair<String, String>>) {
    val linearLayout = createUpdateView(content)

    // Add title
    val titleView = MaterialTextView(this)
    titleView.setTextAppearance(R.style.TextAppearance_Material3_TitleLarge)
    titleView.setPaddingBottom(resources.getDimension(R.dimen.default_padding).toInt())
    titleView.text = getString(R.string.updated_to_x).format(versionName)
    linearLayout.addView(titleView, 0)

    val scrollView = ScrollView(this)
    scrollView.addView(linearLayout)

    // Create an AlertDialog
    AlertDialog.Builder(this)
        .setView(scrollView)
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        .setCancelable(false)
        .show()
}

fun Context.createUpdateView(content: List<Pair<String, String>>): LinearLayoutCompat {
    val linearLayout = LinearLayoutCompat(this)
    linearLayout.orientation = LinearLayoutCompat.VERTICAL
    linearLayout.setPadding(
        resources.getDimension(R.dimen.default_padding).toInt(),
        resources.getDimension(R.dimen.default_padding).toInt(),
        resources.getDimension(R.dimen.default_padding).toInt(),
        resources.getDimension(R.dimen.default_padding).toInt(),
    )

    for ((headline, text) in content) {
        val headlineView = MaterialTextView(this)
        headlineView.setTextAppearance(R.style.TextAppearance_Material3_TitleMedium)
        headlineView.setPaddingBottom(resources.getDimension(R.dimen.title_text_space).toInt())
        headlineView.text = headline

        val textView = MaterialTextView(this)
        textView.setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
        if (android.os.Build.VERSION.SDK_INT >= 29)
            textView.breakStrategy = LineBreaker.BREAK_STRATEGY_HIGH_QUALITY
        textView.setPaddingBottom(resources.getDimension(R.dimen.default_padding).toInt())
        textView.text = text

        linearLayout.addView(headlineView)
        linearLayout.addView(textView)
    }

    return linearLayout
}
