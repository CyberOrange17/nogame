package com.nogame.mybatis.generater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;

@SuppressWarnings("deprecation")
public class GenerateUtils {
	private static final String driverClass = "com.mysql.jdbc.Driver";
	private static final String connectionURL = "jdbc:mysql://localhost/nogame";
	private static final String userName = "root";
	private static final String passwd = "";
	private static final String projectRootDirectory = "C:\\myfiles\\git\\nogame\\nogame\\nogame-primary\\";
	public static final String projectEntityDirectory = "C:\\myfiles\\git\\nogame\\nogame\\nogame-entity\\";
	private static final String projectBasePackageName = "com.oristartech.cim";
	private static final String[] generateTable = new String[] {"primary_login_user"};
	private static Configuration configuration = null;
	static {
		configuration = new Configuration();
		configuration.setDefaultEncoding("UTF-8");
		try {
			configuration.setDirectoryForTemplateLoading(new File("C:\\myfiles\\git\\nogame\\nogame\\nogame-mapper\\src\\test\\resources\\templates"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SQLException {
		List<Table> list = getAllTableData();
		for (Table table : list) {
			generateMapperXml(table, false);
			generateBaseMapperXml(table, true);
			generateMapper(table, false);
			generateBaseMapper(table, true);
			generateEntity(table, true);
			generateService(table, false);
			generateServiceImpl(table, false);
		}
		System.out.println("generate 共 " + list.size() + " 个");
		System.out.println("generate done.");
	}

	public static void generateServiceImpl(Table table, boolean isCover) {
		String fileName = table.getJavaModuleDirectory(projectRootDirectory);
		fileName += "service/impl/";
		fileName += table.getJavaPascalName();
		fileName += "ServiceImpl.java";
		generateFile(table, fileName, "serviceImplTemplate.ftl", isCover);
	}

	public static void generateService(Table table, boolean isCover) {
		String fileName = table.getJavaModuleDirectory(projectRootDirectory);
		fileName += "service/";
		fileName += table.getJavaPascalName();
		fileName += "Service.java";
		generateFile(table, fileName, "serviceTemplate.ftl", isCover);
	}

	public static void generateEntity(Table table, boolean isCover) {
		String directory = projectEntityDirectory;
		if (directory == null || "".equals(directory)) {
			directory = projectRootDirectory;
		}
		String fileName = table.getJavaModuleDirectory(directory);
		fileName += "entity/";
		fileName += table.getJavaPascalName();
		fileName += "Entity.java";
		generateFile(table, fileName, "entityTemplate.ftl", isCover);
	}

	public static void generateBaseMapper(Table table, boolean isCover) {
		String fileName = table.getJavaModuleDirectory(projectRootDirectory);
		fileName += "mapper/base/Base";
		fileName += table.getJavaPascalName();
		fileName += "Mapper.java";
		generateFile(table, fileName, "baseMapperTemplate.ftl", isCover);
	}

	public static void generateMapper(Table table, boolean isCover) {
		String fileName = table.getJavaModuleDirectory(projectRootDirectory);
		fileName += "mapper/";
		fileName += table.getJavaPascalName();
		fileName += "Mapper.java";
		generateFile(table, fileName, "mapperTemplate.ftl", isCover);
	}

	public static void generateBaseMapperXml(Table table, boolean isCover) {
		String fileName = table.getJavaMapperXmlDirectory(projectRootDirectory);
		fileName += "base/Base";
		fileName += table.getJavaPascalName();
		fileName += "Mapper.xml";
		generateFile(table, fileName, "baseMapperXmlTemplate.ftl", isCover);
	}

	public static void generateMapperXml(Table table, boolean isCover) {
		String fileName = table.getJavaMapperXmlDirectory(projectRootDirectory);
		fileName += table.getJavaCamelName();
		fileName += "Mapper.xml";
		generateFile(table, fileName, "mapperXmlTemplate.ftl", isCover);
	}

	public static void generateFile(Table table, String fileName, String templte, boolean isCover) {
		File outFile = new File(fileName); // 生成文件的路径
		if (!isCover && outFile.exists()) {
			return;
		}
		Writer out = null;
		try {
			File path = outFile.getParentFile();
			if (!path.exists()) {
				path.mkdirs();
			}
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile)));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			configuration.getTemplate(templte).process(table, out, ObjectWrapper.BEANS_WRAPPER);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isIgnore(String tableName) {
		if (null == generateTable || generateTable.length <= 0) {
			return false;
		}
		if (null == tableName) {
			return true;
		}
		for (String name : generateTable) {
			if (null == name || name.trim().length() <= 0) {
				continue;
			}
			if (!name.trim().endsWith("_") && tableName.trim().toLowerCase().equals(name.trim().toLowerCase())) {
				return false;
			}
			if (name.trim().endsWith("_") && tableName.trim().toLowerCase().startsWith(name.trim().toLowerCase())) {
				return false;
			}
		}
		return true;
	}

	public static List<Table> getAllTableData() throws SQLException {
		List<Table> tableList = new ArrayList<>();
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet tableRs = databaseMetaData.getTables(null, "%", "%", new String[] { "TABLE" });
		while (tableRs.next()) {
			Table table = new Table();
			String tableName = tableRs.getString("TABLE_NAME");
			if (isIgnore(tableName)) {
				continue;
			}
			System.out.println("generate " + tableName);
			table.setJavaBasePackageName(projectBasePackageName);
			table.setMysqlTableName(tableName);
			table.setMysqlTableComment(tableRs.getString("REMARKS"));
			bindTableColumnData(databaseMetaData, table);
			table.buildAll();
			tableList.add(table);
		}
		return tableList;
	}

	public static void bindTableColumnData(DatabaseMetaData databaseMetaData, Table table) throws SQLException {
		ResultSet rs = databaseMetaData.getColumns(null, "%", table.getMysqlTableName(), "%");
		while (rs.next()) {
			Column column = new Column();
			column.setMysqlColumnName(rs.getString("COLUMN_NAME"));
			column.setMysqlDataType(rs.getString("TYPE_NAME"));
			column.setMysqlColumnComment(rs.getString("REMARKS"));
			table.addColumn(column);
		}
	}

	public static Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName(driverClass); // classLoader,加载对应驱动
			conn = (Connection) DriverManager.getConnection(connectionURL, userName, passwd);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}
}
