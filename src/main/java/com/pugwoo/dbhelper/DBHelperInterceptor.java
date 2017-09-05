package com.pugwoo.dbhelper;

import java.util.List;

/**
 * 2017年9月5日 11:12:00
 * 数据库操作拦截器，拦截器的命名为[before|after][select|update|insert|delete]，拦截器将影响全局，请谨慎使用。
 * 关于拦截器的设计想法：
 * 1. 查询拦截器，用于数据安全拦截和数据查询审计
 * 2. 
 * @author pugwoo
 */
public interface DBHelperInterceptor {

	/**
	 * select查询之前调用
	 * @param clazz 查询的对象
	 * @param sql 查询的完整sql
	 * @param args 查询的完整参数
	 * @return 返回true，则查询继续; 返回false将终止查询并抛出NotAllowQueryException
	 */
	boolean beforeSelect(Class<?> clazz, String sql, Object... args);
	
	/**
	 * 查询结果后处理
	 * @param clazz 查询的对象 
	 * @param result 查询结果值，对于返回值是一个的，也放入该list中。对于没有的，这里会传入空字符串
	 * @param count 当查询总数或有分页总数时，该数有值
	 * @return DBHelper会使用返回值作为新的查询结果值，因此，没修改时请务必将result返回
	 */
	<T> List<T> afterSelect(Class<T> clazz, List<T> result, int count);
	
}