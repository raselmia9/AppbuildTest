package com.raselahomed467.advancedmedia3player;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.common.util.UnstableApi;

import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;

import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TrackSelectionDialogBuilder;
import androidx.media3.ui.AspectRatioFrameLayout;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;

@UnstableApi
public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    private LinearLayout rootLayout;
    private TextView titleBar;
    private ScrollView inputScroll;

    private FrameLayout playerContainer;
    private FrameLayout topControlsOverlay;
    private LinearLayout bottomControlsOverlay;

    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private TextView currentTimeView;
    private TextView totalDurationView;

    private EditText urlInput;
    private TextView statusText;

    private boolean fullscreen = false;
    private int resizeMode = 0;
    private boolean isTrackingTouch = false;

    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;

    private final int BG_COLOR = Color.rgb(9, 13, 22);
    private final int BUTTON_COLOR = Color.rgb(35, 48, 67);
    private final int PRIMARY_COLOR = Color.rgb(0, 126, 220);

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG_COLOR);
        getWindow().setNavigationBarColor(BG_COLOR);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        buildScreen();
        initializePlayer();

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (fullscreen) {
                            toggleFullscreen();
                        } else {
                            finish();
                        }
                    }
                }
        );
    }

    // ==========================================
    // BUILD USER INTERFACE
    // ==========================================

    private void buildScreen() {

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(BG_COLOR);

        titleBar = new TextView(this);
        titleBar.setText("ADVANCED MEDIA3 PLAYER");
        titleBar.setTextColor(Color.WHITE);
        titleBar.setTextSize(18);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        titleBar.setTypeface(null, android.graphics.Typeface.BOLD);

        rootLayout.addView(
                titleBar,
                new LinearLayout.LayoutParams(-1, dp(52))
        );

        // Player Container
        playerContainer = new FrameLayout(this);
        playerContainer.setBackgroundColor(Color.BLACK);

        playerView = new PlayerView(this);
        playerView.setUseController(false); // Custom on-screen overlay controls
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setBackgroundColor(Color.BLACK);

        playerContainer.addView(
                playerView,
                new FrameLayout.LayoutParams(-1, -1)
        );

        // Tap video to toggle controls visibility
        playerView.setOnClickListener(v -> toggleControls());

        // Build Top Overlay (Fullscreen Title & Back button)
        buildTopOverlay();

        // Build Unified Bottom Controller Bar (Screenshot design)
        buildBottomControls();

        rootLayout.addView(
                playerContainer,
                new LinearLayout.LayoutParams(-1, 0, 0.30f)
        );

        // Input section
        inputScroll = new ScrollView(this);
        inputScroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(20));

        inputScroll.addView(content);

        rootLayout.addView(
                inputScroll,
                new LinearLayout.LayoutParams(-1, 0, 0.70f)
        );

        content.addView(
                sectionTitle("STREAM / DRM / REQUEST SETTINGS")
        );

        urlInput = addField(
                content,
                "Video URL | Referer=...&Cookie=...&drmScheme=...",
                true
        );

        urlInput.setMinLines(3);
        urlInput.setTextSize(13);

        // Play Button
        TextView playButton = makeActionButton(
                "▶   PLAY STREAM",
                true
        );

        playButton.setOnClickListener(v -> playStream());

        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(-1, dp(52));

        playParams.topMargin = dp(8);
        playParams.bottomMargin = dp(8);

        content.addView(playButton, playParams);

        // Status
        statusText = new TextView(this);
        statusText.setText("Ready - Enter a stream URL");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(12);
        statusText.setPadding(dp(4), dp(12), dp(4), dp(4));

        content.addView(statusText);

        setContentView(rootLayout);
    }

    // ==========================================
    // TOP OVERLAY (FULLSCREEN BAR)
    // ==========================================

    private void buildTopOverlay() {
        topControlsOverlay = new FrameLayout(this);
        GradientDrawable topGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(190, 0, 0, 0), Color.TRANSPARENT}
        );
        topControlsOverlay.setBackground(topGradient);
        topControlsOverlay.setPadding(dp(14), dp(12), dp(14), dp(16));
        topControlsOverlay.setVisibility(View.GONE);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTextSize(24);
        backBtn.setPadding(dp(6), dp(4), dp(14), dp(4));
        backBtn.setClickable(true);
        backBtn.setOnClickListener(v -> {
            if (fullscreen) {
                toggleFullscreen();
            }
        });
        topBar.addView(backBtn);

        TextView playerTitle = new TextView(this);
        playerTitle.setText("Advanced Media3 Player");
        playerTitle.setTextColor(Color.WHITE);
        playerTitle.setTextSize(15);
        playerTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        topBar.addView(playerTitle);

        topControlsOverlay.addView(
                topBar,
                new FrameLayout.LayoutParams(-1, -2)
        );

        FrameLayout.LayoutParams topParams =
                new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        playerContainer.addView(topControlsOverlay, topParams);
    }

    // ==========================================
    // BOTTOM OVERLAY CONTROLLER (MATCHES SCREENSHOT)
    // ==========================================

    private void buildBottomControls() {
        bottomControlsOverlay = new LinearLayout(this);
        bottomControlsOverlay.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bottomGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(170, 0, 0, 0), Color.argb(235, 0, 0, 0)}
        );
        bottomControlsOverlay.setBackground(bottomGradient);
        bottomControlsOverlay.setPadding(dp(10), dp(20), dp(10), dp(10));

        // 1. BUTTONS ROW (Centered horizontal bar)
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setFillViewport(true);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        // 1: Fullscreen / Aspect Ratio
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_FULLSCREEN, Color.WHITE, 22),
                "Fullscreen",
                v -> toggleFullscreen()
        ));

        // 2: Audio Tracks
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_AUDIO, Color.WHITE, 22),
                "Audio Tracks",
                v -> showTrackSelection(C.TRACK_TYPE_AUDIO, "Audio Tracks")
        ));

        // 3: Picture in Picture
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_PIP, Color.WHITE, 22),
                "Picture in Picture",
                v -> enterPictureInPicture()
        ));

        // 4: Rewind 10 Seconds
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_REWIND_10, Color.WHITE, 22),
                "Rewind 10 Seconds",
                v -> seekRelative(-10000)
        ));

        // 5: Play / Pause (Center)
        btnPlayPause = createControlButton(
                createVectorIcon(SVG_PLAY, Color.WHITE, 24),
                "Play / Pause",
                v -> togglePlayPause()
        );
        buttonRow.addView(btnPlayPause);

        // 6: Forward 10 Seconds
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_FORWARD_10, Color.WHITE, 22),
                "Forward 10 Seconds",
                v -> seekRelative(10000)
        ));

        // 7: Subtitles (CC)
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_SUBTITLES, Color.WHITE, 22),
                "Subtitles",
                v -> showTrackSelection(C.TRACK_TYPE_TEXT, "Subtitle Tracks")
        ));

        // 8: Settings (Quality & Speed)
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_SETTINGS, Color.WHITE, 22),
                "Settings",
                v -> showSettingsDialog()
        ));

        // 9: Aspect Ratio / Resize Mode
        buttonRow.addView(createControlButton(
                createVectorIcon(SVG_ASPECT, Color.WHITE, 22),
                "Resize Mode",
                v -> cycleResizeMode()
        ));

        hScroll.addView(
                buttonRow,
                new FrameLayout.LayoutParams(-1, -2)
        );

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(-1, -2);
        scrollParams.bottomMargin = dp(6);
        bottomControlsOverlay.addView(hScroll, scrollParams);

        // 2. TIMELINE ROW (Time - Seekbar - Duration)
        LinearLayout timelineRow = new LinearLayout(this);
        timelineRow.setOrientation(LinearLayout.HORIZONTAL);
        timelineRow.setGravity(Gravity.CENTER_VERTICAL);
        timelineRow.setPadding(dp(6), 0, dp(6), 0);

        currentTimeView = new TextView(this);
        currentTimeView.setText("00:00");
        currentTimeView.setTextColor(Color.WHITE);
        currentTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        currentTimeView.setTypeface(android.graphics.Typeface.MONOSPACE);
        currentTimeView.setGravity(Gravity.CENTER_VERTICAL);
        timelineRow.addView(currentTimeView);

        seekBar = new SeekBar(this);
        seekBar.setProgressDrawable(createSeekBarProgressDrawable());
        seekBar.setThumb(createSeekBarThumbDrawable());
        seekBar.setThumbOffset(dp(7));
        seekBar.setPadding(dp(12), dp(6), dp(12), dp(6));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeView.setText(formatTime(progress * 1000L));
                    resetAutoHideTimer();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isTrackingTouch = true;
                controlsHandler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isTrackingTouch = false;
                if (player != null) {
                    player.seekTo(sb.getProgress() * 1000L);
                }
                resetAutoHideTimer();
            }
        });

        LinearLayout.LayoutParams seekParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        timelineRow.addView(seekBar, seekParams);

        totalDurationView = new TextView(this);
        totalDurationView.setText("00:00");
        totalDurationView.setTextColor(Color.WHITE);
        totalDurationView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        totalDurationView.setTypeface(android.graphics.Typeface.MONOSPACE);
        totalDurationView.setGravity(Gravity.CENTER_VERTICAL);
        timelineRow.addView(totalDurationView);

        bottomControlsOverlay.addView(
                timelineRow,
                new LinearLayout.LayoutParams(-1, -2)
        );

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        playerContainer.addView(bottomControlsOverlay, bottomParams);
    }


    // ==========================================
    // STANDALONE VECTOR ICONS (Zero XML needed)
    // ==========================================
    private static final String SVG_PLAY = "M8 5v14l11-7z";
    private static final String SVG_PAUSE = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";
    private static final String SVG_REWIND_10 = "M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8zm-1.1 11h-.85v-3.26l-1.01.31v-.59l1.8-.6h.06V16zm4.29-1.73c0 .35-.07.65-.21.89-.14.24-.33.42-.58.53-.25.11-.53.17-.85.17s-.61-.06-.86-.17c-.25-.11-.44-.29-.58-.53-.14-.24-.21-.54-.21-.89v-1.12c0-.35.07-.65.21-.89.14-.24.33-.42.58-.53.25-.11.53-.17.86-.17.32 0 .6.06.85.17.25.11.44.29.58.53.14.24.21.54.21.89v1.12zm-.86-1.22c0-.28-.05-.49-.15-.63-.1-.14-.24-.21-.43-.21-.19 0-.33.07-.43.21-.1.14-.15.35-.15.63v1.32c0 .28.05.49.15.63.1.14.24.21.43.21.19 0 .33-.07.43-.21.1-.14.15-.35.15-.63v-1.32z";
    private static final String SVG_FORWARD_10 = "M12 5V1l5 5-5 5V7c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6h2c0 4.42-3.58 8-8 8s-8-3.58-8-8 8-8 8-8zm-1.1 11h-.85v-3.26l-1.01.31v-.59l1.8-.6h.06V16zm4.29-1.73c0 .35-.07.65-.21.89-.14.24-.33.42-.58.53-.25.11-.53.17-.85.17s-.61-.06-.86-.17c-.25-.11-.44-.29-.58-.53-.14-.24-.21-.54-.21-.89v-1.12c0-.35.07-.65.21-.89.14-.24.33-.42.58-.53.25-.11.53-.17.86-.17.32 0 .6.06.85.17.25.11.44.29.58.53.14.24.21.54.21.89v1.12zm-.86-1.22c0-.28-.05-.49-.15-.63-.1-.14-.24-.21-.43-.21-.19 0-.33.07-.43.21-.1.14-.15.35-.15.63v1.32c0 .28.05.49.15.63.1.14.24.21.43.21.19 0 .33-.07.43-.21.1-.14.15-.35.15-.63v-1.32z";
    private static final String SVG_PIP = "M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z";
    private static final String SVG_AUDIO = "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z";
    private static final String SVG_SUBTITLES = "M19 4H5c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 7H9.5v-.5h-2v3h2V13H11v1c0 .55-.45 1-1 1H7c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zm7 0h-1.5v-.5h-2v3h2V13H18v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1z";
    private static final String SVG_SETTINGS = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z";
    private static final String SVG_FULLSCREEN = "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z";
    private static final String SVG_ASPECT = "M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z";

    private android.graphics.drawable.Drawable createVectorIcon(String pathData, int color, int sizeDp) {
        int sizePx = dp(sizeDp);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        try {
            android.graphics.Path path = androidx.core.graphics.PathParser.createPathFromPathData(pathData);
            if (path != null) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.setScale(sizePx / 24f, sizePx / 24f);
                path.transform(matrix);
                android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                paint.setColor(color);
                paint.setStyle(android.graphics.Paint.Style.FILL);
                canvas.drawPath(path, paint);
            }
        } catch (Exception ignored) {}
        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    private android.graphics.drawable.Drawable createSeekBarProgressDrawable() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#4DFFFFFF"));
        bg.setCornerRadius(dp(2));

        GradientDrawable progress = new GradientDrawable();
        progress.setColor(Color.WHITE);
        progress.setCornerRadius(dp(2));
        android.graphics.drawable.ClipDrawable clip = new android.graphics.drawable.ClipDrawable(
                progress, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL
        );

        android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{bg, clip}
        );
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);
        return layer;
    }

    private android.graphics.drawable.Drawable createSeekBarThumbDrawable() {
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(Color.WHITE);
        thumb.setSize(dp(14), dp(14));
        return thumb;
    }

    private ImageView createControlButton(
            android.graphics.drawable.Drawable iconDrawable,
            String contentDescription,
            View.OnClickListener onClick
    ) {
        ImageView button = new ImageView(this);
        button.setImageDrawable(iconDrawable);
        button.setContentDescription(contentDescription);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setColorFilter(Color.WHITE);
        button.setFocusable(true);
        button.setClickable(true);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                outValue,
                true
        );
        button.setBackgroundResource(outValue.resourceId);

        int size = dp(38);
        int pad = dp(6);
        button.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(6), 0, dp(6), 0);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.onClick(v);
            }
            resetAutoHideTimer();
        });

        return button;
    }

    // ==========================================
    // CONTROLS VISIBILITY & TIMERS
    // ==========================================

    private void showControls() {
        if (bottomControlsOverlay != null) {
            bottomControlsOverlay.setVisibility(View.VISIBLE);
        }
        if (topControlsOverlay != null && fullscreen) {
            topControlsOverlay.setVisibility(View.VISIBLE);
        }
        resetAutoHideTimer();
    }

    private void hideControls() {
        if (player != null && player.isPlaying()) {
            if (bottomControlsOverlay != null) {
                bottomControlsOverlay.setVisibility(View.GONE);
            }
            if (topControlsOverlay != null) {
                topControlsOverlay.setVisibility(View.GONE);
            }
        }
    }

    private void toggleControls() {
        if (bottomControlsOverlay != null && bottomControlsOverlay.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void resetAutoHideTimer() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        if (player != null && player.isPlaying()) {
            controlsHandler.postDelayed(hideControlsRunnable, 3500);
        }
    }

    private void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            player.play();
        }
        resetAutoHideTimer();
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (btnPlayPause != null) {
            btnPlayPause.setImageDrawable(
                    createVectorIcon(isPlaying ? SVG_PAUSE : SVG_PLAY, Color.WHITE, 24)
            );
        }
    }

    private void seekRelative(long offsetMs) {
        if (player == null) return;
        long current = player.getCurrentPosition();
        long duration = player.getDuration();
        long target = current + offsetMs;
        if (target < 0) target = 0;
        if (duration > 0 && target > duration) target = duration;
        player.seekTo(target);
        updateProgress();
        resetAutoHideTimer();
    }

    // ==========================================
    // PROGRESS TRACKING & FORMATTING
    // ==========================================

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (player != null && player.isPlaying()) {
                controlsHandler.postDelayed(this, 500);
            }
        }
    };

    private void startProgressUpdater() {
        controlsHandler.removeCallbacks(updateProgressAction);
        controlsHandler.post(updateProgressAction);
    }

    private void stopProgressUpdater() {
        controlsHandler.removeCallbacks(updateProgressAction);
    }

    private void updateProgress() {
        if (player == null || isTrackingTouch) return;

        long position = player.getCurrentPosition();
        long duration = player.getDuration();

        if (currentTimeView != null) {
            currentTimeView.setText(formatTime(position));
        }

        if (duration > 0 && duration != C.TIME_UNSET) {
            if (totalDurationView != null) {
                totalDurationView.setText(formatTime(duration));
            }
            if (seekBar != null) {
                seekBar.setMax((int) (duration / 1000));
                seekBar.setProgress((int) (position / 1000));
                seekBar.setEnabled(true);
            }
        } else {
            if (player.isCurrentMediaItemLive()) {
                if (currentTimeView != null) currentTimeView.setText("LIVE");
                if (totalDurationView != null) totalDurationView.setText("");
                if (seekBar != null) seekBar.setEnabled(false);
            } else {
                if (totalDurationView != null) totalDurationView.setText("--:--");
            }
        }
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long seconds = millis / 1000;
        long m = (seconds / 60) % 60;
        long h = seconds / 3600;
        long s = seconds % 60;

        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        } else {
            return String.format(Locale.US, "%02d:%02d", m, s);
        }
    }

    private void showSettingsDialog() {
        String[] options = new String[] {
                "Video Quality (Resolution)",
                "Playback Speed",
                "Audio Track",
                "Subtitle Tracks"
        };

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showTrackSelection(C.TRACK_TYPE_VIDEO, "Video Quality");
                            break;
                        case 1:
                            showSpeedDialog();
                            break;
                        case 2:
                            showTrackSelection(C.TRACK_TYPE_AUDIO, "Audio Tracks");
                            break;
                        case 3:
                            showTrackSelection(C.TRACK_TYPE_TEXT, "Subtitle Tracks");
                            break;
                    }
                })
                .show();
    }

    private TextView makeActionButton(
            String text,
            boolean primary
    ) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(primary ? 15 : 12);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? PRIMARY_COLOR : BUTTON_COLOR);
        background.setCornerRadius(dp(10));

        button.setBackground(background);
        button.setClickable(true);

        return button;
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
        field.setPadding(dp(13), dp(8), dp(13), dp(8));

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
                        multiline ? dp(100) : dp(54)
                );

        params.bottomMargin = dp(8);
        parent.addView(field, params);

        return field;
    }

    // ==========================================
    // FFmpeg + AUDIO DOWNMIX + EXOPLAYER
    // ==========================================

    private void initializePlayer() {

        try {
            ChannelMixingAudioProcessor downmixProcessor =
                    new ChannelMixingAudioProcessor();

            ChannelMixingMatrix matrix51toStereo =
                    new ChannelMixingMatrix(
                            6,
                            2,
                            new float[]{
                                    0.50f, 0.00f,
                                    0.00f, 0.50f,
                                    0.35f, 0.35f,
                                    0.25f, 0.25f,
                                    0.35f, 0.00f,
                                    0.00f, 0.35f
                            }
                    );

            downmixProcessor.putChannelMixingMatrix(matrix51toStereo);

            DefaultRenderersFactory renderersFactory =
                    new DefaultRenderersFactory(this) {
                @Override
                protected AudioSink buildAudioSink(
                        android.content.Context context,
                        boolean enableFloatOutput,
                        boolean enableAudioOutputPlaybackParams) {
                    return new DefaultAudioSink.Builder(context)
                            .setAudioProcessors(
                                    new AudioProcessor[]{
                                            downmixProcessor
                                    }
                            )
                            .build();
                }
            };

            renderersFactory.setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            );
            renderersFactory.setEnableDecoderFallback(true);

            player = new ExoPlayer.Builder(
                    this,
                    renderersFactory
            ).build();

            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePlayPauseButton(isPlaying);
                    if (isPlaying) {
                        startProgressUpdater();
                        resetAutoHideTimer();
                    } else {
                        stopProgressUpdater();
                        showControls();
                    }
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) {
                        setStatus("Buffering...");
                    } else if (state == Player.STATE_READY) {
                        setStatus("Playing / Ready");
                        updateProgress();
                    } else if (state == Player.STATE_ENDED) {
                        setStatus("Playback ended");
                        updatePlayPauseButton(false);
                        showControls();
                    } else if (state == Player.STATE_IDLE) {
                        setStatus("Ready");
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    setStatus("Error: " + error.getMessage());
                    toast("Play error: " + error.getMessage());
                    updatePlayPauseButton(false);
                    showControls();
                }
            });

        } catch (Exception e) {
            toast("Player initialization error: " + e.getMessage());
        }
    }

    // ==========================================
    // URL + PARAMETERS
    // ==========================================

    private static class StreamConfig {
        String url = "";
        String drmScheme = "";
        String drmLicense = "";
        Map<String, String> headers = new LinkedHashMap<>();
    }

    private StreamConfig parseStreamInput(String raw) {
        StreamConfig config = new StreamConfig();
        if (raw == null) return config;

        int pipe = raw.indexOf('|');
        if (pipe < 0) {
            config.url = raw.trim();
            return config;
        }

        config.url = raw.substring(0, pipe).trim();
        String parameterText = raw.substring(pipe + 1).trim();

        for (String part : parameterText.split("&")) {
            int equals = part.indexOf('=');
            if (equals <= 0) continue;

            String key = part.substring(0, equals).trim().toLowerCase(Locale.US);
            String value = part.substring(equals + 1).trim();
            if (value.isEmpty()) continue;

            switch (key) {
                case "drmscheme":
                    config.drmScheme = value.toLowerCase(Locale.US);
                    break;
                case "drmlicense":
                    config.drmLicense = value;
                    break;
                case "cookie":
                    config.headers.put("Cookie", value);
                    break;
                case "referer":
                    config.headers.put("Referer", value);
                    break;
                case "user-agent":
                    config.headers.put("User-Agent", value);
                    break;
                case "origin":
                    config.headers.put("Origin", value);
                    break;
                case "authorization":
                    config.headers.put("Authorization", value);
                    break;
            }
        }

        if (config.drmScheme.isEmpty() && !config.drmLicense.isEmpty()) {
            if (config.drmLicense.contains(":") && !config.drmLicense.startsWith("http")) {
                config.drmScheme = "clearkey";
            } else {
                config.drmScheme = "widevine";
            }
        }

        return config;
    }

    // ==========================================
    // PLAY STREAM
    // ==========================================

    private void playStream() {
        StreamConfig config = parseStreamInput(value(urlInput));

        if (config.url.isEmpty()) {
            toast("প্রথমে Video URL দিন");
            return;
        }

        if (player == null) {
            toast("Player is not ready");
            return;
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder().build();
            OkHttpDataSource.Factory httpFactory = new OkHttpDataSource.Factory(client);

            if (!config.headers.isEmpty()) {
                httpFactory.setDefaultRequestProperties(config.headers);
            }

            DefaultMediaSourceFactory sourceFactory =
                    new DefaultMediaSourceFactory(this)
                            .setDataSourceFactory(httpFactory);

            MediaItem mediaItem = buildMediaItem(config);

            if ("clearkey".equals(config.drmScheme)) {
                String keyResponse = createClearKeyResponse(config.drmLicense);

                DrmSessionManager clearKeyManager =
                        new DefaultDrmSessionManager.Builder()
                                .setUuidAndExoMediaDrmProvider(
                                        C.CLEARKEY_UUID,
                                        FrameworkMediaDrm.DEFAULT_PROVIDER
                                )
                                .build(
                                        new LocalMediaDrmCallback(
                                                keyResponse.getBytes(StandardCharsets.UTF_8)
                                        )
                                );

                sourceFactory.setDrmSessionManagerProvider(mediaItem1 -> clearKeyManager);

            } else {
                DefaultDrmSessionManagerProvider drmProvider =
                        new DefaultDrmSessionManagerProvider();

                drmProvider.setDrmHttpDataSourceFactory(httpFactory);
                sourceFactory.setDrmSessionManagerProvider(drmProvider);
            }

            player.setMediaSource(sourceFactory.createMediaSource(mediaItem));
            player.prepare();
            player.play();

            showControls();
            setStatus("Starting playback: " + config.url);

        } catch (Exception e) {
            setStatus("Could not start: " + e.getMessage());
            toast("Stream error: " + e.getMessage());
        }
    }

    // ==========================================
    // MEDIA ITEM + DRM
    // ==========================================

    private MediaItem buildMediaItem(StreamConfig config) {
        MediaItem.Builder item = new MediaItem.Builder().setUri(Uri.parse(config.url));

        if ("clearkey".equals(config.drmScheme)) {
            item.setDrmConfiguration(
                    new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build()
            );
        } else if ("widevine".equals(config.drmScheme)) {
            if (config.drmLicense.isEmpty()) {
                throw new IllegalArgumentException("Widevine drmLicense URL is missing");
            }

            MediaItem.DrmConfiguration.Builder drm =
                    new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(config.drmLicense);

            if (!config.headers.isEmpty()) {
                drm.setLicenseRequestHeaders(config.headers);
            }

            item.setDrmConfiguration(drm.build());
        }

        return item.build();
    }

    // ==========================================
    // CLEARKEY JSON RESPONSE
    // ==========================================

    private String createClearKeyResponse(String license) {
        String[] parts = license.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("ClearKey format must be KID:KEY");
        }

        byte[] kid = hexToBytes(parts[0]);
        byte[] key = hexToBytes(parts[1]);

        if (kid.length != 16 || key.length != 16) {
            throw new IllegalArgumentException("ClearKey KID and KEY must be 32 hexadecimal characters");
        }

        String encodedKid = Base64.encodeToString(
                kid,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );

        String encodedKey = Base64.encodeToString(
                key,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );

        return "{\"keys\":[{\"kty\":\"oct\",\"kid\":\""
                + encodedKid
                + "\",\"k\":\""
                + encodedKey
                + "\"}],\"type\":\"temporary\"}";
    }

    private byte[] hexToBytes(String hex) {
        String clean = hex.replace("-", "").trim();

        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal key");
        }

        byte[] result = new byte[clean.length() / 2];

        for (int i = 0; i < result.length; i++) {
            int value = Integer.parseInt(
                    clean.substring(i * 2, i * 2 + 2),
                    16
            );
            result[i] = (byte) value;
        }

        return result;
    }

    // ==========================================
    // TRACK SELECTION DIALOG
    // ==========================================

    private void showTrackSelection(int trackType, String title) {
        if (player == null) {
            toast("Player is not ready");
            return;
        }

        new TrackSelectionDialogBuilder(
                this,
                title,
                player,
                trackType
        )
                .setTrackNameProvider(this::getTrackName)
                .build()
                .show();
    }

    private String getTrackName(Format format) {
        StringBuilder name = new StringBuilder();

        // Video resolution formatting
        if (format.width > 0 && format.height > 0) {
            name.append(format.height).append("p");
            if (format.frameRate > 0) {
                name.append(" (").append((int) format.frameRate).append("fps)");
            }
            if (format.bitrate > 0) {
                name.append(" • ").append(format.bitrate / 1000).append(" kbps");
            }
            return name.toString();
        }

        if (format.label != null && !format.label.trim().isEmpty()) {
            name.append(format.label.trim());
        } else if (format.language != null && !format.language.equals("und")) {
            Locale language = Locale.forLanguageTag(format.language);
            String displayName = language.getDisplayLanguage(Locale.getDefault());
            name.append(displayName.isEmpty() ? format.language : displayName);
        } else {
            name.append("Track");
        }

        String mime = format.sampleMimeType;
        if (mime != null) {
            String codec;
            switch (mime) {
                case "audio/ac3": codec = "AC3"; break;
                case "audio/eac3": codec = "E-AC3"; break;
                case "audio/vnd.dts": codec = "DTS"; break;
                case "audio/vnd.dts.hd": codec = "DTS-HD"; break;
                case "audio/true-hd": codec = "TrueHD"; break;
                case "audio/mp4a-latm": codec = "AAC"; break;
                case "audio/opus": codec = "Opus"; break;
                case "text/vtt": codec = "WebVTT"; break;
                case "application/ttml+xml": codec = "TTML"; break;
                default: codec = mime; break;
            }
            name.append(" • ").append(codec);
        }

        if (format.channelCount > 0) {
            String channels;
            switch (format.channelCount) {
                case 1: channels = "Mono"; break;
                case 2: channels = "Stereo"; break;
                case 6: channels = "5.1"; break;
                case 8: channels = "7.1"; break;
                default: channels = format.channelCount + "ch"; break;
            }
            name.append(" • ").append(channels);
        }

        return name.toString();
    }

    // ==========================================
    // PLAYBACK SPEED
    // ==========================================

    private void showSpeedDialog() {
        if (player == null) {
            toast("Player is not ready");
            return;
        }

        float[] speeds = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        String[] labels = new String[]{"0.5×", "0.75×", "1.0× Normal", "1.25×", "1.5×", "2.0×"};

        float currentSpeed = player.getPlaybackParameters().speed;
        int checkedItem = 2;

        for (int i = 0; i < speeds.length; i++) {
            if (Math.abs(speeds[i] - currentSpeed) < 0.05f) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setSingleChoiceItems(
                        labels,
                        checkedItem,
                        (dialog, which) -> {
                            player.setPlaybackSpeed(speeds[which]);
                            toast("Speed: " + labels[which]);
                            dialog.dismiss();
                        }
                )
                .show();
    }

    // ==========================================
    // VIDEO RESIZE
    // ==========================================

    private void cycleResizeMode() {
        if (playerView == null) return;

        resizeMode++;
        if (resizeMode > 2) {
            resizeMode = 0;
        }

        if (resizeMode == 0) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            toast("Resize: FIT");
        } else if (resizeMode == 1) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            toast("Resize: FILL");
        } else {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            toast("Resize: ZOOM");
        }
    }

    // ==========================================
    // FULLSCREEN
    // ==========================================

    private void toggleFullscreen() {
        fullscreen = !fullscreen;

        if (fullscreen) {
            titleBar.setVisibility(View.GONE);
            inputScroll.setVisibility(View.GONE);

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            );

            playerContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(-1, -1)
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(attrs);
            }

            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            getWindow().getDecorView().setSystemUiVisibility(flags);

            if (topControlsOverlay != null) {
                topControlsOverlay.setVisibility(View.VISIBLE);
            }

            setStatus("Fullscreen Mode");

        } else {
            titleBar.setVisibility(View.VISIBLE);
            inputScroll.setVisibility(View.VISIBLE);

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            );

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );

            getWindow().setStatusBarColor(BG_COLOR);
            getWindow().setNavigationBarColor(BG_COLOR);

            playerContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(-1, 0, 0.30f)
            );

            if (topControlsOverlay != null) {
                topControlsOverlay.setVisibility(View.GONE);
            }

            setStatus("Normal Mode");
        }

        showControls();
    }

    // ==========================================
    // PICTURE IN PICTURE
    // ==========================================

    private boolean isInPipMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && isInPictureInPictureMode();
    }

    private void enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            toast("Picture-in-Picture requires Android 8.0 or newer");
            return;
        }

        if (player == null || player.getMediaItemCount() == 0) {
            toast("প্রথমে ভিডিও চালু করুন");
            return;
        }

        try {
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(16, 9))
                            .build();

            enterPictureInPictureMode(params);

        } catch (Exception e) {
            toast("PiP error: " + e.getMessage());
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && player != null
                && player.isPlaying()) {
            enterPictureInPicture();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode,
            Configuration newConfig
    ) {
        super.onPictureInPictureModeChanged(
                isInPictureInPictureMode,
                newConfig
        );

        if (isInPictureInPictureMode) {
            titleBar.setVisibility(View.GONE);
            inputScroll.setVisibility(View.GONE);
            if (bottomControlsOverlay != null) {
                bottomControlsOverlay.setVisibility(View.GONE);
            }
            if (topControlsOverlay != null) {
                topControlsOverlay.setVisibility(View.GONE);
            }

            playerContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(-1, 0, 1f)
            );
        } else {
            if (!fullscreen) {
                titleBar.setVisibility(View.VISIBLE);
                inputScroll.setVisibility(View.VISIBLE);

                playerContainer.setLayoutParams(
                        new LinearLayout.LayoutParams(-1, 0, 0.30f)
                );
            }
            showControls();
        }
    }

    // ==========================================
    // STATUS + TOAST
    // ==========================================

    private void setStatus(String text) {
        if (statusText != null) {
            statusText.setText(text);
        }
    }

    private void toast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }

    private String value(EditText edit) {
        return edit == null
                ? ""
                : edit.getText().toString().trim();
    }

    // ==========================================
    // LIFECYCLE
    // ==========================================

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && !isInPipMode()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controlsHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
