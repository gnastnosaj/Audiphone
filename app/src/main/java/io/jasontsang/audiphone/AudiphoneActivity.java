package io.jasontsang.audiphone;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.wandoujia.ads.sdk.Ads;

import net.qiujuer.genius.widget.GeniusSeekBar;

import java.lang.reflect.Method;

import es.claucookie.miniequalizerlibrary.EqualizerView;
import rx.functions.Action1;

/**
 * Created by jasontsang on 2/29/16.
 */
public class AudiphoneActivity extends AppCompatActivity {

    AudioManager audioManager;
    RecordThread recordThread;
    VolumeReceiver volumeReceiver;
    Equalizer equalizer;
    BassBoost bassBoost;

    FrameLayout adWrapper;

    Button start;
    Button stop;
    Button exit;
    Button mic;
    GeniusSeekBar seekBar;
    EqualizerView equalizerView;
    LinearLayout equalizerWidget;

    Button standard;
    Button quiet;
    Button meeting;

    Button compatibility;

    boolean trackingTouch = false;
    boolean internalMic = false;
    boolean newDevice = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audiphone);

        adWrapper = (FrameLayout) findViewById(R.id.ad_wrapper);
        new RxPermissions(this).request(Manifest.permission.READ_PHONE_STATE).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean grant) {
                if (grant) {
                    try {
                        Ads.init(AudiphoneActivity.this, "100048054", "b25704d7c3184e5f36e65b5cf941ec83");
                        Ads.preLoad("dc80c0c4318fc7994e9f82a157556903", Ads.AdFormat.banner);
                        View bannerAdView = Ads.createBannerView(AudiphoneActivity.this, "dc80c0c4318fc7994e9f82a157556903");
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        adWrapper.addView(bannerAdView, layoutParams);
                    } catch (Exception e) {
                        Log.e("AudiphoneActivity", "wandoujia ad error", e);
                    }
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.e("AudiphoneActivity", "wandoujia ad error", throwable);
            }
        });

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        try {
            equalizer = new Equalizer(0, 0);
            bassBoost = new BassBoost(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        exit = (Button) findViewById(R.id.exit);
        mic = (Button) findViewById(R.id.mic);
        seekBar = (GeniusSeekBar) findViewById(R.id.seekBar);
        equalizerView = (EqualizerView) findViewById(R.id.equalizer_view);
        equalizerWidget = (LinearLayout) findViewById(R.id.equalizer);

        standard = (Button) findViewById(R.id.standard);
        quiet = (Button) findViewById(R.id.quiet);
        meeting = (Button) findViewById(R.id.meeting);

        compatibility = (Button) findViewById(R.id.compatibility);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordThread == null) {
                    recordThread = new RecordThread();
                    recordThread.start();
                    equalizerView.animateBars();
                    equalizerView.setVisibility(View.VISIBLE);
                    if (equalizer != null) {
                        equalizer.release();
                        equalizer = null;
                    }
                    if (bassBoost != null) {
                        bassBoost.release();
                        bassBoost = null;
                    }
                    try {
                        equalizer = new Equalizer(0, recordThread.getAudioSessionId());
                        equalizer.setEnabled(true);
                        bassBoost = new BassBoost(0, recordThread.getAudioSessionId());
                        bassBoost.setEnabled(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    initEqualizer();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bassBoost != null) {
                    bassBoost.release();
                    bassBoost = null;
                }
                if (equalizer != null) {
                    equalizer.release();
                    equalizer = null;
                }
                if (recordThread != null) {
                    equalizerView.setVisibility(View.INVISIBLE);
                    equalizerView.stopBars();
                    recordThread.cancel();
                    recordThread = null;
                }
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMic();
                if (recordThread == null) {
                    recordThread = new RecordThread();
                    recordThread.start();
                } else {
                    recordThread.restart();
                }
            }
        });

        final int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar.setProgress(100 * current / max);

        volumeReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);

        seekBar.setOnSeekBarChangeListener(new GeniusSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(GeniusSeekBar seekBar, int progress, boolean fromUser) {
                if (trackingTouch) {
                    int volume = max * progress / 100;
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(GeniusSeekBar seekBar) {
                trackingTouch = true;
                unregisterReceiver(volumeReceiver);
            }

            @Override
            public void onStopTrackingTouch(GeniusSeekBar seekBar) {
                trackingTouch = false;
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");
                registerReceiver(volumeReceiver, filter);
            }
        });

        initEqualizer();

        changeMic();

        standard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int volume = max * 90 / 100;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            }
        });

        quiet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int volume = max * 30 / 100;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            }
        });

        meeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int volume = max * 60 / 100;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            }
        });

        compatibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newDevice) {
                    newDevice = false;
                } else {
                    newDevice = true;
                }

                internalMic = false;

                changeMic();

                if (recordThread == null) {
                    recordThread = new RecordThread();
                    recordThread.start();
                } else {
                    recordThread.restart();
                }
            }
        });

        new RxPermissions(this).request(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
        .subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean granted) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });
    }

    private void initEqualizer() {
        equalizerWidget.removeAllViews();

        if (equalizer != null) {
            final short minEQLevel = equalizer.getBandLevelRange()[0];
            short maxEQLevel = equalizer.getBandLevelRange()[1];
            short brands = equalizer.getNumberOfBands();
            for (short i = 0; i < brands; i++) {
                View bandView = LayoutInflater.from(this).inflate(R.layout.item_band, null);
                TextView title = (TextView) bandView.findViewById(R.id.title);
                title.setText((equalizer.getCenterFreq(i) / 1000) + "Hz");
                VerticalSeekBar verticalSeekBar = (VerticalSeekBar) bandView.findViewById(R.id.band);
                verticalSeekBar.setMax(maxEQLevel - minEQLevel);
                verticalSeekBar.setProgress(equalizer.getBandLevel(i));
                final short finalI = i;
                verticalSeekBar.setOnSeekBarChangeListener(new SeekBar
                        .OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (equalizer != null) {
                            equalizer.setBandLevel(finalI, (short) (progress + minEQLevel));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                equalizerWidget.addView(bandView);
            }
        }

        if(bassBoost != null) {
            View bandView = LayoutInflater.from(this).inflate(R.layout.item_band, null);
            TextView title = (TextView) bandView.findViewById(R.id.title);
            title.setText("重低音");
            VerticalSeekBar verticalSeekBar = (VerticalSeekBar) bandView.findViewById(R.id.band);
            verticalSeekBar.setMax(1000);
            verticalSeekBar.setProgress(0);
            verticalSeekBar.setOnSeekBarChangeListener(new SeekBar
                    .OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar
                        , int progress, boolean fromUser) {
                    if (bassBoost != null) {
                        bassBoost.setStrength((short) progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            equalizerWidget.addView(bandView);
        }
    }

    private void changeMic() {
        if(internalMic) {

            internalMic = false;

            RecordThread.isCAMCORDER = false;

            try {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setBluetoothScoOn(true);
                audioManager.startBluetoothSco();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(newDevice) {
                try {
                    Class audioSystemClass = Class.forName("android.media.AudioSystem");
                    Method setForceUse = audioSystemClass.getMethod("setForceUse", int.class, int.class);
                    int result = (int)setForceUse.invoke(null, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else {

            internalMic = true;

            RecordThread.isCAMCORDER = true;

            try{
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(newDevice) {
                RecordThread.isCAMCORDER = false;

                try {
                    Class audioSystemClass = Class.forName("android.media.AudioSystem");
                    Method setForceUse = audioSystemClass.getMethod("setForceUse", int.class, int.class);
                    setForceUse.invoke(null, 0, 1);
                    setForceUse.invoke(null, 0, 1);
                    setForceUse.invoke(null, 0, 1);
                    setForceUse.invoke(null, 0, 1);
                    setForceUse.invoke(null, 0, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }
        if(equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if(recordThread != null) {
            recordThread.cancel();
        }
        if(volumeReceiver != null) {
            unregisterReceiver(volumeReceiver);
        }
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private class VolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                seekBar.setProgress(100 * current / max);
            }
        }
    }
}
