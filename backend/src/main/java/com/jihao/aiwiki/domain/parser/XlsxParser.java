package com.jihao.aiwiki.domain.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * XLSX 文件解析器，使用 Apache POI 提取表格文本。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class XlsxParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        try (var wb = new XSSFWorkbook(input)) {
            DataFormatter formatter = new DataFormatter();
            List<String> lines = new ArrayList<>();
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                var sheet = wb.getSheetAt(si);
                lines.add("# " + sheet.getSheetName());
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(formatter.formatCellValue(cell));
                    }
                    lines.add(String.join("\t", cells));
                }
                lines.add("");
            }
            return String.join("\n", lines);
        }
    }
}
