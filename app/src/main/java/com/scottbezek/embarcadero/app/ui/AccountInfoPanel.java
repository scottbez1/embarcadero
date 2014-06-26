package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccountInfo;
import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.util.SubscribeWhileAttached;

import rx.Observable;
import rx.Observer;

public class AccountInfoPanel extends LinearLayout {

    public AccountInfoPanel(Context context, Observable<DbxAccountInfo> accountInfo) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.account_info_panel, this, true);

        final TextView userName = (TextView)findViewById(R.id.user_name);
        final TextView orgName = (TextView)findViewById(R.id.org_name);
        addOnAttachStateChangeListener(new SubscribeWhileAttached<DbxAccountInfo>(accountInfo, new Observer<DbxAccountInfo>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onNext(DbxAccountInfo dbxAccountInfo) {
                userName.setText(dbxAccountInfo.userName);
                if (dbxAccountInfo.orgName != null) {
                    orgName.setText(dbxAccountInfo.orgName);
                }
                orgName.setVisibility(dbxAccountInfo.orgName == null ? GONE : VISIBLE);
            }
        }));
    }


}
