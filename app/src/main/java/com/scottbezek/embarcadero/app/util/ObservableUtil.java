package com.scottbezek.embarcadero.app.util;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccount.Listener;
import com.dropbox.sync.android.DbxAccountInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
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

    public static <T> Func1<List<T>, List<T>>  sort(final Comparator<T> comparator) {
        return new Func1<List<T>, List<T>>() {
            @Override
            public List<T> call(List<T> input) {
                List<T> sorted = new ArrayList<>(input);
                Collections.sort(sorted, comparator);
                return sorted;
            }
        };
    }
}
