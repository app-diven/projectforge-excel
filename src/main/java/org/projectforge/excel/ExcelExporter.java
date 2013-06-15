/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
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

import java.lang.reflect.Field;
import java.util.List;

import org.projectforge.common.BeanHelper;
import org.projectforge.core.PropUtils;
import org.projectforge.core.PropertyInfo;
import org.projectforge.core.PropertyType;

public class ExcelExporter
{
  private static final org.projectforge.common.Logger log = org.projectforge.common.Logger.getLogger(ExportWorkbook.class);

  private final ExportWorkbook workBook;

  private int defaultColWidth = 20;

  public ExcelExporter(final String filename)
  {
    this.workBook = new ExportWorkbook();
    this.workBook.setFilename(filename);
  }

  public <T> ExportSheet addSheet(final ContentProvider sheetProvider, final String sheetTitle, final List<T> list)
  {
    final ExportSheet sheet = workBook.addSheet(sheetTitle);
    if (list == null || list.size() == 0) {
      // Nothing to export.
      log.info("Nothing to export for sheet '" + sheetTitle + "'.");
      return sheet;
    }
    // create a default Date format and currency column
    sheet.setContentProvider(sheetProvider);

    sheet.createFreezePane(0, 1);

    final Class< ? > classType = list.get(0).getClass();
    final Field[] fields = PropUtils.getPropertyInfoFields(classType);
    final ExportColumn[] cols = new ExportColumn[fields.length];
    int i = 0;
    for (final Field field : fields) {
      final PropertyInfo propInfo = field.getAnnotation(PropertyInfo.class);
      if (propInfo == null) {
        // Shouldn't occur.
        continue;
      }
      final ExportColumn exportColumn = new I18nExportColumn(field.getName(), propInfo.i18nKey(), defaultColWidth);
      cols[i++] = exportColumn;
      putFieldFormat(sheetProvider, field, propInfo, exportColumn);
    }
    // column property names
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    for (final Object entry : list) {
      for (final Field field : fields) {
        final PropertyInfo propInfo = field.getAnnotation(PropertyInfo.class);
        if (propInfo == null) {
          // Shouldn't occur.
          continue;
        }
        field.setAccessible(true);
        mapping.add(field.getName(), BeanHelper.getFieldValue(entry, field));
      }
      sheet.addRow(mapping.getMapping(), 0);
    }

    return sheet;
  }

  /**
   * @return the xls
   */
  public ExportWorkbook getWorkbook()
  {
    return workBook;
  }

  /**
   * @param defaultColWidth the defaultColWidth to set
   * @return this for chaining.
   */
  public ExcelExporter setDefaultColWidth(final int defaultColWidth)
  {
    this.defaultColWidth = defaultColWidth;
    return this;
  }

  /**
   * Adds customized formats. Put here your customized formats to your ExportSheet.
   * @param field
   * @param propInfo may-be null.
   * @param column
   * @return true, if format is handled by this method, otherwise false.
   */
  protected void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
      final ExportColumn exportColumn)
  {
    final PropertyType type = propInfo.type();
    if (type == PropertyType.CURRENCY) {
      sheetProvider.putFormat(exportColumn, "#,##0.00;[Red]-#,##0.00");
      exportColumn.setWidth(10);
    } else if (type == PropertyType.DATE) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy");
    } else if (type == PropertyType.DATE_TIME) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm");
    } else if (type == PropertyType.DATE_TIME_SECONDS) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm:ss");
    } else if (type == PropertyType.DATE_TIME_MILLIS) {
      sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm:ss.fff");
    } else if (type == PropertyType.UNSPECIFIED) {
      if (java.sql.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, "MM/dd/yyyy");
      } else if (java.util.Date.class.isAssignableFrom(field.getType()) == true) {
        sheetProvider.putFormat(exportColumn, "MM/dd/yyyy HH:mm");
      } else if (java.lang.Integer.class.isAssignableFrom(field.getType()) == true) {
        exportColumn.setWidth(10);
      } else if (java.lang.Boolean.class.isAssignableFrom(field.getType()) == true) {
        exportColumn.setWidth(10);
      }
    }
  }
}
