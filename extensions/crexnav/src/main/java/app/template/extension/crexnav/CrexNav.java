package app.template.extension.crexnav;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Moves the Matches screen's top tabs (Live, For you, Upcoming, Finished) into the bottom
 * navigation bar of CREX.
 *
 * <p>The bottom bar keeps its five menu items. Four of them are re-labelled and re-iconed as the
 * four tabs and the fifth (Matches) is hidden. Tapping one never switches the app's own tab (the
 * app stays on Matches); it selects the matching tab of the Matches screen's TabLayout instead,
 * and that TabLayout is hidden. Material classes are used through reflection so the extension
 * has no compile-time dependency on the app's Material version.
 *
 * <p>Every reflective call is wrapped so a missing member degrades to "nothing happens" instead
 * of a crash.
 */
@SuppressWarnings("unused")
public final class CrexNav {

    // Bottom-nav menu item ids (res/values/public.xml, v26.10.07), in tab order.
    private static final int ID_LIVE = 0x7f0a0bf8;      // was Home
    private static final int ID_FOR_YOU = 0x7f0a0456;   // was Social
    private static final int ID_UPCOMING = 0x7f0a1d79;  // was Videos
    private static final int ID_FINISHED = 0x7f0a1846;  // was Series
    private static final int[] KNOWN = {ID_LIVE, ID_FOR_YOU, ID_UPCOMING, ID_FINISHED};
    // Item id for each tab index. Filled in setupMenu in the bar's own left-to-right order, so
    // the labels always read Live, For you, Upcoming, Finished whatever order the menu has.
    private static int[] idForIndex = KNOWN.clone();
    private static final String[] TITLES = {"Live", "For you", "Upcoming", "Finished"};

    private static final String TAB_LAYOUT = "com.google.android.material.tabs.TabLayout";
    private static final String TAB_LISTENER =
            "com.google.android.material.tabs.TabLayout$OnTabSelectedListener";
    private static final String NAV_BAR_VIEW =
            "com.google.android.material.navigation.NavigationBarView";

    private static WeakReference<View> tabLayoutRef = new WeakReference<>(null);
    private static boolean syncing;

    private CrexNav() {
    }

    // ---------------------------------------------------------------- bottom navigation menu

    /** Re-labels the four visible items and hides every other one (Matches). */
    public static void setupMenu(Menu menu) {
        try {
            int[] order = new int[KNOWN.length];
            int next = 0;
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (!isKnown(item.getItemId()) || next >= order.length) {
                    item.setVisible(false);
                    continue;
                }
                int index = next++;
                order[index] = item.getItemId();
                item.setVisible(true);
                item.setTitle(TITLES[index]);
                item.setIcon(new BitmapDrawable(Resources.getSystem(), icon(index)));
            }
            if (next == order.length) idForIndex = order;
        } catch (Throwable ignored) {
        }
    }

    private static boolean isKnown(int id) {
        for (int known : KNOWN) if (known == id) return true;
        return false;
    }

    private static int indexOf(int id) {
        for (int i = 0; i < idForIndex.length; i++) if (idForIndex[i] == id) return i;
        return -1;
    }

    /**
     * Called first thing in the bottom-nav item handler. Returns true when the item is one of the
     * four tabs (the app's own handler is then skipped and the item is shown as selected).
     */
    public static boolean onItemSelected(Activity activity, MenuItem item) {
        int index = indexOf(item.getItemId());
        if (index < 0) return false;
        selectTab(activity, index);
        return true;
    }

    // ---------------------------------------------------------------- top TabLayout

    /** Called at the end of HomeActivity.onCreate. */
    public static void install(final Activity activity) {
        try {
            final View decor = activity.getWindow().getDecorView();
            decor.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            attach(activity);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static void attach(Activity activity) {
        try {
            View current = tabLayoutRef.get();
            if (current != null && current.isAttachedToWindow()) return;

            View tabs = findTabLayout(activity.getWindow().getDecorView());
            if (tabs == null) return;
            tabLayoutRef = new WeakReference<>(tabs);
            tabs.setVisibility(View.GONE);

            Class<?> listener = Class.forName(TAB_LISTENER);
            Object proxy = Proxy.newProxyInstance(
                    listener.getClassLoader(), new Class<?>[]{listener}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object p, Method m, Object[] args) {
                            if ("onTabSelected".equals(m.getName()) && args != null && args.length > 0) {
                                try {
                                    int position = (Integer) args[0].getClass()
                                            .getMethod("getPosition").invoke(args[0]);
                                    highlight(tabLayoutRef.get(), position);
                                } catch (Throwable ignored) {
                                }
                            }
                            return null;
                        }
                    });
            tabs.getClass().getMethod("addOnTabSelectedListener", listener).invoke(tabs, proxy);

            // Show the tab the screen opened on as the selected bottom item.
            int selected = (Integer) tabs.getClass().getMethod("getSelectedTabPosition").invoke(tabs);
            final int start = selected < 0 ? 0 : selected;
            highlight(tabs, start);
            // The app selects its own (hidden) Matches item while it starts up; set ours again.
            final View anchor = tabs;
            Runnable again = new Runnable() {
                @Override
                public void run() {
                    try {
                        int now = (Integer) anchor.getClass()
                                .getMethod("getSelectedTabPosition").invoke(anchor);
                        highlight(anchor, now < 0 ? start : now);
                    } catch (Throwable ignored) {
                    }
                }
            };
            tabs.postDelayed(again, 400);
            tabs.postDelayed(again, 1500);
        } catch (Throwable ignored) {
        }
    }

    private static void selectTab(Activity activity, int index) {
        try {
            View tabs = tabLayoutRef.get();
            if (tabs == null || !tabs.isAttachedToWindow()) {
                tabs = findTabLayout(activity.getWindow().getDecorView());
                if (tabs != null) tabLayoutRef = new WeakReference<>(tabs);
            }
            if (tabs == null) return;
            Object tab = tabs.getClass().getMethod("getTabAt", int.class).invoke(tabs, index);
            if (tab != null) tab.getClass().getMethod("select").invoke(tab);

            // Also move the pager itself, in case the tab selection alone does not carry it.
            View pager = findPager(activity.getWindow().getDecorView());
            if (pager != null) {
                int current = (Integer) pager.getClass().getMethod("getCurrentItem").invoke(pager);
                if (current != index) {
                    pager.getClass().getMethod("setCurrentItem", int.class, boolean.class)
                            .invoke(pager, index, true);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // The Matches pager is the ViewPager2 that holds exactly four pages.
    private static View findPager(View root) {
        if (isInstanceOf(root, "androidx.viewpager2.widget.ViewPager2")) {
            try {
                Object adapter = root.getClass().getMethod("getAdapter").invoke(root);
                if (adapter != null) {
                    int count = (Integer) adapter.getClass().getMethod("getItemCount").invoke(adapter);
                    if (count == 4) return root;
                }
            } catch (Throwable ignored) {
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findPager(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Marks the bottom item for a tab position as selected (swipes, programmatic changes). */
    private static void highlight(View anyView, int position) {
        if (syncing || position < 0 || position >= idForIndex.length || anyView == null) return;
        syncing = true;
        try {
            View bar = findByClass(anyView.getRootView(), NAV_BAR_VIEW);
            if (bar != null) {
                bar.getClass().getMethod("setSelectedItemId", int.class).invoke(bar, idForIndex[position]);
            }
        } catch (Throwable ignored) {
        } finally {
            syncing = false;
        }
    }

    // The Matches TabLayout is the one with exactly four tabs (Live, For you, Upcoming, Finished).
    private static View findTabLayout(View root) {
        if (isInstanceOf(root, TAB_LAYOUT)) {
            try {
                int count = (Integer) root.getClass().getMethod("getTabCount").invoke(root);
                if (count == 4) return root;
            } catch (Throwable ignored) {
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTabLayout(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View findByClass(View root, String className) {
        if (isInstanceOf(root, className)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findByClass(group.getChildAt(i), className);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean isInstanceOf(Object view, String className) {
        for (Class<?> c = view.getClass(); c != null; c = c.getSuperclass()) {
            if (className.equals(c.getName())) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- icons (white, tinted by the bar)

    private static Bitmap icon(int index) {
        float d = Resources.getSystem().getDisplayMetrics().density;
        int size = Math.round(24 * d);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(size / 24f, size / 24f);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0xFFFFFFFF);
        Paint stroke = new Paint(fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        switch (index) {
            case 0: // Live: ring with a solid centre
                canvas.drawCircle(12, 12, 9, stroke);
                canvas.drawCircle(12, 12, 4, fill);
                break;
            case 1: { // For you: star
                Path star = new Path();
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 5 * i - Math.PI / 2;
                    float r = i % 2 == 0 ? 10f : 4.4f;
                    float x = 12 + (float) Math.cos(angle) * r;
                    float y = 12.5f + (float) Math.sin(angle) * r;
                    if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
                }
                star.close();
                canvas.drawPath(star, fill);
                break;
            }
            case 2: // Upcoming: calendar
                canvas.drawRoundRect(new RectF(3.5f, 5, 20.5f, 20.5f), 2.5f, 2.5f, stroke);
                canvas.drawLine(3.5f, 10, 20.5f, 10, stroke);
                canvas.drawLine(8, 2.5f, 8, 6.5f, stroke);
                canvas.drawLine(16, 2.5f, 16, 6.5f, stroke);
                break;
            default: { // Finished: circle with a check
                canvas.drawCircle(12, 12, 9, stroke);
                Path check = new Path();
                check.moveTo(7.8f, 12.3f);
                check.lineTo(10.7f, 15.2f);
                check.lineTo(16.3f, 9.2f);
                canvas.drawPath(check, stroke);
                break;
            }
        }
        return bitmap;
    }
}
