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

package org.fastquery.test;

import org.fastquery.bean.UserInfo;
import org.fastquery.core.RepositoryException;
import org.fastquery.dao.UserInfoDBService;
import org.fastquery.example.StudentDBService;
import org.fastquery.service.FQuery;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author xixifeng (fastquery@126.com)
 */
public class MethodQueryTest {
	
	@Rule
	public FastQueryTestRule rule = new FastQueryTestRule();

	private StudentDBService studentDBService = FQuery.getRepository(StudentDBService.class);
	private UserInfoDBService userInfoDBService = FQuery.getRepository(UserInfoDBService.class);

	@Test
	public void testSave() {

		Integer id = 36;
		String name = "Jsxxv";
		Integer age = 23;
		UserInfo u = new UserInfo(id, name, age);

		while (userInfoDBService.findById(id) != null) { // 该主键已经存在,直到该主键不存在时,才会结束循环
			id += 1;
		}

		u.setId(id);
		int effect = studentDBService.save(u);
		assertThat(effect,is(1));
	}

	@Test
	public void testSave2() {
		Integer id = null;
		String name = "凤侯";
		Integer age = 32;
		UserInfo u = new UserInfo(id, name, age);
		int effect = studentDBService.save("xk-c3p0", "xk", u);
		assertThat(effect,is(1));
	}

	@Test
	public void save3() {
		UserInfo u1 = new UserInfo(1, "equinox", 10);
		UserInfo u2 = new UserInfo(2, "Eclipse", 3);
		UserInfo u3 = new UserInfo(3, "ement", 2);
		int effect = studentDBService.save(true, u1, u2, u3);
		assertThat(effect, is(0));
	}

	@Test(expected = RepositoryException.class)
	public void save4() {
		UserInfo u1 = new UserInfo(1, "equ", 10);
		UserInfo u2 = new UserInfo(2, "Ecl", 3);
		UserInfo u3 = new UserInfo(3, "ement", 2);
		// 断言: 在执行studentDBService.save(false, u1,u2,u3)之后,将会抛出
		// RepositoryException 异常!
		// thrown.expect(RepositoryException.class);
		// 断言: 在执行studentDBService.save(false, u1,u2,u3)之后,抛出的异常信息中包含有"Duplicate
		// entry '1' for key 'PRIMARY'"字符串
		// thrown.expectMessage(containsString("Duplicate entry '1' for key
		// 'PRIMARY'"));
		int effect = studentDBService.save(false, u1, u2, u3);
		assertThat(effect, is(0));
	}

	@Test
	public void save5() {
		UserInfo u1 = new UserInfo("equ", 10);
		UserInfo u2 = new UserInfo("Ecl", 3);
		UserInfo u3 = new UserInfo("ement", 2);
		int effect = studentDBService.save(false, u1, u2, u3);
		assertThat(effect, is(3));
	}

	@Test
	public void save6() {
		UserInfo u1 = new UserInfo("安小惠", 10);
		UserInfo u2 = new UserInfo("袁承志", 3);
		UserInfo u3 = new UserInfo("袁崇焕", 2);
		Collection<UserInfo> userInfos = new ArrayList<>();
		userInfos.add(u1);
		userInfos.add(u2);
		userInfos.add(u3);
		userInfos.add(new UserInfo("我是谁", null));
		int effect = studentDBService.save(false, userInfos);
		assertThat(effect, is(4));
	}

	@Test
	public void executeBatch() {
		studentDBService.executeBatch("update.sql", "sqlout.log");
	}

	@Test
	public void update2() {
		Integer id = 3;
		UserInfo userInfo = userInfoDBService.findById(id);
		assertThat(userInfo.getId(), equalTo(id));
		UserInfo entity = new UserInfo(userInfo.getId(), userInfo.getName(), userInfo.getAge());
		int i = userInfoDBService.update(entity, null);
		assertThat(i, is(1));
	}

	@Test
	public void update3() {
		String dataSourceName = "xk-c3p0";
		String dbName = "xk";
		UserInfo entity = new UserInfo(1, "好哇瓦", 3);
		int effect = studentDBService.update(dataSourceName, dbName, entity);
		assertThat(effect, is(1));
	}

	@Test
	public void saveOrUpdate() {
		Integer id = 100;
		UserInfo userInfo = new UserInfo(id, "小蜜蜂", 5);
		
		int effect = studentDBService.saveOrUpdate(userInfo);
		assertThat(effect, either(is(0)).or(is(1)));
		UserInfo u1 = userInfoDBService.findById(id);
		Integer id1 = u1.getId();
		assertThat(id1, notNullValue());
		assertThat(u1.getName(), equalTo("小蜜蜂"));
		assertThat(u1.getAge(), equalTo(5));

		effect = studentDBService.saveOrUpdate(u1);
		UserInfo u2 = userInfoDBService.findById(id1);
		Integer id2 = u2.getId();
		assertThat(id2, equalTo(id1));
		assertThat(u2.getName(), equalTo("小蜜蜂"));
		assertThat(u2.getAge(), equalTo(5));

	}

	// 使用update时,同时自定义条件的例子
	@Test
	public void update4() {
		Integer id = 1;
		String name = "框架测试!";
		Integer age = 3;
		UserInfo entity = new UserInfo(id, name, age);

		int e = studentDBService.update(entity);
		assertThat(e, is(1));

		// 会解析成:update `UserInfo` set `id`=?, `age`=? where name = ?
		int effect = studentDBService.update(entity, "name = :name");
		// 断言: 影响的行数大于0行
		assertThat(effect, greaterThan(0));

		// 不想让id字段参与改运算
		entity.setId(null);
		// 会解析成:update `UserInfo` set `age`=? where name = ?
		effect = studentDBService.update(entity, "name = :name");
		assertThat(effect, greaterThan(0));

		// 不想让age字段参与改运算
		entity.setAge(null);
		effect = studentDBService.update(entity, "name = :name");
		assertThat(effect, is(0));
	}

	@Test
	public void update5() {
		Integer id = 1;
		String name = "框架测试!";
		Integer age = 23;
		UserInfo ui = FQuery.reset(UserInfo.class);
		ui.setName(name);
		ui.setAge(age);
		ui.setId(id);
		int i = userInfoDBService.update(ui, "id in (:id,:id)");
		assertThat(i, equalTo(1));
		UserInfo userInfo = userInfoDBService.findById(ui.getId());
		assertThat(userInfo.getId(), equalTo(id));
		assertThat(userInfo.getName(), equalTo(name));
		assertThat(userInfo.getAge(), equalTo(age));
	}

	@Test
	public void update6() {
		UserInfo ui = FQuery.reset(UserInfo.class);
		ui.setId(1);
		int i = userInfoDBService.update(ui, "id = :id");
		assertThat(i, lessThan(1));
	}

	// 测试批量更新集合
	@Test
	public void updateCollection1() {
		userInfoDBService.saveOrUpdate(new UserInfo(77, "河虾", 2));
		userInfoDBService.saveOrUpdate(new UserInfo(88, "番茄", 5));
		userInfoDBService.saveOrUpdate(new UserInfo(99, "酸奶", 2));

		List<UserInfo> userInfos = new ArrayList<>();
		userInfos.add(new UserInfo(77, "茝若", 18));
		userInfos.add(new UserInfo(88, "芸兮", null));
		userInfos.add(new UserInfo(99, "梓", 16));

		int effect = userInfoDBService.update(userInfos);
		assertThat(effect, is(3));
	}

	@Test
	public void updateCollection2() {
		userInfoDBService.saveOrUpdate(new UserInfo(77, "河虾", 2));
		userInfoDBService.saveOrUpdate(new UserInfo(88, "番茄", 5));
		userInfoDBService.saveOrUpdate(new UserInfo(99, "酸奶", 2));

		List<UserInfo> userInfos = new ArrayList<>();
		userInfos.add(new UserInfo(77, "茝若", 18));
		userInfos.add(new UserInfo(88, "芸兮", null));
		userInfos.add(new UserInfo(99, null, 16));

		int effect = userInfoDBService.update(userInfos);
		assertThat(effect, is(3));
	}

	@Test
	public void updateCollection3() {
		userInfoDBService.saveOrUpdate(new UserInfo(77, "河虾", 2));
		userInfoDBService.saveOrUpdate(new UserInfo(88, "番茄", 5));
		userInfoDBService.saveOrUpdate(new UserInfo(99, "酸奶", 2));

		List<UserInfo> userInfos = new ArrayList<>();
		userInfos.add(new UserInfo(77, null, null));
		userInfos.add(new UserInfo(88, null, null));
		userInfos.add(new UserInfo(99, null, null));

		int effect = userInfoDBService.update(userInfos);
		assertThat(effect, is(3));

		UserInfo[] uis = userInfoDBService.findByIds(new int[] { 77, 88, 99 });
		for (UserInfo userInfo : uis) {
			if (userInfo.getId().equals(77)) {
				assertThat(userInfo.getName(), equalTo("河虾"));
				assertThat(userInfo.getAge(), equalTo(2));
			} else if (userInfo.getId().equals(88)) {
				assertThat(userInfo.getName(), equalTo("番茄"));
				assertThat(userInfo.getAge(), equalTo(5));
			} else if (userInfo.getId().equals(99)) {
				assertThat(userInfo.getName(), equalTo("酸奶"));
				assertThat(userInfo.getAge(), equalTo(2));
			}
		}
	}
	// 测试批量更新集合 End

	@Test
	public void find() {
		assertThat(userInfoDBService.find(UserInfo.class, 3).getId().intValue(), is(3));
		assertThat(userInfoDBService.find(UserInfo.class, 3, null).getId().intValue(), is(3)); // 测试数据源传递null
		assertThat(userInfoDBService.find(UserInfo.class, 3, null,null).getId().intValue(), is(3)); // 测试数据库名称为null
	}
	
	@Test
	public void delete() {
		int id = 89890;
		int effect = userInfoDBService.save(new UserInfo(id, "植物", 17));
		assertThat(effect, is(1));
		effect = userInfoDBService.delete("UserInfo", "id", id);
		assertThat(effect, is(1));
	}
}
