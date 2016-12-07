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

import htw.bui.openreskit.odata.RepositoryChangedListener;
import htw.bui.openreskit.odata.DiscardRepository;
import htw.bui.openreskit.discard.R;
import htw.bui.openreskit.discard.adapters.InspectionAdapter;
import htw.bui.openreskit.domain.discard.Inspection;

import java.util.EventObject;

import com.google.inject.Inject;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

import android.app.Activity;
import android.graphics.Color;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class InspectionFragment extends RoboFragment {


	@Inject
	private DiscardRepository mRepository;

	@InjectView
	(R.id.seriesListView) ListView mListView;


	private Parcelable mListState;
	private Activity mContext;
	IInspectionHandling mListener;
	boolean mDualPane;
	int mListPosition = 0;

	private RepositoryChangedListener mRepositoryChangedListener = new RepositoryChangedListener() 
	{
		public void handleRepositoryChange(EventObject e) 
		{
			populateInspections();
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		registerForContextMenu(mListView);
		mRepository.addEventListener(mRepositoryChangedListener);

		if (savedInstanceState != null) 
		{
			mListState = savedInstanceState.getParcelable("listState");
			mListPosition = savedInstanceState.getInt("listPosition");
		}
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mListView.setCacheColorHint(Color.TRANSPARENT);
		mListView.setOnItemClickListener(mListItemClickListener);

		TextView emptyView = new TextView(mContext);
		emptyView.setText("Es sind keine Daten vorhanden");
		mListView.setEmptyView(emptyView);

		populateInspections();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(mListState!=null){
			mListView.onRestoreInstanceState(mListState);
		} 
		mListState = null;

	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.inspection_list, container, false);
	}

	@Override
	public void onSaveInstanceState (Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		Parcelable state = mListView.onSaveInstanceState();
		outState.putParcelable("listState", state);
		int listPosition = mListView.getSelectedItemPosition();
		outState.putInt("listPosition", listPosition);
	}

	public void populateInspections() 
	{
		InspectionAdapter adapter = new InspectionAdapter(mContext, R.layout.inspection_list_row, mRepository.mInspections);
		mListView.setAdapter(adapter);
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mContext.getMenuInflater();
		inflater.inflate(R.menu.inspection_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.openInspection:
			openInspection(info.id);
			return true;
		case R.id.copyInspection:
			copyInspection(info.id);
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void copyInspection(long id) 
	{
		mListener.onInspectionCopied(id);
	}

	private void openInspection(long id) 
	{
		Inspection insp = mRepository.getInspectionById(id);
		insp.setFinished(false);
		populateInspections();
	}

	public long getSelectedSeriesId() 
	{
		return mListView.getItemIdAtPosition(mListPosition);
	}

	private OnItemClickListener mListItemClickListener = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
		{
			mListener.onInspectionSelected(id);
		}
	};

	// Container Activity must implement this interface
	public interface IInspectionHandling 
	{
		public void onInspectionSelected(long id);
		public void onInspectionCopied(long id);
	}

	//Throw if interface not implemented
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (IInspectionHandling) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnInspectionSelectedListener");
		}
	}


}