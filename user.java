package com.hsyco;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

public class user extends userBase {
	
	private static List<String> sessionList = new ArrayList<String>();
	private static List<String> tableData = new ArrayList<String>();	

	public static void StartupEvent() {
		String tableDataString = varGet("$TABLEDATA!");
		System.out.println(tableDataString);

		if (!tableDataString.equals("")) {
			String dataSplit[] = tableDataString.split("],");
			for (String data : dataSplit) {
				String lines = data.replace("[", "").replace("]", "").replace("On", "Off").trim();
				tableData.add("[" + lines + "]");
			}
		}
		for (String session : sessionList) {
			uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
		}
	}

	
	public static String userCommand(String session, String userid, String name, String param) {
		System.out.println(name + " | " + param);
		try {
			String jsonString = param;
			JSONObject obj = new JSONObject(jsonString);
			String action = obj.getString("action");
			
			if (action.equals("insert")) {
				JSONObject objData = obj.getJSONObject("data");
				String lightName = objData.getString("0");
				String lightStatus = objData.getString("_1");
				String lightUpTime = objData.getString("_2");
				String datapoint = objData.getString("3");
				
				String packet = "[\"" + lightName + "\",\"" + lightStatus + "\",\"" + lightUpTime + "\",\"dpt:" + datapoint +"\"]";
				
				tableData.add(packet);
				
				System.out.println(tableData.toString());
				uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLEDATA!", tableData.toString());
				
			} else if (action.equals("update")) {
				JSONObject objData = obj.getJSONObject("data");
				String lightName = objData.getString("0");
				String lightStatus = objData.getString("_1");
				String lightUpTime = objData.getString("_2");
				String datapoint = objData.getString("3");
	
				int key = Integer.valueOf(obj.getString("key"));
	
				String packet = "[\"" + lightName + "\",\"" + lightStatus + "\",\"" + lightUpTime + "\",\"dpt:" + datapoint +"\"]";
				tableData.remove(key);
				tableData.add(key, packet);
				uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLEDATA!", tableData.toString());
	
			} else if (action.equals("delete")) {
				int key = Integer.valueOf(obj.getString("key"));
				tableData.remove(key);
				uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLEDATA!", tableData.toString());
	
			} else if (action.equals("move")) {
				int key = Integer.valueOf(obj.getString("key"));
				String packet = tableData.get(key);
				tableData.remove(key);
	
				String before = "";
				String after = "";
				try {
					before = obj.getString("before");
				} catch (Exception ignored) { }
				try {
					after = obj.getString("after");
				} catch (Exception ignored) { }
				
				if (before.length() != 0) {
					tableData.add(Integer.valueOf(before), packet);
				} else {
					tableData.add(Integer.valueOf(after), packet);
				}
				uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLEDATA!", tableData.toString());
	
			}
			
			String tableDataString = varGet("$TABLEDATA!");
			System.out.println(tableDataString);
		} catch (Exception ignored) { }
		
		//when "on/off" button is pressed
		if (name.equals("table.light")) {
			
			int key = Integer.valueOf(param);
			String packet = tableData.get(key);
			
			String datapoint = packet.split("dpt:")[1].replace("]", "").replace("\"", "").trim();
			
			if (packet.contains("On") && (ioGet(datapoint) != null && ioGet(datapoint).equals("1"))) {
				ioSet(datapoint, "0" );
				packet = packet.replace("On", "Off");
			}else if (packet.contains("Off") && ((ioGet(datapoint) != null && ioGet(datapoint).equals("0")))){
				ioSet(datapoint, "1" );
				packet = packet.replace("Off", "On");
			}
			
			tableData.remove(key);
			tableData.add(key, packet);
			
			
			uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
			varSet("$TABLEDATA!", tableData.toString());
			
		} 
		return "";
	}
	
	//lightUpTime expressed in Hours
	public static void TimeEvent(long time) {
		String tableDataString = varGet("$TABLEDATA!");
		System.out.println("tableDataString: " + tableDataString);
		
		if (!tableDataString.equals("")) {
			String dataSplit[] = tableDataString.split("],");
			for (int row = 0; row < dataSplit.length; row++) {
				String data = dataSplit[row];
				String datapoint = data.split("dpt:")[1].replace("]", "").replace("\"", "").trim();
				System.out.println("datapoint: " + datapoint);
				
				String datapointIOValue = ioGet(datapoint);
				String datapointVarName = "$TABLEDATA-" + datapoint.toUpperCase() + "-TIMEON!";
				
				if (datapointIOValue != null && datapointIOValue.equals("1")) {
					String actualValue = varGet(datapointVarName);
					
					if (actualValue != null) {
						String incrementedValue = String.valueOf(Integer.parseInt(actualValue) + 1);
						
						varSet(datapointVarName, incrementedValue);
						
						int hours = Integer.parseInt( varGet(datapointVarName) ) / 60;
						int minutes = Integer.parseInt( varGet(datapointVarName) ) % 60;
						
						String packet[] = tableData.get(row).split(",");
						
						packet[2] = "\"" + String.format(Locale.US, "%d:%02d", hours, minutes)  + "\""; 			//replace old lightUpTime with the new one
						String packetComplete = String.join(",", packet);
						
						tableData.remove(row);
						tableData.add(row, packetComplete);
					}else {
						varSet(datapointVarName, "0");
					}
					
				}
				
			}
			System.out.println("tableData: " + tableData.toString());
			for (String session : sessionList) {
				uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLEDATA!", tableData.toString());
			}
		}
		
	}
	
	public static void pageEvent(String address, String session, String userid, String project, String page) {
		if (project.equals("table") && page.equals("home")) {	
			uiSet(session, "tabledata", "items", tableData.toString().replace("dpt:", ""));
			varSet("$TABLEDATA!", tableData.toString());
			sessionList.add(session);
		} else {
			sessionList.remove(sessionList.indexOf(session));
		}
	}
	
	public static void logoutEvent(String address, String session, String userid, boolean lock) {
		sessionList.remove(sessionList.indexOf(session));
	}
}
