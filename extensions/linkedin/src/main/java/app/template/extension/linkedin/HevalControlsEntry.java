package app.template.extension.linkedin;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.linkedin.android.home.navpanel.AccountSectionType;
import com.linkedin.android.home.navpanel.HomeNavPanelAccountSectionViewData;
import com.linkedin.android.infra.list.DefaultObservableList;
import com.linkedin.android.litrackinglib.metric.Tracker;
import com.linkedin.android.tracking.v2.event.CustomTrackingEventBuilder;
import com.linkedin.android.tracking.v2.listeners.TrackingOnClickListener;

/**
 * The "Heval Controls" row in LinkedIn's home nav panel (opened from the avatar, top left). It is
 * a stock footer row (the same view as "Settings &amp; Privacy", with its gear icon) appended to the
 * footer list, and its click listener opens the Heval Controls screen without sending LinkedIn's
 * control-interaction tracking event. Every entry point falls back to stock on any error.
 */
@SuppressWarnings("unused")
public final class HevalControlsEntry {
    private static final String TAG = "Morphe-LinkedIn";

    /**
     * The row label. A distinct instance, compared by identity, so only the row added here matches,
     * never a stock row that happens to carry the same text.
     */
    @SuppressWarnings("StringOperationCanBeSimplified")
    private static final String LABEL = new String("Heval Controls");

    private static final String ACTIVITY = "app.template.extension.hevalcontrols.ui.HevalControlsActivity";

    /**
     * Patched in before the nav panel footer list (divider, "Settings &amp; Privacy", Premium
     * upsell) is published: appends the "Heval Controls" row at the end.
     */
    public static void addNavPanelRow(Object list) {
        try {
            DefaultObservableList rows = (DefaultObservableList) list;
            rows.addItem(rows.currentSize(), new HomeNavPanelAccountSectionViewData(AccountSectionType.SETTINGS, LABEL));
        } catch (Throwable failure) {
            Log.e(TAG, "Heval Controls row not added: " + failure.getClass().getName());
        }
    }

    /**
     * Patched in at the start of the footer presenter's attachViewData: the click listener of the
     * "Heval Controls" row, or null for every stock row (which then gets its stock listener).
     */
    public static TrackingOnClickListener navPanelClickListener(Tracker tracker, Object viewData) {
        try {
            if (viewData instanceof HomeNavPanelAccountSectionViewData
                    && ((HomeNavPanelAccountSectionViewData) viewData).premiumText == LABEL) {
                return new OpenControlsListener(tracker);
            }
        } catch (Throwable failure) {
            Log.e(TAG, "Heval Controls row listener: " + failure.getClass().getName());
        }
        return null;
    }

    /** Starts the (non-exported) Heval Controls screen on top of the current LinkedIn activity. */
    static void open(Context context) {
        try {
            Activity activity = activityOf(context);
            Intent intent = new Intent().setClassName(context.getPackageName(), ACTIVITY);
            if (activity != null) {
                activity.startActivity(intent);
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Throwable failure) {
            Log.e(TAG, "Cannot open Heval Controls: " + failure.getClass().getName());
        }
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * A LinkedIn TrackingOnClickListener (the footer binding takes only that type) that never calls
     * super.onClick, so the click is not reported to LinkedIn's tracking.
     */
    static final class OpenControlsListener extends TrackingOnClickListener implements View.OnClickListener {
        OpenControlsListener(Tracker tracker) {
            super(tracker, "heval_controls", null, new CustomTrackingEventBuilder[0]);
        }

        @Override
        public void onClick(View view) {
            if (view != null) open(view.getContext());
        }
    }

    private HevalControlsEntry() { }
}
