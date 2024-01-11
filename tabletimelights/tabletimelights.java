package plugins.tabletimelights;

import com.hsyco.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

public class tabletimelights extends userBase {

	private static List<String> sessionList = new ArrayList<String>();
	private static List<String> tableData = new ArrayList<String>();

	public static void StartupEvent() {
		String tableDataString = varGet("$TABLETIMELIGHTSDATA!");
		if (tableDataString != null && !tableDataString.equals("") && !tableDataString.equals("[]")) {
			String dataSplit[] = tableDataString.split("],");
			for (String data : dataSplit) {
				String lines = data.replace("[", "").replace("]", "").trim();
				tableData.add("[" + lines + "]");
			}
		}
	}
	
	public static String userCommand(String session, String userid, String name, String param) {

		if (name.equals("tabletimelightsdata")) {
			try {
				String jsonString = param;
				JSONObject obj = new JSONObject(jsonString);
				String action = obj.getString("action");
				
				if (action.equals("insert")) {
					JSONObject objData = obj.getJSONObject("data");
					String lightName = objData.getString("0");
					String lightStatus = objData.getString("_1");	//default: Off
					String lightOnTime = objData.getString("_2");	//default: 0:00
					String datapoint = objData.getString("3");
					String datapointIOValue = ioGet(datapoint);
					boolean isDimmer = false;
					
					//lightStatus
					if(datapointIOValue != null) {
						if(lightName.toLowerCase().contains("dimmer")) {
							isDimmer = true;
						}
						if(datapointIOValue.contains("%")) {
							datapointIOValue = datapointIOValue.replace("%", "");
						}
						
						int datapointIOIntValue = Integer.parseInt(datapointIOValue);
						
						if(isDimmer) {
							if(datapointIOValue.equals("0%") || datapointIOValue.equals("0")) {
								lightStatus = "0%";
							}else {
								lightStatus = datapointIOIntValue + "%";
							}
						}else {
							if(datapointIOValue.equals("1")) {
								lightStatus = "On";
							}else {
								lightStatus = "Off";
							}
						}
					}
					
					//lightOnTime
					String datapointVarName = "$TABLETIMELIGHTSDATA-" + datapoint.toUpperCase() + "-TIMEON!";
					String datapointVarGet = varGet(datapointVarName);
					if(datapointVarGet != null) {
						int hours = Integer.parseInt( varGet(datapointVarName) ) / 60;
						int minutes = Integer.parseInt( varGet(datapointVarName) ) % 60;
						lightOnTime = String.format("%d:%02d", hours, minutes);
					}
					
					String packet = "[\"" + lightName + "\",\"" + lightStatus + "\",\"" + lightOnTime + "\",\"dpt:" + datapoint +"\"]";
							
					tableData.add(packet);
					
					uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
					varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
					
					return "ok";
					
				} else if (action.equals("update")) {
					JSONObject objData = obj.getJSONObject("data");
					String lightName = objData.getString("0");
					String lightStatus = objData.getString("_1");
					String lightOnTime = objData.getString("_2");
					String datapoint = objData.getString("3");
		
					int key = Integer.valueOf(obj.getString("key"));
		
					String packet = "[\"" + lightName + "\",\"" + lightStatus + "\",\"" + lightOnTime + "\",\"dpt:" + datapoint +"\"]";
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
		}	
			
		//when on/off button is pressed
		if (name.equals("tabletimelights.light")) {
			boolean isDimmer = false;
			
			int key = Integer.valueOf(param);
			String packet = tableData.get(key);
			String lightName = packet.split(",")[0].replace("\"", "");
			String status = packet.split(",")[1].replace("\"", "");
			String datapoint = packet.split("dpt:")[1].replace("]", "").replace("\"", "").trim();
			String datapointIOValue = ioGet(datapoint);
			
			if(datapointIOValue != null) {
				
				if(lightName.toLowerCase().contains("dimmer")) {
					isDimmer = true;
				}
				if(packet.contains("%")) {
					datapointIOValue = datapointIOValue.replace("%", "");
				}
				
				int datapointIOIntValue = Integer.parseInt(datapointIOValue);
				
				if(isDimmer) {
					if (status.equals("0%") && (datapointIOIntValue <= 0)) {
						ioSet(datapoint, "1");
						packet = packet.replace("0%", ioGet(datapoint));
					}else if (!status.equals("0%") && datapointIOIntValue > 0){
						ioSet(datapoint, "0");
						String fields[] = packet.split(",");
						fields[1] = "\"0%\"";
						packet = String.join(",", fields);
					}
				}else {
					if (packet.contains("On") && datapointIOValue.equals("1")) {
						ioSet(datapoint, "0");
						packet = packet.replace("On", "Off");
					}else if (packet.contains("Off") && datapointIOValue.equals("0")){
						ioSet(datapoint, "1");
						packet = packet.replace("Off", "On");
					}
					
				}
				
				tableData.remove(key);
				tableData.add(key, packet);
				
				uiSet(session, "tabletimelightsdata", "items", tableData.toString().replace("dpt:", ""));
				varSet("$TABLETIMELIGHTSDATA!", tableData.toString());
			}
			
			return "ok";
		}
		
		return null;
	}
	
	//lightOnTime [Hours]
	public static void TimeEvent(long time) {
		
		//clear sessionList at every midnight
		String date = new SimpleDateFormat("HH:mm").format(new Date(time));
		String[] hourMinutes = date.split(":");
		if(hourMinutes[0].equals("00") && hourMinutes[1].equals("00")) {
			sessionList.clear();
		}
		
		try {
			String tableDataString = varGet("$TABLETIMELIGHTSDATA!");
			
			if (tableDataString != null && !tableDataString.equals("") && !tableDataString.equals("[[]]") && !tableDataString.equals("[]")) {
				String dataSplit[] = tableDataString.split("],");
				for (int row = 0; row < dataSplit.length; row++) {
					String data = dataSplit[row];
					String datapoint = data.split("dpt:")[1].replace("]", "").replace("\"", "").trim();
					
					String datapointIOValue = ioGet(datapoint);
					if(datapointIOValue != null) {
						
						if(datapointIOValue.contains("%")) {
							datapointIOValue = datapointIOValue.replace("%", "");
						}
						
						int datapointIOIntValue = Integer.parseInt(datapointIOValue);
						
						String datapointVarName = "$TABLETIMELIGHTSDATA-" + datapoint.toUpperCase() + "-TIMEON!";
						
						if (datapointIOIntValue > 0) {
							String actualValue = varGet(datapointVarName);
							
							if (actualValue != null) {
								String incrementedValue = String.valueOf(Integer.parseInt(actualValue) + 1);
								
								varSet(datapointVarName, incrementedValue);
								
								int hours = Integer.parseInt( varGet(datapointVarName) ) / 60;
								int minutes = Integer.parseInt( varGet(datapointVarName) ) % 60;
								
								String packet[] = tableData.get(row).split(",");
								
								packet[2] = "\"" + String.format("%d:%02d", hours, minutes)  + "\"";	//replace old lightOnTime with the new one
								String packetComplete = String.join(",", packet);
								
								tableData.remove(row);
								tableData.add(row, packetComplete);
							}else {
								varSet(datapointVarName, "0");
							}
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
			
			for (int row = 0; row < tableData.size(); row++) {
				String line = tableData.get(row);
				String lightName = line.split(",")[0].replace("[", "").replace("]", "").replace("\"", "").trim();
				String lightStatus = "";
				String lightOnTime = line.split(",")[2].replace("[", "").replace("]", "").replace("\"", "").trim();
				String datapoint = line.split("dpt:")[1].replace("[", "").replace("]", "").replace("\"", "").trim();
				String datapointIOValue = ioGet(datapoint);
				boolean isDimmer = false;
				
				if(datapointIOValue != null) {

					if(lightName.toLowerCase().contains("dimmer")) {
						isDimmer = true;
					}
					if(datapointIOValue.contains("%")) {
						datapointIOValue = datapointIOValue.replace("%", "");
					}

					int datapointIOIntValue = Integer.parseInt(datapointIOValue);
					
					if(isDimmer) {
						if(datapointIOValue.equals("0%") || datapointIOValue.equals("0")) {
							lightStatus = "0%";
						}else {
							lightStatus = datapointIOIntValue + "%";
						}
					}else {
						if(datapointIOValue.equals("1")) {
							lightStatus = "On";
						}else {
							lightStatus = "Off";
						}
					}
					
					line = "\"" + lightName + "\",\"" + lightStatus + "\",\"" + lightOnTime + "\",\"dpt:" + datapoint +"\"";
					tableData.remove(row);
					tableData.add(row, "[" + line + "]");
				}
				
			}
			
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
