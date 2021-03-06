/*
 * Copyright (c) 2016-2088, fastquery.org and/or its affiliates. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For more information, please see http://www.fastquery.org/.
 * 
 */

package org.fastquery.handler;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fastquery.core.QueryContext;
import org.fastquery.core.RepositoryException;
import org.fastquery.util.TypeUtil;

import java.util.Set;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 
 * @author xixifeng (fastquery@126.com)
 */
public class QueryHandler {

	private static QueryHandler queryHandler;

	private QueryHandler() {
	}

	public static QueryHandler getInstance() {
		if (queryHandler == null) {
			synchronized (QueryHandler.class) {
				if (queryHandler == null) {
					queryHandler = new QueryHandler();
				}
			}
		}
		return queryHandler;
	}

	// 对于查操作可根据其返回值分类如下(也就是说只允许这这些类型,在生成类之前已经做预处理,越界类型是进来不了的)
	// 1). long/int 用于统计总行数
	// 2). boolean 判断是否存在
	// 3). Map<String,Object>
	// 4). List<Map<String,Object>> 或 List<Map<String,String>>
	// 5). JSONObject
	// 6). JSONArray
	// 7). Integer,Double,Long,Short,Byte,Character,Float,String
	// 八种基本类型(除了Boolean)
	// 8). Integer[],Double[],Long[],Short[],Byte[],Character[],Float[]
	// 9). bean[]
	// 10). bean

	// 为什么要分类?
	// 如果全部集中处理的话,代码堆积会很多,可读性差,不利于扩展.
	// 对于复杂的事情,一定要找适合的模式,尽可能地分化成的小的模块

	public long longType(List<Map<String, Object>> keyvals) {
		Method method = QueryContext.getMethod();
		if(keyvals.isEmpty()) {
			return 0;
		}

		if (keyvals.size() > 1) {
			throw new RepositoryException(method + " 不能把一个集合赋值给long");
		}

		Map<String, Object> map = keyvals.get(0);
		Set<Entry<String, Object>> entries = map.entrySet();
		if (entries.size() > 1) {
			throw new RepositoryException(method + " 不能把" + map + "赋值给long");
		}
		Iterator<Entry<String, Object>> iterator = entries.iterator();
		if (!iterator.hasNext()) {
			throw new RepositoryException(method + " 不能把" + map + "赋值给long");
		}
		
		return Long.parseLong(iterator.next().getValue().toString());
	}
	
	public int intType(List<Map<String, Object>> keyvals) {
		// 求和的返回值如果是int类型,那么需要把long强制转换成int,可能会越界!
		// 建议:求和返回值用long
		return (int)longType(keyvals);
	}

	public boolean booleanType(List<Map<String, Object>> keyvals) {
       return !keyvals.isEmpty();
	}
	
	// convertType 表示map种的value需要转换的目标类型
	public Map<String, Object> mapType(List<Map<String, Object>> keyvals,Class<?> convertType) {
		if( keyvals.isEmpty() ) {
			return new HashMap<>();
		}
		if (keyvals.size() > 1) {
			throw new RepositoryException(QueryContext.getMethod() + "不能把多条记录赋值给Map");
		}
		
		Map<String, Object> map = keyvals.get(0);
		if(convertType == String.class) {
			Map<String, Object> map2 = new HashMap<>();
			map.forEach((k,v)-> map2.put(k, v.toString()));
			return map2;
		}
		return map;
	}

	// convertType 表示map种的value需要转换的目标类型
	public List<Map<String, Object>> listType(List<Map<String, Object>> keyvals,Class<?> convertType) {
		if(convertType == String.class) {
			List<Map<String, Object>> kvs = new ArrayList<>();
			keyvals.forEach(map->{
				Map<String, Object> m = new HashMap<>();
				map.forEach((k,v) -> {
					m.put(k, v);
					kvs.add(m);
				});
			});
			return kvs;
		}
		return keyvals;
	}
	
	
	public Object list(List<Map<String, Object>> keyvals) { // list bean
		List<Object> list = new ArrayList<>();
		if(keyvals.isEmpty()) {
			return list;
		}
		
		// -- start
		String returnTypeName = QueryContext.getMethod().getGenericReturnType().getTypeName();
		ParameterizedType pt = (ParameterizedType) QueryContext.getMethod().getGenericReturnType();
	    java.lang.reflect.Type[] types = pt.getActualTypeArguments();
	    if( types.length==1) {
	    	java.lang.reflect.Type ct = types[0];
	    	if(TypeUtil.isWarrp(ct)) {
	    		keyvals.forEach(map ->{
	    			if(map.values().size()>1){
	    				throw new RepositoryException("不能把"+keyvals+"转换成" + returnTypeName);
	    			}
	    			Object obj = map.values().iterator().next();
	    			Class<?> clazz = (Class<?>) ct; 
	    			// 如果obj就是clazz对应实例的子类
	    			if(clazz.isAssignableFrom(obj.getClass())) {
		    			list.add(obj);
	    			} else {
	    				throw new RepositoryException("从数据库查出的类型是:" + obj.getClass() + "不能充当List<"+ct+">的元素");
	    			}
	    		});
	    		return list;
	    	}
	    }
		// end
		
			Class<?> beanType = (Class<?>) pt.getActualTypeArguments()[0];
		
		for (Map<String, Object> map : keyvals) {
			list.add(JSON.toJavaObject(new JSONObject(map), beanType));
		}
		return list;
	}

	public JSONObject jsonObjeType(List<Map<String, Object>> keyvals) {
		if (keyvals.size() > 1) {
			throw new RepositoryException(QueryContext.getMethod() + "不能把多条记录赋值给JSONObject");
		}
		return new JSONObject(mapType(keyvals,Object.class));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSONArray jsonArrayType(List<Map<String, Object>> keyvals) {
		return new JSONArray((List) keyvals);
	}

	// 基本包装类型
	public Object wrapperType(Method method, Class<?> returnType, List<Map<String, Object>> keyvals) {
		
		// 包装类型没有空
		if(keyvals.isEmpty()){
			return null;
		}
		
		if (keyvals.size() > 1) {
			throw new RepositoryException(method + "不能把多条记录赋值给" + returnType);
		}
		Map<String, Object> map = keyvals.get(0);
		Set<Entry<String, Object>> entries = map.entrySet();
		if (entries.size() > 1) {
			throw new RepositoryException(method + " 不能把" + map + "赋值给" + returnType);
		}
		Iterator<Entry<String, Object>> iterator = entries.iterator();
		if (!iterator.hasNext()) {
			throw new RepositoryException(method + " 不能把" + map + "赋值给" + returnType);
		}

		Object val = iterator.next().getValue();
		if(val==null){
			return null;
		}
		// Integer,Double,Long,Short,Byte,Character,Float,String
		if (returnType == Integer.class) {
			val = Integer.valueOf(val.toString());
		} else if (returnType == Double.class) {
			val = Double.valueOf(val.toString());
		} else if (returnType == Long.class) {
			val = Long.valueOf(val.toString());
		} else if (returnType == Short.class) {
			val = Short.valueOf(val.toString());
		} else if (returnType == Byte.class) {
			val = Byte.valueOf(val.toString());
		} else if (returnType == Character.class) {
			String strs = val.toString();
			if (strs.length() > 1) {
				throw new RepositoryException(method + " 不能把" + strs + "赋值给" + returnType);
			}
			val = Character.valueOf(val.toString().charAt(0));
		} else if (returnType == Float.class) {
			val = Float.valueOf(val.toString());
		} else if (returnType == String.class) {
			return val.toString();
		}
		return val;
	}

	// 基本包装类型,数组格式
	public Object wrapperAarryType(Class<?> returnType, List<Map<String, Object>> keyvals) {
				
		// 该数组元素的类型
		Class<?> componentType = returnType.getComponentType();

		boolean hasConstructor = TypeUtil.hasDefaultConstructor(componentType);

		// 结果集的条数
		int count = keyvals.size();
		// 创建componentType类型的数组
		Object array = Array.newInstance(componentType, count);

		Map<String, Object> map;
		Set<String> keys;
		String key;
		for (int index = 0; index < count; index++) {
			if (hasConstructor && componentType!=String.class) { // 存在默认构造方法,表明是一个bean
				Array.set(array, index, JSON.toJavaObject(new JSONObject(keyvals.get(index)), componentType));
			} else { // 不是bean
				map = keyvals.get(index);
				keys = map.keySet();
				if (keys.size() == 1) { // 集合中的单个元素是一对键值
					key = keys.iterator().next();
					if(componentType==String.class) {
						Array.set(array, index, map.get(key)!=null?map.get(key).toString():null);
					} else {
						Array.set(array, index, map.get(key));
					}
				} else {
					throw new RepositoryException(QueryContext.getMethod() + " 执行的结果集" + keyvals + "不能转换成" + returnType);
				}
			}
		}

		return array;
	}

	public Object beanType(List<Map<String, Object>> keyvals) {
		Class<?> returnType = QueryContext.getReturnType();
		if(keyvals.isEmpty()) {
			return null;
		}
		if (keyvals.size() != 1) {
			// method + "不能把一个集合转换成" + returnType
			throw new RepositoryException(String.format("%s 不能把一个集合转换成 %s %n根据输入的SQL所查询的结果是一个集合.", QueryContext.getMethod(),returnType));
		}

		if (TypeUtil.hasDefaultConstructor(returnType)) {
			return JSON.toJavaObject(new JSONObject(keyvals.get(0)), returnType);
		} else {
			return keyvals.get(0).entrySet().iterator().next().getValue();
		}
	}
}
