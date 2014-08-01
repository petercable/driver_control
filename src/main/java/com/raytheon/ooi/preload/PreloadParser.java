package com.raytheon.ooi.preload;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PreloadParser {
    private PreloadParser() {}
    private static final String preloadPath = "https://docs.google.com/spreadsheet/pub?key=0AttCeOvLP6XMdG82NHZfSEJJOGdQTkgzb05aRjkzMEE&output=xls";
    private static Logger log = LogManager.getLogger();

    public static void loadWorkbook(URL url) throws IOException {
        log.debug("Attempting to load spreadsheet from {}", url);
        Workbook wb = new XSSFWorkbook(url.openStream());
        log.debug("Finished loading spreadsheet");
        for (int i=0; i<wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            log.debug("Found sheet name: {}", sheet.getSheetName());
            List<List<Object>> rows = new ArrayList<>(sheet.getPhysicalNumberOfRows());
            for (int j=0; j<sheet.getPhysicalNumberOfRows(); j++) {
                Row row = sheet.getRow(j);
                List<Object> values = new ArrayList<>(row.getPhysicalNumberOfCells());
                for (int k = 0; k < row.getPhysicalNumberOfCells(); k++) {
                    Object value = null;
                    Cell cell = row.getCell(k);
                    if (cell != null) {
                        int type = cell.getCellType();

                        switch (type) {
                            case Cell.CELL_TYPE_NUMERIC:
                                value = cell.getNumericCellValue();
                                break;
                            case Cell.CELL_TYPE_BLANK:
                                break;
                            case Cell.CELL_TYPE_BOOLEAN:
                                value = cell.getBooleanCellValue();
                                break;
                            case Cell.CELL_TYPE_ERROR:
                                value = cell.getErrorCellValue();
                                break;
                            case Cell.CELL_TYPE_FORMULA:
                                value = cell.getCachedFormulaResultType();
                                break;
                            case Cell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            default:
                                log.debug("Unknown type found");
                        }
                    }
                    values.add(value);
                }
                rows.add(values);
            }
            log.debug("Sheet: {} Numrows: {}", sheet.getSheetName(), rows.size());
        }
    }

    public static void main(String[] args) throws IOException {
        loadWorkbook(new URL(preloadPath));
    }
}
