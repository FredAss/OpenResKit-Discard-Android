package htw.bui.openreskit.discard;

import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.odata.DiscardRepository;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.inject.Inject;

import roboguice.activity.RoboFragmentActivity;

public class RunningInspectionActivity extends RoboFragmentActivity {

	@Inject
	private FragmentManager mFragMan;

	@Inject
	private DiscardRepository mRepository;

	private long mInspectionId;
	private Activity mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.running_inspection_fragment);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mContext = this;
		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}

		Intent launchingIntent = getIntent();
		mInspectionId = launchingIntent.getExtras().getLong("InspectionId");
		RunningInspectionFragment runningInspectionFragment = (RunningInspectionFragment) mFragMan.findFragmentById(R.id.running_inspection_fragment);
		runningInspectionFragment.getInspectionDetails(mInspectionId);

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
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
		case R.id.finishInspection:
			finishInspection(mInspectionId);
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
		startItem.setVisible(false);

		MenuItem stopItem = menu.findItem(R.id.finishInspection);
		stopItem.setVisible(true);

		MenuItem startSync = menu.findItem(R.id.startSync);
		startSync.setVisible(false);

		MenuItem delLocal = menu.findItem(R.id.deleteLocalData);
		delLocal.setVisible(false);

		MenuItem writeData = menu.findItem(R.id.writeData);
		writeData.setVisible(false);

		return true;
	}

	@Override
	public void onBackPressed() 
	{
		finishInspection(mInspectionId);
	}

	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		this.startActivity(startPreferences);
	}

	private void finishInspection(final long inspectionId) 
	{
		new AlertDialog.Builder(mContext)
		.setTitle("Prüfung abschließen")
		.setMessage("Möchten Sie die Prüfung abschließen?")
		.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				Inspection insp = mRepository.getInspectionById(inspectionId);
				insp.setFinished(true);
				insp.setManipulated(true);
				mRepository.clearDiscardItems(insp.getInternalId());
				mRepository.persistLocalInspections();
				finish();
			}
		}).setNegativeButton("Nein", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
				finish();

			}
		}).show();	
	}
}
