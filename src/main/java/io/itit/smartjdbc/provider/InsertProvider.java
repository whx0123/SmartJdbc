package io.itit.smartjdbc.provider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.itit.smartjdbc.SmartJdbcException;
import io.itit.smartjdbc.SqlBean;
import io.itit.smartjdbc.annotations.DomainField;
import io.itit.smartjdbc.annotations.NonPersistent;
import io.itit.smartjdbc.util.JSONUtil;

/**
 * 
 * @author skydu
 *
 */
public class InsertProvider extends SqlProvider{
	//
	Object bean;
	String[] excludeProperties;
	//
	public InsertProvider(Object bean,String ... excludeProperties) {
		this.bean=bean;
		this.excludeProperties=excludeProperties;
	}
	
	@Override
	public SqlBean build() {
		StringBuilder sql=new StringBuilder();
		Class<?>type=bean.getClass();
		checkExcludeProperties(excludeProperties,type);
		String tableName=getTableName(type);
		sql.append("insert into ").append(tableName).append("(");
		Set<String> excludesNames = new TreeSet<String>();
		for (String e : excludeProperties) {
			excludesNames.add(e);
		}
		List<Object>fieldList=new ArrayList<Object>();
		List<Field> list=getPersistentFields(type);
		for (Field f : list) {
			if (excludesNames.contains(f.getName())) {
				continue;
			}
			NonPersistent nonPersistent=f.getAnnotation(NonPersistent.class);
			if(nonPersistent!=null) {
				continue;
			}
			DomainField domainField=f.getAnnotation(DomainField.class);
			if(domainField!=null&&domainField.autoIncrement()) {
				continue;
			}
			String fieldName = convertFieldName(f.getName());
			try {
				Object fieldValue=f.get(bean);
				if(fieldValue!=null&&!WRAP_TYPES.contains(fieldValue.getClass())){
					fieldList.add(JSONUtil.toJson(fieldValue));
				}else{
					fieldList.add(fieldValue);
				}
			} catch (Exception e) {
				throw new SmartJdbcException(e);
			}
			sql.append("`").append(fieldName).append("`,");
		}
		sql.deleteCharAt(sql.length()-1);
		sql.append(")");
		sql.append("values(");
		for(int i=0;i<fieldList.size();i++){
			sql.append("?,");
		}
		sql.deleteCharAt(sql.length()-1);
		sql.append(")");
		//
		return createSqlBean(sql.toString(),fieldList.toArray(new Object[fieldList.size()]));
	}

}
