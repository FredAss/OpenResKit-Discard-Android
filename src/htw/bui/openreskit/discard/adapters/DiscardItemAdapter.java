package htw.bui.openreskit.discard.adapters;

import htw.bui.openreskit.discard.R;
import htw.bui.openreskit.domain.discard.DiscardItem;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DiscardItemAdapter extends BaseAdapter {

	private final Activity mContext;
	private final List<DiscardItem> mDiscardItems;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;

	public DiscardItemAdapter(final Activity context, final int rowResID, final List<DiscardItem> discardItems) 
	{
		mContext = context;
		mRowResID = rowResID;
		mDiscardItems = discardItems;
		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() 
	{
		return mDiscardItems.size();
	}

	//returns position in List
	public Object getItem(int position) 
	{
		return mDiscardItems.get(position);
	}

	//returns the Database id of the item
	public long getItemId(int position) {
		return mDiscardItems.get(position).getInternalID();
	}

	// connects Unit members to be displayed with (text)views in a layout
	// per item
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, parent, false);

			DiscardItemViewHolder viewHolder = new DiscardItemViewHolder();
			viewHolder.quantity = (TextView) rowView.findViewById(R.id.inspAttrNumberTV);
			viewHolder.description = (TextView) rowView.findViewById(R.id.inspAttrDescTV);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.discardImage);
			viewHolder.notice = (TextView) rowView.findViewById(R.id.longPressNotice);
			rowView.setTag(viewHolder);
		}

		final DiscardItem discardItem = mDiscardItems.get(position);
		DiscardItemViewHolder holder = (DiscardItemViewHolder) rowView.getTag();
		holder.quantity.setText(String.valueOf(discardItem.getQuantity()));
		holder.description.setText(discardItem.getInspectionAttribute().getDescription());
		if (discardItem.getInspectionAttribute().getDiscardImageSource() != null)
		{  

			if (discardItem.getInspectionAttribute().getDiscardImageSource().getImage() == null) 
			{
				discardItem.getInspectionAttribute().getDiscardImageSource().setImage(Base64.decode(discardItem.getInspectionAttribute().getDiscardImageSource().getBinarySource(),0));	
			}

			Bitmap bMap = BitmapFactory.decodeByteArray(discardItem.getInspectionAttribute().getDiscardImageSource().getImage(), 0, discardItem.getInspectionAttribute().getDiscardImageSource().getImage().length);
			holder.image.setImageBitmap(bMap);
			holder.image.setVisibility(View.VISIBLE);
			if (holder.notice != null) 
			{
				holder.notice.setVisibility(View.GONE);	
			}

		}

		return rowView;
	}

	static class DiscardItemViewHolder 
	{
		public ImageView image;
		public TextView quantity;
		public TextView description;
		public TextView notice;

	}

}



