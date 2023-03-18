package plugins.tabletimelights;

import com.hsyco.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class tabletimelights extends userBase {

	private static List<String> sessionList = new ArrayList<String>();
	
	private static List<String> tableData = new ArrayList<String>();

	public static void StartupEvent() {
		String tableDataString = varGet("$TABLETIMELIGHTSDATA!");

		if (tableDataString != null && !tableDataString.equals("")) {
			String dataSplit[] = tableDataString.split("],");
			for (String data : dataSplit) {
				String lines = data.replace("[", "").replace("]", "").replace("On", "Off").trim();
				tableData.add("[" + lines + "]");
			}
		}
	}
	
	public static String userCommand(String session, String userid, String name, String param) {
		
		if (name.startsWith("tabletimelights")) {
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
					
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
					varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
					
					return "ok";
					
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
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
					varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
					
					return "ok";
		
				} else if (action.equals("delete")) {
					int key = Integer.valueOf(obj.getString("key"));
					tableData.remove(key);
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
					varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
					
					return "ok";
		
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
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
					varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
					
					return "ok";
				}
				
			} catch (Exception e) {
				messageLog("PLUGIN - tabletimelights - exception in userCommand: " + e.getMessage());
			}
			
			
			//quando si preme bottone on/off
			if (name.equals("tabletimelights.light")) {
				
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
				
				uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
				
				return "ok";
			} 			
		}
		

		return null;
	}
	
	//lightUpTime [Hours]
	public static void TimeEvent(long time) {
		
		try {
			String tableDataString = varGet("$TABLETIMELIGHTSDATA!");
			
			if (tableDataString != null && !tableDataString.equals("") && !tableDataString.equals("[[]]") && !tableDataString.equals("[]")) {
				String dataSplit[] = tableDataString.split("],");
				for (int row = 0; row < dataSplit.length; row++) {
					String data = dataSplit[row];
					String datapoint = data.split("dpt:")[1].replace("]", "").replace("\"", "").trim();
					
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
							
							packet[2] = "\"" + String.format("%d:%02d", hours, minutes)  + "\"";		//replace old lightUpTime with the new one
							String packetComplete = String.join(",", packet);
							
							tableData.remove(row);
							tableData.add(row, packetComplete);
						}else {
							varSet(datapointVarName, "0");
						}
						
					}
					
				}
				for (String session : sessionList) {
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
				}
				varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
			}
		} catch (Exception e) {
			messageLog("PLUGIN - tabletimelights - exception in timeEvent: " + e.getMessage());
		}
	}
	
	public static void pageEvent(String address, String session, String userid, String project, String page) {
		if (project.equals("office") && page.equals("lights#landscape")) {
		    sessionList.add(session);
			uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
			varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
		} else {
			try {
				if (sessionList.contains(session)) {
					sessionList.remove(sessionList.indexOf(session));
				}
			} catch (Exception e) {
				messageLog("PLUGIN - tabletimelights - exception in pageEvent: " + e.getMessage());
			}	
		}
	}
	
	public static void logoutEvent(String address, String session, String userid, boolean lock) {
		try {
	        if (sessionList.contains(session)) {
			    sessionList.remove(sessionList.indexOf(session));
			}
	    } catch (Exception e) {
			messageLog("PLUGIN - tabletimelights - exception in logoutEvent: " + e.getMessage());
		}
	}
	

}
