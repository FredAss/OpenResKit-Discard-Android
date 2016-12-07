package htw.bui.openreskit.discard;



import com.google.inject.Inject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import htw.bui.openreskit.odata.DiscardRepository;
import roboguice.activity.RoboFragmentActivity;

public class InspectionInfoActivity extends RoboFragmentActivity 
{
	@Inject
	private FragmentManager mfragMan;

	@Inject
	private DiscardRepository mRepository;

	private Activity mContext;

	private  InspectionInfoFragment mInspectionInfoFragment;
	private long mInspectionId;

	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inspection_info_fragment);
		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}
		mContext = this;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		Intent launchingIntent = getIntent();
		mInspectionId = launchingIntent.getExtras().getLong("InspectionId");
		mInspectionInfoFragment = (InspectionInfoFragment) mfragMan.findFragmentById(R.id.inspection_info_fragment);
		mInspectionInfoFragment.getInspectionDetails(mInspectionId);

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
	}

	@Override
	public void onResume() 
	{
		super.onResume(); 
		mInspectionInfoFragment.getInspectionDetails(mInspectionId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.startInspection:
			startInspection(mInspectionId);
			return true;
		case R.id.showPreferences:
			startPreferences();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem startItem = menu.findItem(R.id.startInspection);
		startItem.setVisible(true);

		MenuItem stopItem = menu.findItem(R.id.finishInspection);
		stopItem.setVisible(false);

		MenuItem startSync = menu.findItem(R.id.startSync);
		startSync.setVisible(false);

		MenuItem delLocal = menu.findItem(R.id.deleteLocalData);
		delLocal.setVisible(false);

		MenuItem writeData = menu.findItem(R.id.writeData);
		writeData.setVisible(false);

		return true;
	}

	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		this.startActivity(startPreferences);
	}

	private void startInspection(long inspectionId) 
	{
		String responsibleSubjectId = mPrefs.getString("default_responsibleSubject", "none");
		if (responsibleSubjectId != "none") 
		{
			//if ReadingFragment not present start new Activity (Phone)
			Intent startInspection = new Intent(mContext, RunningInspectionActivity.class);
			startInspection.putExtra("InspectionId", mInspectionId);
			startActivity(startInspection);
		}
		else 
		{
			Toast.makeText(mContext, "Bitte wählen Sie zuerst in den Einstellungen einen Mitarbeiter aus.", Toast.LENGTH_SHORT).show();
		}
	}

}