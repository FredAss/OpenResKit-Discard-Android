/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package htw.bui.openreskit.discard;

import roboguice.activity.RoboFragmentActivity;
import htw.bui.openreskit.discard.InspectionFragment.IInspectionHandling;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.odata.DiscardRepository;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;

public class InspectionActivity extends RoboFragmentActivity implements IInspectionHandling
{
	@Inject
	private DiscardRepository mRepository;

	@Inject
	private android.support.v4.app.FragmentManager mFragMan;


	private Activity mContext;
	private long mInspectionId;
	private boolean mInspectionRunning;
	private InspectionInfoFragment mInspectionInfoFragment;
	private RunningInspectionFragment mRunningInspectionFragment;
	private InspectionFragment mInspFrag;
	private SharedPreferences mPrefs;
	private FrameLayout mOverlayFramelayout;
	private View mHelpView;
	private Button mAddInspectionButton;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mOverlayFramelayout = new FrameLayout(this);

		setContentView(mOverlayFramelayout);
		View view = getLayoutInflater().inflate(R.layout.inspection_fragment, mOverlayFramelayout, false);
		mOverlayFramelayout.addView(view);
		mContext = this;
		mPrefs= PreferenceManager.getDefaultSharedPreferences(mContext);
		mHelpView = getLayoutInflater().inflate(R.layout.help_overlay, mOverlayFramelayout,false);
		mInspFrag = (InspectionFragment) mFragMan.findFragmentById(R.id.inspection_fragment);
		mAddInspectionButton = (Button) mOverlayFramelayout.findViewById(R.id.addInspectionButton);
		mAddInspectionButton.setOnClickListener(mButtonListener);

		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			if(findViewById(R.id.inspectionLayoutContainer) != null)
			{
				// if we are being restored from a previous state, then we dont need to do anything and should
				// return or else we could end up with overlapping fragments.
				if(savedInstanceState != null)
					return;

				// Create an instance of inspectionInfoFragment
				mInspectionInfoFragment = new InspectionInfoFragment();
				// Create an instance of RunningInspectionFragment	            
				mRunningInspectionFragment = new RunningInspectionFragment();

				// add fragment to the fragment container layout
				FragmentTransaction ft = mFragMan.beginTransaction();
				ft.add(R.id.inspectionFrameLayout, mInspectionInfoFragment);
				ft.hide(mInspectionInfoFragment).commit();

				FragmentTransaction ft1 = mFragMan.beginTransaction();
				ft1.add(R.id.inspectionFrameLayout, mRunningInspectionFragment);
				ft1.hide(mRunningInspectionFragment).commit();
			}
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
	}

	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			if (v.getId() == R.id.executeSyncButton) 
			{
				startSync();
			}
			else if (v.getId() == R.id.openPreferences) 
			{
				startPreferences();	
			}
			else if (v.getId() == R.id.addInspectionButton)
			{
				addInspection();
			}
		}
	};


	private void addInspection(long id) 
	{
		Intent showAddForm = new Intent(mContext, AddInspectionActivity.class);
		showAddForm.putExtra("InspectionId", id);
		startActivity(showAddForm);
	}

	private void addInspection() 
	{
		Intent showAddForm = new Intent(mContext, AddInspectionActivity.class);
		showAddForm.putExtra("InspectionId", 0);
		startActivity(showAddForm);
	}

	@Override
	public void onResume() 
	{
		invalidateOptionsMenu();
		mInspFrag.populateInspections();
		if (Utils.isTablet(mContext)) 
		{
			if (mRepository.mInspections.size() < 1) 
			{
				hideFragment(mInspectionInfoFragment);
			}
		}
		super.onResume();

		Button settingsButton = (Button) mOverlayFramelayout.findViewById(R.id.openPreferences);


		//if no Data
		if (mRepository.mProductionItems.isEmpty()) 
		{
			//if overlay is not present
			if (settingsButton == null)
			{
				mOverlayFramelayout.addView(mHelpView);

				TextView editSettingsText = (TextView)  mOverlayFramelayout.findViewById(R.id.editSettingsTV);
				TextView executeSyncText = (TextView)  mOverlayFramelayout.findViewById(R.id.executeSyncTV);
				settingsButton = (Button) mOverlayFramelayout.findViewById(R.id.openPreferences);
				Button syncButton = (Button) mOverlayFramelayout.findViewById(R.id.executeSyncButton);

				//settings provided 
				if (settingsProvided()) 
				{

					editSettingsText.setVisibility(View.GONE);
					executeSyncText.setVisibility(View.VISIBLE);
					settingsButton.setVisibility(View.GONE);
					syncButton.setVisibility(View.VISIBLE);
					syncButton.setOnClickListener(mButtonListener);

				}
				//settings not provided (probably first start)
				else 
				{
					editSettingsText.setVisibility(View.VISIBLE);
					executeSyncText.setVisibility(View.GONE);
					settingsButton.setVisibility(View.VISIBLE);
					syncButton.setVisibility(View.GONE);
					settingsButton.setOnClickListener(mButtonListener);
				}

			}
		}
		//if Data
		else
		{
			//if overlay is present
			if (settingsButton != null)
			{
				settingsButton.setOnClickListener(null);
				mOverlayFramelayout.removeView(mHelpView);
			}
		}
	}

	private boolean settingsProvided() 
	{
		return true;
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
		case R.id.startSync:
			startSync();
			return true;
		case R.id.startInspection:
			startInspection(mInspectionId);
			return true;
		case R.id.finishInspection:
			finishInspection(mInspectionId);
			return true;
		case R.id.writeData:
			mRepository.writeDataToOdataService(mContext);
			return true;
		case R.id.showPreferences:
			startPreferences();
			return true;
		case R.id.deleteLocalData:
			deleteLocalData();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void deleteLocalData() 
	{
		resetView();
		mRepository.deleteLocalData();
		onResume();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		Inspection insp = mRepository.getInspectionById(mInspectionId);
		if (insp != null && !insp.isFinished()) {
			MenuItem startItem = menu.findItem(R.id.startInspection);
			MenuItem stopItem = menu.findItem(R.id.finishInspection);
			if (mInspectionId != 0)
			{
				if (mInspectionRunning) 
				{
					//show stop button
					startItem.setVisible(false);
					stopItem.setVisible(true);
				}
				else 
				{
					//show start button
					startItem.setVisible(true);
					stopItem.setVisible(false);	
				}
			} 
		}
		return true;
	}

	private void startInspection(long mInspectionId) 
	{
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		String responsibleSubjectId = mPrefs.getString("default_responsibleSubject", "none");
		if (responsibleSubjectId != "none") 
		{
			mInspectionRunning = true;
			invalidateOptionsMenu();

			mRunningInspectionFragment.getInspectionDetails(mInspectionId);

			// add fragment to the fragment container layout
			hideFragment(mInspectionInfoFragment);
			showFragment(mRunningInspectionFragment);
			toggleInspectionList();
		}
		else
		{
			Toast.makeText(mContext, "Bitte wählen Sie zuerst in den Einstellungen einen Mitarbeiter aus.", Toast.LENGTH_SHORT).show();
		}
	}

	private void finishInspection(final long inspectionId) 
	{


		new AlertDialog.Builder(mContext)
		.setTitle("Prüfung abschließen")
		.setMessage("Möchten Sie die Prüfung abschließen?")
		.setPositiveButton("Ja", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				Inspection insp = mRepository.getInspectionById(inspectionId);
				mRepository.clearDiscardItems(insp.getInternalId());
				insp.setFinished(true);
				insp.setManipulated(true);
				mRepository.persistLocalInspections();
				mInspectionRunning = false;
				invalidateOptionsMenu();
				mInspFrag.populateInspections();
			}
		}).setNegativeButton("Nein", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mInspectionRunning = false;
		invalidateOptionsMenu();
		hideFragment(mRunningInspectionFragment);
		showFragment(mInspectionInfoFragment);
		toggleInspectionList();

		mInspectionInfoFragment.updateInspectionInfo(inspectionId);
	}
	private void showFragment(Fragment frag) 
	{
		FragmentTransaction ft = mFragMan.beginTransaction();
		ft.setCustomAnimations(android.R.anim.fade_in, 0, android.R.anim.fade_in, 0);
		ft.show(frag);
		ft.commit();
	}

	private void hideFragment(Fragment frag) 
	{
		FragmentTransaction ft = mFragMan.beginTransaction();
		ft.setCustomAnimations(android.R.anim.fade_in, 0, android.R.anim.fade_in, 0);
		ft.hide(frag);
		ft.commit();
	}

	private void toggleInspectionList() 
	{
		FrameLayout inspListLayout = (FrameLayout) findViewById(R.id.inspectionListFrameLayout);
		if (inspListLayout.getVisibility() == View.VISIBLE) 
		{
			inspListLayout.setVisibility(View.GONE);
		} 
		else
		{
			//Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
			//inspListLayout.startAnimation(slideIn);
			inspListLayout.setVisibility(View.VISIBLE);
		}
	}

	private void startSync() 
	{
		boolean unsavedData = false;
		if ( mRepository.mInspections != null) 
		{
			for (Inspection insp : mRepository.mInspections) 
			{
				if (insp.isManipulated()) 
				{
					unsavedData = true;
					break;
				}
			}
		}

		if (unsavedData) 
		{
			// 1. Instantiate an AlertDialog.Builder with its constructor
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage("Es gibt ungespeicherte Data. Durch ein erneutes Abrufen gehen diese verloren! Möchten sie fortfahren?")
			.setTitle("Ungespeicherte Daten");
			builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

				}
			});

			builder.setPositiveButton("Fortfahren", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					syncAndResetView();
				}
			});

			// 3. Get the AlertDialog from create()
			AlertDialog dialog = builder.create();
			dialog.show();
		}
		else
		{
			syncAndResetView();
			mOverlayFramelayout.removeView(mHelpView);
		}
	}

	private void resetView() 
	{
		if (Utils.isTablet(mContext))
		{
			hideFragment(mInspectionInfoFragment);
			hideFragment(mRunningInspectionFragment);

			if (mInspectionRunning) 
			{
				toggleInspectionList();
				mInspectionRunning = false;
			}
		}
	}

	private void syncAndResetView() 
	{
		resetView();
		mRepository.deleteLocalData();
		mRepository.getDataFromOdataService(mContext);
		invalidateOptionsMenu();
	}

	@Override
	public void onBackPressed() 
	{
		if (Utils.isTablet(mContext)) 
		{
			if (mInspectionRunning) 
			{
				finishInspection(mInspectionId);
			}
			else 
			{
				super.onBackPressed();
			}
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		this.startActivity(startPreferences);
	}

	@Override
	public void onInspectionSelected(long internalInspectionId) {

		mInspectionId = internalInspectionId;
		if (mInspectionInfoFragment == null) 
		{
			//if ReadingFragment not present start new Activity (Phone)
			Intent showInfos = new Intent(mContext, InspectionInfoActivity.class);
			showInfos.putExtra("InspectionId", mInspectionId);
			startActivity(showInfos);
		}
		else
		{
			//if ReadingFragment is present update (Tablet)
			mInspectionInfoFragment.updateInspectionInfo(mInspectionId);

			//if fragment is invisible
			if (!mInspectionInfoFragment.isVisible()) 
			{
				showFragment(mInspectionInfoFragment);
			} 
			invalidateOptionsMenu();
		}
	}

	@Override
	public void onInspectionCopied(long id) 
	{
		addInspection(id);

	}
}
