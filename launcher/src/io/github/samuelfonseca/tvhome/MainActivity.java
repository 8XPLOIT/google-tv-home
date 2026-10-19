package io.github.samuelfonseca.tvhome;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The home screen: rotating nature wallpaper, clock, what's playing, and a dock of favourite apps. */
public class MainActivity extends Activity {
    private static final long ROTATE_MS = 60_000;
    private static final long FADE_MS = 1_500;
    private static final String KEY_WALL = "wall";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final List<String[]> walls = new ArrayList<>(); // {resName, place, author, licence}

    private ImageView back, front;
    private TextView place, credit;
    private LinearLayout dock;
    private List<String> shownDock; // what buildDock last drew
    private HorizontalScrollView dockScroll;
    private NowPlaying nowPlaying;
    private AlertDialog dialog;
    private int wall;
    private int focusedIndex;

    private final Runnable rotate = new Runnable() {
        @Override public void run() {
            showWallpaper(wall + 1, true);
            main.postDelayed(this, ROTATE_MS);
        }
    };

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            buildDock();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        loadWallpaperList();
        setContentView(buildLayout());
        wall = Apps.prefs(this).getInt(KEY_WALL, 0);
        showWallpaper(wall, false);
        buildDock();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packagesChanged, f);
    }

    @Override protected void onStart() {
        super.onStart();
        main.postDelayed(rotate, ROTATE_MS);
        nowPlaying.start();
    }

    /** Back from an app or All apps: the dock may have been edited there. */
    @Override protected void onRestart() {
        super.onRestart();
        buildDock();
    }

    // All apps is translucent, so this activity only pauses behind it and onRestart doesn't run.
    @Override protected void onResume() {
        super.onResume();
        if (!Apps.dock(this).equals(shownDock)) buildDock();
        focusDock(focusedIndex);
    }

    @Override protected void onStop() {
        main.removeCallbacks(rotate);
        nowPlaying.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        decoder.shutdownNow();
        nowPlaying.destroy();
        super.onDestroy();
    }

    /** Home pressed while already home: close any menu and go back to the first app. */
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (dialog != null) dialog.dismiss();
        focusedIndex = 0;
        dockScroll.smoothScrollTo(0, 0);
        focusDock(0);
    }

    @Override public void onBackPressed() {
        // Home is the bottom of the stack; Back does nothing here.
    }

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClipChildren(false);

        back = new ImageView(this);
        front = new ImageView(this);
        for (ImageView iv : new ImageView[] {back, front}) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        }

        // Soft scrims so text and the dock stay legible on bright photos.
        root.addView(scrim(GradientDrawable.Orientation.TOP_BOTTOM, 0x99000000),
                new FrameLayout.LayoutParams(-1, Ui.dp(this, 170), Gravity.TOP));
        root.addView(scrim(GradientDrawable.Orientation.BOTTOM_TOP, 0xB3000000),
                new FrameLayout.LayoutParams(-1, Ui.dp(this, 230), Gravity.BOTTOM));

        int margin = Ui.dp(this, 48);

        // Top left: time and date.
        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("H:mm");
        style(time, 46, Ui.LIGHT, 0xFFFFFFFF);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, MMMM d");
        date.setFormat24Hour("EEEE, d MMMM");
        style(date, 15, Ui.MEDIUM, 0xD9FFFFFF);
        clock.addView(time);
        clock.addView(date);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        clp.setMargins(margin, Ui.dp(this, 28), 0, 0);
        root.addView(clock, clp);

        // Top right: all apps and settings buttons, with the photo credit underneath.
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.setClipChildren(false); // focused buttons scale past their bounds
        LinearLayout buttons = new LinearLayout(this);
        buttons.setClipChildren(false);
        View allApps = Ui.roundButton(this, Ui.gridIcon(), "All apps");
        allApps.setOnClickListener(v -> startActivity(new Intent(this, AllAppsActivity.class)));
        View settings = Ui.roundButton(this, Ui.gearIcon(), "Settings");
        settings.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        buttons.addView(allApps);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
        gap.leftMargin = Ui.dp(this, 14);
        buttons.addView(settings, gap);
        // Wrap content, so each row hugs the right edge and the column resizes with the caption.
        right.addView(buttons, new LinearLayout.LayoutParams(-2, -2));
        place = new TextView(this);
        style(place, 13, Ui.MEDIUM, 0xE6FFFFFF);
        place.setPadding(0, Ui.dp(this, 16), 0, 0);
        credit = new TextView(this);
        style(credit, 10, Ui.LIGHT, 0xB3FFFFFF);
        right.addView(place, new LinearLayout.LayoutParams(-2, -2));
        right.addView(credit, new LinearLayout.LayoutParams(-2, -2));
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        rlp.setMargins(0, Ui.dp(this, 36), margin, 0);
        root.addView(right, rlp);

        // Above the dock, left: what's playing in the background, with controls.
        nowPlaying = new NowPlaying(this, () -> focusDock(focusedIndex));
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        nlp.setMargins(margin, 0, 0, Ui.dp(this, 172));
        root.addView(nowPlaying, nlp);

        // Bottom: the dock.
        dockScroll = new HorizontalScrollView(this);
        dockScroll.setHorizontalScrollBarEnabled(false);
        dockScroll.setClipChildren(false);
        dockScroll.setClipToPadding(false);
        dockScroll.setFillViewport(true);
        dockScroll.setPadding(margin, Ui.dp(this, 24), margin, 0);
        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        dock.setClipChildren(false);
        dockScroll.addView(dock, new FrameLayout.LayoutParams(-1, -2));
        root.addView(dockScroll, new FrameLayout.LayoutParams(-1, Ui.dp(this, 160), Gravity.BOTTOM));
        return root;
    }

    private void buildDock() {
        dock.removeAllViews();
        int gap = Ui.dp(this, 22);
        int avail = getResources().getDisplayMetrics().widthPixels - 2 * Ui.dp(this, 48);
        // Six cards fill the width; more than that scroll.
        int width = Math.min(Ui.dp(this, 150), (avail - 5 * gap) / 6);
        int i = 0;
        shownDock = Apps.dock(this);
        for (String pkg : shownDock) {
            Apps.App app = Apps.get(this, pkg);
            if (app == null) continue; // not installed or disabled
            View tile = Ui.appCard(this, app, width);
            final int index = i++;
            tile.setOnClickListener(v -> {
                focusedIndex = index;
                Bundle opts = ActivityOptions.makeScaleUpAnimation(
                        v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
                Apps.launch(this, app.pkg, opts);
            });
            tile.setOnLongClickListener(v -> {
                editDock(app);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (dock.getChildCount() > 0) lp.leftMargin = gap;
            dock.addView(tile, lp);
        }
        focusDock(Math.min(focusedIndex, dock.getChildCount() - 1));
    }

    private void editDock(Apps.App app) {
        List<String> pkgs = Apps.dock(this);
        int pos = pkgs.indexOf(app.pkg);
        String[] choices = {"Move left", "Move right", "Remove from dock"};
        dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.label)
                .setItems(choices, (d, which) -> {
                    if (which == 0 && pos > 0) {
                        pkgs.add(pos - 1, pkgs.remove(pos));
                        focusedIndex = Math.max(0, focusedIndex - 1);
                    } else if (which == 1 && pos < pkgs.size() - 1) {
                        pkgs.add(pos + 1, pkgs.remove(pos));
                        focusedIndex++;
                    } else if (which == 2) {
                        pkgs.remove(pos);
                    }
                    Apps.saveDock(this, pkgs);
                    buildDock();
                })
                .show();
    }

    private void focusDock(int index) {
        if (index < 0 || index >= dock.getChildCount()) return;
        View v = dock.getChildAt(index);
        v.post(v::requestFocus);
    }

    // --- Wallpapers ---

    private void loadWallpaperList() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.walls)))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) walls.add(parts);
            }
        } catch (IOException ignored) {
        }
    }

    /** Decode off the main thread as RGB_565 (half the memory), then crossfade. */
    private void showWallpaper(int index, boolean fade) {
        if (walls.isEmpty()) return;
        final int i = ((index % walls.size()) + walls.size()) % walls.size();
        final String[] w = walls.get(i);
        final int res = getResources().getIdentifier(w[0], "drawable", getPackageName());
        decoder.execute(() -> {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            o.inScaled = false;
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), res, o);
            main.post(() -> {
                if (isDestroyed() || bmp == null) return;
                wall = i;
                Apps.prefs(this).edit().putInt(KEY_WALL, i).apply();
                place.setText(w[1]);
                credit.setText("Photo: " + w[2] + " · " + w[3]);
                if (!fade) {
                    back.setImageBitmap(bmp);
                    return;
                }
                front.setAlpha(0f);
                front.setImageBitmap(bmp);
                front.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> {
                    back.setImageBitmap(bmp);
                    front.setImageDrawable(null);
                    front.setAlpha(0f);
                }).start();
            });
        });
    }

    private View scrim(GradientDrawable.Orientation o, int color) {
        View v = new View(this);
        v.setBackground(new GradientDrawable(o, new int[] {color, 0x00000000}));
        return v;
    }

    private static void style(TextView t, float sp, android.graphics.Typeface face, int color) {
        t.setTextSize(sp);
        t.setTypeface(face);
        t.setTextColor(color);
        t.setShadowLayer(6f, 0, 2f, 0x99000000);
    }
}
