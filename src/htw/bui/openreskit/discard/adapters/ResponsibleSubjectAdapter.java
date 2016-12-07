package htw.bui.openreskit.discard.adapters;

import htw.bui.openreskit.discard.R;
import htw.bui.openreskit.domain.organisation.Employee;
import htw.bui.openreskit.domain.organisation.EmployeeGroup;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResponsibleSubjectAdapter extends BaseAdapter 
{
	private final List<ResponsibleSubject> mResponsibleSubjects;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;

	public ResponsibleSubjectAdapter(Activity context, int rowResID, LayoutInflater layoutInflater, List<ResponsibleSubject> responsibleSubjects) 
	{
		mResponsibleSubjects = responsibleSubjects;
		mRowResID = rowResID;
		mLayoutInflater = layoutInflater;
	}

	@Override
	public int getCount() 
	{
		return mResponsibleSubjects.size();
	}

	@Override
	public Object getItem(int position) 
	{
		return mResponsibleSubjects.get(position);
	}

	@Override
	public long getItemId(int position) 
	{
		return mResponsibleSubjects.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);

			ResponsibleSubjectViewHolder viewHolder = new ResponsibleSubjectViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.textView1);
			rowView.setTag(viewHolder);
		}

		ResponsibleSubject rs = mResponsibleSubjects.get(position);
		ResponsibleSubjectViewHolder holder = (ResponsibleSubjectViewHolder) rowView.getTag();
		if (rs.getClass().getName() == Employee.class.getName()) 
		{
			Employee e = (Employee) rs;
			holder.name.setText(e.getFirstName() + ", " + e.getLastName());
		} 
		else if (rs.getClass().getName() == EmployeeGroup.class.getName()) 
		{
			EmployeeGroup g = (EmployeeGroup) rs;
			holder.name.setText(g.getName() + " (Gruppe)");
		}
		return rowView;
	}

	static class ResponsibleSubjectViewHolder 
	{
		public TextView name;
	}

}
