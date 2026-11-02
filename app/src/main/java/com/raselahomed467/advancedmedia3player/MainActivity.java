
package com.raselahomed467.advancedmedia3player;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TrackSelectionDialogBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;



public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    private EditText urlInput;
    private EditText cookieInput;
    private EditText refererInput;
    private EditText userAgentInput;
    private EditText originInput;
    private EditText authInput;
    private EditText drmUrlInput;
    private EditText drmHeadersInput;

    private TextView statusText;

    private boolean fullscreen = false;

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(9, 13, 22));
        getWindow().setNavigationBarColor(Color.rgb(9, 13, 22));

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        buildScreen();
        initializePlayer();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 13, 22));

        TextView title = new TextView(this);
        title.setText("ADVANCED MEDIA3 PLAYER");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), dp(12), dp(16), dp(12));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        root.addView(
                title,
                new LinearLayout.LayoutParams(-1, dp(52))
        );

        // Video Player

        playerView = new PlayerView(this);

        playerView.setUseController(true);

        playerView.setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        );

        playerView.setResizeMode(
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        );

        playerView.setBackgroundColor(Color.BLACK);

        root.addView(
                playerView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        0.30f
                )
        );

        // Scrollable Input Section

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(20)
        );

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        0.70f
                )
        );

        content.addView(
                sectionTitle("STREAM / REQUEST SETTINGS")
        );

        urlInput = addField(
                content,
                "Video URL (MPD / M3U8 / MKV / MP4)",
                false
        );

        cookieInput = addField(
                content,
                "Cookie (optional)",
                false
        );

        refererInput = addField(
                content,
                "Referer (optional)",
                false
        );

        userAgentInput = addField(
                content,
                "User-Agent (optional)",
                false
        );

        originInput = addField(
                content,
                "Origin (optional)",
                false
        );

        authInput = addField(
                content,
                "Authorization (optional)",
                false
        );

        drmUrlInput = addField(
                content,
                "DRM License URL - Widevine (optional)",
                false
        );

        drmHeadersInput = addField(
                content,
                "DRM Headers (Key: Value per line)",
                true
        );

        // Play Button

        Button play = makeButton(
                "▶  PLAY STREAM",
                true
        );

        play.setOnClickListener(v -> playStream());

        content.addView(
                play,
                buttonParams()
        );

        // Player Tools

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);

        Button audio = makeButton("AUDIO", false);
        Button subtitles = makeButton("SUBTITLES", false);
        Button speed = makeButton("SPEED", false);
        Button full = makeButton("FULLSCREEN", false);

        tools.addView(
                audio,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        tools.addView(
                subtitles,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        tools.addView(
                speed,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        tools.addView(
                full,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        content.addView(tools);

        audio.setOnClickListener(
                v -> showTrackSelection(
                        C.TRACK_TYPE_AUDIO,
                        "Audio Tracks"
                )
        );

        subtitles.setOnClickListener(
                v -> showTrackSelection(
                        C.TRACK_TYPE_TEXT,
                        "Subtitle Tracks"
                )
        );

        speed.setOnClickListener(v -> showSpeedDialog());

        full.setOnClickListener(v -> toggleFullscreen());

        // Status

        statusText = new TextView(this);
        statusText.setText("Ready - Enter a stream URL");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(12);

        statusText.setPadding(
                dp(4),
                dp(12),
                dp(4),
                dp(4)
        );

        content.addView(statusText);

        setContentView(root);
    }

    private TextView sectionTitle(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.rgb(85, 194, 255));
        view.setTextSize(12);
        view.setTypeface(null, android.graphics.Typeface.BOLD);

        view.setPadding(0, dp(8), 0, dp(8));

        return view;
    }

    private EditText addField(
            LinearLayout parent,
            String hint,
            boolean multiline
    ) {

        EditText field = new EditText(this);

        field.setSingleLine(!multiline);
        field.setHint(hint);

        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.GRAY);
        field.setTextSize(14);

        field.setPadding(
                dp(13),
                dp(8),
                dp(13),
                dp(8)
        );

        field.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(75, 95, 120)
                )
        );

        if (multiline) {
            field.setMinLines(2);
            field.setGravity(Gravity.TOP | Gravity.START);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        multiline ? dp(82) : dp(54)
                );

        params.bottomMargin = dp(8);

        parent.addView(field, params);

        return field;
    }

    private Button makeButton(
            String text,
            boolean primary
    ) {

        Button button = new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(primary ? 15 : 11);
        button.setTextColor(Color.WHITE);

        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        primary
                                ? Color.rgb(0, 126, 220)
                                : Color.rgb(35, 48, 67)
                )
        );

        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(52));

        params.topMargin = dp(5);
        params.bottomMargin = dp(10);

        return params;
    }

    // Initialize ExoPlayer

    private void initializePlayer() {

        OkHttpClient client =
                new OkHttpClient.Builder().build();

        OkHttpDataSource.Factory httpFactory =
                new OkHttpDataSource.Factory(client);

        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(this)
                        .setDataSourceFactory(httpFactory);

       // 5.1 Surround to Stereo Downmix
ChannelMixingAudioProcessor downmixProcessor =
        new ChannelMixingAudioProcessor();

// 3.0 to Stereo
downmixProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantPower(3, 2));

// 4.0 to Stereo
downmixProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantPower(4, 2));

// 5.0 to Stereo
downmixProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantPower(5, 2));

// 5.1 to Stereo
downmixProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantPower(6, 2));

// Custom Audio Renderer
DefaultRenderersFactory renderersFactory =
        new DefaultRenderersFactory(this) {

    @Override
    protected AudioSink buildAudioSink(
            android.content.Context context,
            boolean enableFloatOutput,
            boolean enableAudioTrackPlaybackParams,
            boolean enableOffload) {

        return new DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(
                        enableAudioTrackPlaybackParams)
                .setOffloadMode(
                        DefaultAudioSink.OFFLOAD_MODE_DISABLED)
                .setAudioProcessors(
                        new AudioProcessor[]{downmixProcessor})
                .build();
    }
};

renderersFactory.setEnableDecoderFallback(true);

// Create ExoPlayer
player = new ExoPlayer.Builder(this, renderersFactory).build();

        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {

                if (state == Player.STATE_BUFFERING) {
                    setStatus("Buffering...");
                }

                else if (state == Player.STATE_READY) {
                    setStatus("Playing / Ready");
                }

                else if (state == Player.STATE_ENDED) {
                    setStatus("Playback ended");
                }

                else if (state == Player.STATE_IDLE) {
                    setStatus("Ready");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {

                setStatus(
                        "Playback error: "
                                + error.getErrorCodeName()
                                + " - "
                                + error.getMessage()
                );
            }
        });
    }

    // Play Stream With Custom Headers

    private void playStream() {

        String url = value(urlInput);

        if (url.isEmpty()) {
            toast("প্রথমে Video URL দিন");
            return;
        }

        Map<String, String> headers =
                new LinkedHashMap<>();

        putHeader(headers, "Cookie", value(cookieInput));
        putHeader(headers, "Referer", value(refererInput));
        putHeader(headers, "User-Agent", value(userAgentInput));
        putHeader(headers, "Origin", value(originInput));
        putHeader(headers, "Authorization", value(authInput));

        try {

            OkHttpClient client =
                    new OkHttpClient.Builder().build();

            OkHttpDataSource.Factory httpFactory =
                    new OkHttpDataSource.Factory(client);

            if (!headers.isEmpty()) {
                httpFactory.setDefaultRequestProperties(headers);
            }

            DefaultMediaSourceFactory sourceFactory =
                    new DefaultMediaSourceFactory(this)
                            .setDataSourceFactory(httpFactory);

            MediaItem mediaItem = buildMediaItem(url);

            player.setMediaSource(
                    sourceFactory.createMediaSource(mediaItem)
            );

            player.prepare();
            player.play();

            setStatus("Loading stream...");

        } catch (Exception e) {

            setStatus(
                    "Could not start: " + e.getMessage()
            );
        }
    }

    // Media Item and DRM Configuration

    private MediaItem buildMediaItem(String url) {

        MediaItem.Builder item =
                new MediaItem.Builder()
                        .setUri(Uri.parse(url));

        String licenseUrl = value(drmUrlInput);

        if (!licenseUrl.isEmpty()) {

            Map<String, String> drmHeaders =
                    parseHeaders(value(drmHeadersInput));

            MediaItem.DrmConfiguration.Builder drm =
                    new MediaItem.DrmConfiguration.Builder(
                            C.WIDEVINE_UUID
                    ).setLicenseUri(licenseUrl);

            if (!drmHeaders.isEmpty()) {
                drm.setLicenseRequestHeaders(drmHeaders);
            }

            item.setDrmConfiguration(drm.build());
        }

        return item.build();
    }

    // Parse Custom Headers

    private Map<String, String> parseHeaders(String raw) {

        Map<String, String> result =
                new LinkedHashMap<>();

        for (String line : raw.split("\\r?\\n")) {

            int split = line.indexOf(':');

            if (split > 0) {

                String key =
                        line.substring(0, split).trim();

                String val =
                        line.substring(split + 1).trim();

                if (!key.isEmpty() && !val.isEmpty()) {
                    result.put(key, val);
                }
            }
        }

        return result;
    }

    private void putHeader(
            Map<String, String> map,
            String key,
            String value
    ) {

        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value.trim());
        }
    }

    private String value(EditText edit) {

        return edit == null
                ? ""
                : edit.getText().toString().trim();
    }

    // Audio and Subtitle Selection

    private void showTrackSelection(
            int trackType,
            String title
    ) {

        if (player == null) return;

        new TrackSelectionDialogBuilder(
                this,
                title,
                player,
                trackType
        )
                .setTrackNameProvider(format -> {

                    String name = format.label != null
                            ? format.label
                            : format.language;

                    if (name == null) {
                        name = format.sampleMimeType;
                    }

                    if (name == null) {
                        name = "Track";
                    }

                    if (format.channelCount > 0) {
                        name += " - "
                                + format.channelCount
                                + "ch";
                    }

                    if (format.sampleMimeType != null) {
                        name += " - "
                                + format.sampleMimeType;
                    }

                    return name;
                })
                .build()
                .show();
    }

    // Playback Speed

    private void showSpeedDialog() {

        String[] speeds = {
                "0.25x",
                "0.5x",
                "0.75x",
                "1.0x",
                "1.25x",
                "1.5x",
                "2.0x",
                "2.5x",
                "3.0x"
        };

        float[] values = {
                0.25f,
                0.5f,
                0.75f,
                1f,
                1.25f,
                1.5f,
                2f,
                2.5f,
                3f
        };

        new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setItems(speeds, (dialog, which) ->
                        player.setPlaybackSpeed(values[which])
                )
                .show();
    }

    // Fullscreen

    private void toggleFullscreen() {

        fullscreen = !fullscreen;

        if (fullscreen) {

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            );

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

        } else {

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            );

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }

    private void setStatus(String message) {

        if (statusText != null) {
            statusText.setText(message);
        }
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onStop() {

        super.onStop();

        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (player != null) {

            playerView.setPlayer(null);

            player.release();

            player = null;
        }
    }
}
