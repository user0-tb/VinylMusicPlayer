package com.poupa.vinylmusicplayer.ui.activities.base;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.helper.MusicPlayerRemote;
import com.poupa.vinylmusicplayer.interfaces.MusicServiceEventListener;
import com.poupa.vinylmusicplayer.service.MusicService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsMusicServiceActivity extends AbsBaseActivity implements MusicServiceEventListener {

    private final ArrayList<MusicServiceEventListener> mMusicServiceEventListeners = new ArrayList<>();

    private MusicPlayerRemote.ServiceToken serviceToken;
    private MusicStateReceiver musicStateReceiver;
    private boolean receiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceToken = MusicPlayerRemote.bindToService(this, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AbsMusicServiceActivity.this.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                AbsMusicServiceActivity.this.onServiceDisconnected();
            }
        });

        setPermissionDeniedMessage(getString(R.string.permission_external_storage_denied));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicPlayerRemote.unbindFromService(serviceToken);
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver);
            receiverRegistered = false;
        }
    }

    public void addMusicServiceEventListener(final MusicServiceEventListener listener) {
        if (listener != null) {
            mMusicServiceEventListeners.add(listener);
        }
    }

    public void removeMusicServiceEventListener(final MusicServiceEventListener listener) {
        if (listener != null) {
            mMusicServiceEventListeners.remove(listener);
        }
    }

    @Override
    public void onServiceConnected() {
        if (!receiverRegistered) {
            musicStateReceiver = new MusicStateReceiver(this);

            final IntentFilter filter = new IntentFilter();
            filter.addAction(MusicService.PLAY_STATE_CHANGED);
            filter.addAction(MusicService.SHUFFLE_MODE_CHANGED);
            filter.addAction(MusicService.REPEAT_MODE_CHANGED);
            filter.addAction(MusicService.META_CHANGED);
            filter.addAction(MusicService.QUEUE_CHANGED);
            filter.addAction(MusicService.MEDIA_STORE_CHANGED);
            filter.addAction(MusicService.FAVORITE_STATE_CHANGED);

            registerReceiver(musicStateReceiver, filter);

            receiverRegistered = true;
        }

        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onServiceConnected();
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver);
            receiverRegistered = false;
        }

        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onServiceDisconnected();
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onPlayingMetaChanged();
            }
        }
    }

    @Override
    public void onQueueChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onQueueChanged();
            }
        }
    }

    @Override
    public void onPlayStateChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onPlayStateChanged();
            }
        }
    }

    @Override
    public void onMediaStoreChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onMediaStoreChanged();
            }
        }
    }

    @Override
    public void onRepeatModeChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onRepeatModeChanged();
            }
        }
    }

    @Override
    public void onShuffleModeChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onShuffleModeChanged();
            }
        }
    }

    private static final class MusicStateReceiver extends BroadcastReceiver {

        private final WeakReference<AbsMusicServiceActivity> reference;

        public MusicStateReceiver(final AbsMusicServiceActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(final Context context, @NonNull final Intent intent) {
            final String action = intent.getAction();
            AbsMusicServiceActivity activity = reference.get();
            if (activity != null) {
                switch (action) {
                    case MusicService.FAVORITE_STATE_CHANGED:
                    case MusicService.META_CHANGED:
                        activity.onPlayingMetaChanged();
                        break;
                    case MusicService.QUEUE_CHANGED:
                        activity.onQueueChanged();
                        break;
                    case MusicService.PLAY_STATE_CHANGED:
                        activity.onPlayStateChanged();
                        break;
                    case MusicService.REPEAT_MODE_CHANGED:
                        activity.onRepeatModeChanged();
                        break;
                    case MusicService.SHUFFLE_MODE_CHANGED:
                        activity.onShuffleModeChanged();
                        break;
                    case MusicService.MEDIA_STORE_CHANGED:
                        activity.onMediaStoreChanged();
                        break;
                }
            }
        }
    }

    @Override
    protected void onHasPermissionsChanged(boolean hasPermissions) {
        super.onHasPermissionsChanged(hasPermissions);
        Intent intent = new Intent(MusicService.MEDIA_STORE_CHANGED);
        intent.putExtra("from_permissions_changed", true); // just in case we need to know this at some point
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    protected String[] getPermissionsToRequest() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.R) { // API less or equal to 29
            return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        } else if (Build.VERSION.SDK_INT < VERSION_CODES.TIRAMISU) { // API less than 33
            return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
        } else {
            // TODO: only audio is really necessary for the app to work, images is only useful for cover, should try with it has an option
            return new String[] { Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES };
        }
    }
}
