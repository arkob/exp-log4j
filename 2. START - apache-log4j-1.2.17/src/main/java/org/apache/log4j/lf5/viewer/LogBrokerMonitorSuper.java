package org.apache.log4j.lf5.viewer;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.lf5.LogRecordFilter;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerTree;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryPath;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by Armin on 10/29/2016.
 */
public class LogBrokerMonitorSuper {
  protected LogTable _table;
  protected CategoryExplorerTree _categoryExplorerTree;
  protected String _NDCTextFilter = "";
  protected JLabel _statusLabel;
  protected Map _logLevelMenuItems = new HashMap();
  protected Map _logTableColumnMenuItems = new HashMap();
  protected List _columns = null;

  public LogBrokerMonitorSuper() {
    _columns = LogTableColumn.getLogTableColumns();
  }

  // Added in version 1.2 - sets the NDC Filter based on
  // a String passed in by the user.  This value is persisted
  // in the XML Configuration file.
  public void setNDCLogRecordFilter(String textFilter) {
    _table.getFilteredLogTableModel().
        setLogRecordFilter(createNDCLogRecordFilter(textFilter));
  }

  // Added in version 1.2 - Uses a different filter that sorts
  // based on an NDC string passed in by the user.  If the string
  // is null or is an empty string, we do nothing.
  protected void sortByNDC() {
    String text = _NDCTextFilter;
    if (text == null || text.length() == 0) {
      return;
    }

    // Use new NDC filter
    _table.getFilteredLogTableModel().
        setLogRecordFilter(createNDCLogRecordFilter(text));
  }

  // Added in version 1.2 - Creates a new filter that sorts records based on
  // an NDC string passed in by the user.
  protected LogRecordFilter createNDCLogRecordFilter(String text) {
    _NDCTextFilter = text;
    LogRecordFilter result = new LogRecordFilter() {
      public boolean passes(LogRecord record) {
        String NDC = record.getNDC();
        CategoryPath path = new CategoryPath(record.getCategory());
        if (NDC == null || _NDCTextFilter == null) {
          return false;
        } else if (NDC.toLowerCase().indexOf(_NDCTextFilter.toLowerCase()) == -1) {
          return false;
        } else {
          return getMenuItem(record.getLevel()).isSelected() &&
              _categoryExplorerTree.getExplorerModel().isCategoryPathActive(path);
        }
      }
    };

    return result;
  }

  protected void updateStatusLabel() {
    _statusLabel.setText(getRecordsDisplayedMessage());
  }

  protected String getRecordsDisplayedMessage() {
    FilteredLogTableModel model = _table.getFilteredLogTableModel();
    return getStatusText(model.getRowCount(), model.getTotalRowCount());
  }

  protected String getStatusText(int displayedRows, int totalRows) {
    StringBuffer result = new StringBuffer();
    result.append("Displaying: ");
    result.append(displayedRows);
    result.append(" records out of a total of: ");
    result.append(totalRows);
    result.append(" records.");
    return result.toString();
  }

  protected JCheckBoxMenuItem getMenuItem(LogLevel level) {
    JCheckBoxMenuItem result = (JCheckBoxMenuItem) (_logLevelMenuItems.get(level));
    if (result == null) {
      result = createMenuItem(level);
      _logLevelMenuItems.put(level, result);
    }
    return result;
  }

  protected JCheckBoxMenuItem createMenuItem(LogLevel level) {
    JCheckBoxMenuItem result = new JCheckBoxMenuItem(level.toString());
    result.setSelected(true);
    result.setMnemonic(level.toString().charAt(0));
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _table.getFilteredLogTableModel().refresh();
        updateStatusLabel();
      }
    });
    return result;
  }

  protected JCheckBoxMenuItem getLogTableColumnMenuItem(LogTableColumn column) {
    JCheckBoxMenuItem result = (JCheckBoxMenuItem) (_logTableColumnMenuItems.get(column));
    if (result == null) {
      result = createLogTableColumnMenuItem(column);
      _logTableColumnMenuItems.put(column, result);
    }
    return result;
  }

  protected JCheckBoxMenuItem createLogTableColumnMenuItem(LogTableColumn column) {
    JCheckBoxMenuItem result = new JCheckBoxMenuItem(column.toString());

    result.setSelected(true);
    result.setMnemonic(column.toString().charAt(0));
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // update list of columns and reset the view
        List selectedColumns = updateView();
        _table.setView(selectedColumns);
      }
    });
    return result;
  }

  protected List updateView() {
    ArrayList updatedList = new ArrayList();
    Iterator columnIterator = _columns.iterator();
    while (columnIterator.hasNext()) {
      LogTableColumn column = (LogTableColumn) columnIterator.next();
      JCheckBoxMenuItem result = getLogTableColumnMenuItem(column);
      // check and see if the checkbox is checked
      if (result.isSelected()) {
        updatedList.add(column);
      }
    }

    return updatedList;
  }

  protected JMenuItem createNoLogTableColumnsMenuItem() {
    JMenuItem result = new JMenuItem("Hide all Columns");
    result.setMnemonic('h');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectAllLogTableColumns(false);
        // update list of columns and reset the view
        List selectedColumns = updateView();
        _table.setView(selectedColumns);
      }
    });
    return result;
  }

  protected void selectAllLogTableColumns(boolean selected) {
    Iterator columns = getLogTableColumns();
    while (columns.hasNext()) {
      getLogTableColumnMenuItem((LogTableColumn) columns.next()).setSelected(selected);
    }
  }

  protected Iterator getLogTableColumns() {
    return _columns.iterator();
  }
}
