package htw.bui.openreskit.discard.adapters;

import htw.bui.openreskit.discard.R;
import htw.bui.openreskit.domain.discard.ProductionItem;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class ProductionItemAdapter extends BaseAdapter implements Filterable {

	private List<ProductionItem> mProductionItems;
	private List<ProductionItem> mFilteredProductionItems;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;

	public ProductionItemAdapter(Activity context, int rowResID, LayoutInflater layoutInflater, List<ProductionItem> productionItems) 
	{
		mFilteredProductionItems = productionItems;
		mProductionItems = productionItems;
		mRowResID = rowResID;
		mLayoutInflater = layoutInflater;
	}

	@Override
	public int getCount() 
	{
		return mFilteredProductionItems.size();
	}

	@Override
	public Object getItem(int position) 
	{
		return mFilteredProductionItems.get(position);
	}

	@Override
	public long getItemId(int position) 
	{
		return mFilteredProductionItems.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);
			ProductionItemViewHolder viewHolder = new ProductionItemViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.textView1);
			viewHolder.customer = (TextView) rowView.findViewById(R.id.textView2);
			rowView.setTag(viewHolder);
		}

		ProductionItem pi = mFilteredProductionItems.get(position);
		ProductionItemViewHolder holder = (ProductionItemViewHolder) rowView.getTag();
		holder.name.setText(pi.getItemName() + ", " + pi.getItemNumber());	
		holder.customer.setText(pi.getCustomer().getName());
				
		return rowView;
	}

	static class ProductionItemViewHolder 
	{
		public TextView name;
		public TextView customer;
	}


	@SuppressLint("DefaultLocale")
	@Override
	public Filter getFilter() 
	{
		Filter myFilter = new Filter() 
		{

			@Override
			protected FilterResults performFiltering(CharSequence constraint) 
			{
				String searchText = String.valueOf(constraint);

				if (mProductionItems == null) 
				{
					mProductionItems = new ArrayList<ProductionItem>(mFilteredProductionItems);
				}
				List<ProductionItem> result;
				FilterResults filterResults = new FilterResults();

				if(constraint == null || constraint.length() <= 0)
				{
					result = new ArrayList<ProductionItem>(mProductionItems);
				}
				else
				{
					result = new ArrayList<ProductionItem>();
					for (ProductionItem pi : mProductionItems)
					{
						if (pi.getItemName().toLowerCase().contains(searchText.toLowerCase()) || pi.getItemNumber().toLowerCase().contains(searchText.toLowerCase()) || pi.getItemDescription().toLowerCase().contains(searchText.toLowerCase()))
						{
							result.add(pi);
						}
					}
					// Now assign the values and count to the FilterResults object
				}
				filterResults.values = result;
				filterResults.count = result.size();
				return filterResults;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence contraint, FilterResults results) 
			{
				mFilteredProductionItems = (ArrayList<ProductionItem>)results.values;
				if (results != null && results.count > 0) 
				{

					//mFilteredProductionItems.clear();

					notifyDataSetChanged();
				}
				else 
				{
					notifyDataSetInvalidated();
				}
			}
		};
		return myFilter;
	}
}
