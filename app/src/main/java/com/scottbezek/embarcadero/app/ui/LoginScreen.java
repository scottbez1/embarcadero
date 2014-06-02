package com.scottbezek.embarcadero.app.ui;

import com.scottbezek.embarcadero.app.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class LoginScreen extends LinearLayout {

    public interface Callbacks {
        void onSignInClicked();
    }

    public LoginScreen(Context context, final Callbacks callbacks) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.screen_login, this, true);

        View signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callbacks.onSignInClicked();
            }
        });
    }
}
