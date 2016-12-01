package ocr.pointofsale.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 把前台model转化为业务实体
 * goods.account_id,goods.product_sku_code
 * ---->
 * goods:
 *   account_id,
 *   product_sku_code
 * @author wanghw
 *
 */
public class JsonModel2EntityUtil {

	public static JsonObject convert(JsonObject model) {		
		Map<String,Object> map = new HashMap<String, Object>();
		map = convert2Map(model);
		JsonObject entity = new JsonObject(map);
		return entity;
	}

	private static Map<String, Object> convert2Map(JsonObject model) {
		Map<String,Object> map = new HashMap<String, Object>();
		Iterator<Entry<String, Object>> it = model.iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();  
			String fieldName = entry.getKey();
			if(fieldName.equals("detail")){
				//表体
				JsonArray details = (JsonArray) entry.getValue();
				List<Map<String,Object>> detailList = new ArrayList<Map<String, Object>>();
				for (Object object : details) {
					detailList.add(convert2Map((JsonObject)object));
				}
				map.put(fieldName, detailList);
				continue;
			}
			//表头
			buildHead(map,entry,fieldName);
		}
		return map;
	}

	private static Map<String, Object> buildHead(Map<String, Object> map, Entry<String, Object> entry, String fieldName) {
		if(map == null){
			map = new HashMap<String, Object>();
		}
		int index = fieldName.indexOf(".");
		if(index == -1){
			//末级属性
			Object value = entry.getValue();
			map.put(fieldName, value);
		}else{
			String field_first = fieldName.substring(0,index);
			String field_last = fieldName.substring(index+1, fieldName.length());
			Map<String,Object> mapItemO = (Map<String, Object>) map.get(field_first);
			mapItemO = buildHead(mapItemO,entry,field_last);
			map.put(field_first, mapItemO);
		}
		return map;
	}
	
}
