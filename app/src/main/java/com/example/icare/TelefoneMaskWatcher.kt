package com.example.icare

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class TelefoneMaskWatcher(private val editText: EditText) : TextWatcher {
    private var isUpdating = false
    private val mask = "(##) #####-####"

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val str = unmask(s.toString())
        var formatted = ""
        
        if (isUpdating) {
            isUpdating = false
            return
        }

        var i = 0
        for (m in mask.toCharArray()) {
            if (m != '#' && str.length > i) {
                formatted += m
                continue
            }
            try {
                formatted += str[i]
            } catch (e: Exception) {
                break
            }
            i++
        }

        isUpdating = true
        editText.setText(formatted)
        editText.setSelection(formatted.length)
    }

    override fun afterTextChanged(s: Editable?) {}

    private fun unmask(s: String): String {
        return s.replace("[^\\d]".toRegex(), "")
    }
}
