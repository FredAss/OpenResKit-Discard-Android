package htw.bui.openreskit.discard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import java.util.List;

import htw.bui.openreskit.discard.adapters.DiscardItemAdapter;
import htw.bui.openreskit.domain.discard.DiscardImageSource;
import htw.bui.openreskit.domain.discard.DiscardItem;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.domain.discard.InspectionAttribute;
import htw.bui.openreskit.domain.organisation.Employee;
import htw.bui.openreskit.domain.organisation.EmployeeGroup;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;

import htw.bui.openreskit.odata.DiscardRepository;

import com.google.inject.Inject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;

import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class RunningInspectionFragment extends RoboFragment {

	private Activity mContext;
	private Inspection mInspection;
	private List<DiscardItem> mDiscardItems;
	private DiscardItemAdapter mDiscardItemAdapter;
	private int mEditedDiscardItemId;
	private SharedPreferences mPrefs;
	private Uri mOutputFileUri;

	@InjectView
	(R.id.runningInspNameTV) TextView mInspNameText;
	@InjectView
	(R.id.inspProductionItemTV) TextView mInspProductionItemText;
	@InjectView
	(R.id.runningInspDateTV) TextView mInspDateText;
	@InjectView
	(R.id.runningInspResponsibleSubjectTV) TextView mInspResponsibleSubjectText;
	@InjectView
	(R.id.runningInspUnitTV) TextView mRunningInspUnitTV;
	@InjectView
	(R.id.runningInspAttributes)  AdapterView<DiscardItemAdapter> mRunningInspAttributesGridView;

	@Inject
	DiscardRepository mRepository;

	private OnItemClickListener mItemClickListener = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
		{
			discardItemSelected(id);
		}
	};


	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		super.onCreateView(inflater, container, savedInstanceState);
		mContext = getActivity();
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.running_inspection, container, false);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mContext.getMenuInflater();
		inflater.inflate(R.menu.discarditem_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.takePicture:
			takePictureForDiscardItem(info.id);
			return true;
		case R.id.editDescription:
			editDescriptionForDiscardItem(info.id);
			return true;
		case R.id.showDiscardItemInfo:
			showDiscardItemInfo(info.id);
			return true;
		case R.id.editQuantity:
			editQuantityForDiscardItem(info.id);
		default:
			return super.onContextItemSelected(item);
		}
	}


	private void editQuantityForDiscardItem(final long id) 
	{
		int quantity = 0;
		for (DiscardItem di : mInspection.getDiscardItems()) 
		{
			if(di.getInternalID() == id) 
			{
				quantity = di.getQuantity();
			}
		}

		final EditText quantityText = new EditText(mContext);
		quantityText.setInputType(InputType.TYPE_CLASS_NUMBER);
		quantityText.setText(String.valueOf(quantity));

		new AlertDialog.Builder(mContext)
		.setTitle("Manuelle Eingabe der Anzahl")
		.setMessage("Bitte geben Sie die Anzahl der Fehlteile ein")
		.setView(quantityText)
		.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				for (DiscardItem di : mInspection.getDiscardItems()) 
				{
					if(di.getInternalID() == id) 
					{
						di.setQuantity(Integer.valueOf(quantityText.getText().toString()));
					}
				}
				mRepository.persistLocalInspections();
			}
		}).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();

	}

	private void showDiscardItemInfo(long discardItemid) 
	{
		Intent showInfos = new Intent(mContext, DiscardItemInfoActivity.class);
		showInfos.putExtra("InspectionId", mInspection.getId());
		showInfos.putExtra("DiscardItemId", discardItemid);
		startActivity(showInfos);
	}

	private void editDescriptionForDiscardItem(final long id) 
	{
		String desc = null;
		for (DiscardItem di : mInspection.getDiscardItems()) 
		{
			if(di.getInternalID() == id) 
			{
				desc = di.getDescription();
			}
		}

		final EditText descText = new EditText(mContext);
		descText.setText(desc);

		new AlertDialog.Builder(mContext)
		.setTitle("Beschreibung")
		.setMessage("Bitte geben Sie eine Beschreibung zum Fehlteil ein")
		.setView(descText)
		.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				for (DiscardItem di : mInspection.getDiscardItems()) 
				{
					if(di.getInternalID() == id) 
					{
						di.setDescription(descText.getText().toString());
					}
				}
				mRepository.persistLocalInspections();
			}
		}).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();
	}

	private void takePictureForDiscardItem(long id) 
	{
		if (isIntentAvailable(mContext, MediaStore.ACTION_IMAGE_CAPTURE)) 
		{
			File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.jpg");
			mOutputFileUri = Uri.fromFile(file);
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputFileUri);
			mEditedDiscardItemId = (int)id;
			startActivityForResult(takePictureIntent, 54233);
		}
	}

	public static boolean isIntentAvailable(Context context, String action) 
	{
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
				packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{

		if (requestCode == 54233)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				Bitmap bmpBitmap;
				try {
					bmpBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mOutputFileUri);


					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					Bitmap finalBitmap = Utils.scaleToFill(bmpBitmap, 1024, 768);
					finalBitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
					byte[] byteArray = stream.toByteArray();

					for (DiscardItem di : mInspection.getDiscardItems()) 
					{
						if(di.getInternalID() == mEditedDiscardItemId) 
						{
							DiscardImageSource dis = new DiscardImageSource();
							dis.setBinarySource(Base64.encodeToString(byteArray,0));
							InspectionAttribute ia = mRepository.getInspectionAttributeById(di.getInspectionAttribute().getId());
							di.getInspectionAttribute().setDiscardImageSource(dis);
							ia.setDiscardImageSource(dis);
							ia.setManipulated(true);
						}
					}
				} 
				catch (FileNotFoundException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		mDiscardItemAdapter.notifyDataSetChanged();
		mRepository.persistLocalInspections();
	}

	public void getInspectionDetails(long inspectionId) 
	{

		if (mRunningInspAttributesGridView.getOnItemClickListener() == null) 
		{
			mRunningInspAttributesGridView.setOnItemClickListener(mItemClickListener);
		}

		registerForContextMenu(mRunningInspAttributesGridView);

		mPrefs= PreferenceManager.getDefaultSharedPreferences(mContext);
		String responsibleSubjectId = mPrefs.getString("default_responsibleSubject", "none");
		ResponsibleSubject rs = mRepository.getResponsibleSubjectById(Long.valueOf(responsibleSubjectId));

		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
		mInspection = mRepository.getInspectionById(inspectionId);
		mInspection.setResponsibleSubject(rs);
		mInspection.setInspectionDate(new Date());

		
		if (mInspection.getDiscardItems() != null) 
		{
			mDiscardItems = mInspection.getDiscardItems();
			List<InspectionAttribute> presentInspectionAttributes = new ArrayList<InspectionAttribute>();
			List<InspectionAttribute> allInspectionAttributes = new ArrayList<InspectionAttribute>(mInspection.getProductionItem().getInspectionAttributes());
			List<Integer> usedIds = new ArrayList<Integer>();

			for (InspectionAttribute ia : mInspection.getProductionItem().getInspectionAttributes()) 
			{
				for (DiscardItem di : mDiscardItems) 
				{
					if (di.getInternalID() == 0) 
					{
						di.setInternalID(di.getId());
					}
					usedIds.add(di.getId());
					if (di.getInspectionAttribute().getId() == ia.getId()) 
					{
						presentInspectionAttributes.add(ia);
					}
				}
			} 
			allInspectionAttributes.removeAll(presentInspectionAttributes);

			int internalIdCount = 1;
			for (InspectionAttribute ia : allInspectionAttributes) 
			{
				mInspection.getDiscardItems().add(createDiscardItem(generateId(internalIdCount, usedIds), ia));
				internalIdCount++;
			}
		} 
		else 
		{
			int internalIdCount = 1;
			List<DiscardItem> newDiscardItems = new ArrayList<DiscardItem>();
			for (InspectionAttribute ia : mInspection.getProductionItem().getInspectionAttributes()) 
			{
				
				newDiscardItems.add(createDiscardItem(internalIdCount, ia));
				internalIdCount++;
			}
			mInspection.setDiscardItems(newDiscardItems);
			mDiscardItems = mInspection.getDiscardItems();
		}

		mInspNameText.setText(mInspection.getName());
		mInspProductionItemText.setText(mInspection.getProductionItem().getItemName() + ", " + mInspection.getProductionItem().getItemNumber()+ ", " + mInspection.getProductionItem().getCustomer().getName());
		
		mInspDateText.setText(formatter.format(mInspection.getInspectionDate()));
		if (mInspection.getResponsibleSubject().getClass() == Employee.class) 
		{
			Employee e = (Employee) mInspection.getResponsibleSubject();
			mInspResponsibleSubjectText.setText(e.getLastName() + ", " + e.getFirstName());
		}
		else if (mInspection.getResponsibleSubject().getClass() == EmployeeGroup.class) 
		{
			EmployeeGroup g = (EmployeeGroup) mInspection.getResponsibleSubject();
			mInspResponsibleSubjectText.setText(g.getName() + " (Gruppe)");
		}

		mRunningInspUnitTV.setText(mInspection.getTotalAmountUnit());

		mDiscardItemAdapter = new DiscardItemAdapter(getActivity(), R.layout.discarditem_list_item, mDiscardItems);
		mRunningInspAttributesGridView.setAdapter(mDiscardItemAdapter);
	}

	private int generateId (int id, List<Integer> blacklist) 
	{
		while (blacklist.contains(id)) 
		{
			id++;
		}
		return id;
	}

	private DiscardItem createDiscardItem(int id, InspectionAttribute ia) 
	{
		DiscardItem di = new DiscardItem();
		di.setInternalID(id);
		di.setQuantity(0);
		di.setInspectionAttribute(ia);
		return di;
	}

	private void discardItemSelected(long id) 
	{
		if (!mInspection.isManipulated()) 
		{
			mInspection.setManipulated(true);
		}

		for (DiscardItem di : mDiscardItems) 
		{
			if (di.getInternalID() == id) 
			{
				di.setQuantity(di.getQuantity()+1);
			}
		}
		mDiscardItemAdapter.notifyDataSetChanged();
		mRepository.persistLocalInspections();

	}
}