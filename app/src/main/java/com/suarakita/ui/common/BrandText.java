package com.suarakita.ui.common;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TypefaceSpan;

// Beberapa kata kunci brand ("Kita", "Tercatat", "Voting", dst) ditulis pakai
// font script/cursive sebagai aksen visual, sisanya font biasa.
public class BrandText {

    private BrandText() {
    }

    public static SpannableString accent(String regularPart, String scriptPart) {
        String full = regularPart + scriptPart;
        SpannableString spannable = new SpannableString(full);
        spannable.setSpan(
                new TypefaceSpan("cursive"),
                regularPart.length(),
                full.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return spannable;
    }
}
