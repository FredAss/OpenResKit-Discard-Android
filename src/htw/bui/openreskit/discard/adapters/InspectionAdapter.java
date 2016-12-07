package htw.bui.openreskit.discard.adapters;

import htw.bui.openreskit.discard.R;
import htw.bui.openreskit.domain.discard.Inspection;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class InspectionAdapter extends BaseAdapter {

	private final Activity mContext;
	private final List<Inspection> mInspections;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;

	public InspectionAdapter(final Activity context, final int rowResID, final List<Inspection> inspections) 
	{
		mContext = context;
		mRowResID = rowResID;
		mInspections = inspections;
		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() 
	{
		return mInspections.size();
	}

	//returns position in List
	public Object getItem(int position) 
	{
		return mInspections.get(position);
	}

	//returns the Database id of the item
	public long getItemId(int position) {
		return mInspections.get(position).getInternalId();
	}

	// connects Unit members to be displayed with (text)views in a layout
	// per item
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);

			InspectionViewHolder viewHolder = new InspectionViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.text1);
			viewHolder.coloredBox = (ImageView) rowView.findViewById(R.id.ImageView01);
			viewHolder.productInfo = (TextView) rowView.findViewById(R.id.text2);
			rowView.setTag(viewHolder);
		}

		final Inspection insp = mInspections.get(position);
		InspectionViewHolder holder = (InspectionViewHolder) rowView.getTag();
		holder.name.setText(insp.getName());
		holder.productInfo.setText(insp.getProductionItem().getItemName() + ", " +insp.getProductionItem().getCustomer().getName());

		if (insp.isFinished()) 
		{

			holder.coloredBox.setImageResource(R.drawable.device_access_secure);
		}
		return rowView;
	}

	static class InspectionViewHolder 
	{
		public TextView name;
		public ImageView coloredBox;
		public TextView productInfo;
	}

}



