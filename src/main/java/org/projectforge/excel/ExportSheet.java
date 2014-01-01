/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.excel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class ExportSheet
{
  private static final org.projectforge.common.Logger log = org.projectforge.common.Logger.getLogger(ExportWorkbook.class);

  /** Sheet names are limited to this length */
  public final static int MAX_XLS_SHEETNAME_LENGTH = 31;

  /** Constant for an empty cell */
  public static final String EMPTY = "LEAVE_CELL_EMPTY";

  private final Sheet poiSheet;

  private final List<ExportRow> rows;

  private final String name;

  private String[] propertyNames;

  private int rowCounter = 0;

  private ContentProvider contentProvider;

  private boolean imported;

  public ExportSheet(final ContentProvider contentProvider, final String name, final Sheet poiSheet)
  {
    this.contentProvider = contentProvider;
    this.name = name;
    this.poiSheet = poiSheet;
    this.rows = new ArrayList<ExportRow>();
    final int lastRowNum = poiSheet.getLastRowNum();
    if (lastRowNum > 0) {
      // poiSheet does already exists.
      for (int i = poiSheet.getFirstRowNum(); i < poiSheet.getLastRowNum(); i++) {
        Row poiRow = poiSheet.getRow(i);
        if (poiRow == null) {
          poiRow = poiSheet.createRow(i);
        }
        final ExportRow row = new ExportRow(contentProvider, this, poiRow, i);
        rows.add(row);
      }
    }
    final PrintSetup printSetup = getPrintSetup();
    printSetup.setPaperSize(ExportConfig.getInstance().getDefaultPaperSizeId());
  }

  /**
   * Convenient method: Adds all column names, titles, width and adds a head row.
   * @param columns
   */
  public void setColumns(final List<ExportColumn> columns)
  {
    if (columns == null) {
      return;
    }
    // build all column names, title, widths from fixed and variable columns
    final String[] colNames = new String[columns.size()];
    final ExportRow headRow = addRow();
    int idx = 0;
    for (final ExportColumn col : columns) {
      addHeadRowCell(headRow, col, colNames, idx++);
    }
    setPropertyNames(colNames);
  }

  /**
   * Convenient method: Adds all column names, titles, width and adds a head row.
   * @param columns
   */
  public void setColumns(final ExportColumn... columns)
  {
    if (columns == null) {
      return;
    }
    // build all column names, title, widths from fixed and variable columns
    final String[] colNames = new String[columns.length];
    final ExportRow headRow = addRow();
    int idx = 0;
    for (final ExportColumn col : columns) {
      addHeadRowCell(headRow, col, colNames, idx++);
    }
    setPropertyNames(colNames);
  }

  private void addHeadRowCell(final ExportRow headRow, final ExportColumn col, final String[] colNames, final int idx) {
    headRow.addCell(idx, col.getTitle());
    colNames[idx] = col.getName();
    contentProvider.putColWidth(idx, col.getWidth());
  }


  public PrintSetup getPrintSetup()
  {
    return poiSheet.getPrintSetup();
  }

  public ExportRow addRow()
  {
    final Row poiRow = poiSheet.createRow(rowCounter);
    final ExportRow row = new ExportRow(contentProvider, this, poiRow, rowCounter++);
    this.rows.add(row);
    return row;
  }

  public ExportRow addRow(final Object... values)
  {
    final ExportRow row = addRow();
    row.setValues(values);
    return row;
  }

  public ExportRow addRow(final Object rowBean)
  {
    return addRow(rowBean, 0);
  }

  public ExportRow addRow(final Object rowBean, final int startCol)
  {
    final ExportRow row = addRow();
    row.fillBean(rowBean, propertyNames, 0);
    return row;
  }

  public void addRows(final Object[] rowBeans)
  {
    addRows(rowBeans, 0);
  }

  public void addRows(final Object[] rowBeans, final int startCol)
  {
    for (final Object rowBean : rowBeans) {
      addRow(rowBean, startCol);
    }
  }

  public void addRows(final Collection< ? > rowBeans)
  {
    addRows(rowBeans, 0);
  }

  public void addRows(final Collection< ? > rowBeans, final int startCol)
  {
    for (final Object rowBean : rowBeans) {
      addRow(rowBean, startCol);
    }
  }

  public String getName()
  {
    return name;
  }

  public ExportRow getRow(final int row)
  {
    return this.rows.get(row);
  }

  /**
   * @return the rowCounter
   */
  public int getRowCounter()
  {
    return rowCounter;
  }

  public List<ExportRow> getRows()
  {
    return rows;
  }

  /**
   * For filling the table via beans.
   * @param propertyNames
   */
  public void setPropertyNames(final String[] propertyNames)
  {
    this.propertyNames = propertyNames;
  }

  /**
   * @return the propertyNames
   */
  public String[] getPropertyNames()
  {
    return propertyNames;
  }

  /**
   * @see ExportRow#updateStyles(StyleProvider)
   */
  public void updateStyles()
  {
    if (contentProvider != null) {
      contentProvider.updateSheetStyle(this);
      for (final ExportRow row : rows) {
        row.updateStyles(contentProvider);
      }
    }
  }

  public ContentProvider getContentProvider()
  {
    return contentProvider;
  }

  public void setContentProvider(final ContentProvider contentProvider)
  {
    this.contentProvider = contentProvider;
  }

  public void setColumnWidth(final int col, final int width)
  {
    poiSheet.setColumnWidth(col, width);
  }

  /**
   * Freezes the first toCol columns and the first toRow lines.
   * @param toCol
   * @param toRow
   * @see Sheet#createFreezePane(int, int)
   */
  public void createFreezePane(final int toCol, final int toRow)
  {
    poiSheet.createFreezePane(toCol, toRow);
  }

  /**
   * @param x
   * @param y
   * @see Sheet#setZoom(int, int)
   */
  public void setZoom(final int x, final int y)
  {
    poiSheet.setZoom(x, y);
  }

  /**
   * Merges cells and sets the value.
   * @param firstRow
   * @param lastRow
   * @param firstCol
   * @param lastCol
   * @param value
   */
  public ExportCell setMergedRegion(final int firstRow, final int lastRow, final int firstCol, final int lastCol, final Object value)
  {
    final CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
    poiSheet.addMergedRegion(region);
    final ExportRow row = getRow(firstRow);
    final ExportCell cell = row.addCell(firstCol, value);
    return cell;
  }

  public Sheet getPoiSheet()
  {
    return poiSheet;
  }

  /**
   * Set auto-filter for the whole first row. Maximum number of supported cells is 26 (A1:Z1)! Must be called after adding the first row
   * with all heading cells.
   * @return this for chaining.
   */
  public ExportSheet setAutoFilter()
  {
    final ExportRow row = getRow(0);
    int numberOfCols = row.getMaxCol();
    if (numberOfCols > 26) {
      log.warn("#setAutoFilter supports only up to 26 columns! " + numberOfCols + " exceeds 26.");
      numberOfCols = 26;
    }
    getPoiSheet().setAutoFilter(
        org.apache.poi.ss.util.CellRangeAddress.valueOf("A1:" + (Character.toString((char) ('A' + numberOfCols))) + "1"));

    return this;
  }

  /**
   * @return true if this sheet was imported by a file.
   */
  public boolean isImported()
  {
    return imported;
  }

  public void setImported(final boolean imported)
  {
    this.imported = imported;
  }
}
