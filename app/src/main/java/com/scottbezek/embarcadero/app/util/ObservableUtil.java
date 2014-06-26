package com.scottbezek.embarcadero.app.util;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccount.Listener;
import com.dropbox.sync.android.DbxAccountInfo;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class ObservableUtil {

    private ObservableUtil() {}

    public static Observable<DbxAccountInfo> createAccountInfoObservable(final DbxAccount account) {
        return Observable.create(new OnSubscribe<DbxAccountInfo>() {
            @Override
            public void call(final Subscriber<? super DbxAccountInfo> subscriber) {
                final Listener accountChangeListener = new Listener() {
                    @Override
                    public void onAccountChange(DbxAccount dbxAccount) {
                        final DbxAccountInfo info = dbxAccount.getAccountInfo();
                        if (info != null) {
                            subscriber.onNext(info);
                        }
                    }
                };
                account.addListener(accountChangeListener);

                final DbxAccountInfo initialInfo = account.getAccountInfo();
                if (initialInfo != null) {
                    subscriber.onNext(account.getAccountInfo());
                }

                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        account.removeListener(accountChangeListener);
                    }
                }));
            }
        });
    }
}
