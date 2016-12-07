package htw.bui.openreskit.discard;



import java.text.SimpleDateFormat;
import java.util.Locale;

import com.google.inject.Inject;

import htw.bui.openreskit.discard.enums.Shifts;
import htw.bui.openreskit.discard.enums.InspectionTypes;
import htw.bui.openreskit.domain.discard.DiscardItem;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.domain.discard.InspectionAttribute;
import htw.bui.openreskit.domain.organisation.Employee;
import htw.bui.openreskit.domain.organisation.EmployeeGroup;
import htw.bui.openreskit.odata.DiscardRepository;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class InspectionInfoFragment extends RoboFragment 
{
	@InjectView
	(R.id.inspNameTV) TextView mInspNameText;
	@InjectView
	(R.id.inspDateTV) TextView mInspDateText;
	@InjectView
	(R.id.inspResponsibleSubjectTV) TextView mInspResponsibleSubjectText;
	@InjectView
	(R.id.inspStatusTV) TextView mInspStatusText;

	@InjectView
	(R.id.inspProductionItemTV) TextView mInspProductionItemText;
	@InjectView
	(R.id.prodDateTV) TextView mProdDateText;
	@InjectView
	(R.id.prodShiftTV) TextView mProdShiftText;
	@InjectView
	(R.id.inspDescriptionTV) TextView mInspDescriptionText;

	@InjectView
	(R.id.inspTypeTV) TextView mInspTypeText;
	@InjectView
	(R.id.inspShiftTV) TextView mInspShiftText;
	@InjectView
	(R.id.inspSampleSizeTV) TextView mInspSampleSizeText;
	@InjectView
	(R.id.inspTotalAmountTV) TextView mInspTotalAmountText;
	@InjectView
	(R.id.inspTotalAmountUnitTV) TextView mInspTotalAmountUnitText;

	@InjectView
	(R.id.inspAttrTable) TableLayout mInspAttrTable;

	@Inject
	private DiscardRepository mRepository;

	private int mInspectionId;
	private Activity mContext;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		mContext = getActivity();
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.inspection_info, container, false);
	}

	public void getInspectionDetails(long internalInspectionId) 
	{
		mInspectionId = (int) internalInspectionId;
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

		Inspection insp = mRepository.getInspectionById(mInspectionId);
		mInspNameText.setText(insp.getName());
		if (insp.getInspectionDate() != null) 
		{
			mInspDateText.setText(formatter.format(insp.getInspectionDate()));
		}
		else
		{
			mInspDateText.setText("noch nicht durchgeführt");
		}
		if (insp.getResponsibleSubject().getClass() == Employee.class) 
		{
			Employee e = (Employee) insp.getResponsibleSubject();
			mInspResponsibleSubjectText.setText(e.getLastName() + ", " + e.getFirstName());
		}
		else if (insp.getResponsibleSubject().getClass() == EmployeeGroup.class) 
		{
			EmployeeGroup g = (EmployeeGroup) insp.getResponsibleSubject();
			mInspResponsibleSubjectText.setText(g.getName() + " (Gruppe)");
		}

		if (insp.isFinished()) 
		{
			mInspStatusText.setText("Abgeschlossen");
		}
		else
		{
			mInspStatusText.setText("Offen");
		}

		mInspProductionItemText.setText(insp.getProductionItem().getItemName() + ", " + insp.getProductionItem().getItemNumber()+ ", " + insp.getProductionItem().getCustomer().getName());
		mProdDateText.setText(formatter.format(insp.getProductionDate()));
		mProdShiftText.setText(Shifts.values()[insp.getProductionShift()].toString());
		mInspDescriptionText.setText(insp.getDescription());

		mInspTypeText.setText(InspectionTypes.values()[insp.getInspectionType()].toString());
		mInspShiftText.setText(Shifts.values()[insp.getInspectionShift()].toString());
		mInspSampleSizeText.setText(String.valueOf(insp.getInspectionFrequencyOrSampleSize()));
		mInspTotalAmountText.setText(String.valueOf(insp.getTotalAmount()));
		mInspTotalAmountUnitText.setText(insp.getTotalAmountUnit());


		mInspAttrTable.removeAllViews();
		TableRow headerRow = (TableRow)LayoutInflater.from(mContext).inflate(R.layout.inspection_attribute_table_header, null);
		mInspAttrTable.addView(headerRow);
		if (insp.getProductionItem().getInspectionAttributes() != null) 
		{
			for (InspectionAttribute ai : insp.getProductionItem().getInspectionAttributes()) 
			{
	
				TableRow row = (TableRow)LayoutInflater.from(mContext).inflate(R.layout.inspection_attribute_table_row, null);
				((TextView)row.findViewById(R.id.textItem1)).setText(ai.getNumber());
				((TextView)row.findViewById(R.id.textItem2)).setText(ai.getDescription());
				String quantityText = "nV";
				if (insp.getDiscardItems() != null) 
				{
					for (DiscardItem di : insp.getDiscardItems()) 
					{
						if (di.getInspectionAttribute().getId() == ai.getId()) 
						{
							quantityText = String.valueOf(di.getQuantity());
						}
					}
				}
	
				((TextView)row.findViewById(R.id.textItem3)).setText(quantityText);
				mInspAttrTable.addView(row);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshInspectionInfo();
	}

	private void refreshInspectionInfo() 
	{
		if (mInspectionId > 0) 
		{
			getInspectionDetails(mInspectionId);
		}
	}

	public void updateInspectionInfo(long inspectionId) 
	{
		getInspectionDetails(inspectionId);
	}
}
