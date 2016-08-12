package com.futurice.rxandroidchatexercise;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.jakewharton.rxbinding.view.RxView;

import java.net.URISyntaxException;
import java.util.ArrayList;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private CompositeSubscription subscription;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subscription = new CompositeSubscription();

        try {
            socket = IO.socket("http://10.0.2.2:3000");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }

        if (socket != null) {
            Observable<String> messages = createMessageListener(socket);
            subscription.add(
                    messages
                            .scan(new ArrayList<>(),
                                    (list, value) -> {
                                        list.add(value);
                                        return list;
                                    })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    msg -> {
                                        Log.d(TAG, "chat message: " + msg);
                                    }));
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "connection1");
            });
            socket.connect();
        }

        RxView.clicks(findViewById(R.id.send_button))
                .subscribe(ev -> {
                    socket.emit("chat message", "hello world");
                });

    }

    private static Observable<String> createMessageListener(final Socket socket) {
        return Observable.create(subscriber -> {
            final Emitter.Listener listener =
                    args -> subscriber.onNext(args[0].toString());
            socket.on("chat message", listener);
            subscriber.add(BooleanSubscription.create(
                    () -> {
                        Log.d(TAG, "unsubscribe");
                        socket.off("chat message", listener);
                    }));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
        if (subscription != null) {
            subscription.clear();
            subscription = null;
        }
    }
}
