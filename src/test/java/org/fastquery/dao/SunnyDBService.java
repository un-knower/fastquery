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

package org.fastquery.dao;

import org.fastquery.core.Modifying;
import org.fastquery.core.Query;
import org.fastquery.core.QueryRepository;

/**
 * 
 * @author xixifeng (fastquery@126.com)
 */
public interface SunnyDBService extends QueryRepository {

	@Modifying
	@Query("delete from Card where id = ?1")
	int delete(int id);

	@Modifying
	@Query("delete from Card where id = ?1")
	boolean deleteById(int id);
	
	@Modifying
	@Query("delete from Card where number = ?1") 
	int deleteByNumber(String number);

	@Query("select id from Card where id = ?1")
	boolean exists(long id);
	
	@Query("select id from Card where number = ?1")
	boolean exists(String number);

	@Query("select id from Tenant where id = ?1")
	boolean existsTenant(Long id);
}
