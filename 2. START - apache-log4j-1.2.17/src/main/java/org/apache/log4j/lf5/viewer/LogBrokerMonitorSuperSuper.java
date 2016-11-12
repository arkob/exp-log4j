package org.apache.log4j.lf5.viewer;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.lf5.LogRecordFilter;
import org.apache.log4j.lf5.util.LogFileParser;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerTree;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryPath;
import org.apache.log4j.lf5.viewer.configure.MRUFileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by Armin on 10/30/2016.
 */
public class LogBrokerMonitorSuperSuper extends LogBrokerMonitorSuper {
  //    public static final String STANDARD_VIEW = "Standard";
  //    public static final String COMPACT_VIEW = "Compact";
    //--------------------------------------------------------------------------
    //   Protected Variables:
    //--------------------------------------------------------------------------
    protected JFrame _logMonitorFrame;
  protected String _searchText;
  protected LogLevel _leastSevereDisplayedLogLevel = LogLevel.DEBUG;
  protected JScrollPane _logTableScrollPane;
  protected boolean _callSystemExitOnClose = false;
  protected boolean _isDisposed = false;
  protected MRUFileManager _mruFileManager = null;
  protected File _fileLocation = null;

  public LogBrokerMonitorSuperSuper() {
    super();
  }

  /**
   * Show the frame for the LogBrokerMonitor. Dispatched to the
   * swing thread.
   */
  public void show(final int delay) {
    if (_logMonitorFrame.isVisible()) {
      return;
    }
    // This request is very low priority, let other threads execute first.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Thread.yield();
        pause(delay);
        _logMonitorFrame.setVisible(true);
      }
    });
  }

  public void show() {
    show(0);
  }

  /**
   * Dispose of the frame for the LogBrokerMonitor.
   */
  public void dispose() {
    _logMonitorFrame.dispose();
    _isDisposed = true;

    if (_callSystemExitOnClose == true) {
      System.exit(0);
    }
  }

  /**
   * Set the value of whether or not System.exit() will be called
   * when the LogBrokerMonitor is closed.
   */
  public void setCallSystemExitOnClose(boolean callSystemExitOnClose) {
    _callSystemExitOnClose = callSystemExitOnClose;
  }

  public JFrame getBaseFrame() {
    return _logMonitorFrame;
  }

  public Map getLogLevelMenuItems() {
    return _logLevelMenuItems;
  }

  public Map getLogTableColumnMenuItems() {
    return _logTableColumnMenuItems;
  }

  public JCheckBoxMenuItem getTableColumnMenuItem(LogTableColumn column) {
    return getLogTableColumnMenuItem(column);
  }

  public CategoryExplorerTree getCategoryExplorerTree() {
    return _categoryExplorerTree;
  }

  protected void setSearchText(String text) {
    _searchText = text;
  }

  // Added in version 1.2 - Sets the text filter for the NDC
  protected void setNDCTextFilter(String text) {
    // if no value is set, set it to a blank string
    // otherwise use the value provided
    if (text == null) {
      _NDCTextFilter = "";
    } else {
      _NDCTextFilter = text;
    }
  }

  protected void findSearchText() {
    String text = _searchText;
    if (text == null || text.length() == 0) {
      return;
    }
    int startRow = getFirstSelectedRow();
    int foundRow = findRecord(
        startRow,
        text,
        _table.getFilteredLogTableModel().getFilteredRecords()
    );
    selectRow(foundRow);
  }

  protected int getFirstSelectedRow() {
    return _table.getSelectionModel().getMinSelectionIndex();
  }

  protected void selectRow(int foundRow) {
    if (foundRow == -1) {
      String message = _searchText + " not found.";
      JOptionPane.showMessageDialog(
          _logMonitorFrame,
          message,
          "Text not found",
          JOptionPane.INFORMATION_MESSAGE
      );
      return;
    }
    LF5SwingUtils.selectRow(foundRow, _table, _logTableScrollPane);
  }

  protected int findRecord(
      int startRow,
      String searchText,
      List records
      ) {
    if (startRow < 0) {
      startRow = 0; // start at first element if no rows are selected
    } else {
      startRow++; // start after the first selected row
    }
    int len = records.size();

    for (int i = startRow; i < len; i++) {
      if (matches((LogRecord) records.get(i), searchText)) {
        return i; // found a record
      }
    }
    // wrap around to beginning if when we reach the end with no match
    len = startRow;
    for (int i = 0; i < len; i++) {
      if (matches((LogRecord) records.get(i), searchText)) {
        return i; // found a record
      }
    }
    // nothing found
    return -1;
  }

  /**
   * Check to see if the any records contain the search string.
   * Searching now supports NDC messages and date.
   */
  protected boolean matches(LogRecord record, String text) {
    String message = record.getMessage();
    String NDC = record.getNDC();

    if (message == null && NDC == null || text == null) {
      return false;
    }
    if (message.toLowerCase().indexOf(text.toLowerCase()) == -1 &&
        NDC.toLowerCase().indexOf(text.toLowerCase()) == -1) {
      return false;
    }

    return true;
  }

  /**
   * When the fontsize of a JTextArea is changed, the word-wrapped lines
   * may become garbled.  This method clears and resets the text of the
   * text area.
   */
  protected void refresh(JTextArea textArea) {
    String text = textArea.getText();
    textArea.setText("");
    textArea.setText(text);
  }

  protected void pause(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {

    }
  }

  protected LogRecordFilter createLogRecordFilter() {
    LogRecordFilter result = new LogRecordFilter() {
      public boolean passes(LogRecord record) {
        CategoryPath path = new CategoryPath(record.getCategory());
        return
            getMenuItem(record.getLevel()).isSelected() &&
            _categoryExplorerTree.getExplorerModel().isCategoryPathActive(path);
      }
    };
    return result;
  }

  /**
   * Menu item added to allow log files to be opened with
   * the LF5 GUI.
   */
  protected JMenuItem createOpenMI() {
    JMenuItem result = new JMenuItem("Open...");
    result.setMnemonic('o');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        requestOpen();
      }
    });
    return result;
  }

  /**
   * Menu item added to allow log files loaded from a URL
   * to be opened by the LF5 GUI.
   */
  protected JMenuItem createOpenURLMI() {
    JMenuItem result = new JMenuItem("Open URL...");
    result.setMnemonic('u');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        requestOpenURL();
      }
    });
    return result;
  }

  protected JMenuItem createCloseMI() {
    JMenuItem result = new JMenuItem("Close");
    result.setMnemonic('c');
    result.setAccelerator(KeyStroke.getKeyStroke("control Q"));
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        requestClose();
      }
    });
    return result;
  }

  /**
   * Creates a Most Recently Used file list to be
   * displayed in the File menu
   */
  protected void createMRUFileListMI(JMenu menu) {

    String[] files = _mruFileManager.getMRUFileList();

    if (files != null) {
      menu.addSeparator();
      for (int i = 0; i < files.length; i++) {
        JMenuItem result = new JMenuItem((i + 1) + " " + files[i]);
        result.setMnemonic(i + 1);
        result.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            requestOpenMRU(e);
          }
        });
        menu.add(result);
      }
    }
  }

  protected JMenuItem createExitMI() {
    JMenuItem result = new JMenuItem("Exit");
    result.setMnemonic('x');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        requestExit();
      }
    });
    return result;
  }

  protected JMenu createEditMenu() {
    JMenu editMenu = new JMenu("Edit");
    editMenu.setMnemonic('e');
    editMenu.add(createEditFindMI());
    editMenu.add(createEditFindNextMI());
    editMenu.addSeparator();
    editMenu.add(createEditSortNDCMI());
    editMenu.add(createEditRestoreAllNDCMI());
    return editMenu;
  }

  protected JMenuItem createEditFindNextMI() {
    JMenuItem editFindNextMI = new JMenuItem("Find Next");
    editFindNextMI.setMnemonic('n');
    editFindNextMI.setAccelerator(KeyStroke.getKeyStroke("F3"));
    editFindNextMI.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        findSearchText();
      }
    });
    return editFindNextMI;
  }

  protected JMenuItem createEditFindMI() {
    JMenuItem editFindMI = new JMenuItem("Find");
    editFindMI.setMnemonic('f');
    editFindMI.setAccelerator(KeyStroke.getKeyStroke("control F"));

    editFindMI.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String inputValue =
                JOptionPane.showInputDialog(
                    _logMonitorFrame,
                    "Find text: ",
                    "Search Record Messages",
                    JOptionPane.QUESTION_MESSAGE
                );
            setSearchText(inputValue);
            findSearchText();
          }
        }

    );
    return editFindMI;
  }

  // Added version 1.2 - Allows users to Sort Log Records by an
  // NDC text filter. A new LogRecordFilter was created to
  // sort the records.
  protected JMenuItem createEditSortNDCMI() {
    JMenuItem editSortNDCMI = new JMenuItem("Sort by NDC");
    editSortNDCMI.setMnemonic('s');
    editSortNDCMI.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String inputValue =
                JOptionPane.showInputDialog(
                    _logMonitorFrame,
                    "Sort by this NDC: ",
                    "Sort Log Records by NDC",
                    JOptionPane.QUESTION_MESSAGE
                );
            setNDCTextFilter(inputValue);
            sortByNDC();
            _table.getFilteredLogTableModel().refresh();
            updateStatusLabel();
          }
        }

    );
    return editSortNDCMI;
  }

  // Added in version 1.2 - Resets the LogRecordFilter back to default
  // filter.
  protected JMenuItem createEditRestoreAllNDCMI() {
    JMenuItem editRestoreAllNDCMI = new JMenuItem("Restore all NDCs");
    editRestoreAllNDCMI.setMnemonic('r');
    editRestoreAllNDCMI.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            _table.getFilteredLogTableModel().setLogRecordFilter(createLogRecordFilter());
            // reset the text filter
            setNDCTextFilter("");
            _table.getFilteredLogTableModel().refresh();
            updateStatusLabel();
          }
        }
    );
    return editRestoreAllNDCMI;
  }

  protected void setLeastSevereDisplayedLogLevel(LogLevel level) {
    if (level == null || _leastSevereDisplayedLogLevel == level) {
      return; // nothing to do
    }
    _leastSevereDisplayedLogLevel = level;
    _table.getFilteredLogTableModel().refresh();
    updateStatusLabel();
  }

  /**
   * Ensures that the Table's ScrollPane Viewport will "track" with updates
   * to the Table.  When the vertical scroll bar is at its bottom anchor
   * and tracking is enabled then viewport will stay at the bottom most
   * point of the component.  The purpose of this feature is to allow
   * a developer to watch the table as messages arrive and not have to
   * scroll after each new message arrives.  When the vertical scroll bar
   * is at any other location, then no tracking will happen.
   * @deprecated tracking is now done automatically.
   */
  protected void trackTableScrollPane() {
    // do nothing
  }

  protected void centerFrame(JFrame frame) {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension comp = frame.getSize();

    frame.setLocation(((screen.width - comp.width) / 2),
        ((screen.height - comp.height) / 2));

  }

  /**
   * Uses a JFileChooser to select a file to opened with the
   * LF5 GUI.
   */
  protected void requestOpen() {
    JFileChooser chooser;

    if (_fileLocation == null) {
      chooser = new JFileChooser();
    } else {
      chooser = new JFileChooser(_fileLocation);
    }

    int returnVal = chooser.showOpenDialog(_logMonitorFrame);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      if (loadLogFile(f)) {
        _fileLocation = chooser.getSelectedFile();
        _mruFileManager.set(f);
        updateMRUList();
      }
    }
  }

  /**
   * Uses a Dialog box to accept a URL to a file to be opened
   * with the LF5 GUI.
   */
  protected void requestOpenURL() {
    LogFactor5InputDialog inputDialog = new LogFactor5InputDialog(
        getBaseFrame(), "Open URL", "URL:");
    String temp = inputDialog.getText();

    if (temp != null) {
      if (temp.indexOf("://") == -1) {
        temp = "http://" + temp;
      }

      try {
        URL url = new URL(temp);
        if (loadLogFile(url)) {
          _mruFileManager.set(url);
          updateMRUList();
        }
      } catch (MalformedURLException e) {
        LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
            getBaseFrame(), "Error reading URL.");
      }
    }
  }

  /**
   * Removes old file list and creates a new file list
   * with the updated MRU list.
   */
  protected void updateMRUList() {
    JMenu menu = _logMonitorFrame.getJMenuBar().getMenu(0);
    menu.removeAll();
    menu.add(createOpenMI());
    menu.add(createOpenURLMI());
    menu.addSeparator();
    menu.add(createCloseMI());
    createMRUFileListMI(menu);
    menu.addSeparator();
    menu.add(createExitMI());
  }

  protected void requestClose() {
    setCallSystemExitOnClose(false);
    closeAfterConfirm();
  }

  /**
   * Opens a file in the MRU list.
   */
  protected void requestOpenMRU(ActionEvent e) {
    String file = e.getActionCommand();
    StringTokenizer st = new StringTokenizer(file);
    String num = st.nextToken().trim();
    file = st.nextToken("\n");

    try {
      int index = Integer.parseInt(num) - 1;

      InputStream in = _mruFileManager.getInputStream(index);
      LogFileParser lfp = new LogFileParser(in);
      lfp.parse(this);

      _mruFileManager.moveToTop(index);
      updateMRUList();

    } catch (Exception me) {
      LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
          getBaseFrame(), "Unable to load file " + file);
    }

  }

  protected void requestExit() {
    _mruFileManager.save();
    setCallSystemExitOnClose(true);
    closeAfterConfirm();
  }

  protected void closeAfterConfirm() {
    StringBuffer message = new StringBuffer();

    if (_callSystemExitOnClose == false) {
      message.append("Are you sure you want to close the logging ");
      message.append("console?\n");
      message.append("(Note: This will not shut down the Virtual Machine,\n");
      message.append("or the Swing event thread.)");
    } else {
      message.append("Are you sure you want to exit?\n");
      message.append("This will shut down the Virtual Machine.\n");
    }

    String title =
        "Are you sure you want to dispose of the Logging Console?";

    if (_callSystemExitOnClose == true) {
      title = "Are you sure you want to exit?";
    }
    int value = JOptionPane.showConfirmDialog(
        _logMonitorFrame,
        message.toString(),
        title,
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null
    );

    if (value == JOptionPane.OK_OPTION) {
      dispose();
    }
  }

  /**
   * Loads and parses a log file.
   */
  protected boolean loadLogFile(File file) {
    boolean ok = false;
    try {
      LogFileParser lfp = new LogFileParser(file);
      lfp.parse(this);
      ok = true;
    } catch (IOException e) {
      LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
          getBaseFrame(), "Error reading " + file.getName());
    }

    return ok;
  }

  /**
   * Loads a parses a log file running on a server.
   */
  protected boolean loadLogFile(URL url) {
    boolean ok = false;
    try {
      LogFileParser lfp = new LogFileParser(url.openStream());
      lfp.parse(this);
      ok = true;
    } catch (IOException e) {
      LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
          getBaseFrame(), "Error reading URL:" + url.getFile());
    }
    return ok;
  }
}
