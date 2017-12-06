package com.monitor.traffic.demyati.seddik.trafficmonitor;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.TextUtils;
import com.android.internal.telephony.*;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class APNHelper {

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    static final String APN_TYPE_HIPRI = "hipri";

    public class APN {
        public String MMSCenterUrl = "";
        public String MMSPort = "";
        public String MMSProxy = "";
    }

    private Context context;

    public APNHelper(final Context context) {
        this.context = context;
    }

    public List<APN> getMMSApns() {
        final Cursor apnCursor = this.context.getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current"), null, null, null, null);
        if ( apnCursor == null ) {
            return Collections.EMPTY_LIST;
        } else {
            final List<APN> results = new ArrayList<APN>();
            while ( apnCursor.moveToNext() ) {
                final String type = apnCursor.getString(apnCursor.getColumnIndex(Telephony.Carriers.TYPE));
                if ( !TextUtils.isEmpty(type) && ( type.contains(APN_TYPE_ALL) || type.contains(APN_TYPE_MMS)) ) {
                    final String mmsc = apnCursor.getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSC));
                    final String mmsProxy = apnCursor.getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSPROXY));
                    final String port = apnCursor.getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSPORT));
                    final APN apn = new APN();
                    apn.MMSCenterUrl = mmsc;
                    apn.MMSProxy = mmsProxy;
                    apn.MMSPort = port.trim().length() > 0 ? port : "80";
                    results.add(apn);
                }
            }
            apnCursor.close();
            return results;
        }
    } }