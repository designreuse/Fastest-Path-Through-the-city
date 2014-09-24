package com.mahefa.mpath;

import com.mahefa.mpath.GraphPath;
import com.mahefa.mpath.Address;
import com.mahefa.mpath.Path;
import com.mahefa.mpath.Direction;
import com.mahefa.mpath.Util;
import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;
import com.mahefa.mpath.GraphPath;

import net.sf.json.*;


public class MapFunc {
	static String baseURL = "http://maps.googleapis.com/maps/api/";
	static String output = "json";
	static String apiKey = "AIzaSyDXDGIwJ3i4pbe3XLCVMhe1BJPKp7Y52Mo";
	static String sensor = "false";
	static String[] markerColors = new String[]{"black", "brown", "green", "purple", "yellow", "blue", "gray", "orange", "red", "white"};
	static String[] labels = new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
	
	public MapFunc(){
		
	}
	
	public static ArrayList<Address> geoCode(String inAddress) throws IOException{
		ArrayList<Parameter> arr_addr = new ArrayList<Parameter>(Arrays.asList((new Parameter("address",inAddress))));
		String service = "geocode";
		Util.debugInf("Searching for address: "+inAddress);
		JSONObject geoResult = applyRequest(service,arr_addr);
		String status = geoResult.getString("status");
		if(!status.equals("OK")) {
			Util.debugAlert("Address not found. Response: "+geoResult.getString("status"));
			return null;
		}
		else{
			Util.debugInf("Found address for: "+inAddress);
		}
		return parseResult(geoResult);
	}
	
	public static ArrayList<Address> parseResult(JSONObject geoResult) throws IOException{
		JSONArray results = geoResult.getJSONArray("results");
		ArrayList<Address> address_list = null;
		address_list = new ArrayList<Address>();
		for(int i=0;i<results.size();i++){
			JSONObject guess = (JSONObject) results.get(i);
			JSONArray address_components = guess.getJSONArray("address_components");
			int street_number = 0;
			String street_name = "<street_name>", city = "<city>", country = "<country>", zip_code = "<zip_code>";
			for(int j=0;j<address_components.size();j++){
				JSONObject components = address_components.getJSONObject(j);
				try{
					String types = components.getJSONArray("types").getString(0);
					String value = components.getString("long_name");
					if(types.equals("street_number")) street_number = Integer.valueOf(value);
					else if(types.equals("route")) street_name = value;
					else if(types.equals("locality")) city = value;
					else if(types.equals("country")) country = value;
					else if(types.equals("postal_code")) zip_code = value;
				} catch (Exception e){
					Util.debugAlert("Some error in address component response ...");
				}
			}
			Util.debugInf("Formatting address.");
			String formatted_address = guess.getString("formatted_address");
			JSONObject location = guess.getJSONObject("geometry").getJSONObject("location");
			double lng = Double.valueOf(location.getString("lng"));
			double lat = Double.valueOf(location.getString("lat"));
			address_list.add(new Address(street_number, street_name, city, country, zip_code, formatted_address, lng, lat));
			Util.debugInf("Address formated");
		}
		return address_list;
		
	}
	
	public static Address getAddress(String addr) throws IOException{
		// assuming that addr is already exact
		return MapFunc.geoCode(addr).get(0);
	}
	
	public static Direction getDirection(Address start_address, Address end_address){
		Direction directionResult = null;
		ArrayList<Parameter> arr_addr = null;
		Util.debugInf("Getting direction from "+start_address.toString()+" to "+end_address.toString());
		try {
			arr_addr = new ArrayList<Parameter>(Arrays.asList(
				new Parameter("origin",start_address.toString()),
				new Parameter("destination",end_address.toString())
			));
		} catch(Exception e){
			e.printStackTrace();
		}
		String service = "directions";
		JSONObject dirResult = null;
		try{
			dirResult = applyRequest(service,arr_addr);
		} catch(Exception e){
			JOptionPane.showMessageDialog(null, "It seems that your internet is down.\n"+
				"Please, make sure you are connected to the internet.", "Internet down", JOptionPane.WARNING_MESSAGE);
			Util.debugAlert("Cannot get direction from "+start_address.toString()+" to "+end_address.toString());
		}
		String status = null;
		try{
			status = dirResult.getString("status");
		} catch(JSONException e){
			return null;
		}
		
		if(!status.equals("OK")) {
			Util.debugAlert("Cannot get direction from "+start_address.toString()+" to "+end_address.toString()+" not found. Response: "+dirResult.getString("status"));
			return null;
		}
		else{
			Util.debugInf("Successfully found direction from "+start_address.toString()+" to "+end_address.toString());
		}
		JSONObject legs = (JSONObject) ((JSONObject)dirResult.getJSONArray("routes").get(0)).getJSONArray("legs").get(0);
		long distance = ((JSONObject)legs.get("distance")).getLong("value");
		long time = ((JSONObject)legs.get("duration")).getLong("value");
		double start_lat = ((JSONObject)legs.get("start_location")).getDouble("lat");
		double start_lng = ((JSONObject)legs.get("start_location")).getDouble("lng");
		double end_lat = ((JSONObject)legs.get("end_location")).getDouble("lat");
		double end_lng = ((JSONObject)legs.get("end_location")).getDouble("lng");
		
		directionResult = new Direction(distance,time,start_address.toString(),end_address.toString(),"DRIVING",start_lat,start_lng,end_lat,end_lng);
		JSONArray steps = legs.getJSONArray("steps");
		for(int i=0;i<steps.size();i++){
			JSONObject dirStep = (JSONObject)steps.get(i);
			long d = dirStep.getJSONObject("distance").getLong("value");
			long t = dirStep.getJSONObject("duration").getLong("value");
			double latA = dirStep.getJSONObject("start_location").getDouble("lat");
			double lngA = dirStep.getJSONObject("start_location").getDouble("lng");
			double latB = dirStep.getJSONObject("end_location").getDouble("lat");
			double lngB = dirStep.getJSONObject("end_location").getDouble("lng");
			directionResult.addStep(new Direction(d,t,latA,lngA,latB,lngB));
		}
		return directionResult;
	}
	
	public static JSONObject applyRequest(String service, ArrayList<Parameter> params){
		String urlString = baseURL+service+"/"+output+"?";
		String paramString = "";
		StringBuilder builder = null;
		for(Parameter p : params)
			paramString += p.toString()+"&";
		paramString += "sensor="+sensor;
		urlString += paramString;
		Util.debugInf("Retrieving data from GoogleMap");
		try {
			URL url = new URL(urlString);
			System.out.println("Requesting : "+urlString);
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(2000);
			String line;
			builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
			System.out.println("done");
		} catch (Exception e){
			JOptionPane.showMessageDialog(null, "It seems that your internet is down.\n"+
				"Please, make sure you are connected to the internet.", "Internet down", JOptionPane.WARNING_MESSAGE);
		}
		Util.debugInf("Data retrieved, now parsing them.");
		return JSONObject.fromObject(builder.toString());
	}
	
	public static String formUrlForPath(Path p, GraphPath gp, int zoom, int xSize, int ySize, String maptype, double dLat, double dLng, boolean zoomed){
		Direction[][] dir = gp.getDirectionMatrix();
		String output = "staticmap";
		String color = "0xff0000ff";
		String weight = "3";
		String urlString = baseURL+output+"?";
		String addresses = "";
		int nMarkers = 1, nLabel = 1;
		String markers = "markers=color:black|label:"+labels[0]+"|"+gp.get(0).lat+","+gp.get(0).lng;
		double maxLat,maxLng,minLat,minLng;
		int[] pInt = p.toArray();
		minLat = maxLat = gp.get(0).lat;
		minLng = maxLng = gp.get(0).lng;
		for(int i=1;i<pInt.length;i++){
			nMarkers = i%markerColors.length;
			nLabel = i%labels.length;
			addresses += "|"+dir[pInt[i-1]][pInt[i]].formUrlParameter();
			double[] minmax = dir[pInt[i-1]][pInt[i]].getMinMax();
			if(minmax[0]<minLat) minLat = minmax[0];
			if(minmax[1]<minLng) minLng = minmax[1];
			if(minmax[2]>maxLat) maxLat = minmax[2];
			if(minmax[3]>maxLng) maxLng = minmax[3];
			markers += "&markers=color:"+markerColors[nMarkers]+"|label:"+labels[nLabel]+"|"+gp.get(i).lat+","+gp.get(i).lng;
		}
		Util.debugInf("Ready to request map image from Google");
		urlString += "center="+Double.toString((minLat+maxLat)/2+dLat)+","+Double.toString((minLng+maxLng)/2+dLng)+""
			+ (zoomed?"&zoom="+Integer.toString(zoom):"")+"&size="+xSize+"x"+ySize+"&"+
			"maptype="+maptype+"&"+markers+"&path=color:"+color+"|weight:"+weight+addresses+"&key="+apiKey+"&scale=2&sensor=false";
		//urlString += "center="+Double.toString((minLat+maxLat)/2+dLat)+","+Double.toString((minLng+maxLng)/2+dLng)+"&size="+xSize+"x"+ySize+"&"+
		//		"maptype="+maptype+"&"+markers+"&path=color:"+color+"|weight:"+weight+addresses+"&sensor=false";
		//System.out.println("Map URL = "+urlString);
		return urlString;
	}
}
