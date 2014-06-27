package com.scottbezek.embarcadero.app.util;

import android.content.res.Resources;

import com.scottbezek.embarcadero.app.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocaleUtil {

    private LocaleUtil() {}

    public static Locale getSupportedLocale(Resources resources) {
        final String language = resources.getString(R.string.locale_language);
        final String country = resources.getString(R.string.locale_country);
        if (country.isEmpty()) {
            return new Locale(language);
        } else {
            return new Locale(language, country);
        }
    }

    public static DateFormat getDateFormat(Resources resources, int formatStringRes) {
        return new SimpleDateFormat(resources.getString(formatStringRes), getSupportedLocale(resources));
    }
}
