package htw.bui.openreskit.discard;

import htw.bui.openreskit.domain.discard.DiscardItem;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.odata.DiscardRepository;

import com.google.inject.Inject;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class DiscardItemInfoActivity extends RoboActivity 
{

	@Inject
	DiscardRepository mRepository;

	@InjectView
	(R.id.discardItemInfoImage) ImageView mDiscardItemInfoImage;

	@InjectView
	(R.id.noImageText) TextView mDiscardItemNoImageText;

	@InjectView
	(R.id.discardItemInfoText) TextView mDiscardItemInfoText;

	private int mInspectionId;
	private long mDiscardItemId;
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.discarditem_info);

		Intent launchingIntent = getIntent();
		mInspectionId = launchingIntent.getExtras().getInt("InspectionId");
		mDiscardItemId = launchingIntent.getExtras().getLong("DiscardItemId");

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);

		getInfo();

	}

	private void getInfo() {
		Inspection inspection = mRepository.getInspectionById(mInspectionId);
		DiscardItem discardItem = null;
		for (DiscardItem di : inspection.getDiscardItems()) 
		{
			if (di.getInternalID() == mDiscardItemId) 
			{
				discardItem = di;
			}
		}
		if (discardItem.getInspectionAttribute().getDiscardImageSource() != null) 
		{
			if (discardItem.getInspectionAttribute().getDiscardImageSource().getImage() == null) 
			{
				discardItem.getInspectionAttribute().getDiscardImageSource().setImage(Base64.decode(discardItem.getInspectionAttribute().getDiscardImageSource().getBinarySource(),0));	
			}
			Bitmap bMap = BitmapFactory.decodeByteArray(discardItem.getInspectionAttribute().getDiscardImageSource().getImage(), 0, discardItem.getInspectionAttribute().getDiscardImageSource().getImage().length);
			mDiscardItemInfoImage.setImageBitmap(bMap);	
			mDiscardItemNoImageText.setVisibility(View.GONE);
		}
		if (discardItem.getDescription() != null)
		{
			mDiscardItemInfoText.setText(discardItem.getDescription());
		}
		else
		{
			mDiscardItemInfoText.setText("keine Beschreibung vorhanden");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.discarditem_info_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.closeInfo:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
