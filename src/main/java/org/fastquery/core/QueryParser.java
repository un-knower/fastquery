/*
 * Copyright (c) 2016-2017, fastquery.org and/or its affiliates. All rights reserved.
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

package org.fastquery.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.fastquery.mapper.QueryPool;
import org.fastquery.page.NotCount;
import org.fastquery.page.Pageable;
import org.fastquery.page.PageableImpl;
import org.fastquery.struct.ParamMap;
import org.fastquery.struct.SQLValue;
import org.fastquery.util.TypeUtil;

/**
 * 
 * @author mei.sir@aliyun.cn
 */
public class QueryParser {

	
	private static final Logger LOG = Logger.getLogger(QueryParser.class);
	
	private QueryParser(){}
	
	/**
	 * 改操作分析
	 * @param queries
	 * @return
	 */
	static List<SQLValue> modifyParser() {
		Method method = QueryContext.getMethod();
		Object[] args = QueryContext.getArgs();
		Query[] queries = method.getAnnotationsByType(Query.class);
		
		Modifying modifying = method.getAnnotation(Modifying.class);
		String id = modifying.id(); // 不可能为null
		String table = modifying.table();

		List<String> sqls = TypeUtil.getQuerySQL(method, queries, args);

		int sqlCount = sqls.size();

		List<SQLValue> msvs = new ArrayList<>(sqlCount);

		for (int jk = 0; jk < sqlCount; jk++) {
			// 获取sql
			String sql = sqls.get(jk);
			int[] ints = TypeUtil.getSQLParameter(sql);
			sql = sql.replaceAll(Placeholder.SP1_REG, "?");
			// 替换SQL中的占位变量符
			sql = sql.replaceAll(Placeholder.TABLE_REG, table);
			sql = sql.replaceAll(Placeholder.ID_REG, id);

			// sql 中的"?"号调整
			// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
			ParamMap paramMap = TypeUtil.getParamMap(ints, args);
			Map<Integer, Integer> rpates = paramMap.getRps();
			List<Object> objs = paramMap.getObjs();

			Set<Entry<Integer, Integer>> entities = rpates.entrySet();
			for (Entry<Integer, Integer> entry : entities) {
				sql = TypeUtil.replace(sql, entry.getKey(), entry.getValue());
			}
			msvs.add(new SQLValue(sql, objs));
		}

		return msvs;
	}
	
	/**
	 * 如果没有标识@Query或@QueryByNamed,则返回null
	 * @return
	 */
	static SQLValue queryParser() {
		Method method = QueryContext.getMethod();
		Object[] args = QueryContext.getArgs();
		Query[] queries = method.getAnnotationsByType(Query.class);
		String sql = TypeUtil.getQuerySQL(method, queries, QueryContext.getArgs()).get(0);
		int[] ints = TypeUtil.getSQLParameter(sql);
		sql = sql.replaceAll(Placeholder.SP1_REG, "?");
		
					// sql 中的"?"号调整
					// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
					ParamMap paramMap = TypeUtil.getParamMap(ints, args);
					Map<Integer, Integer> rpates = paramMap.getRps();
					List<Object> objs = paramMap.getObjs();
					
					Set<Entry<Integer, Integer>> entities = rpates.entrySet();
					for (Entry<Integer, Integer> entry : entities) {
						sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
					}
					return new SQLValue(sql, objs);
	}

	public static List<SQLValue> pageParser() {
		
		// 当前页query
		// 求和query
		// 下一页query
		List<SQLValue> sqlValues = new ArrayList<>(2);
		
		
		Method method = QueryContext.getMethod();
		Object[] args = QueryContext.getArgs();
		Query[] querys = method.getAnnotationsByType(Query.class);
		
		
		
		// 获取sql
				String sql = TypeUtil.getQuerySQL(method, querys, args).get(0);
				int[] ints = TypeUtil.getSQLParameter(sql);
				Pageable pageable = null;
				for (Object arg : args) {
					if(arg instanceof Pageable) { // 如果当前arg是Pageable接口的一个实例
						pageable = (Pageable) arg;
						break;
					}
				}
				Parameter[] parameters = method.getParameters();
				if(pageable == null ) {
					// 没有传递Pageable,那么必然有 pageIndex, pageSize 不然,不能通过初始化
					pageable = new PageableImpl(TypeUtil.findPageIndex(parameters, args), TypeUtil.findPageSize(parameters, args));
				}
				
				int firstResult = pageable.getOffset();
				int maxResults = pageable.getPageSize();
				
				LOG.debug("firstResult:"+firstResult+" maxResults:" + maxResults);
				
				// 针对 mysql 分页
				// 获取limit
				StringBuilder sb = new StringBuilder(" limit ");
				sb.append(firstResult);
				sb.append(',');
				sb.append(maxResults);
				String limit = sb.toString();
				
				List<String> strs = TypeUtil.matches(sql, Placeholder.LIMIT_RGE);
				if(strs.isEmpty()) { // 如果没有#{#limit}, 默认在末尾增加.
					sql += Placeholder.LIMIT;
				}
				
				String ssql = new String(sql);
				
				sql = sql.replaceFirst(Placeholder.LIMIT_RGE, limit);
				
				sql = sql.replaceAll(Placeholder.SP1_REG, "?");
				
				// sql 中的"?"号调整
				// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
				ParamMap paramMap = TypeUtil.getParamMap(ints, args);
				Map<Integer, Integer> rpates = paramMap.getRps();
				List<Object> objs = paramMap.getObjs();
				
				Set<Entry<Integer, Integer>> entities = rpates.entrySet();
				for (Entry<Integer, Integer> entry : entities) {
					sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
				}
				// sql 中的"?"号调整 End
				LOG.info(sql);
				sqlValues.add(new SQLValue(sql, objs));
				
				
				
				
				
				
				
				Query query = querys[0];
				if(method.getAnnotation(NotCount.class)==null) {
					// 求和 ---------------------------------------------------
					String countField = query.countField();
					// 获取求和sql
					String countQuery = query.countQuery();
					if("".equals(countQuery)) { // 表明在声明时没有指定求和语句
						// 计算求和语句
						// 把select 与 from 之间的 内容变为 count(countField)
						 // (?i)表示正则中不区分大小写 \\b 表示单词的分界
						 int beginIndex = ignoreCaseWordEndIndex("(?i)\\bselect ", sql);
						 // 作为substring中的endIndex
						 int endIndex = ignoreCaseWordStartIndex("(?i) from ", sql);
						 if(beginIndex!=-1 && endIndex!=-1) {
							// 注意: 求和字段默认为 "id"
							// 重要: sqlStr.substring(selectStart+6,fromStart) 的值 很可能包含正则表达式, 因此必须用Pattern.quote
							sql = sql.replaceFirst(Pattern.quote(sql.substring(beginIndex,endIndex)),new StringBuilder("count(").append(countField).append(')').toString());
						 } else {
							 throw new RepositoryException("求和SQL错误:" + sql);
						 }
						 
						 sql = TypeUtil.getCountQuerySQL(method, sql, args);
					} else {
						sql = TypeUtil.getCountQuerySQL(method, countQuery, args);
						sql = sql.replaceAll(Placeholder.SP1_REG, "?");
					}
					
					 // 求和语句不需要order by 和 limit
					 // (?i) : 表示不区分大小写
					 // 过滤order by 后面的字符串(包含本身)
					 sql = sql.replaceFirst("(?i)(order by )(.|\n)+", ""); 
					 // 过滤limit后面的字符串(包含自身)
					 sql = sql.replaceFirst("(?i)(limit )(.|\n)+", "");

			try {
				// sql 中的"?"号调整
				// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?)
				// 那么这个?的实际个数取决于传递的集合长度
				ParamMap paramMap2 = TypeUtil.getParamMap(ints, args);
				Map<Integer, Integer> rps = paramMap2.getRps();
				List<Object> vos = paramMap2.getObjs();

				Set<Entry<Integer, Integer>> ens = rps.entrySet();
				for (Entry<Integer, Integer> entry : ens) {
					sql = TypeUtil.replace(sql, entry.getKey(), entry.getValue());
				}
				// sql 中的"?"号调整 End
				sqlValues.add(new SQLValue(sql, vos));
			} catch (Exception e) {
			}
		} else {
			// 在查一下推算出下一页是否有数据, 要不要把下一页的数据存储起来,有待考虑...
			firstResult = pageable.getOffset()+pageable.getPageSize();
			sb = new StringBuilder(" limit ");
			sb.append(firstResult);
			sb.append(',');
			sb.append(maxResults);
			limit = sb.toString();
			sql = ssql.replaceFirst(Placeholder.LIMIT_RGE, limit);
			sql = sql.replaceAll(Placeholder.SP1_REG, "?");
			try {
				// sql 中的"?"号调整
				// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
				ParamMap paramMap3 = TypeUtil.getParamMap(ints, args);
				Map<Integer, Integer> rts = paramMap3.getRps();
				List<Object> pjs = paramMap3.getObjs();
				
				Set<Entry<Integer, Integer>> ens = rts.entrySet();
				for (Entry<Integer, Integer> entry : ens) {
					sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
				}
				// sql 中的"?"号调整 End
				sqlValues.add(new SQLValue(sql, pjs));
			} catch (Exception e) {
				throw new RepositoryException(e.getMessage(),e);
			} finally {
			}
		}
				

		return sqlValues;
	}
	
	
	public static List<SQLValue> pageParserByNamed(){

		// 当前页query
		// 求和query
		// 下一页query
		List<SQLValue> sqlValues = new ArrayList<>(2);
		
		String query = QueryPool.render(true);
		String countQuery = QueryPool.render(false);
		
		Method method = QueryContext.getMethod();
		Object[] args = QueryContext.getArgs();
		// 获取 pageable
		Pageable pageable = null;
		for (Object arg : args) {
			if(arg instanceof Pageable) { // 如果当前arg是Pageable接口的一个实例
				pageable = (Pageable) arg;
				break;
			}
		}
		Parameter[] parameters = method.getParameters();
		if(pageable == null ) {
			// 没有传递Pageable,那么必然有 pageIndex, pageSize 不然,不能通过初始化
			pageable = new PageableImpl(TypeUtil.findPageIndex(parameters, args), TypeUtil.findPageSize(parameters, args));
		}
		// 获取 pageable End
		
		// 获取sql
		String sql = TypeUtil.paramNameFilter(method, args, query);
		int[] ints = TypeUtil.getSQLParameter(sql);
				
		sql = sql.replaceAll(Placeholder.SP1_REG, "?");
		String limit = getLimit(pageable.getOffset(),pageable.getPageSize());
		if(sql.indexOf(Placeholder.LIMIT)!=-1){ // 如果#{#limit}存在
			sql = sql.replaceAll(Placeholder.LIMIT_RGE, limit);	
		} else {
			sql += limit;
		}

		
		try {
			// sql 中的"?"号调整
			// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
			ParamMap paramMap = TypeUtil.getParamMap(ints, args);
			Map<Integer, Integer> rpates = paramMap.getRps();
			List<Object> objs = paramMap.getObjs();
			
			Set<Entry<Integer, Integer>> entities = rpates.entrySet();
			for (Entry<Integer, Integer> entry : entities) {
				sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
			}
			// sql 中的"?"号调整 End
			LOG.info(sql);
			sqlValues.add(new SQLValue(sql, objs));
		} catch (Exception e) {
			throw new RepositoryException(e.getMessage(),e);
		} finally {
		}
		
		if(method.getAnnotation(NotCount.class)==null) { // 需要求和
			 sql = TypeUtil.paramNameFilter(method, args,countQuery);
			 ints = TypeUtil.getSQLParameter(sql); // 06-11-11
			 sql = sql.replaceAll(Placeholder.SP1_REG, "?");
			try {
				// sql 中的"?"号调整
				// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
				ParamMap paramMap = TypeUtil.getParamMap(ints, args);
				Map<Integer, Integer> rpates = paramMap.getRps();
				List<Object> objs = paramMap.getObjs();
				
				Set<Entry<Integer, Integer>> entities = rpates.entrySet();
				for (Entry<Integer, Integer> entry : entities) {
					sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
				}
				// sql 中的"?"号调整 End
				LOG.info("求和语句: " + sql);
				sqlValues.add(new SQLValue(sql, objs));
			} catch (Exception e) {
				throw new RepositoryException(e);
			} finally {
			}

			// 求和 --------------------------------------------------- End
		} else {
			// 获取sql
			sql = TypeUtil.paramNameFilter(method, args, query); // 06-11-11
			ints = TypeUtil.getSQLParameter(sql); // 06-11-11
			// 在查一下推算出下一页是否有数据, 要不要把下一页的数据存储起来,有待考虑...
			int firstResult = pageable.getOffset()+pageable.getPageSize();
			limit =  getLimit(firstResult, pageable.getPageSize());
			if(sql.indexOf(Placeholder.LIMIT) != -1) {
				sql = sql.replaceAll(Placeholder.LIMIT_RGE,limit);	
			} else {
				sql += limit;
			}
			sql = sql.replaceAll(Placeholder.SP1_REG, "?");
			try {
				// sql 中的"?"号调整
				// sql中的"?"可能会因为方法参数是一个集合,会变成多个, 举例说明: in (?) 那么这个?的实际个数取决于传递的集合长度
				ParamMap paramMap = TypeUtil.getParamMap(ints, args);
				Map<Integer, Integer> rpates = paramMap.getRps();
				List<Object> objs = paramMap.getObjs();
				
				Set<Entry<Integer, Integer>> entities = rpates.entrySet();
				for (Entry<Integer, Integer> entry : entities) {
					sql = TypeUtil.replace(sql, entry.getKey(),entry.getValue());
				}
				// sql 中的"?"号调整 End
				LOG.debug("下一页的SQL:" + sql);
				sqlValues.add(new SQLValue(sql, objs));
			} catch (Exception e) {
				throw new RepositoryException(e.getMessage(),e);
			} finally {
			}
		}
					
		return sqlValues;
	}
	
	
	/**
	 * 根据regex在target中首次匹配到的开始索引, 没有匹配到返回-1
	 * @param regex 正则表达式
	 * @return
	 */
	private static int ignoreCaseWordStartIndex(String regex,String target){
		Matcher m = matcherFind(regex,target); 
		if( m == null ){
			return -1;
		} else {
			return m.start();
		}
	}
	
	/**
	 * 根据regex在target中首次匹配到的结束索引, 没有匹配到返回-1
	 * @param regex 正则表达式
	 * @return
	 */
	private static int ignoreCaseWordEndIndex(String regex,String target){
		Matcher m = matcherFind(regex,target); 
		if( m == null ){
			return -1;
		} else {
			return m.end();
		}
	}
	
	private static Matcher matcherFind(String regex,String target){
		Pattern pattern = Pattern.compile(regex);
		 Matcher m = pattern.matcher(target);
		 if(m.find()) {
			 return m;
		 }
		 return null;
	}
	
	private static String getLimit(int firstResult,int maxResults){
		StringBuilder sb = new StringBuilder(" limit ");
		sb.append(firstResult);
		sb.append(',');
		sb.append(maxResults);
		return sb.toString();
	}
}