package org.apache.log4j.lf5.viewer.configure;

import java.util.Enumeration;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
import org.apache.log4j.lf5.viewer.LogTable;
import org.apache.log4j.lf5.viewer.LogTableColumn;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerModel;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryNode;

public class ConfigurationManagerData {
	public LogBrokerMonitor _monitor;
	public LogTable _table;

	public ConfigurationManagerData(LogBrokerMonitor _monitor, LogTable _table) {
		this._monitor = _monitor;
		this._table = _table;
	}

	public void save(ConfigurationManager configurationManager) {
	    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();
	    CategoryNode root = model.getRootCategoryNode();
	
	    StringBuffer xml = new StringBuffer(2048);
	    configurationManager.openXMLDocument(xml);
	    configurationManager.openConfigurationXML(xml);
	    configurationManager.processLogRecordFilter(_monitor.getNDCTextFilter(), xml);
	    configurationManager.processLogLevels(_monitor.getLogLevelMenuItems(), xml);
	    configurationManager.processLogLevelColors(_monitor.getLogLevelMenuItems(),
	        LogLevel.getLogLevelColorMap(), xml);
	    configurationManager.processLogTableColumns(LogTableColumn.getLogTableColumns(), xml);
	    configurationManager.data.processConfigurationNode(configurationManager, root, xml);
	    configurationManager.closeConfigurationXML(xml);
	    configurationManager.store(xml.toString());
	  }

	protected void selectAllNodes(ConfigurationManager configurationManager) {
	    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();
	    CategoryNode root = model.getRootCategoryNode();
	    Enumeration all = root.breadthFirstEnumeration();
	    CategoryNode n = null;
	    while (all.hasMoreElements()) {
	      n = (CategoryNode) all.nextElement();
	      n.setSelected(true);
	    }
	  }

	protected void processConfigurationNode(ConfigurationManager configurationManager, CategoryNode node, StringBuffer xml) {
	    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();
	
	    Enumeration all = node.breadthFirstEnumeration();
	    CategoryNode n = null;
	    while (all.hasMoreElements()) {
	      n = (CategoryNode) all.nextElement();
	      configurationManager.exportXMLElement(n, model.getTreePathToRoot(n), xml);
	    }
	
	  }
}