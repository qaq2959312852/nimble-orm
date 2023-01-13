package com.pugwoo.dbhelper;

import com.pugwoo.dbhelper.enums.FeatureEnum;
import com.pugwoo.dbhelper.exception.MustProvideConstructorException;
import com.pugwoo.dbhelper.exception.NullKeyValueException;
import com.pugwoo.dbhelper.impl.DBHelperContext;
import com.pugwoo.dbhelper.model.PageData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 2015年8月17日 18:18:57
 * @author pugwoo
 */
public interface DBHelper {

	// =============== Dynamic Table Name ===================================

	/**
	 * 为指定的类设置表名，适合于分表场景；该设置对所有DBHelper实例生效，但仅对当前线程有效。<br>
	 * 【特别注意】设置的信息存储在线程上下文中，因此需要线程模型支持，然后设置完之后要记得调用resetTableNames清除设置。
	 * @param clazz 要替换表名的注解了@Table的类
	 * @param tableName 新的表名
	 */
	static <T> void setTableName(Class<T> clazz, String tableName) {
		DBHelperContext.setTableName(clazz, tableName);
	}

	/**
	 * 清除setTableName设置的表名信息；该设置对所有DBHelper实例生效，但仅对当前线程有效。<br>
	 */
	static void resetTableNames() {
		DBHelperContext.resetTableName();
	}

	// ================ Disable soft delete ================================

	/**
	 * 关闭指定类的软删除设置，关闭后，无论该类是否注解了软删除，都等价于没有注解。<br>
	 * 该设置对所有DBHelper实例生效，但仅对当前线程有效，一般执行完逻辑之后，需要再调用turnOnSoftDelete打开。<br>
	 * 如果需要永久性的移除软删除，可以使用两个DO类描述同一张表，一个DO类是软删除，一个DO类是硬删除。
	 * @param clazz 注解了@Table的类
	 */
	static void turnOffSoftDelete(Class<?>... clazz) {
		DBHelperContext.turnOffSoftDelete(clazz);
	}

	/**
	 * 打开指定类的软删除设置，如果没有调用过turnOffSoftDelete，则不需要调用turnOnSoftDelete。<br>
	 * 该设置对所有DBHelper实例生效，但仅对当前线程有效。
	 * @param clazz 注解了@Table的类
	 */
	static void turnOnSoftDelete(Class<?>... clazz) {
		DBHelperContext.turnOnSoftDelete(clazz);
	}

	// ================= Set SQL comment ==================================

	/**
	 * 设置全局的SQL注释，设置后每条执行的SQL都将自动带上该注释到数据库中执行。<br>
	 * 说明：该方法会对所有的DBHelper实例生效。
	 * @param comment SQL注释（不需要加注释的标识），空字符串为清空
	 */
	static void setGlobalComment(String comment) {
		DBHelperContext.setGlobalComment(comment);
	}

	/**
	 * 设置线程上下文的SQL注释，设置后当前线程执行的每条SQL都将自动带上该注释到数据库中执行。<br>
	 * 说明：该方法会对所有的DBHelper实例生效。
	 * @param comment SQL注释（不需要加注释的标识），空字符串为清空
	 */
	static void setLocalComment(String comment) {
		DBHelperContext.setThreadLocalComment(comment);
	}

	// =====================================================================
	
	/**
	 * 手动回滚@Transactional的事务。
	 * 对于已知需要回滚的动作，我更推荐主动调用让其回滚，而非抛出RuntimeException。
	 * 当前如果没有事务而调用该方法，将抛出org.springframework.transaction.NoTransactionException。<br><br>
	 * 
	 * 对于手动回滚事务rollback抛出`org.springframework.transaction.UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only`
	 * 异常的情况，需要使用者保证满足以下两点中的任意一点：<br>
	 * 1) 调用rollback的代码所在的方法是注解了@Transactional最外层的方法；<br>
	 * 2) 调用rollback的代码最近的@Transactional注解加上propagation = Propagation.NESTED属性。<br>
	 */
	void rollback();
	
	/**
	 * 当前事务提交后执行
	 * @param runnable 执行的回调方法
	 * @return 当提交成功返回true，提交失败返回false；如果当前不在事务中时，返回false
	 */
	boolean executeAfterCommit(Runnable runnable);
	
	/**
	 * 设置SQL执行超时的WARN log，超时时间默认为1秒
	 * @param timeMS 毫秒
	 */
	void setTimeoutWarningValve(long timeMS);
	
	/**
	 * 设置允许的每页最大的个数，当页数超过允许的最大页数时，设置为最大页数。
	 * 默认对每页最大个数没有限制，该限制只对getPage和getPageWithoutCount接口生效。
	 * @param maxPageSize 允许的每页最大的个数
	 */
	void setMaxPageSize(int maxPageSize);

	/**
	 * 对于流式Stream获取数据的，可以指定fetchSize大小，默认1000
	 * @param fetchSize fetchSize
	 */
	void setFetchSize(int fetchSize);
	
	/**
	 * 设置SQL执行超时回调，可用于自行实现将慢sql存放到db
	 * @param callback 执行的回调方法
	 */
	void setTimeoutWarningCallback(IDBHelperSlowSqlCallback callback);
	
	/**
	 * 数据库拦截器
	 * @param interceptors 拦截器列表，全量更新
	 */
	void setInterceptors(List<DBHelperInterceptor> interceptors);

	// =============== Turn-on Or Turn-off features

	/**
	 * 开启某个特性
	 * @param featureEnum 特性枚举，默认特性是否开启详见特性文档说明
	 */
	void turnOnFeature(FeatureEnum featureEnum);

	/**
	 * 关闭某个特性
	 * @param featureEnum 特性枚举，默认特性是否开启详见特性文档说明
	 */
	void turnOffFeature(FeatureEnum featureEnum);

	// =============== Query methods START ==================================
	
	/**
	 * 通过T的主键，将数据查出来并设置到T中<br>
	 * 【会自动处理软删除记录】
	 * 
	 * @param t 值设置在t中
	 * @return 存在返回true，否则返回false
	 */
	<T> boolean getByKey(T t) throws NullKeyValueException;
	
	/**
	 * 适合于只有一个Key的情况<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 查询的DO的类class
	 * @param keyValue 查询的主键key值
	 * @return 如果不存在则返回null
	 */
    <T> T getByKey(Class<T> clazz, Object keyValue) throws NullKeyValueException;
    
    /**
     * 通过多个key查询对象<br>
     * 【会自动处理软删除记录】
     * @param clazz 查询的DO的类class
     * @param keyValues 查询的主键key值列表
     * @return 返回的值是LinkedHashMap对象，按照keyValues的顺序来，但如果key不存在，那么不会再返回值的map key中
     */
    <T, K> Map<K, T> getByKeyList(Class<T> clazz, Collection<K> keyValues);
	
	/**
	 * 查询列表，没有查询条件<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param page 从1开始
	 * @param pageSize 每页查询个数
	 * @return 返回的data不会是null
	 */
	<T> PageData<T> getPage(Class<T> clazz, int page, int pageSize);

	/**
	 * 查询列表，postSql可以带查询条件<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param page 从1开始
	 * @param pageSize 每页查询个数
	 * @param postSql 包含where关键字起的后续SQL语句，【不能】包含limit子句
	 * @param args postSql中的参数列表
	 * @return 返回的data不会是null
	 */
	<T> PageData<T> getPage(Class<T> clazz, int page, int pageSize,
			String postSql, Object... args);

	/**
	 * 计算总数<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @return 总数
	 */
	<T> long getCount(Class<T> clazz);
	
	/**
	 * 计算总数<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param postSql 包含where关键字起的后续SQL语句
	 * @param args postSql中的参数列表
	 * @return 总数
	 */
	<T> long getCount(Class<T> clazz, String postSql, Object... args);
	
	/**
	 * 查询列表，没有查询条件；不查询总数<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param page 从1开始
	 * @param pageSize 每页查询个数
	 * @return 返回的data不会是null
	 */
	<T> PageData<T> getPageWithoutCount(Class<T> clazz, int page, int pageSize);
	
	/**
	 * 查询列表，postSql可以带查询条件；不查询总数<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param page 从1开始
	 * @param pageSize 每页查询个数
	 * @param postSql 包含where关键字起的后续SQL语句
	 * @return 返回的data不会是null
	 */
	<T> PageData<T> getPageWithoutCount(Class<T> clazz, int page, int pageSize,
			String postSql, Object... args);
	
	/**
	 * 查询列表，查询所有记录，如果数据量大请慎用<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @return 返回不会是null
	 */
	<T> List<T> getAll(Class<T> clazz);

	/**
	 * 查询列表，查询所有记录，以Stream形式返回<br>
	 * 【特别注意】为了确保Stream方式查询有效，请确保jdbc的URL参数带上：useCursorFetch=true<br>
	 * 说明：可以通过setFetchSize方法修改流式获取数据时，每次获取的数据条数<br>
	 * 说明：Stream方式不会调用拦截器的afterQuery<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @return 返回不会是null
	 */
	<T> Stream<T> getAllForStream(Class<T> clazz);
	
	/**
	 * 查询列表，查询所有记录，postSql指定查询where及order by limit等后续语句。<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param postSql where及后续语句，可包含order by,group by,limit等语句
	 * @return 返回不会是null
	 */
	<T> List<T> getAll(Class<T> clazz, String postSql, Object... args);

	/**
	 * 查询列表，查询所有记录，postSql指定查询where及order by limit等后续语句。以Stream形式返回<br>
	 * 【特别注意】为了确保Stream方式查询有效，请确保jdbc的URL参数带上：useCursorFetch=true<br>
	 * 说明：可以通过setFetchSize方法修改流式获取数据时，每次获取的数据条数<br>
	 *  说明：Stream方式不会调用拦截器的afterQuery<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param postSql where及后续语句，可包含order by,group by,limit等语句
	 * @return 返回不会是null
	 */
	<T> Stream<T> getAllForStream(Class<T> clazz, String postSql, Object... args);

	/**
	 * 查询列表，但只查询主键出来，postSql指定查询where及order by limit等后续语句。<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @param postSql where及后续语句，可包含order by,group by,limit等语句
	 * @return 返回不会是null
	 */
	<T> List<T> getAllKey(Class<T> clazz, String postSql, Object... args);

	/**
	 * 查询一条记录，如果有多条，也只返回第一条。该方法适合于知道返回值只有一条记录的情况。<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 【-支持@JoinTable-】
	 * @return 如果不存在则返回null
	 */
	<T> T getOne(Class<T> clazz);
	
	/**
	 * 查询一条记录，如果有多条，也只返回第一条。该方法适合于知道返回值只有一条记录的情况。<br>
	 * 【会自动处理软删除记录】
	 * @param clazz 查询的DO类【-支持@JoinTable-】
	 * @param postSql where及后续语句，可包含order by,group by等语句
	 * @param args postSql中的参数列表
	 * @return 如果不存在则返回null
	 */
	<T> T getOne(Class<T> clazz, String postSql, Object... args);

	/**
	 * 执行自行指定的SQL查询语句
	 *
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等;
	 *              特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL
	 * @param args 自定义参数
	 */
	<T> List<T> getRaw(Class<T> clazz, String sql, Object... args);

	/**
	 * 执行自行指定的SQL查询语句，支持通过namedParameter的方式传入参数，放到args里面
	 *
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等;
	 *              特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL，参数用namedParameter的方式
	 * @param args 自定义参数
	 */
	<T> List<T> getRaw(Class<T> clazz, String sql, Map<String, Object> args);

	/**
	 * 执行自行指定的SQL查询语句，以流Stream的形式返回。<br>
	 * 【特别注意】为了确保Stream方式查询有效，请确保jdbc的URL参数带上：useCursorFetch=true<br>
	 * 说明：可以通过setFetchSize方法修改流式获取数据时，每次获取的数据条数<br>
	 * 说明：Stream方式不会调用拦截器的afterQuery<br>
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等; 特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL
	 * @param args 自定义参数
	 */
	<T> Stream<T> getRawForStream(Class<T> clazz, String sql, Object... args);

	/**
	 * 执行自行指定的SQL查询语句，支持通过namedParameter的方式传入参数，放到args里面，以流Stream的形式返回。<br>
	 * 【特别注意】为了确保Stream方式查询有效，请确保jdbc的URL参数带上：useCursorFetch=true<br>
	 * 说明：可以通过setFetchSize方法修改流式获取数据时，每次获取的数据条数<br>
	 * 说明：Stream方式不会调用拦截器的afterQuery<br>
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等; 特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL
	 * @param args 自定义参数
	 */
	<T> Stream<T> getRawForStream(Class<T> clazz, String sql, Map<String, Object> args);

	/**
	 * 执行自行指定的SQL查询语句，只返回第一行
	 * （特别说明，当有多行时也只返回第一行而不会失败或告警）
	 *
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等;
	 *              特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL
	 * @param args 自定义参数
	 */
	<T> T getRawOne(Class<T> clazz, String sql, Object... args);

	/**
	 * 执行自行指定的SQL查询语句，支持通过namedParameter的方式传入参数，放到args里面，只返回第一行
	 * （特别说明，当有多行时也只返回第一行而不会失败或告警）
	 *
	 * @param clazz 转换回来的DO类，也支持关联查询后处理；支持基本类型如Integer/Long/String等;
	 *              特别说明，对于Long和Integer，如果数据库返回的是null，由于ResultSet的getInt会返回0，所以这里也返回0
	 * @param sql 自定义SQL，参数用namedParameter的方式
	 * @param args 自定义参数
	 */
	<T> T getRawOne(Class<T> clazz, String sql, Map<String, Object> args);

	/**
	 * 根据给定的对象t查询跟t的非null值完全相等的记录。
	 * 因为很容易出现当t的全部属性全为null时，把整个表都查询出来的问题，特要求调用者给定limit参数，该参数为返回的最大条目数。
	 * @param t 提供查询条件的对象t
	 * @param limit 限制查询的最大条目数
	 */
	<T> List<T> getByExample(T t, int limit);

	/**
	 * 是否出现至少一条记录
	 * @param clazz 查询的DO类
	 * @param postSql 不能有limit子句
	 * @param args postSql中的参数列表
	 * @return 如果存在则返回true，否则返回false
	 */
	<T> boolean isExist(Class<T> clazz, String postSql, Object... args);
	
	/**
	 * 是否出现至少N条记录(含N条)
	 * @param atLeastCounts 至少有N条记录（isExist方法等级于atLeastCounts=1）
	 * @param clazz 查询的DO类
	 * @param postSql 不能有limit子句
	 * @param args postSql中的参数列表
	 * @return 如果存在则返回true，否则返回false
	 */
	<T> boolean isExistAtLeast(int atLeastCounts, Class<T> clazz, String postSql, Object... args);
	
	/**
	 * 单独抽离出处理RelatedColumn的类，参数t不需要@Table的注解了
	 * @param t 需要处理RelatedColumn的对象
	 */
	<T> void handleRelatedColumn(T t);
	
	/**
	 * 单独抽离出处理RelatedColumn的类，参数list的元素不需要@Table的注解了。但要求list都同一class类型的对象。
	 * @param list 需要处理RelatedColumn的对象列表
	 */
	<T> void handleRelatedColumn(List<T> list);
	
	/**
	 * 单独抽离出处理RelatedColumn的类，参数t不需要@Table的注解了
	 * @param t 需要处理RelatedColumn的对象
	 * @param relatedColumnProperties 只处理制定的这些RelatedColumn注解的成员变量，这个的值是成员变量的名称
	 */
	<T> void handleRelatedColumn(T t, String... relatedColumnProperties);
	
	/**
	 * 单独抽离出处理RelatedColumn的类，参数list的元素不需要@Table的注解了。但要求list都同一class类型的对象。
	 * @param list 需要处理RelatedColumn的对象列表
	 * @param relatedColumnProperties 只处理制定的这些RelatedColumn注解的成员变量，这个的值是成员变量的名称
	 */
	<T> void handleRelatedColumn(List<T> list, String... relatedColumnProperties);
	
	// ===============Query methods END ==================================
	
	/**
	 * 插入一条记录<br>
	 * 如果包含了自增id，则自增Id会被设置。<br>
	 * 【注】只插入非null的值，如要需要插入null值，则用insertWithNull。
	 * @param t 需要插入的DO对象实例
	 * @return 实际修改的条数
	 */
	<T> int insert(T t);
	
	/**
	 * 批量插入多条记录，返回数据库实际修改的条数。<br>
	 * 【说明】该方法没有事务，请在外层加事务。<br>
	 * 【特别说明】因为需要获取自增主键，对于有自增id的插入，会降级为逐条插入，如果想提升性能，请使用insertBatchWithoutReturnId方法。
	 * @param list 需要插入的DO对象实例列表
	 * @return 实际修改的条数
	 */
	int insert(Collection<?> list);

	/**
	 * 批量插入多条记录，返回数据库实际修改的条数。<br>
	 * <br>
	 * 【重点】不会自动设置插入数据的返回自增ID（拦截器里也拿不到ID），也不会设置回数据库的默认值<br>
	 * @param list 需要插入的DO对象实例列表
	 * @return 实际修改的条数
	 */
	<T> int insertBatchWithoutReturnId(Collection<T> list);

	/**
	 * 插入一条记录，返回数据库实际修改条数。<br>
	 * 如果包含了自增id，则自增Id会被设置。
	 * @param t 需要插入的DO对象实例
	 * @return 实际修改的条数
	 */
	<T> int insertWithNull(T t);

	/**
	 * 如果t有主键，则更新值；否则插入记录。只有非null的值会更新或插入。
	 * @param t 需要插入的DO对象实例
	 * @return 返回数据库实际修改的条数
	 */
	<T> int insertOrUpdate(T t);
	
	/**
	 * 如果t有主键，则更新值；否则插入记录。包括null的值会更新或插入。
	 * @param t 需要插入的DO对象实例
	 * @return 返回数据库实际修改的条数
	 */
	<T> int insertOrUpdateWithNull(T t);
	
	/**
	 * 如果t有主键，则更新值；否则插入记录。只有非null的值会更新或插入。
	 * @param list 需要插入的DO对象实例列表
	 * @return 返回数据库实际修改的条数
	 */
	<T> int insertOrUpdate(Collection<T> list);

	/**
	 * 更新单个实例数据库记录，必须带上object的key，包含更新null值的字段
	 * @param t 更新的对象实例
	 * @return 返回数据库实际修改条数
	 * @throws NullKeyValueException 当对象t的主键值为null时抛出
	 */
	<T> int updateWithNull(T t) throws NullKeyValueException;
	
	/**
	 * 带条件的更新单个对象，必须带上object的key，主要用于mysql的update ... where ...这样的CAS修改
	 * 
	 * @param t 更新的对象实例
	 * @param postSql where及后续的sql，包含where关键字
	 * @param args postSql中的参数
	 * @return 返回数据库实际修改条数
	 * @throws NullKeyValueException 当对象t的主键值为null时抛出
	 */
	<T> int updateWithNull(T t, String postSql, Object... args) throws NullKeyValueException;
	
	/**
	 * 更新单条数据库记录,必须带上object的key。【只更新非null字段】
	 * @param t 更新的对象实例
	 * @return 返回数据库实际修改条数
	 * @throws NullKeyValueException 当对象t的主键值为null时抛出
	 */
	<T> int update(T t) throws NullKeyValueException;
	
	/**
	 * 更新单条数据库记录,必须带上object的key，主要用于mysql的update ... where ...这样的CAS修改。
	 * 【只更新非null字段】
	 * 
	 * @param t 更新的对象实例
	 * @param postSql where及后续的sql，包含where关键字
	 * @param args postSql中的参数
	 * @return 返回数据库实际修改条数
	 * @throws NullKeyValueException 当对象t的主键值为null时抛出
	 */
	<T> int update(T t, String postSql, Object... args) throws NullKeyValueException;
	
	/**
	 * 自定义set字句更新，用于单个sql进行值更新，例如set reads = reads + 1这种情况。
	 * @param t 必须提供key
	 * @param setSql 可包含set关键字也可不包含，多个则用逗号隔开，【不能】包含where子句，例如a=a+1,c=b 或 set a=a+1,c=b
	 * @param args set子句的参数
	 * @return 实际修改的条数
	 * @throws NullKeyValueException 当t没有带上key时，抛出该异常
	 */
	<T> int updateCustom(T t, String setSql, Object... args) throws NullKeyValueException;
	
	/**
	 * 自定义更新多行记录，会自动去掉已软删除的行。
	 *
	 * 【重要更新 since 1.0.0】该方法修改的行，不会再调用afterUpdate方法，如果需要获得被修改的行记录，请考虑使用canal方案。
	 *
	 * @param clazz 要更新的DO类
	 * @param setSql update sql中的set sql子句，可以包括set关键字也可以不包括
	 * @param whereSql update sql中的where sql子句，可以包括where关键字也可以不包括
	 * @param args whereSql中的参数
	 * @return 实际修改条数
	 */
	<T> int updateAll(Class<T> clazz, String setSql, String whereSql, Object... args);

	/**
	 * 更新数据库记录，返回数据库实际修改条数。
	 * 【注】批量更新的方法并不会比程序中循环调用int update(T t)更快
	 * 【只更新非null字段】
	 * @param list 要更新的对象列表
	 * @return 实际修改条数
	 * @throws NullKeyValueException 当对象列表中的对象的主键值为null时抛出
	 */
	<T> int update(Collection<T> list) throws NullKeyValueException;
	
	/**
	 * 删除数据库记录，返回数据库实际修改条数。
	 * 该操作【会】自动使用软删除进行删除
	 * 
	 * @param t 要更新的对象
	 * @return 实际删除的条数
	 */
	<T> int deleteByKey(T t) throws NullKeyValueException;
	
	/**
	 * 删除数据库记录，返回数据库实际修改条数。
	 * 推荐使用单个主键的表使用该方法，当list所有对象都是同一个类时，将会拼凑为一条sql进行删除，效率提升多。
	 * 该操作【会】自动使用软删除进行删除
	 * @param list 要更新的对象列表
	 * @return 实际删除的条数
	 * @throws NullKeyValueException 当任意一个值没有带key时，抛出异常
	 */
	<T> int deleteByKey(Collection<T> list) throws NullKeyValueException;
	
	/**
	 * 删除数据库记录，返回实际修改数据库条数，这个接口只支持单个字段是key的情况。
	 * 该操作【会】自动使用软删除进行删除
	 * 
	 * @param clazz 必须有默认构造方法
	 * @param keyValue 要删除的对象的主键值
	 * @return 实际删除的条数
	 * @throws NullKeyValueException 当keyValue为null时，抛出异常
	 */
	<T> int deleteByKey(Class<T> clazz, Object keyValue) throws NullKeyValueException,
	    MustProvideConstructorException;

	/**
	 * 自定义条件删除数据，该操作【会】自动使用软删除标记。
	 * 对于使用了拦截器和deleteValueScript的场景，该方法的实现是先根据条件查出数据，再批量删除，以便拦截器可以记录下实际被删的数据，此时删除性能可能比较差，请权衡使用。
	 * @param clazz 必须有默认构造方法
	 * @param postSql 必须提供，必须写where，【不允许】留空
	 * @param args postSql的参数
	 * @return 实际删除的条数
	 */
	<T> int delete(Class<T> clazz, String postSql, Object... args);

	/**
	 * 执行自行指定的SQL语句，支持in(?)表达式，支持INSERT UPDATE DELETE TRUNCATE操作
	 *
	 * @param sql 自定义SQL
	 * @param args 自定义参数
	 * @return 返回影响的行数
	 */
	int executeRaw(String sql, Object... args);

	/**
	 * 执行自行指定的SQL语句，支持通过namedParameter的方式传入参数，支持in(?)表达式，支持INSERT UPDATE DELETE TRUNCATE操作
	 *
	 * @param sql 自定义SQL，参数用namedParameter的方式
	 * @param paramMap 自定义参数
	 * @return 返回影响的行数
	 */
	int executeRaw(String sql, Map<String, Object> paramMap);

}
