package htw.bui.openreskit.discard;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import htw.bui.openreskit.discard.adapters.ProductionItemAdapter;
import htw.bui.openreskit.discard.adapters.ResponsibleSubjectAdapter;
import htw.bui.openreskit.discard.enums.InspectionTypes;
import htw.bui.openreskit.discard.enums.Shifts;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.domain.discard.ProductionItem;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;
import htw.bui.openreskit.odata.DiscardRepository;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.inject.Inject;


import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.InjectView;

public class AddInspectionActivity extends RoboFragmentActivity 
{

	@Inject
	DiscardRepository mRepository;

	@InjectView
	(R.id.inspNameTV) EditText mInspNameText;
	@InjectView
	(R.id.inspResponsibleSubjectTV) Spinner mInspResponsibleSubjectSpinner;

	@InjectView
	(R.id.inspProductionItemTV1) AutoCompleteTextView mInspProductionItemSearchBox;
	@InjectView
	(R.id.selectedProdItem) TextView mSelectedProdItem;

	@InjectView
	(R.id.prodDateTV) TextView mProdDateText;
	@InjectView
	(R.id.prodDatePicker) ImageButton mProdDatePickerButton;

	@InjectView
	(R.id.prodShiftTV) Spinner mProdShiftSpinner;
	@InjectView
	(R.id.inspDescriptionTV) EditText mInspDescriptionEditText;

	@InjectView
	(R.id.inspTypeTV) Spinner mInspTypeSpinner;
	@InjectView
	(R.id.inspShiftTV) Spinner mInspShiftSpinner;
	@InjectView
	(R.id.inspSampleSizeTV) EditText mInspSampleSizeEditText;
	@InjectView
	(R.id.inspTotalAmountTV) EditText mInspTotalAmountEditText;
	@InjectView
	(R.id.inspTotalAmountUnitTV) EditText mInspTotalAmountUnitEditText;

	private Inspection mNewInspection;
	private SimpleDateFormat mFormatter;
	private Activity mContext;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_inspection);
		mContext = this;
		mFormatter = new SimpleDateFormat("dd.MM.yyyy");
		Intent launchingIntent = getIntent();
		long inspectionId = launchingIntent.getExtras().getLong("InspectionId");
		if (inspectionId > 0) 
		{
			mNewInspection = new Inspection(mRepository.getInspectionById(inspectionId));
		}
		else
		{
			mNewInspection = new Inspection();
		}

		mNewInspection.setId(0);
		mNewInspection.setInternalId((int) (mRepository.getMaxInspectionId()+1));
		mNewInspection.setManipulated(true);

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);

		FillAddInspectionForm();

	}

	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			if (v.getId() == R.id.prodDatePicker) 
			{

				showDatePicker();
			}

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_inspection_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.saveAdd:
			saveNewInspection();
			return true;
		case R.id.cancelAdd:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveNewInspection() 
	{
		String errorMessage = "Das Formular ist nicht vollständig ausgefüllt!\n";
		boolean errorOccured = false;
		if (mInspNameText.getText().toString().trim().length() < 1) 
		{
			errorMessage += "Bitte geben Sie eine Untersuchungsbezeichnung ein!\n";
			errorOccured = true;
		}
		if (mNewInspection.getProductionDate() == null) 
		{
			errorMessage += "Bitte Wählen Sie ein Produktionsdatum!\n";
			errorOccured = true;
		}
		if (mNewInspection.getProductionItem() == null)
		{
			errorMessage += "Bitte Wählen Sie ein zu untersuchendes Product!\n";
			errorOccured = true;
		}

		if (!errorOccured) 
		{
			mNewInspection.setName(mInspNameText.getText().toString());
			mNewInspection.setResponsibleSubject((ResponsibleSubject) mInspResponsibleSubjectSpinner.getSelectedItem());
			//ProductionItem set in OnClick Callback of AutoComplete
			//ProductionDate set in onDateSet Callback of DatePicker
			mNewInspection.setProductionShift(mProdShiftSpinner.getSelectedItemPosition());
			mNewInspection.setDescription(mInspDescriptionEditText.getText().toString());
			mNewInspection.setInspectionType(mInspTypeSpinner.getSelectedItemPosition());
			mNewInspection.setInspectionShift(mInspShiftSpinner.getSelectedItemPosition());
			if (mInspSampleSizeEditText.getText().length() > 0) 
			{
				mNewInspection.setInspectionFrequencyOrSampleSize(Integer.valueOf(mInspSampleSizeEditText.getText().toString()));
			}
			if (mInspTotalAmountEditText.getText().length() > 0) 
			{			
				mNewInspection.setTotalAmount(Integer.valueOf(mInspTotalAmountEditText.getText().toString()));
			}
			mNewInspection.setTotalAmountUnit(mInspTotalAmountUnitEditText.getText().toString());
			mRepository.mInspections.add(mNewInspection);
			mRepository.persistLocalInspections();
			finish();
		} else
		{
			Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
		}
	}

	private void FillAddInspectionForm() 
	{
		if (mNewInspection.getName() != null) 
		{
			mInspNameText.setText(mNewInspection.getName());
		}

		List<ResponsibleSubject> responsibleSubjects = mRepository.mResponsibleSubjects;
		ResponsibleSubjectAdapter responsibleSubjectAdapter = new ResponsibleSubjectAdapter(this, R.layout.default_spinner_item, getLayoutInflater(), responsibleSubjects);
		mInspResponsibleSubjectSpinner.setAdapter(responsibleSubjectAdapter);
		if (mNewInspection.getResponsibleSubject() != null) 
		{
			mInspResponsibleSubjectSpinner.setSelection(responsibleSubjects.indexOf(mNewInspection.getResponsibleSubject()));
		}

		final List<ProductionItem> productionItems = mRepository.mProductionItems;
		ProductionItemAdapter productionItemAdapter = new ProductionItemAdapter(this, R.layout.default_spinner_item, getLayoutInflater(), productionItems);
		mInspProductionItemSearchBox.setAdapter(productionItemAdapter);
		mInspProductionItemSearchBox.setOnItemClickListener(new OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) 
			{
				for (ProductionItem pi: productionItems) 
				{
					if (pi.getId() == id) 
					{
						mNewInspection.setProductionItem(pi);
						mSelectedProdItem.setText(pi.toString());
					}
				}
			}
		});

		if (mNewInspection.getProductionItem() != null)
		{
			mSelectedProdItem.setText(mNewInspection.getProductionItem().toString());
		}

		if (mNewInspection.getProductionDate() != null) 
		{
			mProdDateText.setText(mFormatter.format(mNewInspection.getProductionDate()));	
		}
		else
		{
			mProdDateText.setText("nicht gewählt");
		}

		mProdDateText.setFocusable(false);
		mProdDatePickerButton.setOnClickListener(mButtonListener);

		mProdShiftSpinner.setAdapter(new ArrayAdapter<Shifts>(this,
				android.R.layout.simple_list_item_1, Shifts.values()));
		mProdShiftSpinner.setSelection(mNewInspection.getProductionShift());

		if (mNewInspection.getDescription() != null) 
		{
			mInspDescriptionEditText.setText(mNewInspection.getDescription());	
		}


		mInspTypeSpinner.setAdapter(new ArrayAdapter<InspectionTypes>(this,
				android.R.layout.simple_list_item_1, InspectionTypes.values()));
		mInspTypeSpinner.setSelection(mNewInspection.getInspectionType());
		mInspShiftSpinner.setAdapter(new ArrayAdapter<Shifts>(this,
				android.R.layout.simple_list_item_1, Shifts.values()));
		mInspShiftSpinner.setSelection(mNewInspection.getInspectionShift());

		if (mNewInspection.getInspectionFrequencyOrSampleSize() > 0) 
		{
			mInspSampleSizeEditText.setText(String.valueOf(mNewInspection.getInspectionFrequencyOrSampleSize()));	
		} 
		if (mNewInspection.getTotalAmount() > 0) 
		{
			mInspTotalAmountEditText.setText(String.valueOf(mNewInspection.getTotalAmount()));	
		}		
		if (mNewInspection.getTotalAmountUnit() != null) 
		{
			mInspTotalAmountUnitEditText.setText(mNewInspection.getTotalAmountUnit());
		}
	}

	private void showDatePicker() 
	{
		DatePickerFragment date = new DatePickerFragment();
		/**
		 * Set Up Current Date Into dialog
		 */
		Calendar calender = Calendar.getInstance();
		if (mNewInspection.getProductionDate() != null) 
		{
			calender.setTime(mNewInspection.getProductionDate());
		}
		Bundle args = new Bundle();
		args.putInt("year", calender.get(Calendar.YEAR));
		args.putInt("month", calender.get(Calendar.MONTH));
		args.putInt("day", calender.get(Calendar.DAY_OF_MONTH));
		date.setArguments(args);
		/**
		 * Set Call back to capture selected date
		 */
		date.setCallBack(ondate);
		date.show(getSupportFragmentManager(), "Date Picker");
	}

	OnDateSetListener ondate = new OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) 
		{
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, monthOfYear);
			c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			mNewInspection.setProductionDate(c.getTime());
			mProdDateText.setText(mFormatter.format(c.getTime()));

		}
	};
}
