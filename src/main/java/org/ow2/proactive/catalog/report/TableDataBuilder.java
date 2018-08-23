/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.report;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDPage;
import org.ow2.proactive.catalog.dto.CatalogObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.image.Image;


@Component
public class TableDataBuilder {

    private static final int MEDIUM_FONT = 6;

    private static final int BIG_FONT = 8;

    private static final int SMALL_FONT = 4;

    private static final float TOTAL_NUMBER_OF_COLUMNS = 12f;

    private static final String LIGHT_GRAY = "#D3D3D3";

    private static final String LIGHT_CYAN = "#F3F3F4";

    private static final String WHITE = "#ffffff";

    @Value("${pa.scheduler.url}")
    private String schedulerUrl;

    public void buildTableData(Set<CatalogObjectMetadata> orderedObjectsPerBucket, BaseTable table) {

        // Create data header row
        createDataHeader(table);

        String currentBucketName = "";

        for (CatalogObjectMetadata catalogObject : orderedObjectsPerBucket) {

            if (!currentBucketName.equals(catalogObject.getBucketName())) {
                currentBucketName = catalogObject.getBucketName();
                Row<PDPage> dataRow = table.createRow(10f);
                createDataCell(dataRow, (100), currentBucketName, BIG_FONT);
            }

            Row<PDPage> dataRow = table.createRow(10f);
            createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), catalogObject.getBucketName());
            createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), getProjectName(catalogObject));
            createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), catalogObject.getName());
            createDataCell(dataRow,
                           (100 / TOTAL_NUMBER_OF_COLUMNS) * 2,
                           getDescription(catalogObject),
                           MEDIUM_FONT,
                           WHITE,
                           HorizontalAlignment.LEFT);
            createKeyValueContentDataCell(dataRow,
                                          (100 / TOTAL_NUMBER_OF_COLUMNS) * 2,
                                          getKeyValuesAsUnorderedHTMLList(catalogObject, "variable"),
                                          SMALL_FONT);
            createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), catalogObject.getKind());
            createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), catalogObject.getContentType());

            createKeyValueContentDataCell(dataRow,
                                          (100 / TOTAL_NUMBER_OF_COLUMNS) * 2,
                                          getKeyValuesAsUnorderedHTMLList(catalogObject, "generic_information"),
                                          SMALL_FONT);
            createIconCell(dataRow, getIcon(catalogObject));

        }
        if (orderedObjectsPerBucket.isEmpty()) {
            createEmptyTableRow(table);
        }

    }

    private String getKeyValuesAsUnorderedHTMLList(CatalogObjectMetadata catalogObject, String key) {
        return catalogObject.getMetadataList()
                            .stream()
                            .filter(metadata -> metadata.getLabel().equals(key))
                            .map(metadata -> "<li><b>" + metadata.getKey() + "</b> = " + metadata.getValue() + "</li>")
                            .collect(Collectors.joining(""));

    }

    private void createDataHeader(BaseTable table) {
        Row<PDPage> factHeaderrow = table.createRow(15f);
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Bucket Name");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Project Name");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Object Name");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "Description");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "Variables");

        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Kind");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Content Type");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "Generic Info");
        createDataHeaderCell(factHeaderrow, (100 / TOTAL_NUMBER_OF_COLUMNS), "Icon");
        table.addHeaderRow(factHeaderrow);
    }

    private void createDataHeaderCell(Row<PDPage> factHeaderrow, float width, String title) {
        Cell<PDPage> cell;
        cell = factHeaderrow.createCell(width, title);
        cell.setFontSize(BIG_FONT);
        cell.setFillColor(java.awt.Color.decode(LIGHT_CYAN));
        cell.setAlign(HorizontalAlignment.CENTER);
    }

    private void createDataCell(Row<PDPage> row, float width, String catalogObjectData, int fontSize) {
        createDataCell(row, width, catalogObjectData, fontSize, WHITE, HorizontalAlignment.CENTER);
    }

    private void createDataCell(Row<PDPage> row, float width, String catalogObjectData, int fontSize, String color,
            HorizontalAlignment align) {
        Cell<PDPage> bucketNameCell = row.createCell(width, catalogObjectData);
        bucketNameCell.setFontSize(fontSize);
        bucketNameCell.setAlign(align);
        bucketNameCell.setFillColor(java.awt.Color.decode(color));
    }

    private void createKeyValueContentDataCell(Row<PDPage> row, float width, String catalogObjectData, int fontSize) {
        Cell<PDPage> cell = row.createCell(width, "<ul>" + catalogObjectData + "</ul>");
        cell.setFontSize(fontSize);
        cell.setAlign(HorizontalAlignment.LEFT);
    }

    private void createDataCell(Row<PDPage> row, float width, String catalogObjectData) {
        createDataCell(row, width, catalogObjectData, MEDIUM_FONT);
    }

    private String getProjectName(CatalogObjectMetadata catalogObject) {
        return Optional.ofNullable(catalogObject.getProjectName()).orElse("");
    }

    private String getDescription(CatalogObjectMetadata catalogObject) {
        return catalogObject.getMetadataList()
                            .stream()
                            .filter(metadata -> metadata.getLabel().equals("General") &&
                                                metadata.getKey().equals("description"))
                            .map(metadata -> metadata.getValue())
                            .map(description -> description.replace("\n", " ").replace("\r", "").replace("\t", ""))
                            .findAny()
                            .orElse("");

    }

    private String getIcon(CatalogObjectMetadata catalogObject) {
        return catalogObject.getMetadataList()
                            .stream()
                            .filter(metadata -> metadata.getKey().equals("workflow.icon"))
                            .map(metadata -> metadata.getValue())
                            .map(image_url -> {
                                if (image_url.startsWith("/")) {
                                    return schedulerUrl + image_url;
                                } else {
                                    return schedulerUrl;
                                }
                            })
                            .findAny()
                            .orElse("");

    }

    private void createEmptyTableRow(BaseTable table) {
        Row<PDPage> dataRow = table.createRow(10f);
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS) * 2, "");
        createDataCell(dataRow, (100 / TOTAL_NUMBER_OF_COLUMNS), "");

    }

    private void createIconCell(Row<PDPage> dataRow, String url_path) {
        try {
            URL url = new URL(url_path);
            BufferedImage imageFile = ImageIO.read(url);
            dataRow.createImageCell((100 / TOTAL_NUMBER_OF_COLUMNS), new Image(imageFile));
        } catch (Exception e) {
            createDataCell(dataRow,
                           (100 / TOTAL_NUMBER_OF_COLUMNS),
                           url_path,
                           MEDIUM_FONT,
                           LIGHT_GRAY,
                           HorizontalAlignment.LEFT);
        }
    }

}
