/*
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
 */
package org.apache.log4j.lf5.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.lf5.LogRecordFilter;
import org.apache.log4j.lf5.util.LogFileParser;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerTree;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryPath;
import org.apache.log4j.lf5.viewer.configure.ConfigurationManager;
import org.apache.log4j.lf5.viewer.configure.MRUFileManager;

/**
 * LogBrokerMonitor
 *.
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 * @author Brad Marlborough
 * @author Richard Wan
 * @author Brent Sprecher
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class LogBrokerMonitor extends LogBrokerMonitorSuperSuper {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  public static final String DETAILED_VIEW = "Detailed";
  protected int _logMonitorFrameWidth = 550;
  protected int _logMonitorFrameHeight = 500;

  protected Object _lock = new Object();
  protected JComboBox _fontSizeCombo;

  protected int _fontSize = 10;
  protected String _fontName = "Dialog";
  protected String _currentView = DETAILED_VIEW;

  protected boolean _loadSystemFonts = false;
  protected boolean _trackTableScrollPane = true;
  protected Dimension _lastTableViewportSize;
  protected List _displayedLogBrokerProperties = new Vector();

  protected List _levels = null;

  public ConfigurationManager _configurationManager = null;

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  /**
   * Construct a LogBrokerMonitor.
   */
  public LogBrokerMonitor(List logLevels) {
    super();

    _levels = logLevels;
    // This allows us to use the LogBroker in command line tools and
    // have the option for it to shutdown.

    String callSystemExitOnClose =
        System.getProperty("monitor.exit");
    if (callSystemExitOnClose == null) {
      callSystemExitOnClose = "false";
    }
    callSystemExitOnClose = callSystemExitOnClose.trim().toLowerCase();

    if (callSystemExitOnClose.equals("true")) {
      _callSystemExitOnClose = true;
    }

    initComponents();


    _logMonitorFrame.addWindowListener(
        new LogBrokerMonitorWindowAdaptor(this));

  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Hide the frame for the LogBrokerMonitor.
   */
  public void hide() {
    _logMonitorFrame.setVisible(false);
  }

  /**
   * Get the value of whether or not System.exit() will be called
   * when the LogBrokerMonitor is closed.
   */
  public boolean getCallSystemExitOnClose() {
    return _callSystemExitOnClose;
  }

  /**
   * Add a log record message to be displayed in the LogTable.
   * This method is thread-safe as it posts requests to the SwingThread
   * rather than processing directly.
   */
  public void addMessage(final LogRecord lr) {
    if (_isDisposed == true) {
      // If the frame has been disposed of, do not log any more
      // messages.
      return;
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        _categoryExplorerTree.getExplorerModel().addLogRecord(lr);
        _table.getFilteredLogTableModel().addLogRecord(lr); // update table
        updateStatusLabel(); // show updated counts
      }
    });
  }

  public void setMaxNumberOfLogRecords(int maxNumberOfLogRecords) {
    _table.getFilteredLogTableModel().setMaxNumberOfLogRecords(maxNumberOfLogRecords);
  }

  public void setTitle(String title) {
    _logMonitorFrame.setTitle(title + " - LogFactor5");
  }

  public void setFrameSize(int width, int height) {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    if (0 < width && width < screen.width) {
      _logMonitorFrameWidth = width;
    }
    if (0 < height && height < screen.height) {
      _logMonitorFrameHeight = height;
    }
    updateFrameSize();
  }

  public void setFontSize(int fontSize) {
    changeFontSizeCombo(_fontSizeCombo, fontSize);
    // setFontSizeSilently(actualFontSize); - changeFontSizeCombo fires event
    // refreshDetailTextArea();
  }

  public void addDisplayedProperty(Object messageLine) {
    _displayedLogBrokerProperties.add(messageLine);
  }

  // Added in version 1.2 - gets the value of the NDC text filter
  // This value is set back to null each time the Monitor is initialized.
  public String getNDCTextFilter() {
    return _NDCTextFilter;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected void clearDetailTextArea() {
    _table._detailTextArea.setText("");
  }

  /**
   * Changes the font selection in the combo box and returns the
   * size actually selected.
   * @return -1 if unable to select an appropriate font
   */
  protected int changeFontSizeCombo(JComboBox box, int requestedSize) {
    int len = box.getItemCount();
    int currentValue;
    Object currentObject;
    Object selectedObject = box.getItemAt(0);
    int selectedValue = Integer.parseInt(String.valueOf(selectedObject));
    for (int i = 0; i < len; i++) {
      currentObject = box.getItemAt(i);
      currentValue = Integer.parseInt(String.valueOf(currentObject));
      if (selectedValue < currentValue && currentValue <= requestedSize) {
        selectedValue = currentValue;
        selectedObject = currentObject;
      }
    }
    box.setSelectedItem(selectedObject);
    return selectedValue;
  }

  protected void setFontSize(Component component, int fontSize) {
    Font oldFont = component.getFont();
    Font newFont =
        new Font(oldFont.getFontName(), oldFont.getStyle(), fontSize);
    component.setFont(newFont);
  }

  protected void updateFrameSize() {
    _logMonitorFrame.setSize(_logMonitorFrameWidth, _logMonitorFrameHeight);
    centerFrame(_logMonitorFrame);
  }

  protected void initComponents() {
    //
    // Configure the Frame.
    //
    _logMonitorFrame = new JFrame("LogFactor5");

    _logMonitorFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    String resource =
        "/org/apache/log4j/lf5/viewer/images/lf5_small_icon.gif";
    URL lf5IconURL = getClass().getResource(resource);

    if (lf5IconURL != null) {
      _logMonitorFrame.setIconImage(new ImageIcon(lf5IconURL).getImage());
    }
    updateFrameSize();

    //
    // Configure the LogTable.
    //
    JTextArea detailTA = createDetailTextArea();
    JScrollPane detailTAScrollPane = new JScrollPane(detailTA);
    _table = new LogTable(detailTA);
    setView(_currentView, _table);
    _table.setFont(new Font(_fontName, Font.PLAIN, _fontSize));
    _logTableScrollPane = new JScrollPane(_table);

    if (_trackTableScrollPane) {
      _logTableScrollPane.getVerticalScrollBar().addAdjustmentListener(
          new TrackingAdjustmentListener()
      );
    }


    // Configure the SplitPane between the LogTable & DetailTextArea
    //

    JSplitPane tableViewerSplitPane = new JSplitPane();
    tableViewerSplitPane.setOneTouchExpandable(true);
    tableViewerSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    tableViewerSplitPane.setLeftComponent(_logTableScrollPane);
    tableViewerSplitPane.setRightComponent(detailTAScrollPane);
    // Make sure to do this last..
    //tableViewerSplitPane.setDividerLocation(1.0); Doesn't work
    //the same under 1.2.x & 1.3
    // "350" is a magic number that provides the correct default
    // behaviour under 1.2.x & 1.3.  For example, bumping this
    // number to 400, causes the pane to be completely open in 1.2.x
    // and closed in 1.3
    tableViewerSplitPane.setDividerLocation(350);

    //
    // Configure the CategoryExplorer
    //

    _categoryExplorerTree = new CategoryExplorerTree();

    _table.getFilteredLogTableModel().setLogRecordFilter(createLogRecordFilter());

    JScrollPane categoryExplorerTreeScrollPane =
        new JScrollPane(_categoryExplorerTree);
    categoryExplorerTreeScrollPane.setPreferredSize(new Dimension(130, 400));

    // Load most recently used file list
    _mruFileManager = new MRUFileManager();

    //
    // Configure the SplitPane between the CategoryExplorer & (LogTable/Detail)
    //

    JSplitPane splitPane = new JSplitPane();
    splitPane.setOneTouchExpandable(true);
    splitPane.setRightComponent(tableViewerSplitPane);
    splitPane.setLeftComponent(categoryExplorerTreeScrollPane);
    // Do this last.
    splitPane.setDividerLocation(130);
    //
    // Add the MenuBar, StatusArea, CategoryExplorer|LogTable to the
    // LogMonitorFrame.
    //
    _logMonitorFrame.getRootPane().setJMenuBar(createMenuBar());
    _logMonitorFrame.getContentPane().add(splitPane, BorderLayout.CENTER);
    _logMonitorFrame.getContentPane().add(createToolBar(),
        BorderLayout.NORTH);
    _logMonitorFrame.getContentPane().add(createStatusArea(),
        BorderLayout.SOUTH);

    makeLogTableListenToCategoryExplorer();
    addTableModelProperties();

    //
    // Configure ConfigurationManager
    //
    _configurationManager = new ConfigurationManager(this, _table);

  }


  protected void addTableModelProperties() {
    final FilteredLogTableModel model = _table.getFilteredLogTableModel();

    addDisplayedProperty(new Object() {
      public String toString() {
        return getRecordsDisplayedMessage();
      }
    });
    addDisplayedProperty(new Object() {
      public String toString() {
        return "Maximum number of displayed LogRecords: "
            + model._maxNumberOfLogRecords;
      }
    });
  }

  protected void makeLogTableListenToCategoryExplorer() {
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _table.getFilteredLogTableModel().refresh();
        updateStatusLabel();
      }
    };
    _categoryExplorerTree.getExplorerModel().addActionListener(listener);
  }

  protected JPanel createStatusArea() {
    JPanel statusArea = new JPanel();
    JLabel status =
        new JLabel("No log records to display.");
    _statusLabel = status;
    status.setHorizontalAlignment(JLabel.LEFT);

    statusArea.setBorder(BorderFactory.createEtchedBorder());
    statusArea.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    statusArea.add(status);

    return (statusArea);
  }

  protected JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(createFileMenu());
    menuBar.add(createEditMenu());
    menuBar.add(createLogLevelMenu());
    menuBar.add(createViewMenu());
    menuBar.add(createConfigureMenu());
    menuBar.add(createHelpMenu());

    return (menuBar);
  }

  protected JMenu createLogLevelMenu() {
    JMenu result = new JMenu("Log Level");
    result.setMnemonic('l');
    Iterator levels = getLogLevels();
    while (levels.hasNext()) {
      result.add(getMenuItem((LogLevel) levels.next()));
    }

    result.addSeparator();
    result.add(createAllLogLevelsMenuItem());
    result.add(createNoLogLevelsMenuItem());
    result.addSeparator();
    result.add(createLogLevelColorMenu());
    result.add(createResetLogLevelColorMenuItem());

    return result;
  }

  protected JMenuItem createAllLogLevelsMenuItem() {
    JMenuItem result = new JMenuItem("Show all LogLevels");
    result.setMnemonic('s');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectAllLogLevels(true);
        _table.getFilteredLogTableModel().refresh();
        updateStatusLabel();
      }
    });
    return result;
  }

  protected JMenuItem createNoLogLevelsMenuItem() {
    JMenuItem result = new JMenuItem("Hide all LogLevels");
    result.setMnemonic('h');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectAllLogLevels(false);
        _table.getFilteredLogTableModel().refresh();
        updateStatusLabel();
      }
    });
    return result;
  }

  protected JMenu createLogLevelColorMenu() {
    JMenu colorMenu = new JMenu("Configure LogLevel Colors");
    colorMenu.setMnemonic('c');
    Iterator levels = getLogLevels();
    while (levels.hasNext()) {
      colorMenu.add(createSubMenuItem((LogLevel) levels.next()));
    }

    return colorMenu;
  }

  protected JMenuItem createResetLogLevelColorMenuItem() {
    JMenuItem result = new JMenuItem("Reset LogLevel Colors");
    result.setMnemonic('r');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // reset the level colors in the map
        LogLevel.resetLogLevelColorMap();

        // refresh the table
        _table.getFilteredLogTableModel().refresh();
      }
    });
    return result;
  }

  protected void selectAllLogLevels(boolean selected) {
    Iterator levels = getLogLevels();
    while (levels.hasNext()) {
      getMenuItem((LogLevel) levels.next()).setSelected(selected);
    }
  }

  protected JMenuItem createSubMenuItem(LogLevel level) {
    final JMenuItem result = new JMenuItem(level.toString());
    final LogLevel logLevel = level;
    result.setMnemonic(level.toString().charAt(0));
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showLogLevelColorChangeDialog(result, logLevel);
      }
    });

    return result;

  }

  protected void showLogLevelColorChangeDialog(JMenuItem result, LogLevel level) {
    JMenuItem menuItem = result;
    Color newColor = JColorChooser.showDialog(
        _logMonitorFrame,
        "Choose LogLevel Color",
        result.getForeground());

    if (newColor != null) {
      // set the color for the record
      level.setLogLevelColorMap(level, newColor);
      _table.getFilteredLogTableModel().refresh();
    }

  }

  // view menu
  protected JMenu createViewMenu() {
    JMenu result = new JMenu("View");
    result.setMnemonic('v');
    Iterator columns = getLogTableColumns();
    while (columns.hasNext()) {
      result.add(getLogTableColumnMenuItem((LogTableColumn) columns.next()));
    }

    result.addSeparator();
    result.add(createAllLogTableColumnsMenuItem());
    result.add(createNoLogTableColumnsMenuItem());
    return result;
  }

  protected JMenuItem createAllLogTableColumnsMenuItem() {
    JMenuItem result = new JMenuItem("Show all Columns");
    result.setMnemonic('s');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectAllLogTableColumns(true);
        // update list of columns and reset the view
        List selectedColumns = updateView();
        _table.setView(selectedColumns);
      }
    });
    return result;
  }

  protected JMenu createFileMenu() {
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('f');
    JMenuItem exitMI;
    fileMenu.add(createOpenMI());
    fileMenu.add(createOpenURLMI());
    fileMenu.addSeparator();
    fileMenu.add(createCloseMI());
    createMRUFileListMI(fileMenu);
    fileMenu.addSeparator();
    fileMenu.add(createExitMI());
    return fileMenu;
  }

  protected JMenu createConfigureMenu() {
    JMenu configureMenu = new JMenu("Configure");
    configureMenu.setMnemonic('c');
    configureMenu.add(createConfigureSave());
    configureMenu.add(createConfigureReset());
    configureMenu.add(createConfigureMaxRecords());

    return configureMenu;
  }

  protected JMenuItem createConfigureSave() {
    JMenuItem result = new JMenuItem("Save");
    result.setMnemonic('s');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _configurationManager.saveConfiguration(this);
      }
    });

    return result;
  }

  protected JMenuItem createConfigureReset() {
    JMenuItem result = new JMenuItem("Reset");
    result.setMnemonic('r');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetConfiguration();
      }
    });

    return result;
  }

  protected JMenuItem createConfigureMaxRecords() {
    JMenuItem result = new JMenuItem("Set Max Number of Records");
    result.setMnemonic('m');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setMaxRecordConfiguration();
      }
    });

    return result;
  }


  protected void resetConfiguration() {
    _configurationManager.reset();
  }

  protected void setMaxRecordConfiguration() {
    LogFactor5InputDialog inputDialog = new LogFactor5InputDialog(
        getBaseFrame(), "Set Max Number of Records", "", 10);

    String temp = inputDialog.getText();

    if (temp != null) {
      try {
        setMaxNumberOfLogRecords(Integer.parseInt(temp));
      } catch (NumberFormatException e) {
        LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
            getBaseFrame(),
            "'" + temp + "' is an invalid parameter.\nPlease try again.");
        setMaxRecordConfiguration();
      }
    }
  }


  protected JMenu createHelpMenu() {
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');
    helpMenu.add(createHelpProperties());
    return helpMenu;
  }

  protected JMenuItem createHelpProperties() {
    final String title = "LogFactor5 Properties";
    final JMenuItem result = new JMenuItem(title);
    result.setMnemonic('l');
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showPropertiesDialog(title);
      }
    });
    return result;
  }

  protected void showPropertiesDialog(String title) {
    JOptionPane.showMessageDialog(
        _logMonitorFrame,
        _displayedLogBrokerProperties.toArray(),
        title,
        JOptionPane.PLAIN_MESSAGE
    );
  }

  protected JToolBar createToolBar() {
    JToolBar tb = new JToolBar();
    tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
    JComboBox fontCombo = new JComboBox();
    JComboBox fontSizeCombo = new JComboBox();
    _fontSizeCombo = fontSizeCombo;

    ClassLoader cl = this.getClass().getClassLoader();
    if(cl == null) {
        cl = ClassLoader.getSystemClassLoader();
    }
    URL newIconURL = cl.getResource("org/apache/log4j/lf5/viewer/" +
        "images/channelexplorer_new.gif");

    ImageIcon newIcon = null;

    if (newIconURL != null) {
      newIcon = new ImageIcon(newIconURL);
    }

    JButton newButton = new JButton("Clear Log Table");

    if (newIcon != null) {
      newButton.setIcon(newIcon);
    }

    newButton.setToolTipText("Clear Log Table.");
    //newButton.setBorder(BorderFactory.createEtchedBorder());

    newButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            _table.clearLogRecords();
            _categoryExplorerTree.getExplorerModel().resetAllNodeCounts();
            updateStatusLabel();
            clearDetailTextArea();
            LogRecord.resetSequenceNumber();
          }
        }
    );

    Toolkit tk = Toolkit.getDefaultToolkit();
    // This will actually grab all the fonts

    String[] fonts;

    if (_loadSystemFonts) {
      fonts = GraphicsEnvironment.
          getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    } else {
      fonts = tk.getFontList();
    }

    for (int j = 0; j < fonts.length; j++) {
      fontCombo.addItem(fonts[j]);
    }

    fontCombo.setSelectedItem(_fontName);

    fontCombo.addActionListener(

        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            JComboBox box = (JComboBox) e.getSource();
            String font = (String) box.getSelectedItem();
            _table.setFont(new Font(font, Font.PLAIN, _fontSize));
            _fontName = font;
          }
        }
    );

    fontSizeCombo.addItem("8");
    fontSizeCombo.addItem("9");
    fontSizeCombo.addItem("10");
    fontSizeCombo.addItem("12");
    fontSizeCombo.addItem("14");
    fontSizeCombo.addItem("16");
    fontSizeCombo.addItem("18");
    fontSizeCombo.addItem("24");

    fontSizeCombo.setSelectedItem(String.valueOf(_fontSize));
    fontSizeCombo.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            JComboBox box = (JComboBox) e.getSource();
            String size = (String) box.getSelectedItem();
            int s = Integer.valueOf(size).intValue();

            setFontSizeSilently(s);
            refreshDetailTextArea();
            _fontSize = s;
          }
        }
    );

    tb.add(new JLabel(" Font: "));
    tb.add(fontCombo);
    tb.add(fontSizeCombo);
    tb.addSeparator();
    tb.addSeparator();
    tb.add(newButton);

    newButton.setAlignmentY(0.5f);
    newButton.setAlignmentX(0.5f);

    fontCombo.setMaximumSize(fontCombo.getPreferredSize());
    fontSizeCombo.setMaximumSize(
        fontSizeCombo.getPreferredSize());

    return (tb);
  }

//    protected void setView(String viewString, LogTable table) {
//        if (STANDARD_VIEW.equals(viewString)) {
//            table.setStandardView();
//        } else if (COMPACT_VIEW.equals(viewString)) {
//            table.setCompactView();
//        } else if (DETAILED_VIEW.equals(viewString)) {
//            table.setDetailedView();
//        } else {
//            String message = viewString + "does not match a supported view.";
//            throw new IllegalArgumentException(message);
//        }
//        _currentView = viewString;
//    }

  protected void setView(String viewString, LogTable table) {
    if (DETAILED_VIEW.equals(viewString)) {
      table.setDetailedView();
    } else {
      String message = viewString + "does not match a supported view.";
      throw new IllegalArgumentException(message);
    }
    _currentView = viewString;
  }

  protected JComboBox createLogLevelCombo() {
    JComboBox result = new JComboBox();
    Iterator levels = getLogLevels();
    while (levels.hasNext()) {
      result.addItem(levels.next());
    }
    result.setSelectedItem(_leastSevereDisplayedLogLevel);

    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JComboBox box = (JComboBox) e.getSource();
        LogLevel level = (LogLevel) box.getSelectedItem();
        setLeastSevereDisplayedLogLevel(level);
      }
    });
    result.setMaximumSize(result.getPreferredSize());
    return result;
  }

  protected Iterator getLogLevels() {
    return _levels.iterator();
  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

  class LogBrokerMonitorWindowAdaptor extends WindowAdapter {
    protected LogBrokerMonitor _monitor;

    public LogBrokerMonitorWindowAdaptor(LogBrokerMonitor monitor) {
      _monitor = monitor;
    }

    public void windowClosing(WindowEvent ev) {
      _monitor.requestClose();
    }
  }
}


