package htw.bui.openreskit.odata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import htw.bui.openreskit.domain.discard.DiscardImageSource;
import htw.bui.openreskit.domain.discard.DiscardItem;
import htw.bui.openreskit.domain.discard.Inspection;
import htw.bui.openreskit.domain.discard.InspectionAttribute;
import htw.bui.openreskit.domain.discard.ProductionItem;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import org.json.JSONArray;
import org.json.JSONObject;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DiscardRepository
{
	private Activity mContext;
	public List<Inspection> mInspections;
	public List<ProductionItem> mProductionItems;
	public List<InspectionAttribute> mInspectionAttributes;	
	public List<ResponsibleSubject> mResponsibleSubjects;	

	private ProgressDialog mProgressDialog;
	private List<RepositoryChangedListener> mListeners = new ArrayList<RepositoryChangedListener>();
	private static ObjectMapper objectMapper;


	@Inject
	public DiscardRepository(Activity ctx)
	{
		objectMapper = new ObjectMapper();
		mContext = ctx;
		mInspections = getInspectionsFromFile();
		mProductionItems = getProductionItemsFromFile();
		mInspectionAttributes = getInspectionAttributes();
		mResponsibleSubjects = getResponsibleSubjectsFromFile();
	}

	private List<InspectionAttribute> getInspectionAttributes() {
		List<InspectionAttribute> disctinctInspectionAttributes = new ArrayList<InspectionAttribute>();
		for (ProductionItem pi : mProductionItems) 
		{
			disctinctInspectionAttributes.addAll(pi.getInspectionAttributes());
		}
		return disctinctInspectionAttributes;
	}

	public long getMaxInspectionId() 
	{
		long maxId = 0;
		for (Inspection i : mInspections) 
		{
			if (i.getInternalId() > maxId) 
			{
				maxId = i.getInternalId(); 
			}
		}
		return maxId;
	}

	public Inspection getInspectionById(long inspectionId) 
	{
		for (Inspection i : mInspections) 
		{
			if (i.getInternalId() == inspectionId) {
				return i;
			}
		}
		return null;
	}

	public InspectionAttribute getInspectionAttributeById(long inspectionAttributeId) 
	{
		for (InspectionAttribute ia : mInspectionAttributes) 
		{
			if (ia.getId() == inspectionAttributeId) {
				return ia;
			}
		}
		return null;
	}

	public void clearDiscardItems(int inspectionInternalId) 
	{
		Inspection insp = getInspectionById(inspectionInternalId);
		if (insp.getDiscardItems()!= null) 
		{
			for (DiscardItem di : insp.getDiscardItems())
			{
				if (di.getQuantity() == 0) 
				{
					insp.getDiscardItems().remove(di);
				}
			}
		}
	}

	public ProductionItem getProductionItemById(long productionItemId) 
	{
		for (ProductionItem pi : mProductionItems) 
		{
			if (pi.getId() == productionItemId) {
				return pi;
			}
		}
		return null;
	}

	public ResponsibleSubject getResponsibleSubjectById(long responsibleSubjectId) 
	{
		for (ResponsibleSubject rs : mResponsibleSubjects) 
		{
			if (rs.getId() == responsibleSubjectId) {
				return rs;
			}
		}
		return null;
	}

	public synchronized void addEventListener(RepositoryChangedListener listener)  
	{
		mListeners.add(listener);
	}

	public synchronized void removeEventListener(RepositoryChangedListener listener)   
	{
		mListeners.remove(listener);
	}

	private synchronized void fireRepositoryUpdate() 
	{
		RepositoryChangedEvent event = new RepositoryChangedEvent(this);
		Iterator<RepositoryChangedListener> i = mListeners.iterator();
		while(i.hasNext())  
		{
			((RepositoryChangedListener) i.next()).handleRepositoryChange(event);
		}
	}

	private List<Inspection> getInspectionsFromFile() {

		ArrayList<Inspection> inspections = new ArrayList<Inspection>();
		String inspectionsJSON = loadFromExternal("inspections.json");

		if (inspectionsJSON != null) 
		{
			try {
				JSONArray inspectionsJSONArray = new JSONArray(inspectionsJSON);

				for (int i = 0; i < inspectionsJSONArray.length(); i++) 
				{
					JSONObject inspJSON = inspectionsJSONArray.getJSONObject(i);
					Inspection insp = objectMapper.readValue(inspJSON.toString(), Inspection.class);
					insp.setInternalId(insp.getId());
					inspections.add(insp);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return inspections;
	}

	private List<ProductionItem> getProductionItemsFromFile() {

		ArrayList<ProductionItem> productionItems = new ArrayList<ProductionItem>();
		String productionItemJSON = loadFromExternal("productionItems.json");

		if (productionItemJSON != null) 
		{
			try {
				JSONArray productionItemsJSONArray = new JSONArray(productionItemJSON);

				for (int i = 0; i < productionItemsJSONArray.length(); i++) 
				{
					JSONObject prodItemJSON = productionItemsJSONArray.getJSONObject(i);
					ProductionItem pi = objectMapper.readValue(prodItemJSON.toString(), ProductionItem.class);
					productionItems.add(pi);

				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return productionItems;
	}

	private List<ResponsibleSubject> getResponsibleSubjectsFromFile() {

		ArrayList<ResponsibleSubject> responsibleSubjects = new ArrayList<ResponsibleSubject>();
		String responsibleSubjectsJSON = loadFromExternal("responsibleSubjects.json");

		if (responsibleSubjectsJSON != null) 
		{
			try {
				JSONArray responsibleSubjectsJSONArray = new JSONArray(responsibleSubjectsJSON);

				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = objectMapper.readValue(responsibleSubjectJSON.toString(), ResponsibleSubject.class);
					responsibleSubjects.add(rs);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return responsibleSubjects;
	}

	private void saveInspectionsToFile(List<Inspection> inspections) 
	{
		saveToExternal(serializeInspections(inspections), "inspections.json");
	}

	private void saveProductionItemsToFile(List<ProductionItem> productionItems) 
	{
		saveToExternal(serializeProductionItems(productionItems), "productionItems.json");
	}

	private void saveResponsibleSubjectToFile(List<ResponsibleSubject> responsibleSubjects) 
	{
		saveToExternal(serializeResponsibleSubjects(responsibleSubjects), "responsibleSubjects.json");
	}

	public void persistLocalInspections() 
	{
		saveInspectionsToFile(mInspections);
	}
	
	private void persistLocalData() 
	{
		saveInspectionsToFile(mInspections);
		saveProductionItemsToFile(mProductionItems);
		saveResponsibleSubjectToFile(mResponsibleSubjects);
	}
	
	public void getDataFromOdataService(Activity _start)
	{
		if (isOnline())
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");
			if (defaultIP == "none" || username == "none" || password == "none") 
			{
				Toast.makeText(mContext, "Bitte geben sie in den Einstellungen zuerst die Verbingungsparamenter an", Toast.LENGTH_SHORT).show();
			}
			else
			{
				new GetData().execute((Void[]) null);	
			}
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}

	}

	public void writeDataToOdataService(Activity _start)
	{
		if (isOnline())
		{
			new WriteData().execute((Void[]) null);
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}

	}
	private boolean isOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting())
		{
			return true;
		}
		return false;
	}

	private class GetData extends AsyncTask<Void, Void, Integer>
	{


		protected Integer doInBackground(Void... params)
		{
			mProductionItems = new ArrayList<ProductionItem>();
			mResponsibleSubjects = new ArrayList<ResponsibleSubject>();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");

			int counter = 0;
			try 
			{
				//Inspections
				String expandInspections = "ResponsibleSubject,ProductionItem/InspectionAttributes/DiscardImageSource,ProductionItem/Customer,DiscardItems/InspectionAttribute/DiscardImageSource";
				JSONArray inspectionsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "Inspections", expandInspections, null);

				for (int i = 0; i < inspectionsJSONArray.length(); i++) 
				{
					JSONObject inspectionsJSON = inspectionsJSONArray.getJSONObject(i);
					Inspection insp = objectMapper.readValue(inspectionsJSON.toString(), Inspection.class);
					insp.setInternalId(insp.getId());
					mInspections.add(insp);
					counter++;
				}
			} 
			catch (final Exception e) 
			{
				mContext.runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

					}
				});
				e.printStackTrace();
			}

			try 
			{
				//ProductionItems
				String expandProductionItems = "InspectionAttributes,Customer";
				JSONArray productionItemsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "ProductionItems", expandProductionItems, null);

				for (int i = 0; i < productionItemsJSONArray.length(); i++) 
				{
					JSONObject productionItemsJSON = productionItemsJSONArray.getJSONObject(i);
					ProductionItem pi = objectMapper.readValue(productionItemsJSON.toString(), ProductionItem.class);
					mProductionItems.add(pi);


				}
			} 
			catch (final Exception e) 
			{
				mContext.runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

					}
				});
				e.printStackTrace();
			}

			//ResponsibleSubjects

			try 
			{
				String expandResponsibleSubjects = "OpenResKit.DomainModel.Employee/Groups";
				JSONArray responsibleSubjectsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "ResponsibleSubjects", expandResponsibleSubjects, null);

				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectsJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = objectMapper.readValue(responsibleSubjectsJSON.toString(), ResponsibleSubject.class);
					mResponsibleSubjects.add(rs);

				}
			} 
			catch ( Exception e) 
			{

				e.printStackTrace();
			}
			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Aktualisiere Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			persistLocalData();
			mInspectionAttributes = getInspectionAttributes();
			mProgressDialog.dismiss();
			Toast.makeText(mContext, "Es wurden " + result + " Datensätze vom Server geladen.", Toast.LENGTH_SHORT).show();
			fireRepositoryUpdate();

		}
	}

	private class WriteData extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... params)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");

			for (InspectionAttribute ia : mInspectionAttributes) 
			{
				if (ia.isManipulated()) 
				{
					try {
						writeInspectionAttributeImageToOdata(defaultIP, port, username, password, "OpenResKitHub", "InspectionAttributes", ia);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			int counter = 0;
			for (Inspection insp : mInspections) 
			{
				if (insp.isManipulated()) 
				{
					try 
					{
						writeChangesInInspectionToOdata(defaultIP, port, username, password, "OpenResKitHub", "Inspections", insp);
					} 
					catch (final Exception e) 
					{
						mContext.runOnUiThread(new Runnable() 
						{
							public void run() 
							{
								Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
							}
						});
						e.printStackTrace();
					}
					insp.setManipulated(false);
					counter++;
				}
			}
			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Schreibe Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			mProgressDialog.dismiss();
			Toast.makeText(mContext, "Es wurden " + result + " Ablesungen zum Server übermittelt!", Toast.LENGTH_SHORT).show();

		}
	}

	private static String serializeInspections(List<Inspection> inspections) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<Inspection>>(){}).writeValueAsString(inspections);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}


	private static String serializeProductionItems(List<ProductionItem> productionItems) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<ProductionItem>>(){}).writeValueAsString(productionItems);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private static String serializeResponsibleSubjects(List<ResponsibleSubject> responsibleSubjects) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<ResponsibleSubject>>(){}).writeValueAsString(responsibleSubjects);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private JSONArray getJSONArrayFromOdata(String ip, String port, String username, String password, String endpoint, String collection, String expand, String filter) throws Exception
	{
		JSONArray returnJSONArray = null;
		String jsonText = null;
		String uriString = null;
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			if (filter == null) {
				uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand;
			} 
			else
			{
				uriString = "http://"+ ip +":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand + "&$filter="+ filter;
			}
			HttpGet request = new HttpGet(uriString);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
			HttpClient httpClient = new DefaultHttpClient(httpParams);

			HttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream instream = entity.getContent();
					jsonText = convertStreamToString(instream);
					instream.close();
				}
				returnJSONArray  = new JSONObject(jsonText).getJSONArray("value");
			}
			else if (response.getStatusLine().getStatusCode() == 403) 
			{
				Exception e1 = new AuthenticationException("Der Benutzername oder das Passwort für die Authentifizierung am OData Service sind nicht korrekt");
				throw e1; 
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		return returnJSONArray;
	}

	public void writeInspectionAttributeImageToOdata(String defaultIP, String port, String username, String password, String endpoint, String collection, InspectionAttribute ia) throws Exception
	{

		HttpResponse response;
		HttpParams httpParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		httpParams.setBooleanParameter("http.protocol.expect-continue", false);
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);


		DiscardImageSource newDis = new DiscardImageSource();
		ObjectMapper mapper = new ObjectMapper();
		if (ia.getDiscardImageSource() != null) 
		{
			try 
			{
				JSONObject imageJO = new JSONObject();
				imageJO.put("odata.type", "OpenResKit.DomainModel.DiscardImageSource");
				imageJO.put("BinarySource", ia.getDiscardImageSource().getBinarySource());
				StringEntity stringEntity = new StringEntity(imageJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + defaultIP + ":" + port + "/" + endpoint + "/DiscardImageSources");
				request.setHeader("X-HTTP-Method-Override", "PUT");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();


				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
					newDis = mapper.readValue(answer.toString(), DiscardImageSource.class);
					ia.getDiscardImageSource().setId(newDis.getId());
				}
			}
			catch (Exception e)
			{
				throw e;
			}

			try 
			{

				JSONObject measureJO = new JSONObject();
				measureJO.put("odata.type", "OpenResKit.DomainModel.InspectionAttribute");
				measureJO.put("Id", ia.getId());
				if (newDis != null) 
				{

					JSONObject imageSourceNavProp = new JSONObject();
					imageSourceNavProp.put("uri", "http://" + defaultIP + ":" + port + "/" + endpoint + "/DiscardImageSources("+ newDis.getId() +")");
					JSONObject imageSourceNavPropMetadata = new JSONObject();
					imageSourceNavPropMetadata.put("__metadata", imageSourceNavProp);
					measureJO.put("DiscardImageSource", imageSourceNavPropMetadata);
				}

				StringEntity stringEntity = new StringEntity(measureJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + defaultIP + ":" + port + "/" + endpoint + "/" + collection + "("+ ia.getId()+ ")");
				request.setHeader("X-HTTP-Method", "MERGE");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();

				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
				}
				ia.setManipulated(false);
			}
			catch (Exception e)
			{
				throw e;
			}
		}
	}

	private static void writeChangesInInspectionToOdata(String ip, String port, String username, String password, String endpoint, String collection, Inspection inspection) throws Exception 
	{
		try 
		{
			HttpResponse response;
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

			final DateFormat dfs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY);
			ObjectMapper mapper = new ObjectMapper();
			HttpPost discardItemRequest = null;

			List<Integer> discardItemIds = new ArrayList<Integer>();

			// create or update discardItems and generate list of ids
			if (inspection.getDiscardItems() != null) 
			{
				for (DiscardItem di : inspection.getDiscardItems()) 
				{
					//create new DiscardItem
					if(di.getId() == 0) 
					{
						discardItemRequest = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + "DiscardItems");
						discardItemRequest.setHeader("X-HTTP-Method-Override", "PUT");
						discardItemRequest.setHeader("Accept", "application/json");
						discardItemRequest.setHeader("Content-type", "application/json;odata=verbose");
						discardItemRequest.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));

						String discardItemPayloadDateJSON = "{\"odata.type\":\"OpenResKit.DomainModel.DiscardItem\",";
						discardItemPayloadDateJSON += "\"InspectionAttribute\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/InspectionAttributes("+ di.getInspectionAttribute().getId() +")\"}},";
						discardItemPayloadDateJSON += "\"Description\":\"" +di.getDescription()+ "\",";
						discardItemPayloadDateJSON += "\"Quantity\":\""+ di.getQuantity() +"\"";
						discardItemPayloadDateJSON += "}";

						discardItemRequest.setEntity(new StringEntity(discardItemPayloadDateJSON,HTTP.UTF_8));

						response = httpClient.execute(discardItemRequest);
						HttpEntity discardItemResponseEntity = response.getEntity();
						DiscardItem newDiscardItem = null;

						if(discardItemResponseEntity != null) 
						{
							String jsonText = EntityUtils.toString(discardItemResponseEntity, HTTP.UTF_8);
							JSONObject answer = new JSONObject(jsonText);
							System.out.print(answer);
							newDiscardItem = mapper.readValue(answer.toString(), DiscardItem.class);	    	
							di.setId(newDiscardItem.getId());
							discardItemIds.add(newDiscardItem.getId());
						}

					}
					//update existing DiscardItem
					else
					{
						discardItemRequest = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + "DiscardItems(" + di.getId() +")");
						discardItemRequest.setHeader("X-HTTP-Method", "MERGE");
						discardItemRequest.setHeader("Accept", "application/json");
						discardItemRequest.setHeader("Content-type", "application/json;odata=verbose");
						discardItemRequest.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));

						String discardItemPayloadDateJSON = "{\"odata.type\":\"OpenResKit.DomainModel.DiscardItem\",";
						discardItemPayloadDateJSON += "\"Id\":"+ di.getId()+",";
						discardItemPayloadDateJSON += "\"InspectionAttribute\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/InspectionAttributes("+ di.getInspectionAttribute().getId() +")\"}},";
						discardItemPayloadDateJSON += "\"Description\":\"" +di.getDescription()+ "\",";
						discardItemPayloadDateJSON += "\"Quantity\":\""+ di.getQuantity() +"\"";
						discardItemPayloadDateJSON += "}";

						discardItemRequest.setEntity(new StringEntity(discardItemPayloadDateJSON,HTTP.UTF_8));
						response = httpClient.execute(discardItemRequest);
						HttpEntity dateResponseEntity = response.getEntity();
						discardItemIds.add(di.getId());

						//debug
						if(dateResponseEntity != null) 
						{
							String jsonText = EntityUtils.toString(dateResponseEntity, HTTP.UTF_8);
							JSONObject answer = new JSONObject(jsonText);
							System.out.print(answer);
						}
					}
				}
			}
			String discardItemIdJSONPayload = "[";
			int counter = 1;
			for (Integer diId: discardItemIds) 
			{
				discardItemIdJSONPayload += "{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/DiscardItems("+ diId +")\"}}";
				if (counter < discardItemIds.size()) 
				{
					discardItemIdJSONPayload += ",";
				}
				counter++;
			}

			discardItemIdJSONPayload += "]";

			// inspection added on device
			if (inspection.getId() == 0) 
			{
				String payloadJSON = "{\"odata.type\":\"OpenResKit.DomainModel.Inspection\",";
				payloadJSON += "\"ResponsibleSubject\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/ResponsibleSubjects("+ inspection.getResponsibleSubject().getId() +")\"}},";
				payloadJSON += "\"ProductionItem\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/ProductionItems("+ inspection.getProductionItem().getId() +")\"}},";
				payloadJSON += "\"Name\":\"" +inspection.getName()+ "\",";
				if (inspection.getInspectionDate() != null) 
				{
					payloadJSON += "\"InspectionDate\":\"" + dfs.format(inspection.getInspectionDate()) + "\",";
				}
				payloadJSON += "\"ProductionDate\":\"" + dfs.format(inspection.getProductionDate()) + "\",";
				payloadJSON += "\"ProductionShift\":" + inspection.getProductionShift() + ",";
				payloadJSON += "\"InspectionShift\":" + inspection.getInspectionShift() + ",";
				payloadJSON += "\"InspectionType\":" + inspection.getInspectionType() + ",";
				payloadJSON += "\"SampleSize\":" + inspection.getInspectionFrequencyOrSampleSize() + ",";
				payloadJSON += "\"Unit\":\"" +inspection.getTotalAmountUnit()+ "\",";
				payloadJSON += "\"TotalAmount\":" + inspection.getTotalAmount() + ",";
				payloadJSON += "\"Description\":\"" +inspection.getDescription()+ "\",";
				payloadJSON += "\"Finished\":" + inspection.isFinished() + ",";
				payloadJSON += "\"DiscardItems\":"+ discardItemIdJSONPayload +"}";
				

				StringEntity stringEntity = new StringEntity(payloadJSON,HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection);
				request.setHeader("X-HTTP-Method-Override", "PUT");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				Inspection newInspection = new Inspection();
				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();
				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
					newInspection = mapper.readValue(answer.toString(), Inspection.class);	    	
					inspection.setId(newInspection.getId());
				}
			}
			//inspection from HUB
			else
			{
				String payloadJSON = "{\"odata.type\":\"OpenResKit.DomainModel.Inspection\"," +
						"\"Id\":"+ inspection.getId()+"," +
						"\"ResponsibleSubject\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/ResponsibleSubjects("+ inspection.getResponsibleSubject().getId() +")\"}},"+ 
						"\"InspectionDate\":\"" + dfs.format(inspection.getInspectionDate()) + "\"," + 
						"\"Finished\":" + inspection.isFinished() + "," +
						"\"DiscardItems\":"+ discardItemIdJSONPayload +"}";

				StringEntity stringEntity = new StringEntity(payloadJSON,HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection +"("+ inspection.getId()+")");
				request.setHeader("X-HTTP-Method", "MERGE");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();
				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
				}
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void deleteLocalData()
	{
		mInspections = new ArrayList<Inspection>();
		saveInspectionsToFile(mInspections);
		mProductionItems = new ArrayList<ProductionItem>();
		saveProductionItemsToFile(mProductionItems);
		mResponsibleSubjects = new ArrayList<ResponsibleSubject>();
		saveResponsibleSubjectToFile(mResponsibleSubjects);
		fireRepositoryUpdate();
	}

	private void saveToExternal(String content, String fileName) {
		FileOutputStream fos = null;
		Writer out = null;
		try {
			File file = new File(getAppRootDir(), fileName);
			fos = new FileOutputStream(file);
			out = new OutputStreamWriter(fos, "UTF-8");

			out.write(content);
			out.flush();
		} catch (Throwable e){
			e.printStackTrace();
		} finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException ignored) {}
			}
			if(out!= null){
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}

	private String loadFromExternal(String fileName) {
		String res = null;
		File file = new File(getAppRootDir(), fileName);
		if(!file.exists()){
			Log.e("", "file " +file.getAbsolutePath()+ " not found");
			return null;
		}
		FileInputStream fis = null;
		BufferedReader inputReader = null;
		try {
			fis = new FileInputStream(file);
			inputReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			StringBuilder strBuilder = new StringBuilder();
			String line;
			while ((line = inputReader.readLine()) != null) {
				strBuilder.append(line + "\n");
			}
			res = strBuilder.toString();
		} catch(Throwable e){
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException ignored) {}
			}
			if(inputReader!= null){
				try {
					inputReader.close();
				} catch (IOException ignored) {}
			}
		}
		return res;
	}

	public File getAppRootDir() {
		File appRootDir;
		boolean externalStorageAvailable;
		boolean externalStorageWriteable;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			externalStorageAvailable = externalStorageWriteable = false;
		}
		if (externalStorageAvailable && externalStorageWriteable) {

			appRootDir = mContext.getExternalFilesDir(null);
		} else {
			appRootDir = mContext.getDir("appRootDir", Context.MODE_PRIVATE);
		}
		if (!appRootDir.exists()) {
			appRootDir.mkdir();
		}
		return appRootDir;
	}

	private static String convertStreamToString(InputStream is) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
