package com.linkedin.android.tracking.v2.listeners;

import com.linkedin.android.litrackinglib.metric.Tracker;
import com.linkedin.android.tracking.v2.event.CustomTrackingEventBuilder;

// The real class implements android.view.View.OnClickListener; subclasses declare it themselves,
// so this stub needs no android.jar. Its onClick(View) sends the control-interaction event.
public class TrackingOnClickListener {
    public TrackingOnClickListener(Tracker tracker, String controlName, String controlTrackingId,
                                   CustomTrackingEventBuilder... builders) {
    }
}
