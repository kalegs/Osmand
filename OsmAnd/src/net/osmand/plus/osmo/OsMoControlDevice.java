package net.osmand.plus.osmo;

import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.Version;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Vibrator;

public class OsMoControlDevice implements OsMoReactor {

	private OsMoService service;
	private OsmandApplication app;
	private OsMoTracker tracker;

	public OsMoControlDevice(OsmandApplication app, OsMoService service, OsMoTracker tracker) {
		this.app = app;
		this.service = service;
		this.tracker = tracker;
		service.registerReactor(this);
	}
	
	public float getBatteryLevel() {
	    Intent batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

	    // Error checking that probably isn't needed but I added just in case.
	    if(level == -1 || scale == -1) {
	        return 50.0f;
	    }

	    return ((float)level / (float)scale) * 100.0f; 
	}

	@Override
	public boolean acceptCommand(String command, String id, String data, JSONObject obj, OsMoThread tread) {
		if(command.equals("REMOTE_CONTROL")) {
			if(data.equals("BATTERY_INFO")) {
				String rdata = getBatteryLevel()+"";
				service.pushCommand("BATTERY_INFO|"+rdata);
			} else if(data.equals("VIBRATE")) {
				Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
				 // Vibrate for 500 milliseconds
				 v.vibrate(500);
			} else if(data.equals("STOP_TRACKING")) {
				tracker.disableTracker();
				if (app.getNavigationService() != null) {
					app.getNavigationService().stopIfNeeded(app,NavigationService.USED_BY_LIVE);
				}
			} else if(data.equals("START_TRACKING")) {
				tracker.enableTracker();
				app.startNavigationService(NavigationService.USED_BY_LIVE);
				app.getSettings().SERVICE_OFF_INTERVAL.set(0);
			} else if(data.equals("OSMAND_INFO")) {
				JSONObject robj = new JSONObject();
				try {
					robj.put("full_version", Version.getFullVersion(app));
					robj.put("version", Version.getAppVersion(app));
					TargetPointsHelper tg = app.getTargetPointsHelper();
					if(tg.getPointToNavigate() != null) {
						addPoint(robj, "target_", tg.getPointToNavigate(), tg.getPointNavigateDescription());
					}
					List<String> intermediatePointNames = tg.getIntermediatePointNames();
					List<LatLon> intermediatePoints = tg.getIntermediatePoints();
					if (intermediatePointNames.size() > 0) {
						JSONArray ar = new JSONArray();
						robj.put("intermediates", ar);
						for (int i = 0; i < intermediatePointNames.size(); i++) {
							JSONObject js = new JSONObject();
							ar.put(js);
							addPoint(js, "", intermediatePoints.get(i), intermediatePointNames.get(i));
						}
					}
					service.pushCommand("OSMAND_INFO|"+robj.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}

	private void addPoint(JSONObject robj, String prefix, LatLon pointToNavigate, String pointNavigateDescription) throws JSONException {
		robj.put(prefix+"lat", pointToNavigate.getLatitude());
		robj.put(prefix+"lon", pointToNavigate.getLongitude());
		if(pointNavigateDescription != null) {
			robj.put(prefix+"name", pointNavigateDescription);
		}
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		return null;
	}

	@Override
	public void reconnect() {
	}

}