package htw.bui.openreskit.discard;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import roboguice.fragment.RoboDialogFragment;

public class DatePickerFragment extends RoboDialogFragment {
	OnDateSetListener ondateSet;

	public DatePickerFragment() {
	}

	public void setCallBack(OnDateSetListener ondate) {
		ondateSet = ondate;
	}

	private int year, month, day;

	@Override
	public void setArguments(Bundle args) {
		super.setArguments(args);
		year = args.getInt("year");
		month = args.getInt("month");
		day = args.getInt("day");
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new DatePickerDialog(getActivity(), ondateSet, year, month, day);
	}
}  
