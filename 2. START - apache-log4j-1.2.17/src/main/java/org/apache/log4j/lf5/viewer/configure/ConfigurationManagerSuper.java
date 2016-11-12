package org.apache.log4j.lf5.viewer.configure;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.tree.TreePath;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.viewer.LogTableColumn;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerTree;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryNode;

public class ConfigurationManagerSuper extends Object {

	protected static final String NAME = "name";
	protected static final String PATH = "path";
	protected static final String SELECTED = "selected";
	protected static final String EXPANDED = "expanded";
	protected static final String CATEGORY = "category";
	protected static final String LEVEL = "level";
	protected static final String COLORLEVEL = "colorlevel";
	protected static final String RED = "red";
	protected static final String GREEN = "green";
	protected static final String BLUE = "blue";
	protected static final String COLUMN = "column";
	public ConfigurationManagerData data = new ConfigurationManagerData(null, null);

	public static String treePathToString(TreePath path) {
	    // count begins at one so as to not include the 'Categories' - root category
	    StringBuffer sb = new StringBuffer();
	    CategoryNode n = null;
	    Object[] objects = path.getPath();
	    for (int i = 1; i < objects.length; i++) {
	      n = (CategoryNode) objects[i];
	      if (i > 1) {
	        sb.append(".");
	      }
	      sb.append(n.getTitle());
	    }
	    return sb.toString();
	  }

	public ConfigurationManagerSuper() {
		super();
	}

	protected void processLogLevels(Map logLevelMenuItems, StringBuffer xml) {
	    xml.append("\t<loglevels>\r\n");
	    Iterator it = logLevelMenuItems.keySet().iterator();
	    while (it.hasNext()) {
	      LogLevel level = (LogLevel) it.next();
	      JCheckBoxMenuItem item = (JCheckBoxMenuItem) logLevelMenuItems.get(level);
	      exportLogLevelXMLElement(level.getLabel(), item.isSelected(), xml);
	    }
	
	    xml.append("\t</loglevels>\r\n");
	  }

	protected void processLogLevelColors(Map logLevelMenuItems, Map logLevelColors, StringBuffer xml) {
	    xml.append("\t<loglevelcolors>\r\n");
	    // iterate through the list of log levels being used (log4j, jdk1.4, custom levels)
	    Iterator it = logLevelMenuItems.keySet().iterator();
	    while (it.hasNext()) {
	      LogLevel level = (LogLevel) it.next();
	      // for each level, get the associated color from the log level color map
	      Color color = (Color) logLevelColors.get(level);
	      exportLogLevelColorXMLElement(level.getLabel(), color, xml);
	    }
	
	    xml.append("\t</loglevelcolors>\r\n");
	  }

	protected void processLogTableColumns(List logTableColumnMenuItems, StringBuffer xml) {
	    xml.append("\t<logtablecolumns>\r\n");
	    Iterator it = logTableColumnMenuItems.iterator();
	    while (it.hasNext()) {
	      LogTableColumn column = (LogTableColumn) it.next();
	      JCheckBoxMenuItem item = data._monitor.getTableColumnMenuItem(column);
	      exportLogTableColumnXMLElement(column.getLabel(), item.isSelected(), xml);
	    }
	
	    xml.append("\t</logtablecolumns>\r\n");
	  }

	protected void exportXMLElement(CategoryNode node, TreePath path, StringBuffer xml) {
	    CategoryExplorerTree tree = data._monitor.getCategoryExplorerTree();
	
	    xml.append("\t<").append(CATEGORY).append(" ");
	    xml.append(NAME).append("=\"").append(node.getTitle()).append("\" ");
	    xml.append(PATH).append("=\"").append(treePathToString(path)).append("\" ");
	    xml.append(EXPANDED).append("=\"").append(tree.isExpanded(path)).append("\" ");
	    xml.append(SELECTED).append("=\"").append(node.isSelected()).append("\"/>\r\n");
	  }

	private void exportLogLevelXMLElement(String label, boolean selected, StringBuffer xml) {
	    xml.append("\t\t<").append(LEVEL).append(" ").append(NAME);
	    xml.append("=\"").append(label).append("\" ");
	    xml.append(SELECTED).append("=\"").append(selected);
	    xml.append("\"/>\r\n");
	  }

	private void exportLogLevelColorXMLElement(String label, Color color, StringBuffer xml) {
	    xml.append("\t\t<").append(COLORLEVEL).append(" ").append(NAME);
	    xml.append("=\"").append(label).append("\" ");
	    xml.append(RED).append("=\"").append(color.getRed()).append("\" ");
	    xml.append(GREEN).append("=\"").append(color.getGreen()).append("\" ");
	    xml.append(BLUE).append("=\"").append(color.getBlue());
	    xml.append("\"/>\r\n");
	  }

	private void exportLogTableColumnXMLElement(String label, boolean selected, StringBuffer xml) {
	    xml.append("\t\t<").append(COLUMN).append(" ").append(NAME);
	    xml.append("=\"").append(label).append("\" ");
	    xml.append(SELECTED).append("=\"").append(selected);
	    xml.append("\"/>\r\n");
	  }
	  //--------------------------------------------------------------------------
	  //   Nested Top-Level Classes or Interfaces:
	  //--------------------------------------------------------------------------

}